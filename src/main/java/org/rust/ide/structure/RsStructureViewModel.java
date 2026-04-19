/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.TreeAnchorizer;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Queryable;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.ui.icons.RowIcon;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.PresentationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import javax.swing.*;
import java.util.*;

public class RsStructureViewModel
    extends StructureViewModelBase
    implements StructureViewModel.ElementInfoProvider {

    public RsStructureViewModel(@Nullable Editor editor, @NotNull RsFileBase file) {
        this(editor, file, true);
    }

    public RsStructureViewModel(@Nullable Editor editor, @NotNull RsFileBase file, boolean expandMacros) {
        super(file, editor, new RsStructureViewElement(file, expandMacros));
        withSuitableClasses(
            RsImplItem.class,
            RsMacro.class,
            RsMacro2.class,
            RsFunction.class,
            RsStructOrEnumItemElement.class,
            RsTraitItem.class,
            RsModItem.class,
            RsModDeclItem.class,
            RsExternCrateItem.class,
            RsConstant.class,
            RsTypeAlias.class,
            RsTraitAlias.class,
            RsEnumVariant.class,
            RsNamedFieldDecl.class
        );
        withSorters(
            // Order of sorters matters: if both visibility and alpha sorters are active, we want
            // to sort alphabetically within each privacy category, rather than by privacy within
            // each alphabetic group, which is (mostly) a noop
            new RsVisibilitySorter(),
            Sorter.ALPHA_SORTER
        );
    }

    @Override
    @NotNull
    public Filter @NotNull [] getFilters() {
        return new Filter[]{new RsMacroExpandedFilter()};
    }

    @Override
    public boolean isAlwaysShowsPlus(@NotNull StructureViewTreeElement element) {
        return element.getValue() instanceof RsFile;
    }

    @Override
    public boolean isAlwaysLeaf(@NotNull StructureViewTreeElement element) {
        Object value = element.getValue();
        return value instanceof RsNamedFieldDecl
            || value instanceof RsModDeclItem
            || value instanceof RsConstant
            || value instanceof RsTypeAlias;
    }
}
