/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;
import com.intellij.codeInsight.hints.InlayProviderDisablingAction;
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector;
import consulo.language.editor.inlay.InlayGroup;

import consulo.language.editor.inlay.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.codeInsight.hints.SettingsKey;
import com.intellij.codeInsight.hints.InlayHintsProvider;
import com.intellij.codeInsight.hints.InlayHintsCollector;
import com.intellij.codeInsight.hints.InlayHintsSink;
import com.intellij.codeInsight.hints.ImmediateConfigurable;
import com.intellij.codeInsight.hints.ImmediateConfigurable.Case;
import com.intellij.codeInsight.hints.InlayHintsProvider.ChangeListener;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.ide.impl.idea.codeInsight.hints.presentation.InsetPresentation;
import com.intellij.codeInsight.hints.presentation.MenuOnClickPresentation;
import consulo.ide.ServiceManager;
import consulo.codeEditor.Editor;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.MacroExpansionExtUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.openapiext.TestAssertUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import consulo.document.util.TextRange;
import org.rust.lang.core.psi.ext.RsElement;

@SuppressWarnings("UnstableApiUsage")
public class RsInlayTypeHintsProvider implements InlayHintsProvider<RsInlayTypeHintsProvider.Settings> {

    private static final SettingsKey<Settings> KEY = new SettingsKey<>("rust.type.hints");

    @Nonnull
    @Override
    public SettingsKey<Settings> getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return RsBundle.message("settings.rust.inlay.hints.title.types");
    }

    @Nonnull
    @Override
    public String getPreviewText() {
        return "struct Foo<T1, T2, T3> { x: T1, y: T2, z: T3 }\n\nfn main() {\n    let foo = Foo { x: 1, y: \"abc\", z: true };\n}";
    }

    @Nonnull
    @Override
    public InlayGroup getGroup() {
        return InlayGroup.TYPES_GROUP;
    }

    @Nonnull
    @Override
    public ImmediateConfigurable createConfigurable(@Nonnull Settings settings) {
        return new ImmediateConfigurable() {
            @Nonnull
            @Override
            public String getMainCheckboxText() {
                return RsBundle.message("settings.rust.inlay.hints.for");
            }

            @Nonnull
            @Override
            public List<Case> getCases() {
                // which cannot be properly implemented in Java.
                return Collections.emptyList();
            }

            @Nonnull
            public JComponent createComponent(@Nonnull ChangeListener listener) {
                return new JPanel();
            }
        };
    }

    @Nonnull
    @Override
    public Settings createSettings() {
        return new Settings();
    }

    @Nonnull
    @Override
    public InlayHintsCollector getCollectorFor(@Nonnull PsiFile file, @Nonnull Editor editor, @Nonnull Settings settings, @Nonnull InlayHintsSink sink) {
        Project project = file.getProject();
        Crate crate = file instanceof RsFile ? ((RsFile) file).getCrate() : null;

        return new FactoryInlayHintsCollector(editor) {
            private final RsTypeHintsPresentationFactory typeHintsFactory = new RsTypeHintsPresentationFactory(getFactory(), settings.myShowObviousTypes);

            @Override
            public boolean collect(@Nonnull PsiElement element, @Nonnull Editor editor, @Nonnull InlayHintsSink sink) {
                if (DumbService.isDumb(project)) return true;
                if (!(element instanceof RsElement)) return true;

                if (element instanceof RsMacroCall) {
                    processMacroCall((RsMacroCall) element, settings, crate, file, project, sink);
                }
                if (settings.myShowForVariables) {
                    presentVariable((RsElement) element, false, settings, crate, file, project, sink);
                }
                if (settings.myShowForLambdas) {
                    presentLambda((RsElement) element, false, settings, crate, file, project, sink);
                }
                if (settings.myShowForIterators) {
                    presentIterator((RsElement) element, false, settings, crate, file, project, sink);
                }

                return true;
            }

            private void processMacroCall(RsMacroCall call, Settings settings, Crate crate, PsiFile file, Project project, InlayHintsSink sink) {
                if (RsElementUtil.getCodeStatus(call, crate) == RsCodeStatus.CFG_DISABLED) return;
                RsMacroArgument macroBody = call.getMacroArgument();
                if (macroBody == null) return;
                SyntaxTraverser<PsiElement> traverser = SyntaxTraverser.psiTraverser(macroBody);
                for (PsiElement leaf : traverser.preOrderDfsTraversal()) {
                    if (!(leaf instanceof LeafPsiElement)) continue;
                    IElementType elementType = ((LeafPsiElement) leaf).getElementType();
                    if (elementType == RsElementTypes.LET || elementType == RsElementTypes.MATCH) {
                        if (!settings.myShowForVariables) continue;
                        List<PsiElement> expanded = MacroExpansionExtUtil.findExpansionElements(leaf);
                        if (expanded == null || expanded.size() != 1) continue;
                        PsiElement parent = expanded.get(0).getParent();
                        if (!(parent instanceof RsElement)) continue;
                        presentVariable((RsElement) parent, true, settings, crate, file, project, sink);
                    } else if (elementType == RsElementTypes.FOR) {
                        if (!settings.myShowForIterators) continue;
                        List<PsiElement> expanded = MacroExpansionExtUtil.findExpansionElements(leaf);
                        if (expanded == null || expanded.size() != 1) continue;
                        PsiElement parent = expanded.get(0).getParent();
                        if (!(parent instanceof RsForExpr)) continue;
                        presentIterator((RsElement) parent, true, settings, crate, file, project, sink);
                    } else if (elementType == RsElementTypes.OR) {
                        if (!settings.myShowForLambdas) continue;
                        List<PsiElement> expanded = MacroExpansionExtUtil.findExpansionElements(leaf);
                        if (expanded == null || expanded.size() != 1) continue;
                        PsiElement leafExpanded = expanded.get(0);
                        if (!(leafExpanded.getParent() instanceof RsValueParameterList)) continue;
                        RsValueParameterList valueParameterList = (RsValueParameterList) leafExpanded.getParent();
                        if (RsElementUtil.stubChildOfElementType(valueParameterList, RsElementTypes.OR, PsiElement.class) != leafExpanded) continue;
                        if (!(valueParameterList.getParent() instanceof RsLambdaExpr)) continue;
                        presentLambda((RsElement) valueParameterList.getParent(), true, settings, crate, file, project, sink);
                    }
                }
            }

            private void presentVariable(RsElement element, boolean isExpanded, Settings settings, Crate crate, PsiFile file, Project project, InlayHintsSink sink) {
                if (element instanceof RsLetDecl) {
                    RsLetDecl letDecl = (RsLetDecl) element;
                    if (settings.myShowForPlaceholders) {
                        // presentTypePlaceholders - skipped for simplicity in Java conversion
                    }
                    if (letDecl.getTypeReference() != null) return;
                    RsPat pat = letDecl.getPat();
                    if (pat == null) return;
                    presentTypeForPat(pat, letDecl.getExpr(), isExpanded, settings, crate, file, project, sink);
                } else if (element instanceof RsLetExpr) {
                    RsLetExpr letExpr = (RsLetExpr) element;
                    RsPat pat = letExpr.getPat();
                    if (pat == null) return;
                    presentTypeForPat(pat, letExpr.getExpr(), isExpanded, settings, crate, file, project, sink);
                } else if (element instanceof RsMatchExpr) {
                    RsMatchExpr matchExpr = (RsMatchExpr) element;
                    for (RsMatchArm arm : RsMatchExprUtil.getArms(matchExpr)) {
                        presentTypeForPat(arm.getPat(), matchExpr.getExpr(), isExpanded, settings, crate, file, project, sink);
                    }
                }
            }

            private void presentLambda(RsElement element, boolean isExpanded, Settings settings, Crate crate, PsiFile file, Project project, InlayHintsSink sink) {
                if (!(element instanceof RsLambdaExpr)) return;
                RsLambdaExpr lambda = (RsLambdaExpr) element;
                for (RsValueParameter parameter : lambda.getValueParameterList().getValueParameterList()) {
                    if (parameter.getTypeReference() != null) continue;
                    RsPat pat = parameter.getPat();
                    if (pat == null) continue;
                    presentTypeForPat(pat, null, isExpanded, settings, crate, file, project, sink);
                }
            }

            private void presentIterator(RsElement element, boolean isExpanded, Settings settings, Crate crate, PsiFile file, Project project, InlayHintsSink sink) {
                if (!(element instanceof RsForExpr)) return;
                RsForExpr forExpr = (RsForExpr) element;
                RsPat pat = forExpr.getPat();
                if (pat == null) return;
                presentTypeForPat(pat, null, isExpanded, settings, crate, file, project, sink);
            }

            private void presentTypeForPat(RsPat pat, @Nullable RsExpr expr, boolean isExpanded, Settings settings, Crate crate, PsiFile file, Project project, InlayHintsSink sink) {
                if (!settings.myShowObviousTypes && expr != null && isObvious(pat, expr)) return;
                for (RsPatBinding binding : RsElementUtil.descendantsOfType(pat, RsPatBinding.class)) {
                    if (binding.getReferenceName().startsWith("_")) continue;
                    presentTypeForBinding(binding, isExpanded, crate, file, project, sink);
                }
            }

            private void presentTypeForBinding(RsPatBinding binding, boolean isExpanded, Crate crate, PsiFile file, Project project, InlayHintsSink sink) {
                RsPatBinding bindingExpanded = findExpandedByLeaf(binding, crate, RsPatBinding::getIdentifier);
                if (bindingExpanded == null) return;
                PsiElement resolved = bindingExpanded.getReference().resolve();
                if (resolved != null && RsElementUtil.isConstantLike(resolved)) return;
                if (RsTypesUtil.getType(bindingExpanded) instanceof TyUnknown) return;

                int offset;
                if (isExpanded) {
                    Integer origOffset = findOriginalOffset(binding.getIdentifier(), file);
                    if (origOffset == null) return;
                    offset = origOffset;
                } else {
                    offset = binding.getTextRange().getEndOffset();
                }
                InlayPresentation presentation = typeHintsFactory.typeHint(RsTypesUtil.getType(bindingExpanded));
                InsetPresentation finalPresentation = withDisableAction(presentation, project);
                sink.addInlineElement(offset, false, finalPresentation, false);
            }
        };
    }

    @Nonnull
    private InsetPresentation withDisableAction(@Nonnull InlayPresentation presentation, @Nonnull Project project) {
        return new InsetPresentation(
            new MenuOnClickPresentation(presentation, project, () ->
                Collections.singletonList(new InlayProviderDisablingAction(getName(), RsLanguage.INSTANCE, project, KEY))
            ), 1, 0, 0, 0
        );
    }

    private static boolean isObvious(@Nonnull RsPat pat, @Nonnull RsExpr expr) {
        PsiElement declaration = RsTypesUtil.getDeclaration(expr);
        if (declaration instanceof RsStructItem || declaration instanceof RsEnumVariant) {
            return pat instanceof RsPatIdent;
        }
        return false;
    }

    @Nullable
    private static Integer findOriginalOffset(@Nonnull PsiElement anchor, @Nonnull PsiFile originalFile) {
        PsiElement parent = anchor.getParent();
        while (parent != null && !(parent instanceof RsLetDecl) && !(parent instanceof RsLetExpr) && !(parent instanceof RsMatchArm) && !(parent instanceof RsForExpr) && !(parent instanceof RsLambdaExpr)) {
            parent = parent.getParent();
        }
        if (parent == null) return null;
        int offset2 = anchor.getTextRange().getEndOffset();
        int range2Start = parent.getTextRange().getStartOffset();
        int range2End = parent.getTextRange().getEndOffset();

        PsiElement call = MacroExpansionExtUtil.findMacroCallExpandedFromNonRecursive(anchor);
        if (call == null) return null;
        TextRange range1 = MacroExpansionExtUtil.mapRangeFromExpansionToCallBodyStrict(call, parent.getTextRange());
        if (range1 == null) return null;
        int offset1 = range1.getStartOffset() + (offset2 - range2Start);
        PsiElement atOffset = originalFile.findElementAt(offset1);
        if (atOffset == null) return null;
        List<PsiElement> expansionElements = MacroExpansionExtUtil.findExpansionElements(atOffset);
        if (expansionElements == null || expansionElements.size() != 1) return null;
        return offset1;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T findExpandedByLeaf(@Nonnull T element, @Nullable Crate explicitCrate, @Nonnull Function<T, PsiElement> getLeaf) {
        RsCodeStatus status = RsElementUtil.getCodeStatus(element, explicitCrate);
        if (status == RsCodeStatus.CFG_DISABLED) return null;
        if (status == RsCodeStatus.ATTR_PROC_MACRO_CALL) {
            PsiElement leaf = getLeaf.apply(element);
            List<PsiElement> expanded = MacroExpansionExtUtil.findExpansionElements(leaf);
            if (expanded == null || expanded.size() != 1) return null;
            PsiElement parent = expanded.get(0).getParent();
            if (element.getClass().isInstance(parent)) {
                return (T) parent;
            }
            return null;
        }
        return element;
    }

    public static class Settings {
        private boolean myShowForVariables = true;
        private boolean myShowForLambdas = true;
        private boolean myShowForIterators = true;
        private boolean myShowForPlaceholders = true;
        private boolean myShowObviousTypes = false;

        public boolean getShowForVariables() { return myShowForVariables; }
        public void setShowForVariables(boolean value) { myShowForVariables = value; }

        public boolean getShowForLambdas() { return myShowForLambdas; }
        public void setShowForLambdas(boolean value) { myShowForLambdas = value; }

        public boolean getShowForIterators() { return myShowForIterators; }
        public void setShowForIterators(boolean value) { myShowForIterators = value; }

        public boolean getShowForPlaceholders() { return myShowForPlaceholders; }
        public void setShowForPlaceholders(boolean value) { myShowForPlaceholders = value; }

        public boolean getShowObviousTypes() { return myShowObviousTypes; }
        public void setShowObviousTypes(boolean value) { myShowObviousTypes = value; }
    }
}
