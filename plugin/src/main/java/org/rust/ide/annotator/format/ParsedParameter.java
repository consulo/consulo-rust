/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.regex.MatchResult;

public class ParsedParameter {
    @Nonnull
    private final MatchResult myCompleteMatch;
    @Nullable
    private final MatchResult myInnerContentMatch;

    public ParsedParameter(@Nonnull MatchResult completeMatch, @Nullable MatchResult innerContentMatch) {
        this.myCompleteMatch = completeMatch;
        this.myInnerContentMatch = innerContentMatch;
    }

    public ParsedParameter(@Nonnull MatchResult completeMatch) {
        this(completeMatch, null);
    }

    @Nonnull
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
