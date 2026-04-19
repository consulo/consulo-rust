/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.jetbrains.annotations.NotNull;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeySegment;

public final class CargoTomlLookupElements {
    private CargoTomlLookupElements() {}

    @NotNull
    public static LookupElementBuilder lookupElementForFeature(@NotNull TomlKeySegment feature) {
        return LookupElementBuilder
            .createWithSmartPointer(feature.getText(), feature)
            .withInsertHandler(new Util.StringLiteralInsertionHandler());
    }
}
