/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

public class SelfConvention {
    private final String myPrefix;
    private final Collection<SelfSignature.BasicSelfSignature> mySelfSignatures;
    private final String myPostfix;

    public SelfConvention(
        @NotNull String prefix,
        @NotNull Collection<SelfSignature.BasicSelfSignature> selfSignatures,
        @Nullable String postfix
    ) {
        myPrefix = prefix;
        mySelfSignatures = selfSignatures;
        myPostfix = postfix;
    }

    public SelfConvention(
        @NotNull String prefix,
        @NotNull Collection<SelfSignature.BasicSelfSignature> selfSignatures
    ) {
        this(prefix, selfSignatures, null);
    }

    @NotNull
    public String getPrefix() {
        return myPrefix;
    }

    @NotNull
    public Collection<SelfSignature.BasicSelfSignature> getSelfSignatures() {
        return mySelfSignatures;
    }

    @Nullable
    public String getPostfix() {
        return myPostfix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelfConvention)) return false;
        SelfConvention that = (SelfConvention) o;
        return myPrefix.equals(that.myPrefix) && mySelfSignatures.equals(that.mySelfSignatures) && Objects.equals(myPostfix, that.myPostfix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myPrefix, mySelfSignatures, myPostfix);
    }

    @Override
    public String toString() {
        return "SelfConvention(prefix=" + myPrefix + ", selfSignatures=" + mySelfSignatures + ", postfix=" + myPostfix + ")";
    }
}
