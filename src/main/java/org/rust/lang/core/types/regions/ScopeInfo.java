/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

import java.util.Objects;

public class ScopeInfo {
    private final Scope myScope;
    private final int myDepth;

    public ScopeInfo(Scope scope, int depth) {
        myScope = scope;
        myDepth = depth;
    }

    public Scope getScope() {
        return myScope;
    }

    public int getDepth() {
        return myDepth;
    }

    public ScopeInfo copy() {
        return new ScopeInfo(myScope, myDepth);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopeInfo scopeInfo = (ScopeInfo) o;
        return myDepth == scopeInfo.myDepth && Objects.equals(myScope, scopeInfo.myScope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myScope, myDepth);
    }
}
