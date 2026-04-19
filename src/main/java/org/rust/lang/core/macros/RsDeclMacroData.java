/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.rust.stdext.Lazy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMacroBody;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;

public class RsDeclMacroData extends RsMacroData {
    private final Lazy<RsMacroBody> myMacroBody;

    public RsDeclMacroData(@NotNull Lazy<RsMacroBody> macroBody) {
        myMacroBody = macroBody;
    }

    public RsDeclMacroData(@NotNull RsMacroDefinitionBase def) {
        this(new Lazy<RsMacroBody>() {
            private volatile RsMacroBody myValue;
            private volatile boolean myComputed = false;

            @Nullable
            @Override
            public RsMacroBody getValue() {
                if (!myComputed) {
                    synchronized (this) {
                        if (!myComputed) {
                            myValue = def.getMacroBodyStubbed();
                            myComputed = true;
                        }
                    }
                }
                return myValue;
            }
        });
    }

    @NotNull
    public Lazy<RsMacroBody> getMacroBody() {
        return myMacroBody;
    }

    public interface Lazy<T> {
        @Nullable
        T getValue();
    }
}
