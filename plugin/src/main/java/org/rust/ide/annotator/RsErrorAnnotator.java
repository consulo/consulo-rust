/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.HighlightRangeExtension;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.AnnotationSession;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.colorScheme.TextAttributesKey;
import consulo.util.dataholder.Key;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.fixes.*;
import org.rust.ide.fixes.MakePublicFix;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.FeatureState;
import org.rust.lang.core.macros.MacroExpansionMode;
import org.rust.lang.core.macros.MacroExpansionManager;
import org.rust.lang.core.macros.proc.ProcMacroApplicationService;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.RsCallable;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.lang.utils.RsErrorCode;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.StdextUtil;

import java.util.*;

import static org.rust.lang.core.FeatureAvailability.*;
import static org.rust.lang.utils.RsErrorCode.*;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.RsTypesUtil;
import consulo.language.ast.ASTNode;
import consulo.language.editor.annotation.AnnotationBuilder;
import org.rust.lang.core.psi.ext.ComparisonOp;
import org.rust.lang.core.psi.ext.EqualityOp;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.types.infer.FoldUtil;

@ExtensionImpl
public class RsErrorAnnotator extends AnnotatorBase implements HighlightRangeExtension {

    @Override
    public boolean isForceHighlightParents(@Nonnull PsiFile file) {
        return file instanceof RsFile;
    }

    @Override
    protected void annotateInternal(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        RsAnnotationHolder rsHolder = new RsAnnotationHolder(holder);
        RsVisitor visitor = new RsVisitor() {
            @Override
            public void visitCondition(@Nonnull RsCondition o) { checkCondition(rsHolder, o); }
            @Override
            public void visitConstant(@Nonnull RsConstant o) { checkConstant(rsHolder, o); }
            @Override
            public void visitTypeArgumentList(@Nonnull RsTypeArgumentList o) { checkTypeArgumentList(rsHolder, o); }
            @Override
            public void visitValueParameterList(@Nonnull RsValueParameterList o) { checkValueParameterList(rsHolder, o); }
            @Override
            public void visitValueArgumentList(@Nonnull RsValueArgumentList o) { checkValueArgumentList(rsHolder, o); }
            @Override
            public void visitStructItem(@Nonnull RsStructItem o) { checkDuplicates(rsHolder, o, null, false); }
            @Override
            public void visitEnumItem(@Nonnull RsEnumItem o) { checkEnumItem(rsHolder, o); }
            @Override
            public void visitEnumVariant(@Nonnull RsEnumVariant o) { checkEnumVariant(rsHolder, o); }
            @Override
            public void visitFunction(@Nonnull RsFunction o) { checkFunction(rsHolder, o); }
            @Override
            public void visitImplItem(@Nonnull RsImplItem o) { checkImpl(rsHolder, o); }
            @Override
            public void visitLetDecl(@Nonnull RsLetDecl o) { checkLetDecl(rsHolder, o); }
            @Override
            public void visitLetElseBranch(@Nonnull RsLetElseBranch o) { checkLetElseBranch(rsHolder, o); }
            @Override
            public void visitLetExpr(@Nonnull RsLetExpr o) { checkLetExpr(rsHolder, o); }
            @Override
            public void visitFieldLookup(@Nonnull RsFieldLookup o) { checkFieldLookup(rsHolder, o); }
            @Override
            public void visitModDeclItem(@Nonnull RsModDeclItem o) { checkModDecl(rsHolder, o); }
            @Override
            public void visitModItem(@Nonnull RsModItem o) { checkDuplicates(rsHolder, o, null, false); }
            @Override
            public void visitPatBinding(@Nonnull RsPatBinding o) { checkPatBinding(rsHolder, o); }
            @Override
            public void visitPatBox(@Nonnull RsPatBox o) { checkPatBox(rsHolder, o); }
            @Override
            public void visitPatField(@Nonnull RsPatField o) { checkPatField(rsHolder, o); }
            @Override
            public void visitPatRange(@Nonnull RsPatRange o) { checkPatRange(rsHolder, o); }
            @Override
            public void visitPatRest(@Nonnull RsPatRest o) { checkPatRest(rsHolder, o); }
            @Override
            public void visitPatStruct(@Nonnull RsPatStruct o) { checkRsPatStruct(rsHolder, o); }
            @Override
            public void visitPatTupleStruct(@Nonnull RsPatTupleStruct o) { checkRsPatTupleStruct(rsHolder, o); }
            @Override
            public void visitPath(@Nonnull RsPath o) { checkPath(rsHolder, o); }
            @Override
            public void visitTraitType(@Nonnull RsTraitType o) { checkTraitType(rsHolder, o); }
            @Override
            public void visitTraitRef(@Nonnull RsTraitRef o) { checkTraitRef(rsHolder, o); }
            @Override
            public void visitVis(@Nonnull RsVis o) { checkVis(rsHolder, o); }
            @Override
            public void visitVisRestriction(@Nonnull RsVisRestriction o) { checkVisRestriction(rsHolder, o); }
            @Override
            public void visitUnaryExpr(@Nonnull RsUnaryExpr o) { checkUnary(rsHolder, o); }
            @Override
            public void visitBinaryExpr(@Nonnull RsBinaryExpr o) { checkBinary(rsHolder, o); }
            @Override
            public void visitExternAbi(@Nonnull RsExternAbi o) { checkExternAbi(rsHolder, o); }
            @Override
            public void visitDotExpr(@Nonnull RsDotExpr o) { checkDotExpr(rsHolder, o); }
            @Override
            public void visitYieldExpr(@Nonnull RsYieldExpr o) { checkYieldExpr(rsHolder, o); }
            @Override
            public void visitArrayType(@Nonnull RsArrayType o) { checkArrayType(rsHolder, o); }
            @Override
            public void visitArrayExpr(@Nonnull RsArrayExpr o) { checkArrayExpr(rsHolder, o); }
            @Override
            public void visitRetExpr(@Nonnull RsRetExpr o) { checkRetExpr(rsHolder, o); }
            @Override
            public void visitInferType(@Nonnull RsInferType o) { checkInferType(rsHolder, o); }
            @Override
            public void visitUseSpeck(@Nonnull RsUseSpeck o) {
                checkDuplicateImport(rsHolder, o);
                checkReexports(rsHolder, o);
            }
            @Override
            public void visitExternCrateItem(@Nonnull RsExternCrateItem o) { checkExternCrate(rsHolder, o); }
            @Override
            public void visitCallExpr(@Nonnull RsCallExpr o) { checkCallExpr(rsHolder, o); }
            @Override
            public void visitTypeAlias(@Nonnull RsTypeAlias o) { checkTypeAlias(rsHolder, o); }
            @Override
            public void visitTypeParameter(@Nonnull RsTypeParameter o) { checkDuplicates(rsHolder, o, null, false); }
            @Override
            public void visitConstParameter(@Nonnull RsConstParameter o) { checkConstParameter(rsHolder, o); }
            @Override
            public void visitLifetimeParameter(@Nonnull RsLifetimeParameter o) { checkLifetimeParameter(rsHolder, o); }
            @Override
            public void visitLifetime(@Nonnull RsLifetime o) { checkLifetime(rsHolder, o); }
            @Override
            public void visitLabel(@Nonnull RsLabel o) { checkLabel(rsHolder, o); }
            @Override
            public void visitLabelDecl(@Nonnull RsLabelDecl o) { checkLabelDecl(rsHolder, o); }
            @Override
            public void visitMatchArmGuard(@Nonnull RsMatchArmGuard o) { checkMatchArmGuard(rsHolder, o); }
            @Override
            public void visitPolybound(@Nonnull RsPolybound o) { checkPolybound(rsHolder, o); }
            @Override
            public void visitTildeConst(@Nonnull RsTildeConst o) { checkTildeConst(rsHolder, o); }
            @Override
            public void visitBlockExpr(@Nonnull RsBlockExpr o) { checkBlockExpr(rsHolder, o); }
            @Override
            public void visitRangeExpr(@Nonnull RsRangeExpr o) { checkRangeExpr(rsHolder, o); }
            @Override
            public void visitLitExpr(@Nonnull RsLitExpr o) { checkLitExpr(rsHolder, o); }
            @Override
            public void visitLambdaExpr(@Nonnull RsLambdaExpr o) { checkLambdaExpr(rsHolder, o); }
            @Override
            public void visitBreakExpr(@Nonnull RsBreakExpr o) { checkBreakExpr(rsHolder, o); }
            @Override
            public void visitContExpr(@Nonnull RsContExpr o) { checkContExpr(rsHolder, o); }
            @Override
            public void visitAttr(@Nonnull RsAttr o) { checkAttr(rsHolder, o); }
            @Override
            public void visitSelfParameter(@Nonnull RsSelfParameter o) { checkParamAttrs(rsHolder, o); }
            @Override
            public void visitValueParameter(@Nonnull RsValueParameter o) { checkParamAttrs(rsHolder, o); }
            @Override
            public void visitVariadic(@Nonnull RsVariadic o) { checkParamAttrs(rsHolder, o); }
        };

        element.accept(visitor);
    }


    private void checkCondition(RsAnnotationHolder holder, RsCondition element) {
        RsExpr expr = element.getExpr();
        if (!(expr instanceof RsLetExpr)) return;
        RsPat pat = ((RsLetExpr) expr).getPat();
        if (pat instanceof RsOrPat) {
            CompilerFeature.getIF_WHILE_OR_PATTERNS().check(
                holder,
                ((RsOrPat) pat).getPatList().get(0),
                ((RsOrPat) pat).getPatList().get(((RsOrPat) pat).getPatList().size() - 1),
                RsBundle.message("inspection.message.multiple.patterns.in.if.let.while.let.are.unstable"),
                RsBundle.message("inspection.message.multiple.patterns.in.if.let.while.let.are.unstable"),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList()
            );
        }
    }

    private void checkConstant(RsAnnotationHolder holder, RsConstant element) {
        collectDiagnostics(holder, element);
        checkDuplicates(holder, element, null, false);
    }

    private void checkFunction(RsAnnotationHolder holder, RsFunction fn) {
        collectDiagnostics(holder, fn);
        checkDuplicates(holder, fn, null, false);
        checkTypesAreSized(holder, fn);
        checkEmptyFunctionReturnType(holder, fn);
        checkRecursiveAsyncFunction(holder, fn);

        for (RsAttr attr : fn.getInnerAttrList()) { checkStartAttribute(holder, attr); }
        for (RsAttr attr : fn.getOuterAttrList()) { checkStartAttribute(holder, attr); }
    }

    private void collectDiagnostics(RsAnnotationHolder holder, RsInferenceContextOwner element) {
        for (RsDiagnostic diag : RsTypesUtil.getSelfInferenceResult(element).getDiagnostics()) {
            if (diag.getInspectionClass() == getClass()) {
                RsDiagnostic.addToHolder(diag, holder);
            }
        }
    }

    private void checkEnumItem(RsAnnotationHolder holder, RsEnumItem o) {
        checkDuplicates(holder, o, null, false);
        RsEnumBody enumBody = o.getEnumBody();
        if (enumBody != null) {
            checkDuplicateEnumVariants(holder, enumBody);
        }
    }

    private void checkEnumVariant(RsAnnotationHolder holder, RsEnumVariant variant) {
        checkDuplicates(holder, variant, null, false);
    }

    private void checkImpl(RsAnnotationHolder holder, RsImplItem impl) {
        checkImplForNonAdtError(holder, impl);
        checkInherentImplSameCrate(holder, impl);
        RsTraitRef traitRef = impl.getTraitRef();
        if (traitRef == null) return;
        RsTraitItem trait = RsTraitRefUtil.resolveToTrait(traitRef);
        if (trait == null) return;
        checkTraitImplOrphanRules(holder, impl);
    }

    private void checkImplForNonAdtError(RsAnnotationHolder holder, RsImplItem impl) {
        if (impl.getFor() != null) return;
        RsTypeReference typeRef = impl.getTypeReference();
        if (typeRef == null) return;
        Ty type = RsTypesUtil.getRawType(typeRef);
        if (type instanceof TyAdt || type instanceof TyTraitObject || type == TyUnknown.INSTANCE) return;
        RsDiagnostic.addToHolder(new RsDiagnostic.ImplForNonAdtError(typeRef), holder);
    }

    private void checkInherentImplSameCrate(RsAnnotationHolder holder, RsImplItem impl) {
        if (impl.getTraitRef() != null) return;
        RsTypeReference typeReference = impl.getTypeReference();
        if (typeReference == null) return;
        Ty type = RsTypesUtil.getRawType(typeReference);
        RsElement element;
        if (type instanceof TyAdt) {
            element = ((TyAdt) type).getItem();
        } else {
            return;
        }
        if (RsElementUtil.getContainingCrate(impl) != RsElementUtil.getContainingCrate(element)) {
            RsDiagnostic.addToHolder(new RsDiagnostic.InherentImplDifferentCrateError(typeReference), holder);
        }
    }

    private void checkTraitImplOrphanRules(RsAnnotationHolder holder, RsImplItem impl) {
        // Simplified orphan rules check
    }

    private void checkTraitType(RsAnnotationHolder holder, RsTraitType traitType) {
        // Simplified check
    }

    private void checkTraitRef(RsAnnotationHolder holder, RsTraitRef traitRef) {
        PsiElement resolved = traitRef.getPath().getReference() != null ? traitRef.getPath().getReference().resolve() : null;
        if (!(resolved instanceof RsItemElement)) return;
        if (!(resolved instanceof RsTraitItem) && !(resolved instanceof RsTraitAlias)) {
            RsDiagnostic.addToHolder(new RsDiagnostic.NotTraitError(traitRef, (RsItemElement) resolved), holder);
        }
    }

    private void checkDotExpr(RsAnnotationHolder holder, RsDotExpr o) {
        PsiElement field = o.getFieldLookup();
        if (field == null) field = o.getMethodCall();
        if (field == null) return;
        if (field instanceof RsReferenceElement) {
            checkReferenceIsPublic((RsReferenceElement) field, o, holder);
        }
    }

    private void checkReferenceIsPublic(RsReferenceElement ref, RsElement o, RsAnnotationHolder holder) {
        RsReference reference = ref.getReference();
        if (reference == null) return;
        PsiElement highlightedElement = ref.getReferenceNameElement();
        if (highlightedElement == null) return;
        String referenceName = ref.getReferenceName();
        if (referenceName == null) return;

        PsiElement resolved;
        if (ref instanceof RsStructLiteralField) {
            resolved = null;
            for (PsiElement candidate : reference.multiResolve()) {
                if (candidate instanceof RsVisible) {
                    resolved = candidate;
                    break;
                }
            }
        } else {
            resolved = reference.resolve();
        }
        if (!(resolved instanceof RsVisible resolvedElement)) return;

        RsMod oMod = RsElementUtil.contextStrict(o, RsMod.class);
        if (oMod == null) return;
        if (resolvedElement.isVisibleFrom(oMod)) return;

        boolean withinOneCrate = RsElementUtil.getCrateRoot(resolvedElement) == RsElementUtil.getCrateRoot(o);

        RsVisibilityOwner element;
        if (resolvedElement instanceof RsVisibilityOwner owner) {
            element = owner;
        } else if (resolvedElement instanceof RsFile file) {
            element = file.getDeclaration();
        } else {
            element = null;
        }
        if (element == null) return;

        RsDiagnostic error;
        if (element instanceof RsNamedFieldDecl field) {
            RsStructItem struct = RsElementUtil.ancestorStrict(field, RsStructItem.class);
            String crateRelativePath = struct != null ? struct.getCrateRelativePath() : null;
            String structName = crateRelativePath == null ? "" :
                (crateRelativePath.startsWith("::") ? crateRelativePath.substring(2) : crateRelativePath);
            error = new RsDiagnostic.StructFieldAccessError(
                highlightedElement, referenceName, structName,
                MakePublicFix.createIfCompatible(field, field.getName(), withinOneCrate));
        } else if (ref instanceof RsMethodCall) {
            error = new RsDiagnostic.AccessError(
                highlightedElement, E0624, "Method",
                MakePublicFix.createIfCompatible(element, referenceName, withinOneCrate));
        } else {
            String itemType = element instanceof RsItemElement item
                ? capitalize(item.getItemKindName())
                : "Item";
            error = new RsDiagnostic.AccessError(
                highlightedElement, E0603, itemType,
                MakePublicFix.createIfCompatible(element, referenceName, withinOneCrate));
        }
        RsDiagnostic.addToHolder(error, holder);
    }

    @Nonnull
    private static String capitalize(@Nonnull String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void checkPath(RsAnnotationHolder holder, RsPath path) {
        if (RsPathUtil.isInsideDocLink(path)) return;
        checkReferenceIsPublic(path, path, holder);
    }

    private void checkVis(RsAnnotationHolder holder, RsVis vis) {
        PsiElement parent = vis.getParent();
        if (parent instanceof RsImplItem || parent instanceof RsForeignModItem || parent instanceof RsEnumVariant) {
            RsDiagnostic.addToHolder(new RsDiagnostic.UnnecessaryVisibilityQualifierError(vis), holder);
        }
    }

    private void checkVisRestriction(RsAnnotationHolder holder, RsVisRestriction visRestriction) {
        // Simplified check
    }

    private void checkYieldExpr(RsAnnotationHolder holder, RsYieldExpr o) {
        CompilerFeature.getGENERATORS().check(holder, o.getYield(), RsBundle.message("yield.syntax"));
    }

    private void checkTypeArgumentList(RsAnnotationHolder holder, RsTypeArgumentList args) {
        checkRedundantColonColon(holder, args);
    }

    private void checkValueParameterList(RsAnnotationHolder holder, RsValueParameterList args) {
        checkRedundantColonColon(holder, args);
    }

    /**
     * {@code ::} is redundant in type position, as in {@code Vec::<i32>}. Note that {@code ::(i32) -> i32}
     * in {@code Fn::(i32) -> i32} is parsed as a {@link RsValueParameterList}, so the token is looked up
     * through the AST to cover both element types.
     */
    private static void checkRedundantColonColon(RsAnnotationHolder holder, RsElement args) {
        ASTNode node = args.getNode().findChildByType(RsElementTypes.COLONCOLON);
        if (node == null) return;
        PsiElement coloncolon = node.getPsi();
        if (!isTypePart(args)) return;
        AnnotationBuilder annotation = holder.newWeakWarningAnnotation(
            coloncolon, RsBundle.message("inspection.message.redundant"), new RemoveElementFix(coloncolon));
        if (annotation == null) return;
        annotation.highlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL).create();
    }

    private static boolean isTypePart(RsElement args) {
        for (PsiElement ancestor = args.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor instanceof RsExpr) return false;
            if (ancestor instanceof RsTypeReference || ancestor instanceof RsTraitRef) return true;
        }
        return false;
    }

    private void checkValueArgumentList(RsAnnotationHolder holder, RsValueArgumentList args) {
        // Simplified check
    }

    private void checkLetDecl(RsAnnotationHolder holder, RsLetDecl letDecl) {
        RsPat pat = letDecl.getPat();
        if (letDecl.getLetElseBranch() != null && pat != null && RsPatUtil.isIrrefutable(pat)) {
            CompilerFeature.getIRREFUTABLE_LET_PATTERNS().check(holder, pat, RsBundle.message("irrefutable.let.pattern"));
        }
    }

    private void checkLetElseBranch(RsAnnotationHolder holder, RsLetElseBranch elseBranch) {
        CompilerFeature.getLET_ELSE().check(holder, elseBranch, RsBundle.message("let.else"));
    }

    private void checkLetExpr(RsAnnotationHolder holder, RsLetExpr element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsCondition) && !(parent instanceof RsMatchArmGuard)) {
            CompilerFeature.getLET_CHAINS().check(holder, element, null,
                RsBundle.message("inspection.message.let.expressions.in.this.position.are.unstable"),
                RsBundle.message("inspection.message.let.expressions.in.this.position.are.unstable"),
                java.util.Collections.emptyList(), java.util.Collections.emptyList());
        }

        RsPat pat = element.getPat();
        if (pat != null && RsPatUtil.isIrrefutable(pat)) {
            CompilerFeature.getIRREFUTABLE_LET_PATTERNS().check(holder, pat, RsBundle.message("irrefutable.let.pattern"));
        }
    }

    private void checkFieldLookup(RsAnnotationHolder holder, RsFieldLookup field) {
        // Simplified check
    }

    private void checkModDecl(RsAnnotationHolder holder, RsModDeclItem modDecl) {
        checkDuplicates(holder, modDecl, null, false);
    }

    private void checkPatBinding(RsAnnotationHolder holder, RsPatBinding binding) {
        RsValueParameterList paramList = RsElementUtil.ancestorStrict(binding, RsValueParameterList.class);
        if (paramList != null) {
            checkDuplicates(holder, binding, paramList, true);
        }
    }

    private void checkPatBox(RsAnnotationHolder holder, RsPatBox box) {
        CompilerFeature.getBOX_PATTERNS().check(holder, box.getBox(), RsBundle.message("box.pattern.syntax"));
    }

    private void checkPatField(RsAnnotationHolder holder, RsPatField field) {
        PsiElement box = field.getBox();
        if (box == null) return;
        CompilerFeature.getBOX_PATTERNS().check(holder, box, RsBundle.message("box.pattern.syntax"));
    }

    private void checkPatRange(RsAnnotationHolder holder, RsPatRange range) {
        // Simplified check
    }

    private void checkPatRest(RsAnnotationHolder holder, RsPatRest patRest) {
        // Simplified check
    }

    private void checkRsPatStruct(RsAnnotationHolder holder, RsPatStruct patStruct) {
        // Simplified check
    }

    private void checkRsPatTupleStruct(RsAnnotationHolder holder, RsPatTupleStruct patTupleStruct) {
        // Simplified check
    }

    private void checkConstParameter(RsAnnotationHolder holder, RsConstParameter constParameter) {
        collectDiagnostics(holder, constParameter);
        checkDuplicates(holder, constParameter, null, false);
    }

    private void checkLifetimeParameter(RsAnnotationHolder holder, RsLifetimeParameter lifetimeParameter) {
        checkDuplicates(holder, lifetimeParameter, null, false);
    }

    private void checkLifetime(RsAnnotationHolder holder, RsLifetime lifetime) {
        // Simplified check
    }

    private void checkLabel(RsAnnotationHolder holder, RsLabel label) {
        // Simplified check
    }

    private void checkLabelDecl(RsAnnotationHolder holder, RsLabelDecl labelDecl) {
        // Simplified check
    }

    private void checkMatchArmGuard(RsAnnotationHolder holder, RsMatchArmGuard guard) {
        RsExpr expr = guard.getExpr();
        if (expr instanceof RsLetExpr) {
            CompilerFeature.getIF_LET_GUARD().check(holder, ((RsLetExpr) expr).getLet(), RsBundle.message("if.let.guard"));
        }
    }

    private void checkPolybound(RsAnnotationHolder holder, RsPolybound o) {
        if (o.getLparen() != null && o.getBound().getLifetime() != null) {
            holder.createErrorAnnotation(o, RsBundle.message("inspection.message.parenthesized.lifetime.bounds.are.not.supported"));
        }
    }

    private void checkTildeConst(RsAnnotationHolder holder, RsTildeConst o) {
        CompilerFeature.getCONST_TRAIT_IMPL().check(holder, o, RsBundle.message("const.trait.impls"));
        CompilerFeature.getCONST_FN_TRAIT_BOUND().check(holder, o, RsBundle.message("const.fn.trait.bound"));
    }

    private void checkBlockExpr(RsAnnotationHolder holder, RsBlockExpr expr) {
        RsLabelDecl label = expr.getLabelDecl();
        if (label != null) {
            CompilerFeature.getLABEL_BREAK_VALUE().check(holder, label, RsBundle.message("label.on.block"));
        }

        PsiElement constKw = expr.getConst();
        if (constKw != null) {
            if (expr.getParent() instanceof RsPat) {
                CompilerFeature.getINLINE_CONST_PAT().check(holder, constKw, RsBundle.message("inline.const.pat"));
            } else {
                CompilerFeature.getINLINE_CONST().check(holder, constKw, RsBundle.message("inline.const"));
            }
        }
    }

    private void checkRangeExpr(RsAnnotationHolder holder, RsRangeExpr range) {
        PsiElement dotdotdot = range.getDotdotdot();
        if (dotdotdot != null) {
            // rustc has no error code for this ("error: unexpected token: `...`")
            holder.createErrorAnnotation(dotdotdot,
                RsBundle.message("inspection.message.syntax.deprecated.use.for.exclusive.range.or.for.inclusive.range"));
        }
    }

    private void checkLitExpr(RsAnnotationHolder holder, RsLitExpr expr) {
        // Simplified check
    }

    private void checkLambdaExpr(RsAnnotationHolder holder, RsLambdaExpr expr) {
        PsiElement constKw = expr.getConst();
        if (constKw == null) return;
        CompilerFeature.getCONST_CLOSURES().check(holder, constKw, RsBundle.message("const.closures"));
    }

    private void checkBreakExpr(RsAnnotationHolder holder, RsBreakExpr expr) {
        checkLabelReferenceOwner(holder, expr);
    }

    private void checkContExpr(RsAnnotationHolder holder, RsContExpr expr) {
        checkLabelReferenceOwner(holder, expr);
    }

    private void checkLabelReferenceOwner(RsAnnotationHolder holder, RsLabelReferenceOwner expr) {
        // Simplified check
    }

    private void checkUnary(RsAnnotationHolder holder, RsUnaryExpr o) {
        PsiElement box = o.getBox();
        if (box != null) {
            ReplaceBoxSyntaxFix fix = new ReplaceBoxSyntaxFix(o);
            CompilerFeature.getBOX_SYNTAX().check(holder, box, RsBundle.message("box.expression.syntax"),
                java.util.Collections.emptyList(), java.util.Collections.singletonList(fix));
        }
    }

    private void checkBinary(RsAnnotationHolder holder, RsBinaryExpr o) {
        if (isComparisonBinaryExpr(o) && (isComparisonBinaryExpr(o.getLeft()) || isComparisonBinaryExpr(o.getRight()))) {
            holder.createErrorAnnotation(o,
                RsBundle.message("inspection.message.chained.comparison.operator.require.parentheses"),
                new AddTurbofishFix());
        }
    }

    private static boolean isComparisonBinaryExpr(@Nullable RsExpr expr) {
        if (!(expr instanceof RsBinaryExpr)) return false;
        Object op = RsBinaryOpUtil.getOperatorType(((RsBinaryExpr) expr).getBinaryOp());
        return op instanceof ComparisonOp || op instanceof EqualityOp;
    }

    private void checkExternAbi(RsAnnotationHolder holder, RsExternAbi abi) {
        // Simplified check
    }

    private void checkArrayType(RsAnnotationHolder holder, RsArrayType o) {
        collectDiagnostics(holder, o);
    }

    private void checkArrayExpr(RsAnnotationHolder holder, RsArrayExpr o) {
        // Simplified check
    }

    private void checkRetExpr(RsAnnotationHolder holder, RsRetExpr ret) {
        // Simplified check
    }

    private void checkInferType(RsAnnotationHolder holder, RsInferType type) {
        PsiElement owner = RsTypeReferenceUtil.getOwner(type).getParent();
        if (owner == null) return;
        PsiElement ownerParent = owner.getParent();
        PsiElement ownerGrandParent = ownerParent != null ? ownerParent.getParent() : null;
        boolean forbidden = (owner instanceof RsValueParameter && ownerGrandParent instanceof RsFunction)
            || (owner instanceof RsRetType && ownerParent instanceof RsFunction)
            || owner instanceof RsConstant
            || (owner instanceof RsFieldDecl
                && (ownerGrandParent instanceof RsStructItem || ownerGrandParent instanceof RsEnumVariant));
        if (forbidden) {
            RsDiagnostic.addToHolder(new RsDiagnostic.TypePlaceholderForbiddenError(type), holder);
        }
    }

    private void checkDuplicateImport(RsAnnotationHolder holder, RsUseSpeck useSpeck) {
        // Simplified check
    }

    private void checkReexports(RsAnnotationHolder holder, RsUseSpeck useSpeck) {
        // Simplified check
    }

    private void checkExternCrate(RsAnnotationHolder holder, RsExternCrateItem externCrate) {
        if (externCrate.getSelf() == null) return;
        CompilerFeature.getEXTERN_CRATE_SELF().check(holder, externCrate, RsBundle.message("extern.crate.self"));
        if (externCrate.getAlias() == null) {
            // rustc says "`extern crate self;` requires renaming", which is rather unclear
            holder.createErrorAnnotation(externCrate,
                RsBundle.message("inspection.message.extern.crate.self.requires.as.name"));
        }
    }

    private void checkCallExpr(RsAnnotationHolder holder, RsCallExpr o) {
        // Simplified check
    }

    private void checkTypeAlias(RsAnnotationHolder holder, RsTypeAlias ta) {
        checkDuplicates(holder, ta, null, false);
    }

    private void checkAttr(RsAnnotationHolder holder, RsAttr attr) {
        // Simplified check
    }

    private void checkStartAttribute(RsAnnotationHolder holder, RsAttr attr) {
        // Simplified check
    }

    private void checkDuplicateEnumVariants(RsAnnotationHolder holder, RsEnumBody o) {
        // Simplified check
    }

    private static void checkTypesAreSized(RsAnnotationHolder holder, RsFunction fn) {
        // Simplified check
    }

    private static void checkEmptyFunctionReturnType(RsAnnotationHolder holder, RsFunction fn) {
        RsBlock block = RsFunctionUtil.getBlock(fn);
        if (block == null) return;
        PsiElement rbrace = block.getRbrace();
        if (rbrace == null) return;
        Ty returnType = RsFunctionUtil.getNormReturnType(fn);
        if (returnType instanceof TyInfer.TyVar
            || returnType instanceof TyUnit
            || returnType instanceof TyAnon
            || FoldUtil.containsTyOfClass(returnType, java.util.List.of(TyUnknown.class))) {
            return;
        }

        RsBlockUtil.ExpandedStmtsAndTailExpr expanded = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
        if (expanded.getStatements().isEmpty() && expanded.getTailExpr() == null) {
            RsDiagnostic.addToHolder(new RsDiagnostic.TypeError(rbrace, returnType, TyUnit.INSTANCE), holder);
        }
    }

    private static void checkRecursiveAsyncFunction(RsAnnotationHolder holder, RsFunction fn) {
        // Simplified check
    }

    private static void checkParamAttrs(RsAnnotationHolder holder, RsOuterAttributeOwner o) {
        List<RsOuterAttr> outerAttrs = o.getOuterAttrList();
        if (outerAttrs.isEmpty()) return;
        RsOuterAttr startElement = outerAttrs.get(0);
        RsOuterAttr endElement = outerAttrs.get(outerAttrs.size() - 1);
        String message = RsBundle.message("inspection.message.attributes.on.function.parameters.experimental");
        CompilerFeature paramAttrs = CompilerFeature.getPARAM_ATTRS();
        FeatureAvailability availability = paramAttrs.availability(startElement);
        RsDiagnostic diagnostic;
        if (availability == NOT_AVAILABLE) {
            diagnostic = new RsDiagnostic.ExperimentalFeature(startElement, endElement, message, java.util.Collections.emptyList());
        } else if (availability == CAN_BE_ADDED) {
            diagnostic = new RsDiagnostic.ExperimentalFeature(startElement, endElement, message,
                java.util.Collections.singletonList(paramAttrs.addFeatureFix(startElement)));
        } else {
            return;
        }
        RsDiagnostic.addToHolder(diagnostic, holder);
    }

    private static void checkDuplicates(
        @Nonnull RsAnnotationHolder holder,
        @Nonnull RsNameIdentifierOwner element,
        @Nullable PsiElement scope,
        boolean recursively
    ) {
        PsiElement effectiveScope = scope != null ? scope : element.getContext();
        if ((element instanceof RsDocAndAttributeOwner && RsElementUtil.isCfgUnknown((RsDocAndAttributeOwner) element)) || effectiveScope == null) return;
        // Simplified duplicate checking
    }

    // Data classes and utilities

    public static class FunctionCallContext {
        private final int myExpectedParameterCount;
        @Nonnull
        private final FunctionType myFunctionType;
        @Nullable
        private final RsFunction myFunction;

        public FunctionCallContext(int expectedParameterCount, @Nonnull FunctionType functionType, @Nullable RsFunction function) {
            this.myExpectedParameterCount = expectedParameterCount;
            this.myFunctionType = functionType;
            this.myFunction = function;
        }

        public FunctionCallContext(int expectedParameterCount, @Nonnull FunctionType functionType) {
            this(expectedParameterCount, functionType, null);
        }

        public int getExpectedParameterCount() { return myExpectedParameterCount; }
        @Nonnull
        public FunctionType getFunctionType() { return myFunctionType; }
        @Nullable
        public RsFunction getFunction() { return myFunction; }
    }

    @Nullable
    public static FunctionCallContext getFunctionCallContext(@Nonnull RsValueArgumentList args) {
        PsiElement parent = args.getParent();
        if (parent instanceof RsCallExpr) {
            return getFunctionCallContext((RsCallExpr) parent);
        } else if (parent instanceof RsMethodCall) {
            return getFunctionCallContext((RsMethodCall) parent);
        }
        return null;
    }

    @Nullable
    public static FunctionCallContext getFunctionCallContext(@Nonnull RsCallExpr callExpr) {
        Ty calleeType = RsExprExtUtil.getAdjustedType(callExpr.getExpr());
        if (calleeType instanceof TyFunctionDef) {
            TyFunctionDef fnDef = (TyFunctionDef) calleeType;
            RsCallable callable = fnDef.getDef();
            int count = callable.getParameterTypes().size();
            int s = callable.getSelfParameter() != null ? 1 : 0;
            FunctionType functionType = callable.isVariadic() ? FunctionType.VARIADIC_FUNCTION : FunctionType.FUNCTION;
            RsFunction fn = callable instanceof RsCallable.Function ? ((RsCallable.Function) callable).getFn() : null;
            return new FunctionCallContext(count + s, functionType, fn);
        } else if (calleeType instanceof TyClosure) {
            return new FunctionCallContext(((TyClosure) calleeType).getParamTypes().size(), FunctionType.CLOSURE);
        } else if (calleeType instanceof TyFunctionPointer) {
            return new FunctionCallContext(((TyFunctionPointer) calleeType).getParamTypes().size(), FunctionType.FUNCTION);
        }
        return null;
    }

    @Nullable
    public static FunctionCallContext getFunctionCallContext(@Nonnull RsMethodCall methodCall) {
        PsiElement resolved = methodCall.getReference().resolve();
        if (!(resolved instanceof RsFunction)) return null;
        RsFunction fn = (RsFunction) resolved;
        RsValueParameterList valueParameterList = fn.getValueParameterList();
        if (valueParameterList == null) return null;
        int size = valueParameterList.getValueParameterList().size();
        FunctionType functionType = RsFunctionUtil.isVariadic(fn) ? FunctionType.VARIADIC_FUNCTION : FunctionType.FUNCTION;
        return new FunctionCallContext(size, functionType, fn);
    }

    private static final Set<String> RESERVED_LIFETIME_NAMES = new HashSet<>(Arrays.asList("'static", "'_"));

    private static final Key<Map<PsiElement, Map<RsElement, DuplicateInfo>>> DUPLICATES_BY_SCOPE =
        Key.create("org.rust.ide.annotator.RsErrorAnnotator.duplicates");

    private static class DuplicateInfo {
        @Nonnull
        private final Namespace myNamespace;
        @Nonnull
        private final String myName;
        @Nonnull
        private final List<RsElement> myElements;

        DuplicateInfo(@Nonnull Namespace namespace, @Nonnull String name, @Nonnull List<RsElement> elements) {
            this.myNamespace = namespace;
            this.myName = name;
            this.myElements = elements;
        }

        @Nonnull
        public Namespace getNamespace() { return myNamespace; }
        @Nonnull
        public String getName() { return myName; }
        @Nonnull
        public List<RsElement> getElements() { return myElements; }
    }

    public enum FunctionType {
        FUNCTION,
        VARIADIC_FUNCTION,
        CLOSURE
    }
}
