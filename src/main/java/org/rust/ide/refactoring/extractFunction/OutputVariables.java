/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.types.TypeUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyNever;
import org.rust.lang.core.types.ty.TyTuple;
import org.rust.lang.core.types.ty.TyUnit;

import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.types.RsTypesUtil;

public class OutputVariables {
    @Nullable
    private final String myExprText;
    @NotNull
    private final Ty myType;

    public OutputVariables(@Nullable String exprText, @NotNull Ty type) {
        myExprText = exprText;
        myType = type;
    }

    @Nullable
    public String getExprText() {
        return myExprText;
    }

    @NotNull
    public Ty getType() {
        return myType;
    }

    @NotNull
    public static OutputVariables direct(@NotNull RsExpr expr) {
        Ty type = RsTypesUtil.getType(expr);
        return new OutputVariables(null, type instanceof TyNever ? TyUnit.INSTANCE : type);
    }

    @NotNull
    public static OutputVariables namedValue(@NotNull RsPatBinding value) {
        Ty type = RsTypesUtil.getType(value);
        return new OutputVariables(value.getReferenceName(), type instanceof TyNever ? TyUnit.INSTANCE : type);
    }

    @NotNull
    public static OutputVariables tupleNamedValue(@NotNull List<RsPatBinding> values) {
        String exprText = values.stream()
            .map(RsPatBinding::getReferenceName)
            .collect(Collectors.joining(", ", "(", ")"));
        List<Ty> types = values.stream()
            .map(RsTypesUtil::getType)
            .collect(Collectors.toList());
        return new OutputVariables(exprText, new TyTuple(types));
    }
}
