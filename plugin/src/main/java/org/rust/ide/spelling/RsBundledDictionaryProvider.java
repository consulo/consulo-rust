/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import com.intellij.spellchecker.BundledDictionaryProvider;
import jakarta.annotation.Nonnull;

public class RsBundledDictionaryProvider implements BundledDictionaryProvider {
    @Override
    public @Nonnull String [] getBundledDictionaries() {
        return new String[]{"rust.dic"};
    }
}
