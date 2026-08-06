/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import consulo.language.editor.refactoring.changeSignature.ParameterInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;

public class Parameter {
    @Nonnull
    private final RsPsiFactory myFactory;
    @Nonnull
    private String myPatText;
    @Nonnull
    private ParameterProperty<RsTypeReference> myType;
    private final int myIndex;
    @Nonnull
    private ParameterProperty<RsExpr> myDefaultValue;

    public Parameter(
        @Nonnull RsPsiFactory factory,
        @Nonnull String patText,
        @Nonnull ParameterProperty<RsTypeReference> type,
        int index,
        @Nonnull ParameterProperty<RsExpr> defaultValue
    ) {
        myFactory = factory;
        myPatText = patText;
        myType = type;
        myIndex = index;
        myDefaultValue = defaultValue;
    }

    public Parameter(
        @Nonnull RsPsiFactory factory,
        @Nonnull String patText,
        @Nonnull ParameterProperty<RsTypeReference> type,
        int index
    ) {
        this(factory, patText, type, index, new ParameterProperty.Empty<>());
    }

    public Parameter(
        @Nonnull RsPsiFactory factory,
        @Nonnull String patText,
        @Nonnull ParameterProperty<RsTypeReference> type
    ) {
        this(factory, patText, type, (-1), new ParameterProperty.Empty<>());
    }

    @Nonnull
    public RsPsiFactory getFactory() {
        return myFactory;
    }

    @Nonnull
    public String getPatText() {
        return myPatText;
    }

    public void setPatText(@Nonnull String patText) {
        myPatText = patText;
    }

    @Nonnull
    public ParameterProperty<RsTypeReference> getType() {
        return myType;
    }

    public void setType(@Nonnull ParameterProperty<RsTypeReference> type) {
        myType = type;
    }

    public int getIndex() {
        return myIndex;
    }

    @Nonnull
    public ParameterProperty<RsExpr> getDefaultValue() {
        return myDefaultValue;
    }

    public void setDefaultValue(@Nonnull ParameterProperty<RsExpr> defaultValue) {
        myDefaultValue = defaultValue;
    }

    @Nonnull
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

    @Nonnull
    public RsPat getPat() {
        RsPat pat = parsePat();
        return pat != null ? pat : myFactory.createPat("_");
    }
}
