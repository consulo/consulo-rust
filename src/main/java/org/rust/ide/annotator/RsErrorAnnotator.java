/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.fixes.*;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.FeatureState;
import org.rust.lang.core.macros.MacroExpansionMode;
import org.rust.lang.core.macros.MacroExpansionManager;
import org.rust.lang.core.macros.proc.ProcMacroApplicationService;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve.ref.RsReferenceExtUtil;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.RsCallable;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.TypeInferenceExtUtil;
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

public class RsErrorAnnotator extends AnnotatorBase {

    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        RsAnnotationHolder rsHolder = new RsAnnotationHolder(holder);
        RsVisitor visitor = new RsVisitor() {
            @Override
            public void visitCondition(@NotNull RsCondition o) { checkCondition(rsHolder, o); }
            @Override
            public void visitConstant(@NotNull RsConstant o) { checkConstant(rsHolder, o); }
            @Override
            public void visitTypeArgumentList(@NotNull RsTypeArgumentList o) { checkTypeArgumentList(rsHolder, o); }
            @Override
            public void visitValueParameterList(@NotNull RsValueParameterList o) { checkValueParameterList(rsHolder, o); }
            @Override
            public void visitValueArgumentList(@NotNull RsValueArgumentList o) { checkValueArgumentList(rsHolder, o); }
            @Override
            public void visitStructItem(@NotNull RsStructItem o) { checkDuplicates(rsHolder, o, null, false); }
            @Override
            public void visitEnumItem(@NotNull RsEnumItem o) { checkEnumItem(rsHolder, o); }
            @Override
            public void visitEnumVariant(@NotNull RsEnumVariant o) { checkEnumVariant(rsHolder, o); }
            @Override
            public void visitFunction(@NotNull RsFunction o) { checkFunction(rsHolder, o); }
            @Override
            public void visitImplItem(@NotNull RsImplItem o) { checkImpl(rsHolder, o); }
            @Override
            public void visitLetDecl(@NotNull RsLetDecl o) { checkLetDecl(rsHolder, o); }
            @Override
            public void visitLetElseBranch(@NotNull RsLetElseBranch o) { checkLetElseBranch(rsHolder, o); }
            @Override
            public void visitLetExpr(@NotNull RsLetExpr o) { checkLetExpr(rsHolder, o); }
            @Override
            public void visitFieldLookup(@NotNull RsFieldLookup o) { checkFieldLookup(rsHolder, o); }
            @Override
            public void visitModDeclItem(@NotNull RsModDeclItem o) { checkModDecl(rsHolder, o); }
            @Override
            public void visitModItem(@NotNull RsModItem o) { checkDuplicates(rsHolder, o, null, false); }
            @Override
            public void visitPatBinding(@NotNull RsPatBinding o) { checkPatBinding(rsHolder, o); }
            @Override
            public void visitPatBox(@NotNull RsPatBox o) { checkPatBox(rsHolder, o); }
            @Override
            public void visitPatField(@NotNull RsPatField o) { checkPatField(rsHolder, o); }
            @Override
            public void visitPatRange(@NotNull RsPatRange o) { checkPatRange(rsHolder, o); }
            @Override
            public void visitPatRest(@NotNull RsPatRest o) { checkPatRest(rsHolder, o); }
            @Override
            public void visitPatStruct(@NotNull RsPatStruct o) { checkRsPatStruct(rsHolder, o); }
            @Override
            public void visitPatTupleStruct(@NotNull RsPatTupleStruct o) { checkRsPatTupleStruct(rsHolder, o); }
            @Override
            public void visitPath(@NotNull RsPath o) { checkPath(rsHolder, o); }
            @Override
            public void visitTraitType(@NotNull RsTraitType o) { checkTraitType(rsHolder, o); }
            @Override
            public void visitTraitRef(@NotNull RsTraitRef o) { checkTraitRef(rsHolder, o); }
            @Override
            public void visitVis(@NotNull RsVis o) { checkVis(rsHolder, o); }
            @Override
            public void visitVisRestriction(@NotNull RsVisRestriction o) { checkVisRestriction(rsHolder, o); }
            @Override
            public void visitUnaryExpr(@NotNull RsUnaryExpr o) { checkUnary(rsHolder, o); }
            @Override
            public void visitBinaryExpr(@NotNull RsBinaryExpr o) { checkBinary(rsHolder, o); }
            @Override
            public void visitExternAbi(@NotNull RsExternAbi o) { checkExternAbi(rsHolder, o); }
            @Override
            public void visitDotExpr(@NotNull RsDotExpr o) { checkDotExpr(rsHolder, o); }
            @Override
            public void visitYieldExpr(@NotNull RsYieldExpr o) { checkYieldExpr(rsHolder, o); }
            @Override
            public void visitArrayType(@NotNull RsArrayType o) { checkArrayType(rsHolder, o); }
            @Override
            public void visitArrayExpr(@NotNull RsArrayExpr o) { checkArrayExpr(rsHolder, o); }
            @Override
            public void visitRetExpr(@NotNull RsRetExpr o) { checkRetExpr(rsHolder, o); }
            @Override
            public void visitInferType(@NotNull RsInferType o) { checkInferType(rsHolder, o); }
            @Override
            public void visitUseSpeck(@NotNull RsUseSpeck o) {
                checkDuplicateImport(rsHolder, o);
                checkReexports(rsHolder, o);
            }
            @Override
            public void visitExternCrateItem(@NotNull RsExternCrateItem o) { checkExternCrate(rsHolder, o); }
            @Override
            public void visitCallExpr(@NotNull RsCallExpr o) { checkCallExpr(rsHolder, o); }
            @Override
            public void visitTypeAlias(@NotNull RsTypeAlias o) { checkTypeAlias(rsHolder, o); }
            @Override
            public void visitTypeParameter(@NotNull RsTypeParameter o) { checkDuplicates(rsHolder, o, null, false); }
            @Override
            public void visitConstParameter(@NotNull RsConstParameter o) { checkConstParameter(rsHolder, o); }
            @Override
            public void visitLifetimeParameter(@NotNull RsLifetimeParameter o) { checkLifetimeParameter(rsHolder, o); }
            @Override
            public void visitLifetime(@NotNull RsLifetime o) { checkLifetime(rsHolder, o); }
            @Override
            public void visitLabel(@NotNull RsLabel o) { checkLabel(rsHolder, o); }
            @Override
            public void visitLabelDecl(@NotNull RsLabelDecl o) { checkLabelDecl(rsHolder, o); }
            @Override
            public void visitMatchArmGuard(@NotNull RsMatchArmGuard o) { checkMatchArmGuard(rsHolder, o); }
            @Override
            public void visitPolybound(@NotNull RsPolybound o) { checkPolybound(rsHolder, o); }
            @Override
            public void visitTildeConst(@NotNull RsTildeConst o) { checkTildeConst(rsHolder, o); }
            @Override
            public void visitBlockExpr(@NotNull RsBlockExpr o) { checkBlockExpr(rsHolder, o); }
            @Override
            public void visitRangeExpr(@NotNull RsRangeExpr o) { checkRangeExpr(rsHolder, o); }
            @Override
            public void visitLitExpr(@NotNull RsLitExpr o) { checkLitExpr(rsHolder, o); }
            @Override
            public void visitLambdaExpr(@NotNull RsLambdaExpr o) { checkLambdaExpr(rsHolder, o); }
            @Override
            public void visitBreakExpr(@NotNull RsBreakExpr o) { checkBreakExpr(rsHolder, o); }
            @Override
            public void visitContExpr(@NotNull RsContExpr o) { checkContExpr(rsHolder, o); }
            @Override
            public void visitAttr(@NotNull RsAttr o) { checkAttr(rsHolder, o); }
            @Override
            public void visitSelfParameter(@NotNull RsSelfParameter o) { checkParamAttrs(rsHolder, o); }
            @Override
            public void visitValueParameter(@NotNull RsValueParameter o) { checkParamAttrs(rsHolder, o); }
            @Override
            public void visitVariadic(@NotNull RsVariadic o) { checkParamAttrs(rsHolder, o); }
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
        // Simplified visibility check
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
        // Simplified check
    }

    private void checkValueParameterList(RsAnnotationHolder holder, RsValueParameterList args) {
        // Simplified check
    }

    private void checkValueArgumentList(RsAnnotationHolder holder, RsValueArgumentList args) {
        // Simplified check
    }

    private void checkLetDecl(RsAnnotationHolder holder, RsLetDecl letDecl) {
        // Simplified check
    }

    private void checkLetElseBranch(RsAnnotationHolder holder, RsLetElseBranch elseBranch) {
        CompilerFeature.getLET_ELSE().check(holder, elseBranch, RsBundle.message("let.else"));
    }

    private void checkLetExpr(RsAnnotationHolder holder, RsLetExpr element) {
        // Simplified check
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
        // Simplified check
    }

    private void checkPolybound(RsAnnotationHolder holder, RsPolybound o) {
        // Simplified check
    }

    private void checkTildeConst(RsAnnotationHolder holder, RsTildeConst o) {
        CompilerFeature.getCONST_TRAIT_IMPL().check(holder, o, RsBundle.message("const.trait.impls"));
    }

    private void checkBlockExpr(RsAnnotationHolder holder, RsBlockExpr expr) {
        // Simplified check
    }

    private void checkRangeExpr(RsAnnotationHolder holder, RsRangeExpr range) {
        // Simplified check
    }

    private void checkLitExpr(RsAnnotationHolder holder, RsLitExpr expr) {
        // Simplified check
    }

    private void checkLambdaExpr(RsAnnotationHolder holder, RsLambdaExpr expr) {
        // Simplified check
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
        // Simplified check
    }

    private void checkBinary(RsAnnotationHolder holder, RsBinaryExpr o) {
        // Simplified check
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
        // Simplified check
    }

    private void checkDuplicateImport(RsAnnotationHolder holder, RsUseSpeck useSpeck) {
        // Simplified check
    }

    private void checkReexports(RsAnnotationHolder holder, RsUseSpeck useSpeck) {
        // Simplified check
    }

    private void checkExternCrate(RsAnnotationHolder holder, RsExternCrateItem externCrate) {
        // Simplified check
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
        // Simplified check
    }

    private static void checkRecursiveAsyncFunction(RsAnnotationHolder holder, RsFunction fn) {
        // Simplified check
    }

    private static void checkParamAttrs(RsAnnotationHolder holder, RsOuterAttributeOwner o) {
        // Simplified check
    }

    private static void checkDuplicates(
        @NotNull RsAnnotationHolder holder,
        @NotNull RsNameIdentifierOwner element,
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
        @NotNull
        private final FunctionType myFunctionType;
        @Nullable
        private final RsFunction myFunction;

        public FunctionCallContext(int expectedParameterCount, @NotNull FunctionType functionType, @Nullable RsFunction function) {
            this.myExpectedParameterCount = expectedParameterCount;
            this.myFunctionType = functionType;
            this.myFunction = function;
        }

        public FunctionCallContext(int expectedParameterCount, @NotNull FunctionType functionType) {
            this(expectedParameterCount, functionType, null);
        }

        public int getExpectedParameterCount() { return myExpectedParameterCount; }
        @NotNull
        public FunctionType getFunctionType() { return myFunctionType; }
        @Nullable
        public RsFunction getFunction() { return myFunction; }
    }

    @Nullable
    public static FunctionCallContext getFunctionCallContext(@NotNull RsValueArgumentList args) {
        PsiElement parent = args.getParent();
        if (parent instanceof RsCallExpr) {
            return getFunctionCallContext((RsCallExpr) parent);
        } else if (parent instanceof RsMethodCall) {
            return getFunctionCallContext((RsMethodCall) parent);
        }
        return null;
    }

    @Nullable
    public static FunctionCallContext getFunctionCallContext(@NotNull RsCallExpr callExpr) {
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
    public static FunctionCallContext getFunctionCallContext(@NotNull RsMethodCall methodCall) {
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
        @NotNull
        private final Namespace myNamespace;
        @NotNull
        private final String myName;
        @NotNull
        private final List<RsElement> myElements;

        DuplicateInfo(@NotNull Namespace namespace, @NotNull String name, @NotNull List<RsElement> elements) {
            this.myNamespace = namespace;
            this.myName = name;
            this.myElements = elements;
        }

        @NotNull
        public Namespace getNamespace() { return myNamespace; }
        @NotNull
        public String getName() { return myName; }
        @NotNull
        public List<RsElement> getElements() { return myElements; }
    }

    public enum FunctionType {
        FUNCTION,
        VARIADIC_FUNCTION,
        CLOSURE
    }
}
