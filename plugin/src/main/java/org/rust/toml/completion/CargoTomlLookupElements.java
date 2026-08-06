/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import consulo.language.editor.completion.lookup.LookupElementBuilder;
import jakarta.annotation.Nonnull;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeySegment;

public final class CargoTomlLookupElements {
    private CargoTomlLookupElements() {}

    @Nonnull
    public static LookupElementBuilder lookupElementForFeature(@Nonnull TomlKeySegment feature) {
        return LookupElementBuilder
            .createWithSmartPointer(feature.getText(), feature)
            .withInsertHandler(new Util.StringLiteralInsertionHandler());
    }
}
