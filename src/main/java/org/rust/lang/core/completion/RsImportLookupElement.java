/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementDecorator;
import org.rust.ide.utils.imports.ImportCandidate;

import java.util.Objects;

/**
 * Provides equals and hashCode that take into account the corresponding ImportCandidate.
 */
public class RsImportLookupElement extends LookupElementDecorator<LookupElement> {
    private final ImportCandidate myCandidate;

    public RsImportLookupElement(LookupElement delegate, ImportCandidate candidate) {
        super(delegate);
        myCandidate = candidate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RsImportLookupElement that = (RsImportLookupElement) o;
        return Objects.equals(myCandidate, that.myCandidate);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + myCandidate.hashCode();
        return result;
    }
}
