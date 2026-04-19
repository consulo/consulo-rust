/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class RsTomlBundle extends DynamicBundle {
    public static final String BUNDLE = "messages.RsTomlBundle";
    public static final RsTomlBundle INSTANCE = new RsTomlBundle();

    private RsTomlBundle() {
        super(BUNDLE);
    }

    @Nls
    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
