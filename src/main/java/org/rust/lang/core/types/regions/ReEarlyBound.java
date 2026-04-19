/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

import com.intellij.codeInsight.completion.CompletionUtil;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.types.KindUtil;

/**
 * Region bound in a type or fn declaration, which will be substituted 'early' -- that is,
 * at the same time when type parameters are substituted.
 */
public class ReEarlyBound extends Region {
    private final RsLifetimeParameter myParameter;

    public ReEarlyBound(RsLifetimeParameter parameter) {
        myParameter = CompletionUtil.getOriginalOrSelf(parameter);
    }

    public RsLifetimeParameter getParameter() {
        return myParameter;
    }

    @Override
    public int getFlags() {
        return KindUtil.HAS_RE_EARLY_BOUND_MASK;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReEarlyBound)) return false;
        return ((ReEarlyBound) other).myParameter.equals(myParameter);
    }

    @Override
    public int hashCode() {
        return myParameter.hashCode();
    }

    @Override
    public String toString() {
        String name = myParameter.getName();
        return name != null ? name : "<unknown>";
    }
}
