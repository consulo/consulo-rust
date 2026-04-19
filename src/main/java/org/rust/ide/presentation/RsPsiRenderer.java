/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.stubs.RsStubLiteralKind;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyInteger;
import org.rust.lang.utils.RsEscapesUtils;
import org.rust.stdext.JoinUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;
import org.rust.lang.core.psi.ext.RsBlockExprUtil;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;
import org.rust.lang.core.psi.ext.RsRefLikeTypeUtil;
import org.rust.lang.core.psi.ext.RsBinaryOpUtil;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsFnPointerTypeUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsLitExprUtil;

/**
 * Renders PSI elements to text without switching to AST (loses non-stubbed parts of PSI).
 */
@SuppressWarnings({"MemberVisibilityCanBePrivate", "DuplicatedCode"})
public class RsPsiRenderer {

    protected final PsiRenderingOptions options;

    public RsPsiRenderer(@NotNull PsiRenderingOptions options) {
        this.options = options;
    }

    protected boolean getRenderLifetimes() {
        return options.getRenderLifetimes();
    }

    protected boolean getRenderGenericsAndWhere() {
        return options.getRenderGenericsAndWhere();
    }

    protected boolean getShortPaths() {
        return options.getShortPaths();
    }

    /** Convenience static method to get stub-only text for a type reference */
    @NlsSafe
    @NotNull
    public static String getStubOnlyText(@NotNull RsTypeReference ref) {
        return RsPsiRendererUtil.getStubOnlyText(ref);
    }

    public void appendFunctionSignature(@NotNull StringBuilder sb, @NotNull RsFunction fn) {
        if (RsFunctionUtil.isAsync(fn)) sb.append("async ");
        if (RsFunctionUtil.isConst(fn)) sb.append("const ");
        if (fn.isUnsafe()) sb.append("unsafe ");
        if (RsFunctionUtil.isActuallyExtern(fn)) {
            sb.append("extern ");
            String abiName = RsFunctionUtil.getLiteralAbiName(fn);
            if (abiName != null) {
                sb.append("\"").append(abiName).append("\" ");
            }
        }
        sb.append("fn ");
        String escapedName = RsNamedElementUtil.getEscapedName(fn);
        sb.append(escapedName != null ? escapedName : "");
        RsTypeParameterList typeParameterList = fn.getTypeParameterList();
        if (typeParameterList != null && getRenderGenericsAndWhere()) {
            appendTypeParameterList(sb, typeParameterList);
        }
        RsValueParameterList valueParameterList = fn.getValueParameterList();
        if (valueParameterList != null) {
            appendValueParameterList(sb, valueParameterList);
        }
        RsRetType retType = fn.getRetType();
        if (retType != null) {
            sb.append(" -> ");
            RsTypeReference retTypeReference = retType.getTypeReference();
            if (retTypeReference != null) {
                appendTypeReference(sb, retTypeReference);
            }
        }
        RsWhereClause whereClause = fn.getWhereClause();
        if (whereClause != null && getRenderGenericsAndWhere()) {
            appendWhereClause(sb, whereClause);
        }
    }

    public void appendTypeAliasSignature(@NotNull StringBuilder sb, @NotNull RsTypeAlias ta, boolean renderBounds) {
        sb.append("type ");
        String escapedName = RsNamedElementUtil.getEscapedName(ta);
        sb.append(escapedName != null ? escapedName : "");
        RsTypeParameterList typeParameterList = ta.getTypeParameterList();
        if (typeParameterList != null && getRenderGenericsAndWhere()) {
            appendTypeParameterList(sb, typeParameterList);
        }
        if (getRenderGenericsAndWhere()) {
            for (RsWhereClause whereClause : ta.getWhereClauseList()) {
                appendWhereClause(sb, whereClause);
            }
        }
        RsTypeParamBounds typeParamBounds = ta.getTypeParamBounds();
        if (typeParamBounds != null && renderBounds) {
            appendTypeParamBounds(sb, typeParamBounds);
        }
    }

    private void appendWherePred(@NotNull StringBuilder sb, @NotNull RsWherePred pred) {
        RsLifetime lifetime = pred.getLifetime();
        RsTypeReference type = pred.getTypeReference();
        if (lifetime != null) {
            sb.append(lifetime.getName());
            RsLifetimeParamBounds bounds = pred.getLifetimeParamBounds();
            if (bounds != null) {
                appendLifetimeBounds(sb, bounds);
            }
        } else if (type != null) {
            RsForLifetimes forLifetimes = pred.getForLifetimes();
            if (getRenderLifetimes() && forLifetimes != null) {
                appendForLifetimes(sb, forLifetimes);
            }
            appendTypeReference(sb, type);
            RsTypeParamBounds typeParamBounds = pred.getTypeParamBounds();
            if (typeParamBounds != null) {
                appendTypeParamBounds(sb, typeParamBounds);
            }
        }
    }

    private void appendWhereClause(@NotNull StringBuilder sb, @NotNull RsWhereClause whereClause) {
        sb.append(" where ");
        List<RsWherePred> preds = whereClause.getWherePredList();
        for (int i = 0; i < preds.size(); i++) {
            if (i > 0) sb.append(", ");
            appendWherePred(sb, preds.get(i));
        }
    }

    private void appendTypeParamBounds(@NotNull StringBuilder sb, @NotNull RsTypeParamBounds bounds) {
        sb.append(": ");
        List<RsPolybound> polybounds = bounds.getPolyboundList();
        for (int i = 0; i < polybounds.size(); i++) {
            if (i > 0) sb.append(" + ");
            appendPolybound(sb, polybounds.get(i));
        }
    }

    public void appendTypeParameterList(@NotNull StringBuilder sb, @NotNull RsTypeParameterList list) {
        sb.append("<");
        List<RsElement> children = PsiElementUtil.stubChildrenOfType(list, RsElement.class);
        for (int i = 0; i < children.size(); i++) {
            if (i > 0) sb.append(", ");
            RsElement child = children.get(i);
            if (child instanceof RsLifetimeParameter lp) {
                sb.append(lp.getName());
                RsLifetimeParamBounds bounds = lp.getLifetimeParamBounds();
                if (bounds != null) {
                    appendLifetimeBounds(sb, bounds);
                }
            } else if (child instanceof RsTypeParameter tp) {
                sb.append(tp.getName());
                RsTypeParamBounds bounds = tp.getTypeParamBounds();
                if (bounds != null) {
                    sb.append(": ");
                    List<RsPolybound> polybounds = bounds.getPolyboundList();
                    for (int j = 0; j < polybounds.size(); j++) {
                        if (j > 0) sb.append(" + ");
                        appendPolybound(sb, polybounds.get(j));
                    }
                }
                RsTypeReference defaultValue = tp.getTypeReference();
                if (defaultValue != null) {
                    sb.append(" = ");
                    appendTypeReference(sb, defaultValue);
                }
            } else if (child instanceof RsConstParameter cp) {
                sb.append("const ");
                String cpName = cp.getName();
                sb.append(cpName != null ? cpName : "_");
                RsTypeReference cpType = cp.getTypeReference();
                if (cpType != null) {
                    sb.append(": ");
                    appendTypeReference(sb, cpType);
                }
                RsExpr cpDefaultValue = cp.getExpr();
                if (cpDefaultValue != null) {
                    sb.append(" = ");
                    appendConstExpr(sb, cpDefaultValue);
                }
            }
        }
        sb.append(">");
    }

    private void appendLifetimeBounds(@NotNull StringBuilder sb, @NotNull RsLifetimeParamBounds bounds) {
        sb.append(": ");
        List<RsLifetime> lifetimes = bounds.getLifetimeList();
        for (int i = 0; i < lifetimes.size(); i++) {
            if (i > 0) sb.append(" + ");
            sb.append(lifetimes.get(i).getName());
        }
    }

    public void appendValueParameterList(@NotNull StringBuilder sb, @NotNull RsValueParameterList list) {
        sb.append("(");
        RsSelfParameter selfParameter = list.getSelfParameter();
        List<RsValueParameter> valueParameterList = list.getValueParameterList();
        if (selfParameter != null) {
            appendSelfParameter(sb, selfParameter);
            if (!valueParameterList.isEmpty()) {
                sb.append(", ");
            }
        }
        for (int i = 0; i < valueParameterList.size(); i++) {
            if (i > 0) sb.append(", ");
            RsValueParameter param = valueParameterList.get(i);
            String patText = RsValueParameterUtil.getPatText(param);
            sb.append(patText != null ? patText : "_");
            sb.append(": ");
            RsTypeReference typeReference = param.getTypeReference();
            if (typeReference != null) {
                appendTypeReference(sb, typeReference);
            } else {
                sb.append("()");
            }
        }
        sb.append(")");
    }

    public void appendSelfParameter(@NotNull StringBuilder sb, @NotNull RsSelfParameter selfParameter) {
        RsTypeReference typeReference = selfParameter.getTypeReference();
        if (typeReference != null) {
            sb.append("self: ");
            appendTypeReference(sb, typeReference);
        } else {
            if (RsSelfParameterUtil.isRef(selfParameter)) {
                sb.append("&");
                RsLifetime lifetime = selfParameter.getLifetime();
                if (getRenderLifetimes() && lifetime != null) {
                    appendLifetime(sb, lifetime);
                    sb.append(" ");
                }
                sb.append(RsSelfParameterUtil.getMutability(selfParameter).isMut() ? "mut " : "");
            }
            sb.append("self");
        }
    }

    public void appendTypeReference(@NotNull StringBuilder sb, @NotNull RsTypeReference type) {
        if (type instanceof RsParenType parenType) {
            sb.append("(");
            if (parenType.getTypeReference() != null) appendTypeReference(sb, parenType.getTypeReference());
            sb.append(")");
        } else if (type instanceof RsTupleType tupleType) {
            List<RsTypeReference> types = tupleType.getTypeReferenceList();
            if (types.size() == 1) {
                sb.append("(");
                appendTypeReference(sb, types.get(0));
                sb.append(",)");
            } else {
                sb.append("(");
                for (int i = 0; i < types.size(); i++) {
                    if (i > 0) sb.append(", ");
                    appendTypeReference(sb, types.get(i));
                }
                sb.append(")");
            }
        } else if (type instanceof RsUnitType) {
            sb.append("()");
        } else if (type instanceof RsNeverType) {
            sb.append("!");
        } else if (type instanceof RsInferType) {
            sb.append("_");
        } else if (type instanceof RsPathType pathType) {
            appendPath(sb, pathType.getPath());
        } else if (type instanceof RsRefLikeType refLikeType) {
            if (RsRefLikeTypeUtil.isPointer(refLikeType)) {
                sb.append(RsRefLikeTypeUtil.getMutability(refLikeType).isMut() ? "*mut " : "*const ");
            } else if (RsRefLikeTypeUtil.isRef(refLikeType)) {
                sb.append("&");
                RsLifetime lifetime = refLikeType.getLifetime();
                if (getRenderLifetimes() && lifetime != null) {
                    appendLifetime(sb, lifetime);
                    sb.append(" ");
                }
                if (RsRefLikeTypeUtil.getMutability(refLikeType).isMut()) sb.append("mut ");
            }
            if (refLikeType.getTypeReference() != null) {
                appendTypeReference(sb, refLikeType.getTypeReference());
            }
        } else if (type instanceof RsArrayType arrayType) {
            sb.append("[");
            if (arrayType.getTypeReference() != null) appendTypeReference(sb, arrayType.getTypeReference());
            if (!RsArrayTypeUtil.isSlice(arrayType)) {
                RsExpr arraySizeExpr = arrayType.getExpr();
                sb.append("; ");
                if (arraySizeExpr != null) {
                    appendConstExpr(sb, arraySizeExpr, TyInteger.USize.INSTANCE);
                } else {
                    sb.append("{}");
                }
            }
            sb.append("]");
        } else if (type instanceof RsFnPointerType fnPointerType) {
            if (RsFnPointerTypeUtil.isUnsafe(fnPointerType)) sb.append("unsafe ");
            if (RsFnPointerTypeUtil.isExtern(fnPointerType)) {
                sb.append("extern ");
                String abiName = RsFnPointerTypeUtil.getAbiName(fnPointerType);
                if (abiName != null) {
                    sb.append("\"").append(abiName).append("\" ");
                }
            }
            sb.append("fn");
            appendValueParameterListTypes(sb, RsFnPointerTypeUtil.getValueParameters(fnPointerType));
            appendRetType(sb, fnPointerType.getRetType());
        } else if (type instanceof RsTraitType traitType) {
            sb.append(RsTraitTypeExtUtil.isImpl(traitType) ? "impl " : "dyn ");
            List<RsPolybound> polybounds = traitType.getPolyboundList();
            for (int i = 0; i < polybounds.size(); i++) {
                if (i > 0) sb.append(" + ");
                appendPolybound(sb, polybounds.get(i));
            }
        } else if (type instanceof RsMacroType macroType) {
            appendPath(sb, macroType.getMacroCall().getPath());
            sb.append("!(");
            String macroBody = RsMacroCallUtil.getMacroBody(macroType.getMacroCall());
            if (macroBody != null) sb.append(macroBody);
            sb.append(")");
        }
    }

    private void appendPolybound(@NotNull StringBuilder sb, @NotNull RsPolybound polyBound) {
        RsForLifetimes forLifetimes = polyBound.getForLifetimes();
        if (getRenderLifetimes() && forLifetimes != null) {
            appendForLifetimes(sb, forLifetimes);
        }
        if (RsPolyboundUtil.getHasQ(polyBound)) {
            sb.append("?");
        }
        RsBound bound = polyBound.getBound();
        RsLifetime lifetime = bound.getLifetime();
        if (getRenderLifetimes() && lifetime != null) {
            sb.append(lifetime.getReferenceName());
        } else {
            RsTraitRef traitRef = bound.getTraitRef();
            if (traitRef != null && traitRef.getPath() != null) {
                appendPath(sb, traitRef.getPath());
            }
        }
    }

    private void appendForLifetimes(@NotNull StringBuilder sb, @NotNull RsForLifetimes forLifetimes) {
        sb.append("for<");
        List<RsLifetimeParameter> params = forLifetimes.getLifetimeParameterList();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            String name = params.get(i).getName();
            sb.append(name != null ? name : "'_");
        }
        sb.append("> ");
    }

    public void appendLifetime(@NotNull StringBuilder sb, @NotNull RsLifetime lifetime) {
        sb.append(lifetime.getReferenceName());
    }

    public void appendPath(@NotNull StringBuilder sb, @NotNull RsPath path) {
        appendPathWithoutArgs(sb, path);
        appendPathArgs(sb, path);
    }

    protected void appendPathWithoutArgs(@NotNull StringBuilder sb, @NotNull RsPath path) {
        RsPath qualifier = path.getPath();
        if (!getShortPaths() && qualifier != null) {
            appendPath(sb, qualifier);
        }
        RsTypeQual typeQual = path.getTypeQual();
        if (typeQual != null) {
            appendTypeQual(sb, typeQual);
        }
        if (path.getHasColonColon()) {
            sb.append("::");
        }
        String refName = path.getReferenceName();
        sb.append(refName != null ? refName : "");
    }

    protected void appendTypeQual(@NotNull StringBuilder sb, @NotNull RsTypeQual typeQual) {
        sb.append("<");
        appendTypeReference(sb, typeQual.getTypeReference());
        RsTraitRef traitRef = typeQual.getTraitRef();
        if (traitRef != null) {
            sb.append(" as ");
            appendPath(sb, traitRef.getPath());
        }
        sb.append(">");
        sb.append("::");
    }

    private void appendPathArgs(@NotNull StringBuilder sb, @NotNull RsPath path) {
        RsTypeArgumentList inAngles = path.getTypeArgumentList();
        RsValueParameterList fnSugar = path.getValueParameterList();
        if (inAngles != null) {
            List<RsLifetime> lifetimeArguments = inAngles.getLifetimeList();
            List<RsTypeReference> typeArguments = inAngles.getTypeReferenceList();
            List<RsExpr> constArguments = inAngles.getExprList();
            List<RsAssocTypeBinding> assocTypeBindings = inAngles.getAssocTypeBindingList();

            boolean hasLifetimes = getRenderLifetimes() && !lifetimeArguments.isEmpty();
            boolean hasTypeReferences = !typeArguments.isEmpty();
            boolean hasConstArguments = !constArguments.isEmpty();
            boolean hasAssocTypeBindings = !assocTypeBindings.isEmpty();

            if (hasLifetimes || hasTypeReferences || hasConstArguments || hasAssocTypeBindings) {
                sb.append("<");
                boolean needComma = false;
                if (hasLifetimes) {
                    for (int i = 0; i < lifetimeArguments.size(); i++) {
                        if (i > 0) sb.append(", ");
                        appendLifetime(sb, lifetimeArguments.get(i));
                    }
                    needComma = true;
                }
                if (hasTypeReferences) {
                    if (needComma) sb.append(", ");
                    for (int i = 0; i < typeArguments.size(); i++) {
                        if (i > 0) sb.append(", ");
                        appendTypeReference(sb, typeArguments.get(i));
                    }
                    needComma = true;
                }
                if (hasConstArguments) {
                    if (needComma) sb.append(", ");
                    for (int i = 0; i < constArguments.size(); i++) {
                        if (i > 0) sb.append(", ");
                        appendConstExpr(sb, constArguments.get(i));
                    }
                    needComma = true;
                }
                if (hasAssocTypeBindings) {
                    if (needComma) sb.append(", ");
                    for (int i = 0; i < assocTypeBindings.size(); i++) {
                        if (i > 0) sb.append(", ");
                        RsAssocTypeBinding binding = assocTypeBindings.get(i);
                        appendPath(sb, binding.getPath());
                        sb.append("=");
                        RsTypeReference typeRef = binding.getTypeReference();
                        if (typeRef != null) appendTypeReference(sb, typeRef);
                    }
                }
                sb.append(">");
            }
        } else if (fnSugar != null) {
            appendValueParameterListTypes(sb, fnSugar.getValueParameterList());
            appendRetType(sb, path.getRetType());
        }
    }

    protected void appendRetType(@NotNull StringBuilder sb, @Nullable RsRetType retType) {
        if (retType != null) {
            RsTypeReference retTypeRef = retType.getTypeReference();
            if (retTypeRef != null) {
                sb.append(" -> ");
                appendTypeReference(sb, retTypeRef);
            }
        }
    }

    protected void appendValueParameterListTypes(@NotNull StringBuilder sb, @NotNull List<RsValueParameter> list) {
        sb.append("(");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            RsTypeReference typeRef = list.get(i).getTypeReference();
            if (typeRef != null) appendTypeReference(sb, typeRef);
        }
        sb.append(")");
    }

    public void appendConstExpr(@NotNull StringBuilder sb, @NotNull RsExpr expr) {
        appendConstExpr(sb, expr, org.rust.lang.core.types.RsTypesUtil.getType(expr));
    }

    public void appendConstExpr(@NotNull StringBuilder sb, @NotNull RsExpr expr, @NotNull Ty expectedTy) {
        if (expr instanceof RsPathExpr pathExpr) {
            appendPath(sb, pathExpr.getPath());
        } else if (expr instanceof RsLitExpr litExpr) {
            appendLitExpr(sb, litExpr);
        } else if (expr instanceof RsBlockExpr blockExpr) {
            appendBlockExpr(sb, blockExpr);
        } else if (expr instanceof RsUnaryExpr unaryExpr) {
            appendUnaryExpr(sb, unaryExpr);
        } else if (expr instanceof RsBinaryExpr binaryExpr) {
            appendBinaryExpr(sb, binaryExpr);
        } else {
            sb.append("{}");
        }
    }

    protected void appendLitExpr(@NotNull StringBuilder sb, @NotNull RsLitExpr expr) {
        RsStubLiteralKind kind = RsLitExprUtil.getStubKind(expr);
        if (kind instanceof RsStubLiteralKind.Boolean boolKind) {
            sb.append(boolKind.getValue());
        } else if (kind instanceof RsStubLiteralKind.Integer intKind) {
            if (intKind.getValue() != null) sb.append(intKind.getValue());
        } else if (kind instanceof RsStubLiteralKind.Float floatKind) {
            if (floatKind.getValue() != null) sb.append(floatKind.getValue());
        } else if (kind instanceof RsStubLiteralKind.Char charKind) {
            if (charKind.isByte()) sb.append("b");
            sb.append("'");
            String val = charKind.getValue();
            sb.append(val != null ? RsEscapesUtils.escapeRust(val) : "");
            sb.append("'");
        } else if (kind instanceof RsStubLiteralKind.StringLiteral strKind) {
            if (strKind.isByte()) {
                sb.append("b");
            } else if (strKind.isCStr()) {
                sb.append("c");
            }
            sb.append('"');
            String val = strKind.getValue();
            sb.append(val != null ? RsEscapesUtils.escapeRust(val) : "");
            sb.append('"');
        }
    }

    protected void appendBlockExpr(@NotNull StringBuilder sb, @NotNull RsBlockExpr expr) {
        if (RsBlockExprUtil.isTry(expr)) sb.append("try ");
        if (RsBlockExprUtil.isUnsafe(expr)) sb.append("unsafe ");
        if (RsBlockExprUtil.isAsync(expr)) sb.append("async ");
        if (RsBlockExprUtil.isConst(expr)) sb.append("const ");

        RsExpr tailExpr = RsBlockUtil.getExpandedTailExpr(expr.getBlock());
        if (tailExpr == null) {
            sb.append("{}");
        } else {
            sb.append("{ ");
            appendConstExpr(sb, tailExpr);
            sb.append(" }");
        }
    }

    protected void appendUnaryExpr(@NotNull StringBuilder sb, @NotNull RsUnaryExpr expr) {
        UnaryOperator op = RsUnaryExprUtil.getOperatorType(expr);
        String sign;
        if (op == UnaryOperator.REF) sign = "&";
        else if (op == UnaryOperator.REF_MUT) sign = "&mut ";
        else if (op == UnaryOperator.DEREF) sign = "*";
        else if (op == UnaryOperator.MINUS) sign = "-";
        else if (op == UnaryOperator.NOT) sign = "!";
        else if (op == UnaryOperator.BOX) sign = "box ";
        else if (op == UnaryOperator.RAW_REF_CONST) sign = "&raw const ";
        else if (op == UnaryOperator.RAW_REF_MUT) sign = "&raw mut ";
        else sign = "";
        sb.append(sign);
        RsExpr innerExpr = expr.getExpr();
        if (innerExpr != null) {
            appendConstExpr(sb, innerExpr);
        }
    }

    protected void appendBinaryExpr(@NotNull StringBuilder sb, @NotNull RsBinaryExpr expr) {
        Object op = RsBinaryOpUtil.getOperatorType(expr);
        String sign;
        if (op instanceof ArithmeticOp arithmeticOp) sign = arithmeticOp.getSign();
        else if (op instanceof ArithmeticAssignmentOp assignOp) sign = assignOp.getSign();
        else if (op == AssignmentOp.EQ) sign = "=";
        else if (op instanceof ComparisonOp compOp) sign = compOp.getSign();
        else if (op instanceof EqualityOp eqOp) sign = eqOp.getSign();
        else if (op == LogicOp.AND) sign = "&&";
        else if (op == LogicOp.OR) sign = "||";
        else sign = "";
        appendConstExpr(sb, expr.getLeft());
        sb.append(" ").append(sign).append(" ");
        RsExpr right = expr.getRight();
        if (right != null) {
            appendConstExpr(sb, right);
        }
    }
}
