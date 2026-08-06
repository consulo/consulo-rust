/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust;

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 * Message bundle for the Rust plugin. Loads strings from
 * {@code messages/RsBundle.properties} (and locale-specific variants) via
 * standard {@link ResourceBundle}. The IntelliJ {@code DynamicBundle} base
 * class is not available in Consulo; the interface here is the same subset
 * the rest of the plugin uses.
 */
public final class RsBundle {
    public static final String BUNDLE = "messages.RsBundle";
    private static final ResourceBundle INSTANCE = ResourceBundle.getBundle(BUNDLE);

    private RsBundle() {
    }

    @Nonnull
    public static String message(@PropertyKey(resourceBundle = BUNDLE) @Nonnull String key, @Nonnull Object... params) {
        String pattern = INSTANCE.getString(key);
        if (params.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, params);
    }

    @Nonnull
    public static Supplier<String> messagePointer(@PropertyKey(resourceBundle = BUNDLE) @Nonnull String key, @Nonnull Object... params) {
        return () -> message(key, params);
    }
}
