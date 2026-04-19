/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.MirUtils;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.InferExtUtil;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Thir {
    @NotNull
    public final ThirExpr expr;
    @NotNull
    public final List<ThirParam> params;

    public Thir(@NotNull ThirExpr expr, @NotNull List<ThirParam> params) {
        this.expr = expr;
        this.params = params;
    }

    @NotNull
    public ThirExpr getExpr() {
        return expr;
    }

    @NotNull
    public List<ThirParam> getParams() {
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Thir)) return false;
        Thir thir = (Thir) o;
        return expr.equals(thir.expr) && params.equals(thir.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expr, params);
    }

    @Override
    public String toString() {
        return "Thir(expr=" + expr + ", params=" + params + ")";
    }

    @NotNull
    public static Thir from(@NotNull RsConstant constant) {
        RsExpr body = constant.getExpr();
        if (body == null) throw new IllegalStateException("Could not get expression of constant");
        ThirExpr expr = new MirrorContext(constant).mirrorExpr(body, MirUtils.asSpan(body));
        return new Thir(expr, Collections.emptyList());
    }

    @NotNull
    public static Thir from(@NotNull RsFunction function) {
        RsBlock body = RsFunctionUtil.getBlock(function);
        if (body == null) throw new IllegalStateException("Could not get block of function");
        ThirExpr expr = new MirrorContext(function).mirrorBlock(body, RsFunctionUtil.getNormReturnType(function), MirUtils.asSpan(body));
        List<ThirParam> params = explicitParams(function);
        return new Thir(expr, params);
    }

    @NotNull
    private static List<ThirParam> explicitParams(@NotNull RsFunction function) {
        List<ThirParam> result = new ArrayList<>();
        RsValueParameterList paramList = function.getValueParameterList();
        if (paramList == null) throw new IllegalStateException("Could not get function's parameters");

        RsSelfParameter self = paramList.getSelfParameter();
        if (self != null) {
            ImplicitSelfKind selfKind = ImplicitSelfKind.from(self);
            ImplicitSelfKind selfKindOrNull = selfKind.hasImplicitSelf() ? selfKind : null;
            MirSpan tySpan = null;
            if (self.getColon() != null) {
                RsTypeReference typeRef = self.getTypeReference();
                if (typeRef == null) throw new IllegalStateException("Could not get self parameter's type");
                tySpan = MirUtils.asSpan(typeRef);
            }
            ThirParam thirParam = new ThirParam(
                ThirPat.from(self),
                InferExtUtil.typeOfValue(self),
                tySpan,
                selfKindOrNull
            );
            result.add(thirParam);
        }

        for (RsValueParameter param : paramList.getValueParameterList()) {
            RsTypeReference typeRef = param.getTypeReference();
            if (typeRef == null) throw new IllegalStateException("Could not get parameter's type");
            MirSpan tySpan = MirUtils.asSpan(typeRef);
            // TODO: in case of closures tySpan should be null
            RsPat pat = param.getPat();
            if (pat == null) throw new IllegalStateException("Could not extract pat from parameter");
            ThirParam thirParam = new ThirParam(
                ThirPat.from(pat),
                ExtensionsUtil.getType(pat),
                tySpan,
                null
            );
            result.add(thirParam);
        }

        return result;
    }
}
