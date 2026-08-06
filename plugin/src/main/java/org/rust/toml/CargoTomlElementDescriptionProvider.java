/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import consulo.language.editor.highlight.HighlightUsagesDescriptionLocation;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.usage.UsageViewLongNameLocation;
import consulo.usage.UsageViewNodeTextLocation;
import consulo.usage.UsageViewShortNameLocation;
import consulo.usage.UsageViewTypeLocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.toml.lang.psi.TomlKeySegment;

public class CargoTomlElementDescriptionProvider implements ElementDescriptionProvider {

    @Nullable
    @Override
    public String getElementDescription(@Nonnull PsiElement element, @Nonnull ElementDescriptionLocation location) {
        if (!Util.tomlPluginIsAbiCompatible()) return null;
        if (element instanceof TomlKeySegment) {
            TomlKeySegment keySegment = (TomlKeySegment) element;
            if (Util.isFeatureDef(keySegment)) {
                if (location instanceof UsageViewShortNameLocation) {
                    return keySegment.getName();
                } else if (location instanceof UsageViewNodeTextLocation
                    || location instanceof UsageViewLongNameLocation
                    || location instanceof HighlightUsagesDescriptionLocation) {
                    return "Cargo feature \"" + keySegment.getName() + "\"";
                } else if (location instanceof UsageViewTypeLocation) {
                    return "Cargo feature";
                } else {
                    return null;
                }
            } else {
                if (location instanceof UsageViewTypeLocation) {
                    return "Toml key";
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
}
