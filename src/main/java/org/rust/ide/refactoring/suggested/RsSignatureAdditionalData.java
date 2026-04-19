/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;

import com.intellij.refactoring.suggested.SuggestedRefactoringSupport;

import java.util.Objects;

public class RsSignatureAdditionalData implements SuggestedRefactoringSupport.SignatureAdditionalData {

    private final boolean isFunction;

    public RsSignatureAdditionalData(boolean isFunction) {
        this.isFunction = isFunction;
    }

    public boolean isFunction() {
        return isFunction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RsSignatureAdditionalData that = (RsSignatureAdditionalData) o;
        return isFunction == that.isFunction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isFunction);
    }

    @Override
    public String toString() {
        return "RsSignatureAdditionalData{isFunction=" + isFunction + "}";
    }
}
