/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import org.rust.openapiext.Testmark;

public final class MacroExpansionMarks {
    private MacroExpansionMarks() {}

    public static final Testmark FailMatchPatternByToken = new Testmark();
    public static final Testmark FailMatchPatternByExtraInput = new Testmark();
    public static final Testmark FailMatchPatternByBindingType = new Testmark();
    public static final Testmark FailMatchGroupBySeparator = new Testmark();
    public static final Testmark FailMatchGroupTooFewElements = new Testmark();
    public static final Testmark QuestionMarkGroupEnd = new Testmark();
    public static final Testmark GroupInputEnd1 = new Testmark();
    public static final Testmark GroupInputEnd2 = new Testmark();
    public static final Testmark GroupInputEnd3 = new Testmark();
    public static final Testmark GroupMatchedEmptyTT = new Testmark();
    public static final Testmark SubstMetaVarNotFound = new Testmark();
    public static final Testmark DocsLowering = new Testmark();
}
