/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.format;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.utils.RsInferenceDiagnostic;

public class ErrorAnnotation {
    @Nonnull
    private final TextRange myRange;
    @Nonnull
    private final String myError;
    private final boolean myIsTraitError;
    @Nullable
    private final RsInferenceDiagnostic myDiagnostic;

    public ErrorAnnotation(@Nonnull TextRange range, @Nonnull String error) {
        this(range, error, false, null);
    }

    public ErrorAnnotation(@Nonnull TextRange range, @Nonnull String error, boolean isTraitError, @Nullable RsInferenceDiagnostic diagnostic) {
        this.myRange = range;
        this.myError = error;
        this.myIsTraitError = isTraitError;
        this.myDiagnostic = diagnostic;
    }

    @Nonnull
    public TextRange getRange() {
        return myRange;
    }

    @Nonnull
    public String getError() {
        return myError;
    }

    public boolean isTraitError() {
        return myIsTraitError;
    }

    @Nullable
    public RsInferenceDiagnostic getDiagnostic() {
        return myDiagnostic;
    }
}
