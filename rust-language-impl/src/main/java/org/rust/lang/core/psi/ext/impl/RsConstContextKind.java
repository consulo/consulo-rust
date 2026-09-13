/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.*;

public abstract class RsConstContextKind {
    private RsConstContextKind() {}

    public static final class Constant extends RsConstContextKind {
        @Nonnull public final RsConstant psi;
        public Constant(@Nonnull RsConstant psi) { this.psi = psi; }
    }

    public static final class ConstFn extends RsConstContextKind {
        @Nonnull public final RsFunction psi;
        public ConstFn(@Nonnull RsFunction psi) { this.psi = psi; }
    }

    public static final class EnumVariantDiscriminant extends RsConstContextKind {
        @Nonnull public final RsEnumVariant psi;
        public EnumVariantDiscriminant(@Nonnull RsEnumVariant psi) { this.psi = psi; }
    }

    public static final RsConstContextKind ArraySize = new RsConstContextKind() {};
    public static final RsConstContextKind ConstGenericArgument = new RsConstContextKind() {};
}
