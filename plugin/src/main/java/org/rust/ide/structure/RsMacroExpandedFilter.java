/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import consulo.fileEditor.structureView.tree.ActionPresentation;
import consulo.fileEditor.structureView.tree.ActionPresentationData;
import consulo.fileEditor.structureView.tree.Filter;
import consulo.fileEditor.structureView.tree.TreeElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElementUtil;

public class RsMacroExpandedFilter implements Filter {

    public static final String ID = "STRUCTURE_VIEW_MACRO_EXPANDED_FILTER";

    @Override
    @Nonnull
    public ActionPresentation getPresentation() {
        return new ActionPresentationData(
            RsBundle.message("structure.view.show.macro.expanded"),
            null,
            RsIcons.MACRO_EXPANSION
        );
    }

    @Override
    @Nonnull
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
