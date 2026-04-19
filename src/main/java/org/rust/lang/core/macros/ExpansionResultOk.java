/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.decl.DeclMacroExpander;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpansionResultOk {
    private final String myText;
    private final RangeMap myRanges;
    /** Optimization: occurrences of MACRO_DOLLAR_CRATE_IDENTIFIER */
    private final int[] myDollarCrateOccurrences;

    public ExpansionResultOk(@NotNull String text, @NotNull RangeMap ranges, @NotNull int[] dollarCrateOccurrences) {
        myText = text;
        myRanges = ranges;
        myDollarCrateOccurrences = dollarCrateOccurrences;
    }

    public ExpansionResultOk(@NotNull String text, @NotNull RangeMap ranges) {
        myText = text;
        myRanges = ranges;
        myDollarCrateOccurrences = findDollarCrateOccurrences(text);
    }

    @NotNull
    public String getText() {
        return myText;
    }

    @NotNull
    public RangeMap getRanges() {
        return myRanges;
    }

    @NotNull
    public int[] getDollarCrateOccurrences() {
        return myDollarCrateOccurrences;
    }

    @NotNull
    public ExpansionResultOk shiftDstRangesRight(int offset) {
        int[] newOccurrences = new int[myDollarCrateOccurrences.length];
        for (int i = 0; i < myDollarCrateOccurrences.length; i++) {
            newOccurrences[i] = myDollarCrateOccurrences[i] + offset;
        }
        List<MappedTextRange> newRanges = new ArrayList<>();
        for (MappedTextRange range : myRanges.getRanges()) {
            newRanges.add(range.dstShiftRight(offset));
        }
        return new ExpansionResultOk(myText, new RangeMap(newRanges), newOccurrences);
    }

    private static int[] findDollarCrateOccurrences(@NotNull String text) {
        Pattern pattern = DeclMacroExpander.MACRO_DOLLAR_CRATE_IDENTIFIER_REGEX;
        Matcher matcher = pattern.matcher(text);
        List<Integer> occurrences = new ArrayList<>();
        while (matcher.find()) {
            occurrences.add(matcher.start());
        }
        int[] result = new int[occurrences.size()];
        for (int i = 0; i < occurrences.size(); i++) {
            result[i] = occurrences.get(i);
        }
        return result;
    }
}
