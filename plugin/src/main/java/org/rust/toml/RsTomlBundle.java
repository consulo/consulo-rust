/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import com.intellij.DynamicBundle;

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.PropertyKey;

public class RsTomlBundle extends DynamicBundle {
    public static final String BUNDLE = "messages.RsTomlBundle";
    public static final RsTomlBundle INSTANCE = new RsTomlBundle();

    private RsTomlBundle() {
        super(BUNDLE);
    }

    
    @Nonnull
    public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
