/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;

import com.intellij.refactoring.suggested.SuggestedRefactoringSupport;

import java.util.Objects;

public class RsParameterAdditionalData implements SuggestedRefactoringSupport.ParameterAdditionalData {

    private final boolean isPatIdent;

    public RsParameterAdditionalData(boolean isPatIdent) {
        this.isPatIdent = isPatIdent;
    }

    public boolean isPatIdent() {
        return isPatIdent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RsParameterAdditionalData that = (RsParameterAdditionalData) o;
        return isPatIdent == that.isPatIdent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isPatIdent);
    }

    @Override
    public String toString() {
        return "RsParameterAdditionalData{isPatIdent=" + isPatIdent + "}";
    }
}
