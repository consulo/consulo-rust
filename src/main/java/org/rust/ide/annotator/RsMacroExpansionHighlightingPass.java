/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeInsight.daemon.impl.*;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.Annotator;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
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
    @NotNull
    private final PsiFile myFile;
    @NotNull
    private final TextRange myRestrictedRange;
    @NotNull
    private final List<HighlightInfo> myResults = new ArrayList<>();

    public RsMacroExpansionHighlightingPass(@NotNull PsiFile file, @NotNull TextRange restrictedRange, @NotNull Document document) {
        super(file.getProject(), document);
        this.myFile = file;
        this.myRestrictedRange = restrictedRange;
    }

    @NotNull
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
    public void doCollectInformation(@NotNull ProgressIndicator progress) {
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

            AnnotationHolderImpl holder = new AnnotationHolderImpl(annotationSession, false);
            List<Annotator> annotators = macro.isDeeplyAttrMacro() ? annotatorsForAttrMacros : annotatorsForDeclMacros;
            List<PsiElement> cfgDisabledElements = new ArrayList<>();

            for (PsiElement element : macro.getElementsForHighlighting()) {
                if (RsCfgDisabledCodeAnnotator.shouldHighlightAsCfsDisabled(element, holder)) {
                    cfgDisabledElements.add(element);
                }
                for (Annotator ann : annotators) {
                    ProgressManager.checkCanceled();
                    holder.runAnnotatorWithContext(element, ann);
                }

                if (element instanceof RsMacroCall) {
                    MacroCallPreparedForHighlighting prepared = MacrosUtil.prepareForExpansionHighlighting((RsMacroCall) element, macro);
                    if (prepared != null) macros.add(prepared);
                }
            }

            for (Annotation ann : holder) {
                mapAndCollectAnnotation(macro, ann);
            }

            if (crate != null && AnnotatorBase.isEnabled(RsCfgDisabledCodeAnnotator.class)) {
                highlightCfgDisabledRanges(crate, macro, cfgDisabledElements);
            }
        }
    }

    private void mapAndCollectAnnotation(@NotNull MacroCallPreparedForHighlighting macro, @NotNull Annotation ann) {
        TextRange originRange = new TextRange(ann.getStartOffset(), ann.getEndOffset());
        HighlightInfo originInfo = HighlightInfo.fromAnnotation(ann);
        List<TextRange> mappedRanges = MacrosUtil.mapRangeFromExpansionToCallBody(macro.getExpansion(), macro.getMacroCall(), originRange);
        for (TextRange mappedRange : mappedRanges) {
            HighlightInfo.Builder newInfo = copyWithRange(originInfo, mappedRange);
            originInfo.findRegisteredQuickFix((descriptor, quickfixTextRange) -> {
                List<TextRange> mappedQfRanges = MacrosUtil.mapRangeFromExpansionToCallBody(macro.getExpansion(), macro.getMacroCall(), quickfixTextRange);
                for (TextRange mappedQfRange : mappedQfRanges) {
                    if (!(descriptor.getAction() instanceof RsQuickFixBase)) continue;
                    newInfo.registerFix(descriptor.getAction(), Collections.emptyList(), descriptor.getDisplayName(), mappedQfRange, null);
                }
                return null;
            });
            myResults.add(newInfo.createUnconditionally());
        }
    }

    private void highlightCfgDisabledRanges(
        @NotNull Crate crate,
        @NotNull MacroCallPreparedForHighlighting macro,
        @NotNull List<PsiElement> cfgDisabledElements
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

    @NotNull
    private static HighlightInfo.Builder copyWithRange(@NotNull HighlightInfo info, @NotNull TextRange newRange) {
        HighlightInfo.Builder b = HighlightInfo.newHighlightInfo(info.type)
            .range(newRange)
            .severity(info.getSeverity());

        if (info.forcedTextAttributesKey != null) {
            b.textAttributes(info.forcedTextAttributesKey);
        } else if (info.forcedTextAttributes != null) {
            b.textAttributes(info.forcedTextAttributes);
        }

        if (info.getDescription() != null) {
            b.description(info.getDescription());
        }
        if (info.getToolTip() != null) {
            b.escapedToolTip(info.getToolTip());
        }
        if (info.isAfterEndOfLine()) {
            b.endOfLine();
        }

        return b;
    }
}
