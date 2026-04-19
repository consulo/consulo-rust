/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix.editable;

import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateExpressionCondition;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.rust.RsBundle;
import org.rust.ide.presentation.RsPsiRenderingUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.*;

import java.util.Objects;
import org.rust.ide.presentation.TypeRendering;

public class RsPostfixTemplateExpressionCondition implements PostfixTemplateExpressionCondition<RsExpr> {

    public static final String USER_ENTERED_TYPE_NAME_ATTRIBUTE = "Aetna";

    private final Type myExpressionType;
    private final String myUserEnteredTypeName;

    public enum Type {
        Ref, Slice, Bool, Number, ADT, Array, Tuple, Unit, UserEntered;

        public String getId() {
            return toString();
        }
    }

    public RsPostfixTemplateExpressionCondition(Type expressionType) {
        this(expressionType, "");
    }

    public RsPostfixTemplateExpressionCondition(Type expressionType, String userEnteredTypeName) {
        myExpressionType = expressionType;
        myUserEnteredTypeName = userEnteredTypeName;
    }

    @Override
    public boolean value(RsExpr element) {
        Ty ty = RsTypesUtil.getType(element);
        switch (myExpressionType) {
            case Ref: return ty instanceof TyReference;
            case Slice: return RsTypesUtil.stripReferences(ty) instanceof TySlice;
            case Bool: return RsTypesUtil.stripReferences(ty) instanceof TyBool;
            case Number: return RsTypesUtil.stripReferences(ty) instanceof TyNumeric;
            case ADT: return RsTypesUtil.stripReferences(ty) instanceof TyAdt;
            case Array: return RsTypesUtil.stripReferences(ty) instanceof TyArray;
            case Tuple: return RsTypesUtil.stripReferences(ty) instanceof TyTuple;
            case Unit: return RsTypesUtil.stripReferences(ty) instanceof TyUnit;
            case UserEntered: return isUserEnteredType(element);
            default: return false;
        }
    }

    private boolean isUserEnteredType(RsExpr element) {
        String typePathWithoutParams = withoutTypeParameters(myUserEnteredTypeName);
        int lastColon = typePathWithoutParams.lastIndexOf("::");
        String typePath = lastColon >= 0 ? typePathWithoutParams.substring(0, lastColon) : "";
        String typeName = lastColon >= 0
            ? typePathWithoutParams.substring(lastColon + 2)
            : typePathWithoutParams;
        typeName = typeName.replaceAll("\\s", "");

        Ty stripped = RsTypesUtil.stripReferences(RsTypesUtil.getType(element));
        if (stripped instanceof TyAdt) {
            TyAdt adtType = (TyAdt) stripped;
            String adtName = adtType.getItem().getName();
            boolean useFullPath = !typePath.isEmpty();
            if (useFullPath) {
                String adtFullPath = adtType.getItem().getContainingCrate().getPresentableName()
                    + adtType.getItem().getCrateRelativePath();
                return (typePath + "::" + typeName).equals(adtFullPath);
            } else {
                return typeName.equals(adtName);
            }
        } else {
            String rendered = TypeRendering.renderInsertionSafe(RsTypesUtil.getType(element), false, false)
                .replaceAll("\\s", "");
            return typeName.equals(rendered);
        }
    }

    private static String withoutTypeParameters(CharSequence text) {
        StringBuilder sb = new StringBuilder(text.length());
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '<') depth++;
            else if (ch == '>') depth--;
            else if (depth == 0) sb.append(ch);
        }
        return sb.toString();
    }

    @Override
    public String getPresentableName() {
        if (myExpressionType == Type.UserEntered) {
            return RsBundle.message("type.0", myUserEnteredTypeName);
        }
        return myExpressionType.getId();
    }

    @Override
    public String getId() {
        return myExpressionType.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RsPostfixTemplateExpressionCondition that = (RsPostfixTemplateExpressionCondition) o;
        return myExpressionType == that.myExpressionType
            && myUserEnteredTypeName.equals(that.myUserEnteredTypeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myExpressionType.getId(), myUserEnteredTypeName);
    }

    @Override
    public void serializeTo(Element element) {
        PostfixTemplateExpressionCondition.super.serializeTo(element);
        element.setAttribute(USER_ENTERED_TYPE_NAME_ATTRIBUTE, myUserEnteredTypeName);
    }

    public static RsPostfixTemplateExpressionCondition readExternal(Element condition) {
        String id = condition.getAttributeValue(PostfixTemplateExpressionCondition.ID_ATTR);
        Type externalType = null;
        for (Type t : Type.values()) {
            if (t.getId().equals(id)) {
                externalType = t;
                break;
            }
        }
        if (externalType == null) return null;

        if (externalType == Type.UserEntered) {
            String userTypeName = condition.getAttributeValue(USER_ENTERED_TYPE_NAME_ATTRIBUTE);
            if (StringUtil.isNotEmpty(userTypeName)) {
                return new RsPostfixTemplateExpressionCondition(externalType, userTypeName);
            }
        } else {
            return new RsPostfixTemplateExpressionCondition(externalType);
        }
        return null;
    }
}
