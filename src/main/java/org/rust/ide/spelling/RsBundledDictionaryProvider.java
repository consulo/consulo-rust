/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import com.intellij.spellchecker.BundledDictionaryProvider;
import org.jetbrains.annotations.NotNull;

public class RsBundledDictionaryProvider implements BundledDictionaryProvider {
    @Override
    public @NotNull String @NotNull [] getBundledDictionaries() {
        return new String[]{"rust.dic"};
    }
}
