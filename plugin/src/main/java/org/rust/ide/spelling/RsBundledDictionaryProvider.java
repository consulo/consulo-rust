/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.spellchecker.editor.dictionary.BundledDictionaryProvider;

import java.io.InputStream;
import java.util.function.BiConsumer;

@ExtensionImpl
public class RsBundledDictionaryProvider implements BundledDictionaryProvider {
    private static final String DICTIONARY = "rust.dic";

    @Override
    public void loadDictionaries(BiConsumer<String, InputStream> consumer) {
        InputStream stream = getClass().getResourceAsStream(DICTIONARY);
        if (stream != null) {
            consumer.accept(DICTIONARY, stream);
        }
    }
}
