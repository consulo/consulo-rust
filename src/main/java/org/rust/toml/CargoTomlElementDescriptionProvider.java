/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageViewLongNameLocation;
import com.intellij.usageView.UsageViewNodeTextLocation;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.toml.lang.psi.TomlKeySegment;

public class CargoTomlElementDescriptionProvider implements ElementDescriptionProvider {

    @Nullable
    @Override
    public String getElementDescription(@NotNull PsiElement element, @NotNull ElementDescriptionLocation location) {
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
