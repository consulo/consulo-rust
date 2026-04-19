/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ParameterMatchInfo {
    @NotNull
    private final TextRange myRange;
    @NotNull
    private final String myText;

    public ParameterMatchInfo(@NotNull TextRange range, @NotNull String text) {
        this.myRange = range;
        this.myText = text;
    }

    @NotNull
    public TextRange getRange() {
        return myRange;
    }

    @NotNull
    public String getText() {
        return myText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterMatchInfo)) return false;
        ParameterMatchInfo that = (ParameterMatchInfo) o;
        return myRange.equals(that.myRange) && myText.equals(that.myText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myRange, myText);
    }

    @Override
    public String toString() {
        return "ParameterMatchInfo{range=" + myRange + ", text='" + myText + "'}";
    }
}
