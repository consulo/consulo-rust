/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.toml;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.highlight.HighlightUsagesDescriptionLocation;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.ElementDescriptionProvider;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageViewLongNameLocation;
import consulo.usage.UsageViewNodeTextLocation;
import consulo.usage.UsageViewShortNameLocation;
import consulo.usage.UsageViewTypeLocation;
import jakarta.annotation.Nonnull;
import org.toml.lang.psi.TomlKeySegment;

@ExtensionImpl
public class CargoTomlElementDescriptionProvider implements ElementDescriptionProvider {
    @Override
    @RequiredReadAction
    public LocalizeValue getElementDescription(@Nonnull PsiElement element, @Nonnull ElementDescriptionLocation location) {
        if (!Util.tomlPluginIsAbiCompatible()) {
            return LocalizeValue.empty();
        }
        if (element instanceof TomlKeySegment keySegment) {
            if (Util.isFeatureDef(keySegment)) {
                if (location instanceof UsageViewShortNameLocation) {
                    return LocalizeValue.ofNullable(keySegment.getName());
                }
                else if (location instanceof UsageViewNodeTextLocation
                    || location instanceof UsageViewLongNameLocation
                    || location instanceof HighlightUsagesDescriptionLocation) {
                    return LocalizeValue.localizeTODO("Cargo feature \"" + keySegment.getName() + "\"");
                }
                else if (location instanceof UsageViewTypeLocation) {
                    return LocalizeValue.localizeTODO("Cargo feature");
                }
            }
            else if (location instanceof UsageViewTypeLocation) {
                return LocalizeValue.localizeTODO("Toml key");
            }
        }
        return LocalizeValue.empty();
    }
}
