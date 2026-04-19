/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsFunctionStub;
import org.rust.lang.core.types.NormTypeUtil;
import org.rust.lang.core.types.RawTypeUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class RsFunctionUtil {
    private RsFunctionUtil() {
    }

    private static final Set<String> SAFE_INTRINSICS = new HashSet<>(Arrays.asList(
        "abort", "size_of", "min_align_of", "needs_drop", "caller_location",
        "add_with_overflow", "sub_with_overflow", "mul_with_overflow",
        "wrapping_add", "wrapping_sub", "wrapping_mul",
        "saturating_add", "saturating_sub",
        "rotate_left", "rotate_right",
        "ctpop", "ctlz", "cttz", "bswap", "bitreverse",
        "discriminant_value", "type_id",
        "likely", "unlikely",
        "ptr_guaranteed_eq", "ptr_guaranteed_ne",
        "minnumf32", "minnumf64", "maxnumf32", "rustc_peek", "maxnumf64",
        "type_name", "forget", "black_box", "variant_count"
    ));

    @Nullable
    public static RsBlock getBlock(@NotNull RsFunction fn) {
        return PsiTreeUtil.getChildOfType(fn, RsBlock.class);
    }

    public static boolean isAssocFn(@NotNull RsFunction fn) {
        return !getHasSelfParameters(fn) && RsAbstractableUtil.getOwner(fn).isImplOrTrait();
    }

    public static boolean isMethod(@NotNull RsFunction fn) {
        return getHasSelfParameters(fn) && RsAbstractableUtil.getOwner(fn).isImplOrTrait();
    }

    public static boolean isTest(@NotNull RsFunction fn) {
        return isTestAttr(RsDocAndAttributeOwnerUtil.getQueryAttributes(fn));
    }

    private static boolean isTestAttr(@NotNull QueryAttributes<?> attrs) {
        for (Object meta : attrs.getMetaItems()) {
            if (meta instanceof org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub) {
                org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub m = (org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub) meta;
                org.rust.lang.core.stubs.common.RsPathPsiOrStub path = m.getPath();
                if (path != null) {
                    String refName = path.getReferenceName();
                    if (refName != null && refName.contains("test")) return true;
                }
            }
        }
        return attrs.hasAtomAttribute("quickcheck");
    }

    public static boolean isMain(@NotNull RsFunction fn) {
        PsiElement parent = fn.getContext();
        if (!(parent instanceof RsFile) || !((RsFile) parent).isCrateRoot()) return false;
        if (RsDocAndAttributeOwnerUtil.getQueryAttributes((RsFile) parent).hasAttribute("no_main")) return false;
        org.rust.cargo.project.workspace.CargoWorkspace.Target target = RsElementExtUtil.getContainingCargoTarget(fn);
        if (target == null) return false;
        org.rust.cargo.project.workspace.CargoWorkspace.TargetKind kind = target.getKind();
        if (!(kind.isBin() || kind.isExampleBin() || kind.isCustomBuild())) return false;
        boolean hasStartFeature = org.rust.lang.core.CompilerFeature.getSTART().availability((RsFile) parent)
            == org.rust.lang.core.FeatureAvailability.AVAILABLE;
        return (hasStartFeature && RsDocAndAttributeOwnerUtil.getQueryAttributes(fn).hasAtomAttribute("start"))
            || "main".equals(fn.getName());
    }

    public static boolean isBench(@NotNull RsFunction fn) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(fn).hasAtomAttribute("bench");
    }

    public static boolean isConst(@NotNull RsFunction fn) {
        RsFunctionStub stub = RsPsiJavaUtil.getGreenStub(fn);
        return stub != null ? stub.isConst() : fn.getConst() != null;
    }

    /** Returns true if the function has {@code extern} modifier or located inside a certain {@code extern {}} block. */
    public static boolean isActuallyExtern(@NotNull RsFunction fn) {
        return isExtern(fn) || PsiElementUtil.getStubParent(fn) instanceof RsForeignModItem;
    }

    /** Returns true if the function has {@code extern} modifier. */
    public static boolean isExtern(@NotNull RsFunction fn) {
        RsFunctionStub stub = RsPsiJavaUtil.getGreenStub(fn);
        return stub != null ? stub.isExtern() : fn.getExternAbi() != null;
    }

    public static boolean isVariadic(@NotNull RsFunction fn) {
        RsFunctionStub stub = RsPsiJavaUtil.getGreenStub(fn);
        return stub != null ? stub.isVariadic()
            : fn.getValueParameterList() != null && fn.getValueParameterList().getVariadic() != null;
    }

    @Nullable
    public static String getLiteralAbiName(@NotNull RsFunction fn) {
        RsFunctionStub stub = RsPsiJavaUtil.getGreenStub(fn);
        if (stub != null) return stub.getAbiName();
        RsExternAbi abi = getAbi(fn);
        if (abi != null && abi.getLitExpr() != null) {
            return RsLitExprUtil.getStringValue(abi.getLitExpr());
        }
        return null;
    }

    @NotNull
    public static String getActualAbiName(@NotNull RsFunction fn) {
        String literal = getLiteralAbiName(fn);
        if (literal != null) return literal;
        return getAbi(fn) != null ? "C" : "Rust";
    }

    public static boolean isCOrCdeclAbi(@NotNull RsFunction fn) {
        String abi = getActualAbiName(fn);
        return "C".equals(abi) || "cdecl".equals(abi);
    }

    /**
     * Those function parameters that are not disabled by cfg attributes.
     * Should be used in code analysis.
     */
    @NotNull
    public static List<RsValueParameter> getValueParameters(@NotNull RsFunction fn) {
        List<RsValueParameter> result = new ArrayList<>();
        for (RsValueParameter param : getRawValueParameters(fn)) {
            if (RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(param, null)) {
                result.add(param);
            }
        }
        return result;
    }

    /**
     * All function parameters. Should be used in code (PSI) manipulations.
     */
    @NotNull
    public static List<RsValueParameter> getRawValueParameters(@NotNull RsFunction fn) {
        if (fn.getValueParameterList() == null) return Collections.emptyList();
        return fn.getValueParameterList().getValueParameterList();
    }

    @Nullable
    public static RsSelfParameter getSelfParameter(@NotNull RsFunction fn) {
        if (fn.getValueParameterList() == null) return null;
        return fn.getValueParameterList().getSelfParameter();
    }

    @Nullable
    public static PsiElement getDefault(@NotNull RsFunction fn) {
        com.intellij.lang.ASTNode node = fn.getNode().findChildByType(RsElementTypes.DEFAULT);
        return node != null ? node.getPsi() : null;
    }

    public static boolean isAsync(@NotNull RsFunction fn) {
        RsFunctionStub stub = RsPsiJavaUtil.getGreenStub(fn);
        if (stub != null) return stub.isAsync();
        return fn.getNode().findChildByType(RsElementTypes.ASYNC) != null;
    }

    public static boolean getHasSelfParameters(@NotNull RsFunction fn) {
        RsFunctionStub stub = RsPsiJavaUtil.getGreenStub(fn);
        if (stub != null) return stub.getHasSelfParameters();
        return getSelfParameter(fn) != null;
    }

    @NotNull
    public static String getTitle(@NotNull RsFunction fn) {
        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(fn);
        if (owner == RsAbstractableOwner.Free) return "Function `" + fn.getName() + "`";
        if (owner == RsAbstractableOwner.Foreign) return "Foreign function `" + fn.getName() + "`";
        // Trait or Impl
        return isAssocFn(fn) ? "Associated function `" + fn.getName() + "`" : "Method `" + fn.getName() + "`";
    }

    @NotNull
    public static Ty getRawReturnType(@NotNull RsFunction fn) {
        RsRetType retType = fn.getRetType();
        if (retType == null) return TyUnit.INSTANCE;
        if (retType.getTypeReference() == null) return TyUnknown.INSTANCE;
        return RawTypeUtil.getRawType(retType.getTypeReference());
    }

    @NotNull
    public static Ty getNormReturnType(@NotNull RsFunction fn) {
        RsRetType retType = fn.getRetType();
        if (retType == null) return TyUnit.INSTANCE;
        if (retType.getTypeReference() == null) return TyUnknown.INSTANCE;
        return NormTypeUtil.getNormType(retType.getTypeReference());
    }

    @Nullable
    public static RsExternAbi getAbi(@NotNull RsFunction fn) {
        if (fn.getExternAbi() != null) return fn.getExternAbi();
        PsiElement parent = fn.getParent();
        if (parent instanceof RsForeignModItem) {
            return ((RsForeignModItem) parent).getExternAbi();
        }
        return null;
    }

    @NotNull
    public static String getDeclaration(@NotNull RsFunction fn) {
        StringBuilder sb = new StringBuilder();
        PsiElement constKw = fn.getConst();
        if (constKw != null) sb.append(constKw.getText()).append(' ');
        sb.append(fn.getIdentifier().getText());
        if (fn.getTypeParameterList() != null) sb.append(fn.getTypeParameterList().getText());
        if (fn.getValueParameterList() != null) sb.append(fn.getValueParameterList().getText());
        if (fn.getRetType() != null) sb.append(fn.getRetType().getText());
        return sb.toString();
    }

    /**
     * A function is unsafe if defined with {@code unsafe} modifier or inside a certain
     * {@code extern} block.
     */
    public static boolean isActuallyUnsafe(@NotNull RsFunction fn) {
        if (fn.isUnsafe()) return true;
        PsiElement context = fn.getContext();
        if (context instanceof RsForeignModItem) {
            RsForeignModItem foreignMod = (RsForeignModItem) context;
            if (RsDocAndAttributeOwnerUtil.getQueryAttributes(foreignMod).hasAttribute("wasm_bindgen")) {
                return false;
            }
            if (isIntrinsic(fn)) {
                return fn.getName() == null || !SAFE_INTRINSICS.contains(fn.getName());
            }
            return true;
        }
        return false;
    }

    public static boolean isIntrinsic(@NotNull RsFunction fn) {
        PsiElement context = fn.getContext();
        return context instanceof RsForeignModItem
            && "rust-intrinsic".equals(RsForeignModItemUtil.getEffectiveAbi((RsForeignModItem) context));
    }

    public static boolean isBangProcMacroDef(@NotNull RsFunction fn) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(fn).hasAtomAttribute("proc_macro");
    }

    public static boolean isAttributeProcMacroDef(@NotNull RsFunction fn) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(fn).hasAtomAttribute("proc_macro_attribute");
    }

    public static boolean isCustomDeriveProcMacroDef(@NotNull RsFunction fn) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(fn).hasAttribute("proc_macro_derive");
    }

    public static boolean isProcMacroDef(@NotNull RsFunction fn) {
        return IS_PROC_MACRO_DEF_PROP.getByPsi(fn);
    }

    @NotNull
    public static final StubbedAttributeProperty<RsFunction, RsFunctionStub> IS_PROC_MACRO_DEF_PROP =
        new StubbedAttributeProperty<>(
            attrs -> attrs.hasAnyOfAttributes("proc_macro", "proc_macro_attribute", "proc_macro_derive"),
            RsFunctionStub::getMayBeProcMacroDef
        );

    @Nullable
    public static String getProcMacroName(@NotNull RsFunction fn) {
        if (isBangProcMacroDef(fn) || isAttributeProcMacroDef(fn)) return fn.getName();
        if (isCustomDeriveProcMacroDef(fn)) {
            return RsDocAndAttributeOwnerUtil.getQueryAttributes(fn).getFirstArgOfSingularAttribute("proc_macro_derive");
        }
        return null;
    }

    @NotNull
    public static MacroBraces getPreferredBraces(@NotNull RsFunction fn) {
        RsFunctionStub stub = RsPsiJavaUtil.getGreenStub(fn);
        if (stub != null) return stub.getPreferredBraces();
        return RsMacroDefinitionBaseUtil.guessPreferredBraces(fn);
    }

    /** See also {@link #getProcMacroName(RsFunction)} */
    @Nullable
    public static String getFunctionName(@NotNull RsFunction fn) {
        return ((RsFunctionImplMixin) fn).getFunctionName();
    }

    @NotNull
    public static PsiElement getFirstKeyword(@NotNull RsFunction fn) {
        com.intellij.psi.tree.TokenSet keywords = com.intellij.psi.tree.TokenSet.create(
            RsElementTypes.VIS, RsElementTypes.DEFAULT, RsElementTypes.ASYNC,
            RsElementTypes.CONST, RsElementTypes.UNSAFE, RsElementTypes.EXTERN_ABI
        );
        com.intellij.lang.ASTNode child = fn.getNode().findChildByType(keywords);
        return child != null ? child.getPsi() : fn.getFn();
    }

    @Nullable
    public static PsiElement getAsync(@NotNull RsFunction fn) {
        com.intellij.lang.ASTNode node = fn.getNode().findChildByType(RsElementTypes.ASYNC);
        return node != null ? node.getPsi() : null;
    }

    /** Find all function calls that call this function. */
    @NotNull
    public static Iterable<RsCallExpr> findFunctionCalls(@NotNull RsFunction fn) {
        return findFunctionCalls(fn, null);
    }

    @NotNull
    public static Iterable<RsCallExpr> findFunctionCalls(@NotNull RsFunction fn, @Nullable SearchScope scope) {
        List<RsCallExpr> result = new ArrayList<>();
        for (PsiReference ref : searchReferences(fn, scope)) {
            PsiElement path = ref.getElement();
            PsiElement pathExpr = path.getParent();
            if (pathExpr != null && pathExpr.getParent() instanceof RsCallExpr) {
                result.add((RsCallExpr) pathExpr.getParent());
            }
        }
        return result;
    }

    /** Find all method calls that call this function. */
    @NotNull
    public static Iterable<RsMethodCall> findMethodCalls(@NotNull RsFunction fn) {
        return findMethodCalls(fn, null);
    }

    @NotNull
    public static Iterable<RsMethodCall> findMethodCalls(@NotNull RsFunction fn, @Nullable SearchScope scope) {
        List<RsMethodCall> result = new ArrayList<>();
        for (PsiReference ref : searchReferences(fn, scope)) {
            if (ref.getElement() instanceof RsMethodCall) {
                result.add((RsMethodCall) ref.getElement());
            }
        }
        return result;
    }

    /** Find all usages. */
    @NotNull
    public static Iterable<RsElement> findUsages(@NotNull RsFunction fn) {
        return findUsages(fn, null);
    }

    @NotNull
    public static Iterable<RsElement> findUsages(@NotNull RsFunction fn, @Nullable SearchScope scope) {
        List<RsElement> result = new ArrayList<>();
        for (PsiReference ref : searchReferences(fn, scope)) {
            RsMethodCall mc = ref.getElement() instanceof RsMethodCall ? (RsMethodCall) ref.getElement() : null;
            if (mc != null) { result.add(mc); continue; }
            PsiElement path = ref.getElement();
            PsiElement pathExpr = path.getParent();
            if (pathExpr != null && pathExpr.getParent() instanceof RsCallExpr) {
                result.add((RsCallExpr) pathExpr.getParent());
                continue;
            }
            if (path instanceof RsPath) {
                result.add((RsPath) path);
            }
        }
        return result;
    }

    @NotNull
    public static List<RsWherePred> getWherePreds(@NotNull RsFunction fn) {
        return RsGenericDeclarationUtil.getWherePreds(fn);
    }

    @NotNull
    private static Collection<PsiReference> searchReferences(@NotNull RsFunction fn, @Nullable SearchScope scope) {
        if (scope != null) {
            return ReferencesSearch.search(fn, scope).findAll();
        }
        return ReferencesSearch.search(fn).findAll();
    }

    @NotNull
    public static RsAbstractableOwner getOwner(@NotNull RsFunction fn) {
        return RsAbstractableUtil.getOwner(fn);
    }
}
