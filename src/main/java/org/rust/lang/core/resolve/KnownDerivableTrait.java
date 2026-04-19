/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitItem;

/**
 * Known derivable traits in Rust.
 */
public enum KnownDerivableTrait {
    Clone(true),
    Copy(true, Clone),
    Debug(true),
    Default(true),
    Hash(true),
    PartialEq(true),
    Eq(true, PartialEq),
    PartialOrd(true, PartialEq),
    Ord(true, Eq, PartialOrd, PartialEq),
    Serialize(false),
    Deserialize(false),
    Fail(false);

    private final boolean isStd;
    private final KnownDerivableTrait[] dependencies;

    KnownDerivableTrait(boolean isStd, KnownDerivableTrait... dependencies) {
        this.isStd = isStd;
        this.dependencies = dependencies;
    }

    @NotNull
    public KnownDerivableTrait[] getDependencies() {
        return dependencies;
    }

    @NotNull
    public KnownDerivableTrait[] getWithDependencies() {
        KnownDerivableTrait[] result = new KnownDerivableTrait[dependencies.length + 1];
        result[0] = this;
        System.arraycopy(dependencies, 0, result, 1, dependencies.length);
        return result;
    }

    public boolean isStd() {
        return isStd;
    }

    @Nullable
    public RsTraitItem findTrait(@NotNull KnownItems items) {
        switch (this) {
            case Clone: return items.getClone();
            case Copy: return items.getCopy();
            case Debug: return items.getDebug();
            case Default: return items.getDefault();
            case Hash: return items.getHash();
            case PartialEq: return items.getPartialEq();
            case Eq: return items.getEq();
            case PartialOrd: return items.getPartialOrd();
            case Ord: return items.getOrd();
            case Serialize: return items.findItem("serde::Serialize", false, RsTraitItem.class);
            case Deserialize: return items.findItem("serde::Deserialize", false, RsTraitItem.class);
            case Fail: return items.findItem("failure::Fail", false, RsTraitItem.class);
            default: return null;
        }
    }

    /** Hardcoded trait impl vs proc macro expansion usage. */
    public boolean shouldUseHardcodedTraitDerive() {
        // Don't use hardcoded impls for non-std derives if proc macro expansion is enabled
        return isStd
            || !org.rust.lang.core.macros.proc.ProcMacroApplicationService.isDeriveEnabled();
    }
}
