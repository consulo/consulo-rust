/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.editor.inlay.DeclarativeInlayHintsCollector;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.editor.inlay.DeclarativeInlayOptionInfo;
import consulo.language.editor.inlay.DeclarativeInlayPosition;
import consulo.language.editor.inlay.DeclarativeInlayTreeSink;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SyntaxTraverser;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.MacroExpansionExtUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.impl.*;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@ExtensionImpl
public class RsInlayTypeHintsProvider implements DeclarativeInlayHintsProvider {

    public static final String PROVIDER_ID = "rust.types";

    private static final String OPTION_VARIABLES = "rust.types.variables";
    private static final String OPTION_LAMBDAS = "rust.types.lambdas";
    private static final String OPTION_ITERATORS = "rust.types.iterators";
    private static final String OPTION_OBVIOUS_TYPES = "rust.types.obvious";

    private static final RsTypeHintsFactory FACTORY = new RsTypeHintsFactory(false);

    @Nullable
    @Override
    public DeclarativeInlayHintsCollector createCollector(PsiFile file, Editor editor) {
        if (!(file instanceof RsFile rsFile) || DumbService.isDumb(file.getProject())) {
            return null;
        }
        return new Collector(rsFile);
    }

    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(RsBundle.message("settings.rust.inlay.hints.title.types"));
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.of(RsBundle.message("settings.rust.inlay.hints.title.types"));
    }

    @Override
    public LocalizeValue getPreviewFileText() {
        return LocalizeValue.of("struct Foo<T1, T2, T3> { x: T1, y: T2, z: T3 }\n\nfn main() {\n    let foo = Foo { x: 1, y: \"abc\", z: true };\n}");
    }

    @Override
    public InlayGroup getGroup() {
        return InlayGroup.TYPES_GROUP;
    }

    @Override
    public Set<DeclarativeInlayOptionInfo> getOptions() {
        return Set.of(
            option(OPTION_VARIABLES, true, "settings.rust.inlay.hints.for.variables"),
            option(OPTION_LAMBDAS, true, "settings.rust.inlay.hints.for.closures"),
            option(OPTION_ITERATORS, true, "settings.rust.inlay.hints.for.loop.variables"),
            option(OPTION_OBVIOUS_TYPES, false, "settings.rust.inlay.hints.for.obvious.types")
        );
    }

    private static DeclarativeInlayOptionInfo option(String id, boolean enabledByDefault, String bundleKey) {
        LocalizeValue name = LocalizeValue.of(RsBundle.message(bundleKey));
        return new DeclarativeInlayOptionInfo(id, enabledByDefault, name, name);
    }

    private static class Collector implements DeclarativeInlayHintsCollector.SharedBypassCollector {

        private final RsFile file;
        private final Crate crate;

        Collector(RsFile file) {
            this.file = file;
            this.crate = file.getCrate();
        }

        @Override
        public void collectFromElement(PsiElement element, DeclarativeInlayTreeSink sink) {
            if (!(element instanceof RsElement rsElement)) return;

            if (element instanceof RsMacroCall macroCall) {
                processMacroCall(macroCall, sink);
            }
            sink.whenOptionEnabled(OPTION_VARIABLES, () -> presentVariable(rsElement, false, sink));
            sink.whenOptionEnabled(OPTION_LAMBDAS, () -> presentLambda(rsElement, false, sink));
            sink.whenOptionEnabled(OPTION_ITERATORS, () -> presentIterator(rsElement, false, sink));
        }

        private void processMacroCall(RsMacroCall call, DeclarativeInlayTreeSink sink) {
            if (RsElementUtil.getCodeStatus(call, crate) == RsCodeStatus.CFG_DISABLED) return;
            RsMacroArgument macroBody = call.getMacroArgument();
            if (macroBody == null) return;
            SyntaxTraverser<PsiElement> traverser = SyntaxTraverser.psiTraverser(macroBody);
            for (PsiElement leaf : traverser.preOrderDfsTraversal()) {
                if (!(leaf instanceof LeafPsiElement)) continue;
                IElementType elementType = ((LeafPsiElement) leaf).getElementType();
                if (elementType == RsElementTypes.LET || elementType == RsElementTypes.MATCH) {
                    
                    List<PsiElement> expanded = MacroExpansionExtUtil.findExpansionElements(leaf);
                    if (expanded == null || expanded.size() != 1) continue;
                    final PsiElement parent = expanded.get(0).getParent();
                    if (!(parent instanceof RsElement)) continue;
                    sink.whenOptionEnabled(OPTION_VARIABLES, () -> presentVariable((RsElement) parent, true, sink));
                } else if (elementType == RsElementTypes.FOR) {
                    
                    List<PsiElement> expanded = MacroExpansionExtUtil.findExpansionElements(leaf);
                    if (expanded == null || expanded.size() != 1) continue;
                    final PsiElement parent = expanded.get(0).getParent();
                    if (!(parent instanceof RsForExpr)) continue;
                    sink.whenOptionEnabled(OPTION_ITERATORS, () -> presentIterator((RsElement) parent, true, sink));
                } else if (elementType == RsElementTypes.OR) {
                    
                    List<PsiElement> expanded = MacroExpansionExtUtil.findExpansionElements(leaf);
                    if (expanded == null || expanded.size() != 1) continue;
                    PsiElement leafExpanded = expanded.get(0);
                    if (!(leafExpanded.getParent() instanceof RsValueParameterList)) continue;
                    final RsValueParameterList valueParameterList = (RsValueParameterList) leafExpanded.getParent();
                    if (RsElementUtil.stubChildOfElementType(valueParameterList, RsElementTypes.OR, PsiElement.class) != leafExpanded) continue;
                    if (!(valueParameterList.getParent() instanceof RsLambdaExpr)) continue;
                    sink.whenOptionEnabled(OPTION_LAMBDAS, () -> presentLambda((RsElement) valueParameterList.getParent(), true, sink));
                }
            }
        }

        private void presentVariable(RsElement element, boolean isExpanded, DeclarativeInlayTreeSink sink) {
            if (element instanceof RsLetDecl) {
                RsLetDecl letDecl = (RsLetDecl) element;
                                    if (letDecl.getTypeReference() != null) return;
                RsPat pat = letDecl.getPat();
                if (pat == null) return;
                presentTypeForPat(pat, letDecl.getExpr(), isExpanded, sink);
            } else if (element instanceof RsLetExpr) {
                RsLetExpr letExpr = (RsLetExpr) element;
                RsPat pat = letExpr.getPat();
                if (pat == null) return;
                presentTypeForPat(pat, letExpr.getExpr(), isExpanded, sink);
            } else if (element instanceof RsMatchExpr) {
                RsMatchExpr matchExpr = (RsMatchExpr) element;
                for (RsMatchArm arm : RsMatchExprUtil.getArms(matchExpr)) {
                    presentTypeForPat(arm.getPat(), matchExpr.getExpr(), isExpanded, sink);
                }
            }
        }

        private void presentLambda(RsElement element, boolean isExpanded, DeclarativeInlayTreeSink sink) {
            if (!(element instanceof RsLambdaExpr)) return;
            RsLambdaExpr lambda = (RsLambdaExpr) element;
            for (RsValueParameter parameter : lambda.getValueParameterList().getValueParameterList()) {
                if (parameter.getTypeReference() != null) continue;
                RsPat pat = parameter.getPat();
                if (pat == null) continue;
                presentTypeForPat(pat, null, isExpanded, sink);
            }
        }

        private void presentIterator(RsElement element, boolean isExpanded, DeclarativeInlayTreeSink sink) {
            if (!(element instanceof RsForExpr)) return;
            RsForExpr forExpr = (RsForExpr) element;
            RsPat pat = forExpr.getPat();
            if (pat == null) return;
            presentTypeForPat(pat, null, isExpanded, sink);
        }

        private void presentTypeForPat(RsPat pat, @Nullable RsExpr expr, boolean isExpanded, DeclarativeInlayTreeSink sink) {
            Runnable present = () -> {
                for (RsPatBinding binding : RsElementUtil.descendantsOfType(pat, RsPatBinding.class)) {
                    if (binding.getReferenceName().startsWith("_")) continue;
                    presentTypeForBinding(binding, isExpanded, sink);
                }
            };
            if (expr != null && isObvious(pat, expr)) {
                sink.whenOptionEnabled(OPTION_OBVIOUS_TYPES, present);
            }
            else {
                present.run();
            }
        }

        private void presentTypeForBinding(RsPatBinding binding, boolean isExpanded, DeclarativeInlayTreeSink sink) {
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
            Ty type = RsTypesUtil.getType(bindingExpanded);
            sink.addPresentation(
                new DeclarativeInlayPosition.InlineInlayPosition(offset, false),
                builder -> FACTORY.typeHint(type, builder));
        }
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
}
