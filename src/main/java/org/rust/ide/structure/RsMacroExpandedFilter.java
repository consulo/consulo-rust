/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElementUtil;

public class RsMacroExpandedFilter implements Filter {

    public static final String ID = "STRUCTURE_VIEW_MACRO_EXPANDED_FILTER";

    @Override
    @NotNull
    public ActionPresentation getPresentation() {
        return new ActionPresentationData(
            RsBundle.message("structure.view.show.macro.expanded"),
            null,
            RsIcons.MACRO_EXPANSION
        );
    }

    @Override
    @NotNull
    public String getName() {
        return ID;
    }

    @Override
    public boolean isVisible(TreeElement treeNode) {
        if (!(treeNode instanceof RsStructureViewElement)) return true;
        var psi = ((RsStructureViewElement) treeNode).getValue();
        if (psi == null) return true;
        return !RsExpandedElementUtil.isExpandedFromMacro(psi);
    }

    @Override
    public boolean isReverted() {
        return true;
    }
}
