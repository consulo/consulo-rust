/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

import java.util.Objects;

/**
 * A concrete region naming some statically determined scope (e.g. an expression or sequence of statements) within the
 * current function.
 */
public class ReScope extends Region {
    private final Scope myScope;

    public ReScope(Scope scope) {
        myScope = scope;
    }

    public Scope getScope() {
        return myScope;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReScope reScope = (ReScope) o;
        return Objects.equals(myScope, reScope.myScope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myScope);
    }

    @Override
    public String toString() {
        return "ReScope(scope=" + myScope + ")";
    }
}
