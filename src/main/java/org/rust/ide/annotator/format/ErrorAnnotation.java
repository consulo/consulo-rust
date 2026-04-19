/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.utils.RsDiagnostic;

public class ErrorAnnotation {
    @NotNull
    private final TextRange myRange;
    @NotNull
    private final String myError;
    private final boolean myIsTraitError;
    @Nullable
    private final RsDiagnostic myDiagnostic;

    public ErrorAnnotation(@NotNull TextRange range, @NotNull String error) {
        this(range, error, false, null);
    }

    public ErrorAnnotation(@NotNull TextRange range, @NotNull String error, boolean isTraitError, @Nullable RsDiagnostic diagnostic) {
        this.myRange = range;
        this.myError = error;
        this.myIsTraitError = isTraitError;
        this.myDiagnostic = diagnostic;
    }

    @NotNull
    public TextRange getRange() {
        return myRange;
    }

    @NotNull
    public String getError() {
        return myError;
    }

    public boolean isTraitError() {
        return myIsTraitError;
    }

    @Nullable
    public RsDiagnostic getDiagnostic() {
        return myDiagnostic;
    }
}
