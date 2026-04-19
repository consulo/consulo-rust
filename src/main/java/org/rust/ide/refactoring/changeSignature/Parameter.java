/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import com.intellij.refactoring.changeSignature.ParameterInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;

public class Parameter {
    @NotNull
    private final RsPsiFactory myFactory;
    @NotNull
    private String myPatText;
    @NotNull
    private ParameterProperty<RsTypeReference> myType;
    private final int myIndex;
    @NotNull
    private ParameterProperty<RsExpr> myDefaultValue;

    public Parameter(
        @NotNull RsPsiFactory factory,
        @NotNull String patText,
        @NotNull ParameterProperty<RsTypeReference> type,
        int index,
        @NotNull ParameterProperty<RsExpr> defaultValue
    ) {
        myFactory = factory;
        myPatText = patText;
        myType = type;
        myIndex = index;
        myDefaultValue = defaultValue;
    }

    public Parameter(
        @NotNull RsPsiFactory factory,
        @NotNull String patText,
        @NotNull ParameterProperty<RsTypeReference> type,
        int index
    ) {
        this(factory, patText, type, index, new ParameterProperty.Empty<>());
    }

    public Parameter(
        @NotNull RsPsiFactory factory,
        @NotNull String patText,
        @NotNull ParameterProperty<RsTypeReference> type
    ) {
        this(factory, patText, type, ParameterInfo.NEW_PARAMETER, new ParameterProperty.Empty<>());
    }

    @NotNull
    public RsPsiFactory getFactory() {
        return myFactory;
    }

    @NotNull
    public String getPatText() {
        return myPatText;
    }

    public void setPatText(@NotNull String patText) {
        myPatText = patText;
    }

    @NotNull
    public ParameterProperty<RsTypeReference> getType() {
        return myType;
    }

    public void setType(@NotNull ParameterProperty<RsTypeReference> type) {
        myType = type;
    }

    public int getIndex() {
        return myIndex;
    }

    @NotNull
    public ParameterProperty<RsExpr> getDefaultValue() {
        return myDefaultValue;
    }

    public void setDefaultValue(@NotNull ParameterProperty<RsExpr> defaultValue) {
        myDefaultValue = defaultValue;
    }

    @NotNull
    public RsTypeReference getTypeReference() {
        RsTypeReference parsed = parseTypeReference();
        return parsed != null ? parsed : myFactory.createType("()");
    }

    @Nullable
    public RsTypeReference parseTypeReference() {
        return myType.getItem();
    }

    @Nullable
    private RsPat parsePat() {
        return myFactory.tryCreatePat(myPatText);
    }

    public boolean hasValidPattern() {
        if (parsePat() == null) {
            return false;
        }
        RsTypeReference typeRef = parseTypeReference();
        if (typeRef == null) {
            typeRef = myFactory.createType("()");
        }
        if (myFactory.tryCreateValueParameter(myPatText, typeRef) == null) {
            return false;
        }
        return true;
    }

    @NotNull
    public RsPat getPat() {
        RsPat pat = parsePat();
        return pat != null ? pat : myFactory.createPat("_");
    }
}
