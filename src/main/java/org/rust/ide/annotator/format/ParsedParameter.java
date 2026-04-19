/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.MatchResult;

public class ParsedParameter {
    @NotNull
    private final MatchResult myCompleteMatch;
    @Nullable
    private final MatchResult myInnerContentMatch;

    public ParsedParameter(@NotNull MatchResult completeMatch, @Nullable MatchResult innerContentMatch) {
        this.myCompleteMatch = completeMatch;
        this.myInnerContentMatch = innerContentMatch;
    }

    public ParsedParameter(@NotNull MatchResult completeMatch) {
        this(completeMatch, null);
    }

    @NotNull
    public MatchResult getCompleteMatch() {
        return myCompleteMatch;
    }

    @Nullable
    public MatchResult getInnerContentMatch() {
        return myInnerContentMatch;
    }

    public int getRangeStart() {
        return myCompleteMatch.start();
    }

    public int getRangeEnd() {
        return myCompleteMatch.end() - 1;
    }
}
