/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public class ParameterMatchInfo {
    @Nonnull
    private final TextRange myRange;
    @Nonnull
    private final String myText;

    public ParameterMatchInfo(@Nonnull TextRange range, @Nonnull String text) {
        this.myRange = range;
        this.myText = text;
    }

    @Nonnull
    public TextRange getRange() {
        return myRange;
    }

    @Nonnull
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(myText);
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
