/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;
import consulo.application.util.Queryable;

import consulo.ui.ex.tree.PresentationData;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.language.editor.structureView.StructureViewModelBase;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.ui.ex.tree.TreeAnchorizer;
import consulo.fileEditor.structureView.tree.Filter;
import consulo.fileEditor.structureView.tree.Sorter;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.navigation.ItemPresentation;
import consulo.codeEditor.Editor;
import consulo.navigation.Navigatable;
import consulo.language.psi.PsiElement;
import com.intellij.ui.icons.RowIcon;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.presentation.PresentationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import javax.swing.*;
import java.util.*;

public class RsStructureViewModel
    extends StructureViewModelBase
    implements StructureViewModel.ElementInfoProvider {

    public RsStructureViewModel(@Nullable Editor editor, @Nonnull RsFileBase file) {
        this(editor, file, true);
    }

    public RsStructureViewModel(@Nullable Editor editor, @Nonnull RsFileBase file, boolean expandMacros) {
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
    @Nonnull
    public Filter [] getFilters() {
        return new Filter[]{new RsMacroExpandedFilter()};
    }

    @Override
    public boolean isAlwaysShowsPlus(@Nonnull StructureViewTreeElement element) {
        return element.getValue() instanceof RsFile;
    }

    @Override
    public boolean isAlwaysLeaf(@Nonnull StructureViewTreeElement element) {
        Object value = element.getValue();
        return value instanceof RsNamedFieldDecl
            || value instanceof RsModDeclItem
            || value instanceof RsConstant
            || value instanceof RsTypeAlias;
    }
}
