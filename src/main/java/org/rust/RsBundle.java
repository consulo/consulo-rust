/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class RsBundle extends DynamicBundle {
    public static final String BUNDLE = "messages.RsBundle";
    public static final RsBundle INSTANCE = new RsBundle();

    private RsBundle() {
        super(BUNDLE);
    }

    @Nls
    @NotNull
    public static String message(@PropertyKey(resourceBundle = BUNDLE) @NotNull String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    @NotNull
    public static Supplier<@Nls String> messagePointer(@PropertyKey(resourceBundle = BUNDLE) @NotNull String key, @NotNull Object... params) {
        return INSTANCE.getLazyMessage(key, params);
    }
}
