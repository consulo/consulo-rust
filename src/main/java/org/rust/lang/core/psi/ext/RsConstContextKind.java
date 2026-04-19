/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsFunction;

public abstract class RsConstContextKind {
    private RsConstContextKind() {}

    public static final class Constant extends RsConstContextKind {
        @NotNull public final RsConstant psi;
        public Constant(@NotNull RsConstant psi) { this.psi = psi; }
    }

    public static final class ConstFn extends RsConstContextKind {
        @NotNull public final RsFunction psi;
        public ConstFn(@NotNull RsFunction psi) { this.psi = psi; }
    }

    public static final class EnumVariantDiscriminant extends RsConstContextKind {
        @NotNull public final RsEnumVariant psi;
        public EnumVariantDiscriminant(@NotNull RsEnumVariant psi) { this.psi = psi; }
    }

    public static final RsConstContextKind ArraySize = new RsConstContextKind() {};
    public static final RsConstContextKind ConstGenericArgument = new RsConstContextKind() {};
}
