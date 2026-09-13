/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.presentation.TypeRendering;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyFunctionBase;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.impl.RsValueParameterUtil;
import org.rust.openapiext.PsiElementExtUtil;
import org.rust.lang.core.psi.ext.impl.RsTypeReferenceUtil;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.ext.impl.*;

public class CallInfo {
    @Nullable
    private final String myMethodName;
    @Nullable
    private final String mySelfParameter;
    @Nonnull
    private final List<Parameter> myParameters;

    private CallInfo(@Nullable String methodName, @Nullable String selfParameter, @Nonnull List<Parameter> parameters) {
        this.myMethodName = methodName;
        this.mySelfParameter = selfParameter;
        this.myParameters = parameters;
    }

    @Nullable
    public String getMethodName() {
        return myMethodName;
    }

    @Nullable
    public String getSelfParameter() {
        return mySelfParameter;
    }

    @Nonnull
    public List<Parameter> getParameters() {
        return myParameters;
    }

    public static class Parameter {
        @Nullable
        private final RsTypeReference myTypeRef;
        @Nullable
        private final String myPattern;
        @Nullable
        private final Ty myType;

        public Parameter(@Nullable RsTypeReference typeRef, @Nullable String pattern, @Nullable Ty type) {
            this.myTypeRef = typeRef;
            this.myPattern = pattern;
            this.myType = type;
        }

        public Parameter(@Nullable RsTypeReference typeRef) {
            this(typeRef, null, null);
        }

        @Nonnull
        public String renderType() {
            if (myType != null && !(myType instanceof TyUnknown)) {
                return TypeRendering.render(myType, true);
            }
            return myTypeRef != null ? RsTypeReferenceUtil.substAndGetText(myTypeRef, null) : "_";
        }

        @Nullable
        public RsTypeReference getTypeRef() {
            return myTypeRef;
        }

        @Nullable
        public String getPattern() {
            return myPattern;
        }

        @Nullable
        public Ty getType() {
            return myType;
        }
    }

    @Nullable
    public static CallInfo resolve(@Nonnull RsCallExpr call) {
        RsExpr expr = call.getExpr();
        if (!(expr instanceof RsPathExpr)) return null;
        RsPath path = ((RsPathExpr) expr).getPath();
        consulo.language.psi.PsiElement fn = path.getReference().resolve();
        if (fn == null) return null;
        Ty ty = RsTypesUtil.getType(expr);
        if (!(ty instanceof TyFunctionBase)) return null;
        TyFunctionBase fnType = (TyFunctionBase) ty;

        if (fn instanceof RsFunction) {
            return buildFunctionParameters((RsFunction) fn, fnType);
        } else {
            return buildFunctionLike((RsElement) fn, fnType);
        }
    }

    @Nullable
    public static CallInfo resolve(@Nonnull RsMethodCall methodCall) {
        consulo.language.psi.PsiElement resolved = methodCall.getReference().resolve();
        if (!(resolved instanceof RsFunction)) return null;
        RsFunction function = (RsFunction) resolved;
        Ty type = RsTypesUtil.getInference(methodCall).getResolvedMethodType(methodCall);
        if (!(type instanceof TyFunctionBase)) return null;
        return buildFunctionParameters(function, (TyFunctionBase) type);
    }

    @Nullable
    private static CallInfo buildFunctionLike(@Nonnull RsElement fn, @Nonnull TyFunctionBase ty) {
        List<Pair> parameters = getFunctionLikeParameters(fn);
        if (parameters == null) return null;
        return new CallInfo(null, null, buildParameters(ty.getParamTypes(), parameters));
    }

    @Nullable
    private static List<Pair> getFunctionLikeParameters(@Nonnull RsElement element) {
        if (element instanceof RsEnumVariant) {
            RsEnumVariant variant = (RsEnumVariant) element;
            List<Pair> result = new ArrayList<>();
            for (RsFieldDecl field : RsFieldsOwnerUtil.getPositionalFields(variant)) {
                result.add(new Pair(null, field.getTypeReference()));
            }
            return result;
        }
        if (element instanceof RsStructItem && RsStructItemUtil.isTupleStruct((RsStructItem) element)) {
            RsStructItem struct = (RsStructItem) element;
            RsTupleFields fields = struct.getTupleFields();
            if (fields == null) return null;
            List<Pair> result = new ArrayList<>();
            for (RsTupleFieldDecl field : fields.getTupleFieldDeclList()) {
                result.add(new Pair(null, field.getTypeReference()));
            }
            return result;
        }
        if (element instanceof RsPatBinding) {
            RsLetDecl decl = PsiElementExtUtil.ancestorStrict((RsPatBinding) element, RsLetDecl.class);
            if (decl == null) return null;
            RsExpr expr = decl.getExpr();
            if (!(expr instanceof RsLambdaExpr)) return null;
            RsLambdaExpr lambda = (RsLambdaExpr) expr;
            List<Pair> result = new ArrayList<>();
            for (RsValueParameter param : lambda.getValueParameterList().getValueParameterList()) {
                String text = RsValueParameterUtil.getPatText(param);
                result.add(new Pair(text != null ? text : "_", param.getTypeReference()));
            }
            return result;
        }
        return null;
    }

    @Nonnull
    private static CallInfo buildFunctionParameters(@Nonnull RsFunction function, @Nonnull TyFunctionBase ty) {
        List<Ty> types = ty.getParamTypes();
        if (RsFunctionUtil.isMethod(function)) {
            types = types.subList(1, types.size());
        }
        List<Pair> parameters = new ArrayList<>();
        for (RsValueParameter param : function.getValueParameters()) {
            String pattern = RsValueParameterUtil.getPatText(param);
            if (pattern == null) pattern = "_";
            parameters.add(new Pair(pattern, param.getTypeReference()));
        }

        String selfParam = null;
        RsSelfParameter self = function.getSelfParameter();
        if (self != null) {
            StringBuilder sb = new StringBuilder();
            if (RsSelfParameterUtil.isRef(self)) sb.append("&");
            if (RsSelfParameterUtil.getMutability(self).isMut()) sb.append("mut ");
            sb.append("self");
            selfParam = sb.toString();
        }

        return new CallInfo(function.getName(), selfParam, buildParameters(types, parameters));
    }

    @Nonnull
    private static List<Parameter> buildParameters(@Nonnull List<Ty> argumentTypes, @Nonnull List<Pair> parameters) {
        List<Parameter> result = new ArrayList<>();
        int size = Math.min(argumentTypes.size(), parameters.size());
        for (int i = 0; i < size; i++) {
            Ty type = argumentTypes.get(i);
            Pair param = parameters.get(i);
            result.add(new Parameter(param.myTypeRef, param.myName, type));
        }
        return result;
    }

    private static class Pair {
        @Nullable
        final String myName;
        @Nullable
        final RsTypeReference myTypeRef;

        Pair(@Nullable String name, @Nullable RsTypeReference typeRef) {
            this.myName = name;
            this.myTypeRef = typeRef;
        }
    }
}
