/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsMetaItemUtil;
import org.rust.lang.utils.RsDiagnostic;

import java.util.Set;

public class RsAttrWithoutParenthesesInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMetaItem(@NotNull RsMetaItem metaItem) {
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
}
