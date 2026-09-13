/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementDecorator;
import jakarta.annotation.Nonnull;

public class RsLookupElement extends LookupElementDecorator<LookupElement> {
    private final RsLookupElementProperties props;

    public RsLookupElement(@Nonnull LookupElement delegate, @Nonnull RsLookupElementProperties props) {
        super(delegate);
        this.props = props;
    }

    @Nonnull
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
