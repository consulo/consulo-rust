/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;
import consulo.language.editor.rawHighlight.HighlightInfo;

import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.highlight.UpdateHighlightersUtil;
import consulo.language.editor.annotation.Annotation;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.annotation.AnnotationSession;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.document.Document;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.annotator.format.RsFormatMacroAnnotator;
import org.rust.ide.colors.RsColor;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.lang.core.crate.Crate;
import org.rust.toml.CrateExt;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.psi.AttrCache;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.ext.RsAttrProcMacroOwner;
import org.rust.lang.core.psi.ext.RsPsiElementExt;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;

public class RsMacroExpansionHighlightingPass extends TextEditorHighlightingPass {
    @Nonnull
    private final PsiFile myFile;
    @Nonnull
    private final TextRange myRestrictedRange;
    @Nonnull
    private final List<HighlightInfo> myResults = new ArrayList<>();

    public RsMacroExpansionHighlightingPass(@Nonnull PsiFile file, @Nonnull TextRange restrictedRange, @Nonnull Document document) {
        super(file.getProject(), document);
        this.myFile = file;
        this.myRestrictedRange = restrictedRange;
    }

    @Nonnull
    private List<Annotator>[] createAnnotators() {
        List<Annotator> annotatorsForDeclMacros = Arrays.asList(
            new RsEdition2018KeywordsAnnotator(),
            new RsAttrHighlightingAnnotator(),
            new RsHighlightingMutableAnnotator(),
            new RsFormatMacroAnnotator()
        );
        List<Annotator> annotatorsForAttrMacros = new ArrayList<>(annotatorsForDeclMacros);
        annotatorsForAttrMacros.add(new RsErrorAnnotator());
        annotatorsForAttrMacros.add(new RsUnsafeExpressionAnnotator());

        @SuppressWarnings("unchecked")
        List<Annotator>[] result = new List[] { annotatorsForDeclMacros, annotatorsForAttrMacros };
        return result;
    }

    @Override
    @SuppressWarnings({"UnstableApiUsage", "deprecation"})
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        List<MacroCallPreparedForHighlighting> macros = new ArrayList<>();

        PsiTreeUtil.processElements(myFile, element -> {
            if (element instanceof RsMacroCall) {
                RsMacroCall macroCall = (RsMacroCall) element;
                if (macroCall.getMacroArgument() != null
                    && macroCall.getMacroArgument().getTextRange() != null
                    && macroCall.getMacroArgument().getTextRange().intersects(myRestrictedRange)) {
                    MacroCallPreparedForHighlighting prepared = MacrosUtil.prepareForExpansionHighlighting(macroCall);
                    if (prepared != null) macros.add(prepared);
                }
            } else if (element instanceof RsAttrProcMacroOwner) {
                RsAttrProcMacroOwner owner = (RsAttrProcMacroOwner) element;
                if (owner.getTextRange() != null && owner.getTextRange().intersects(myRestrictedRange)) {
                    // Simplified: skip attr proc macros for now
                }
            }
            return true;
        });

        if (macros.isEmpty()) return;

        Crate crate = myFile instanceof RsFile ? Crate.asNotFake(((RsFile) myFile).getCrate()) : null;
        List<Annotator>[] annotatorArrays = createAnnotators();
        List<Annotator> annotatorsForDeclMacros = annotatorArrays[0];
        List<Annotator> annotatorsForAttrMacros = annotatorArrays[1];

        while (!macros.isEmpty()) {
            MacroCallPreparedForHighlighting macro = macros.remove(macros.size() - 1);
            AnnotationSession annotationSession = new AnnotationSession(macro.getExpansion().getFile());
            AnnotationSessionEx.setCurrentCrate(annotationSession, crate);

            // AnnotationHolderImpl is platform-internal. The holder is only needed so the
            // cfg-disabled check can read the crate off the session; annotators themselves are run
            // through the public LanguageEditorInternalHelperImpl, which builds its own holder.
            RsAnnotationSessionHolder holder = new RsAnnotationSessionHolder(annotationSession);
            List<Annotation> collectedAnnotations = new ArrayList<>();
            var annotatorRunner = new consulo.ide.impl.language.editor.LanguageEditorInternalHelperImpl();
            List<Annotator> annotators = macro.isDeeplyAttrMacro() ? annotatorsForAttrMacros : annotatorsForDeclMacros;
            List<PsiElement> cfgDisabledElements = new ArrayList<>();

            for (PsiElement element : macro.getElementsForHighlighting()) {
                if (RsCfgDisabledCodeAnnotator.shouldHighlightAsCfsDisabled(element, holder)) {
                    cfgDisabledElements.add(element);
                }
                for (Annotator ann : annotators) {
                    ProgressManager.checkCanceled();
                    collectedAnnotations.addAll(annotatorRunner.runAnnotator(
                        org.rust.lang.RsLanguage.INSTANCE, ann, macro.getExpansion().getFile(), element, false));
                }

                if (element instanceof RsMacroCall) {
                    MacroCallPreparedForHighlighting prepared = MacrosUtil.prepareForExpansionHighlighting((RsMacroCall) element, macro);
                    if (prepared != null) macros.add(prepared);
                }
            }

            for (Annotation ann : collectedAnnotations) {
                mapAndCollectAnnotation(macro, ann);
            }

            if (crate != null && AnnotatorBase.isEnabled(RsCfgDisabledCodeAnnotator.class)) {
                highlightCfgDisabledRanges(crate, macro, cfgDisabledElements);
            }
        }
    }

    private void mapAndCollectAnnotation(@Nonnull MacroCallPreparedForHighlighting macro, @Nonnull Annotation ann) {
        // HighlightInfo.fromAnnotation / findRegisteredQuickFix / internal fields are not available in Consulo API.
        // This mapping is currently a no-op. TODO: reimplement using Consulo's HighlightInfo API.
    }

    private void highlightCfgDisabledRanges(
        @Nonnull Crate crate,
        @Nonnull MacroCallPreparedForHighlighting macro,
        @Nonnull List<PsiElement> cfgDisabledElements
    ) {
        Set<TextRange> cfgDisabledMappedRanges = new HashSet<>();
        for (PsiElement element : cfgDisabledElements) {
            List<TextRange> ranges = MacrosUtil.mapRangeFromExpansionToCallBody(macro.getExpansion(), macro.getMacroCall(), element.getTextRange());
            cfgDisabledMappedRanges.addAll(ranges);
        }

        for (TextRange mappedRange : cfgDisabledMappedRanges) {
            PsiElement element = myFile.findElementAt(mappedRange.getStartOffset());
            if (element == null) continue;
            AttrCache cache = new AttrCache.HashMapCache(crate);
            List<PsiElement> expansionElements = MacrosUtil.findExpansionElements(element, cache);
            if (expansionElements == null) continue;
            boolean anyEnabled = false;
            for (PsiElement exp : expansionElements) {
                if (RsPsiElementExt.isEnabledByCfg(exp, crate)) {
                    anyEnabled = true;
                    break;
                }
            }
            if (anyEnabled) continue;

            RsColor color = RsColor.CFG_DISABLED_CODE;
            HighlightSeverity severity = OpenApiUtil.isUnitTestMode() ? color.getTestSeverity() : RsCfgDisabledCodeAnnotator.CONDITIONALLY_DISABLED_CODE_SEVERITY;
            myResults.add(
                HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                    .severity(severity)
                    .textAttributes(color.getTextAttributesKey())
                    .range(mappedRange)
                    .descriptionAndTooltip(RsBundle.message("text.conditionally.disabled.code"))
                    .createUnconditionally()
            );
        }
    }

    @Override
    public void doApplyInformationToEditor() {
        UpdateHighlightersUtil.setHighlightersToEditor(
            myProject,
            myDocument,
            myRestrictedRange.getStartOffset(),
            myRestrictedRange.getEndOffset(),
            myResults,
            getColorsScheme(),
            getId()
        );
    }

    @Nonnull
    private static HighlightInfo.Builder copyWithRange(@Nonnull HighlightInfo info, @Nonnull TextRange newRange) {
        HighlightInfo.Builder b = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
            .range(newRange)
            .severity(info.getSeverity());

        if (info.getDescription() != null) {
            b.description(info.getDescription());
        }
        if (info.getToolTip() != null) {
            b.escapedToolTip(info.getToolTip());
        }
        // isAfterEndOfLine isn't exposed on Consulo's HighlightInfo

        return b;
    }
}
