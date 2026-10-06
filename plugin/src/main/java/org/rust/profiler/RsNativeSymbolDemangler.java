/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.profiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.nativeDev.profiler.NativeSymbolDemangler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.utils.RsDemangler;

@ExtensionImpl
public class RsNativeSymbolDemangler implements NativeSymbolDemangler {
    @Nullable
    @Override
    public String demangle(@Nonnull String symbol) {
        RsDemangler.Demangle demangled = RsDemangler.INSTANCE.tryDemangle(symbol);
        return demangled == null ? null : demangled.format(true);
    }
}
