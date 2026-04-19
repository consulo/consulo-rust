/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.codeInspection.ProblemHighlightType;
import org.jetbrains.annotations.NotNull;

public class RsLintHighlightingType {

    public static final RsLintHighlightingType DEFAULT = new RsLintHighlightingType(
        ProblemHighlightType.INFORMATION,
        ProblemHighlightType.WARNING,
        ProblemHighlightType.GENERIC_ERROR,
        ProblemHighlightType.GENERIC_ERROR
    );

    public static final RsLintHighlightingType WEAK_WARNING = new RsLintHighlightingType(
        ProblemHighlightType.INFORMATION,
        ProblemHighlightType.WEAK_WARNING,
        ProblemHighlightType.GENERIC_ERROR,
        ProblemHighlightType.GENERIC_ERROR
    );

    public static final RsLintHighlightingType UNUSED_SYMBOL = new RsLintHighlightingType(
        ProblemHighlightType.INFORMATION,
        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
        ProblemHighlightType.GENERIC_ERROR,
        ProblemHighlightType.GENERIC_ERROR
    );

    public static final RsLintHighlightingType DEPRECATED = new RsLintHighlightingType(
        ProblemHighlightType.INFORMATION,
        ProblemHighlightType.LIKE_DEPRECATED,
        ProblemHighlightType.GENERIC_ERROR,
        ProblemHighlightType.GENERIC_ERROR
    );

    private final ProblemHighlightType myAllow;
    private final ProblemHighlightType myWarn;
    private final ProblemHighlightType myDeny;
    private final ProblemHighlightType myForbid;

    private RsLintHighlightingType(
        @NotNull ProblemHighlightType allow,
        @NotNull ProblemHighlightType warn,
        @NotNull ProblemHighlightType deny,
        @NotNull ProblemHighlightType forbid
    ) {
        myAllow = allow;
        myWarn = warn;
        myDeny = deny;
        myForbid = forbid;
    }

    @NotNull
    public ProblemHighlightType getAllow() {
        return myAllow;
    }

    @NotNull
    public ProblemHighlightType getWarn() {
        return myWarn;
    }

    @NotNull
    public ProblemHighlightType getDeny() {
        return myDeny;
    }

    @NotNull
    public ProblemHighlightType getForbid() {
        return myForbid;
    }
}
