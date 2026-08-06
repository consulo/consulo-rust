/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion.lint;

import java.util.Objects;

public final class Lint {
    private final String myName;
    private final boolean myIsGroup;

    public Lint(String name, boolean isGroup) {
        myName = name;
        myIsGroup = isGroup;
    }

    public String getName() {
        return myName;
    }

    public boolean isGroup() {
        return myIsGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lint lint = (Lint) o;
        return myIsGroup == lint.myIsGroup && Objects.equals(myName, lint.myName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myIsGroup);
    }

    @Override
    public String toString() {
        return "Lint(" +
            "name=" + myName +
            ", isGroup=" + myIsGroup +
            ')';
    }
}
