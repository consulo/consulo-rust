/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import java.util.Objects;

/**
 * Some known lookup element properties used in
 * completion sorting and Machine Learning-Assisted Completion sorting.
 */
public final class RsLookupElementProperties {
    private final boolean isFullLineCompletion;
    private final KeywordKind keywordKind;
    private final boolean isSelfTypeCompatible;
    private final boolean isReturnTypeConformsToExpectedType;
    private final boolean isLocal;
    private final boolean isInherentImplMember;
    private final ElementKind elementKind;
    private final boolean isOperatorMethod;
    private final boolean isBlanketImplMember;
    private final boolean isUnsafeFn;
    private final boolean isAsyncFn;
    private final boolean isConstFnOrConst;
    private final boolean isExternFn;

    public RsLookupElementProperties() {
        this(false, KeywordKind.NOT_A_KEYWORD, false, false, false, false,
            ElementKind.DEFAULT, false, false, false, false, false, false);
    }

    public RsLookupElementProperties(KeywordKind keywordKind) {
        this(false, keywordKind, false, false, false, false,
            ElementKind.DEFAULT, false, false, false, false, false, false);
    }

    public RsLookupElementProperties(ElementKind elementKind) {
        this(false, KeywordKind.NOT_A_KEYWORD, false, false, false, false,
            elementKind, false, false, false, false, false, false);
    }

    public RsLookupElementProperties(boolean isFullLineCompletion) {
        this(isFullLineCompletion, KeywordKind.NOT_A_KEYWORD, false, false, false, false,
            ElementKind.DEFAULT, false, false, false, false, false, false);
    }

    public RsLookupElementProperties(
        boolean isFullLineCompletion,
        KeywordKind keywordKind,
        boolean isSelfTypeCompatible,
        boolean isReturnTypeConformsToExpectedType,
        boolean isLocal,
        boolean isInherentImplMember,
        ElementKind elementKind,
        boolean isOperatorMethod,
        boolean isBlanketImplMember,
        boolean isUnsafeFn,
        boolean isAsyncFn,
        boolean isConstFnOrConst,
        boolean isExternFn
    ) {
        this.isFullLineCompletion = isFullLineCompletion;
        this.keywordKind = keywordKind;
        this.isSelfTypeCompatible = isSelfTypeCompatible;
        this.isReturnTypeConformsToExpectedType = isReturnTypeConformsToExpectedType;
        this.isLocal = isLocal;
        this.isInherentImplMember = isInherentImplMember;
        this.elementKind = elementKind;
        this.isOperatorMethod = isOperatorMethod;
        this.isBlanketImplMember = isBlanketImplMember;
        this.isUnsafeFn = isUnsafeFn;
        this.isAsyncFn = isAsyncFn;
        this.isConstFnOrConst = isConstFnOrConst;
        this.isExternFn = isExternFn;
    }

    public boolean isFullLineCompletion() { return isFullLineCompletion; }
    public KeywordKind getKeywordKind() { return keywordKind; }
    public boolean isSelfTypeCompatible() { return isSelfTypeCompatible; }
    public boolean isReturnTypeConformsToExpectedType() { return isReturnTypeConformsToExpectedType; }
    public boolean isLocal() { return isLocal; }
    public boolean isInherentImplMember() { return isInherentImplMember; }
    public ElementKind getElementKind() { return elementKind; }
    public boolean isOperatorMethod() { return isOperatorMethod; }
    public boolean isBlanketImplMember() { return isBlanketImplMember; }
    public boolean isUnsafeFn() { return isUnsafeFn; }
    public boolean isAsyncFn() { return isAsyncFn; }
    public boolean isConstFnOrConst() { return isConstFnOrConst; }
    public boolean isExternFn() { return isExternFn; }

    public RsLookupElementProperties withKeywordKind(KeywordKind keywordKind) {
        return new RsLookupElementProperties(isFullLineCompletion, keywordKind, isSelfTypeCompatible,
            isReturnTypeConformsToExpectedType, isLocal, isInherentImplMember, elementKind,
            isOperatorMethod, isBlanketImplMember, isUnsafeFn, isAsyncFn, isConstFnOrConst, isExternFn);
    }

    public RsLookupElementProperties withReturnTypeConformsToExpectedType(boolean value) {
        return new RsLookupElementProperties(isFullLineCompletion, keywordKind, isSelfTypeCompatible,
            value, isLocal, isInherentImplMember, elementKind,
            isOperatorMethod, isBlanketImplMember, isUnsafeFn, isAsyncFn, isConstFnOrConst, isExternFn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RsLookupElementProperties)) return false;
        RsLookupElementProperties that = (RsLookupElementProperties) o;
        return isFullLineCompletion == that.isFullLineCompletion
            && keywordKind == that.keywordKind
            && isSelfTypeCompatible == that.isSelfTypeCompatible
            && isReturnTypeConformsToExpectedType == that.isReturnTypeConformsToExpectedType
            && isLocal == that.isLocal
            && isInherentImplMember == that.isInherentImplMember
            && elementKind == that.elementKind
            && isOperatorMethod == that.isOperatorMethod
            && isBlanketImplMember == that.isBlanketImplMember
            && isUnsafeFn == that.isUnsafeFn
            && isAsyncFn == that.isAsyncFn
            && isConstFnOrConst == that.isConstFnOrConst
            && isExternFn == that.isExternFn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFullLineCompletion, keywordKind, isSelfTypeCompatible,
            isReturnTypeConformsToExpectedType, isLocal, isInherentImplMember, elementKind,
            isOperatorMethod, isBlanketImplMember, isUnsafeFn, isAsyncFn, isConstFnOrConst, isExternFn);
    }

    public enum KeywordKind {
        // Top Priority
        PUB,
        PUB_CRATE,
        PUB_PARENS,
        LAMBDA_EXPR,
        ELSE_BRANCH,
        AWAIT,
        KEYWORD,
        NOT_A_KEYWORD,
        // Least priority
    }

    public enum ElementKind {
        // Top Priority
        DERIVE_GROUP,
        DERIVE,
        LINT,
        LINT_GROUP,
        VARIABLE,
        ENUM_VARIANT,
        FIELD_DECL,
        ASSOC_FN,
        DEFAULT,
        MACRO,
        DEPRECATED,
        FROM_UNRESOLVED_IMPORT,
        // Least priority
    }
}
