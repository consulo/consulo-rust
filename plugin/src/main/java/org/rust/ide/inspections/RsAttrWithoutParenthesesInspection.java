/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.impl.RsMetaItemUtil;

import java.util.Set;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsAttrWithoutParenthesesInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMetaItem(@Nonnull RsMetaItem metaItem) {
                if (!RsMetaItemUtil.isRootMetaItem(metaItem)) return;
                String name = metaItem.getName();
                if (name == null) return;
                if (ATTRIBUTES_WITH_PARENTHESES.contains(name) && metaItem.getMetaItemArgs() == null) {
                    RsDiagnostic.addToHolder(new RsDiagnostic.NoAttrParentheses(metaItem, name), holder);
                }
            }
        };
    }

    private static final Set<String> ATTRIBUTES_WITH_PARENTHESES = Set.of(
        "link", "repr", "derive", "cfg", "cfg_attr",
        "allow", "warn", "forbid", "deny", "proc_macro_derive"
    );

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.attr.without.parentheses.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
