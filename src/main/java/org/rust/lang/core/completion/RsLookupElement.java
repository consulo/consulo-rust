/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import org.jetbrains.annotations.NotNull;

public class RsLookupElement extends LookupElementDecorator<LookupElement> {
    private final RsLookupElementProperties props;

    public RsLookupElement(@NotNull LookupElement delegate, @NotNull RsLookupElementProperties props) {
        super(delegate);
        this.props = props;
    }

    @NotNull
    public RsLookupElementProperties getProps() {
        return props;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        if (!super.equals(other)) return false;

        RsLookupElement that = (RsLookupElement) other;
        return props.equals(that.props);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + props.hashCode();
        return result;
    }
}
