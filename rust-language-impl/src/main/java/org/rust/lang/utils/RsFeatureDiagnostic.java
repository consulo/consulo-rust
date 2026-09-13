/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A use of a compiler feature that the toolchain of the containing crate does not provide: either a
 * feature that is still unstable, or one that has been removed from the compiler.
 */
public final class RsFeatureDiagnostic implements RsInferenceDiagnostic {
    /**
     * Why the feature cannot be used here.
     */
    public enum Kind {
        /** The feature exists but is still unstable. */
        EXPERIMENTAL,
        /** The feature has been removed from the compiler. */
        REMOVED
    }

    @Nonnull
    private final PsiElement myElement;
    @Nullable
    private final PsiElement myEndElement;
    @Nonnull
    private final String myFeatureName;
    @Nonnull
    private final String myMessage;
    @Nonnull
    private final Kind myKind;
    private final boolean myCanAddFeatureAttribute;

    public RsFeatureDiagnostic(
        @Nonnull PsiElement element,
        @Nullable PsiElement endElement,
        @Nonnull String featureName,
        @Nonnull String message,
        @Nonnull Kind kind,
        boolean canAddFeatureAttribute
    ) {
        myElement = element;
        myEndElement = endElement;
        myFeatureName = featureName;
        myMessage = message;
        myKind = kind;
        myCanAddFeatureAttribute = canAddFeatureAttribute;
    }

    @Nonnull
    public PsiElement getElement() {
        return myElement;
    }

    /**
     * The last element of the reported range, or {@code null} when only {@link #getElement()} is
     * reported.
     */
    @Nullable
    public PsiElement getEndElement() {
        return myEndElement;
    }

    /**
     * Name of the feature as it is written inside a {@code #![feature(...)]} attribute.
     */
    @Nonnull
    public String getFeatureName() {
        return myFeatureName;
    }

    @Nonnull
    public String getMessage() {
        return myMessage;
    }

    @Nonnull
    public Kind getKind() {
        return myKind;
    }

    /**
     * Whether the crate could enable the feature by declaring {@code #![feature(name)]} in its root,
     * which is only possible for an unstable feature on a toolchain that accepts unstable features.
     */
    public boolean canAddFeatureAttribute() {
        return myCanAddFeatureAttribute;
    }
}
