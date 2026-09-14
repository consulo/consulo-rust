/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.tree.TreeElement;
import consulo.language.psi.PsiElement;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.tree.TreeAnchorizer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.presentation.PresentationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.impl.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RsStructureViewElement implements StructureViewTreeElement {

    public static final String NAME_KEY = "name";
    public static final String VISIBILITY_KEY = "visibility";

    @Nonnull
    private final Object psiAnchor;
    private final boolean expandMacros;

    public RsStructureViewElement(@Nonnull RsElement psiArg, boolean expandMacros) {
        this.psiAnchor = TreeAnchorizer.getService().createAnchor(psiArg);
        this.expandMacros = expandMacros;
    }

    @Nullable
    private RsElement getPsi() {
        Object retrieved = TreeAnchorizer.getService().retrieveElement(psiAnchor);
        return retrieved instanceof RsElement ? (RsElement) retrieved : null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        RsElement psi = getPsi();
        if (psi instanceof Navigatable) {
            ((Navigatable) psi).navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        RsElement psi = getPsi();
        return psi instanceof Navigatable && ((Navigatable) psi).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        RsElement psi = getPsi();
        return psi instanceof Navigatable && ((Navigatable) psi).canNavigateToSource();
    }

    @Override
    @Nullable
    public RsElement getValue() {
        return getPsi();
    }

    @Override
    @Nonnull
    public ItemPresentation getPresentation() {
        RsElement psi = getPsi();
        if (psi != null) {
            return PresentationUtil.getPresentationForStructure(psi);
        }
        return new PresentationData("", null, null, null);
    }

    @Override
    @Nonnull
    public TreeElement[] getChildren() {
        List<RsElement> children = getChildElements();
        TreeElement[] result = new TreeElement[children.size()];
        for (int i = 0; i < children.size(); i++) {
            result[i] = new RsStructureViewElement(children.get(i), expandMacros);
        }
        return result;
    }

    @Nonnull
    private List<RsElement> getChildElements() {
        RsElement psi = getPsi();
        if (psi == null) {
            return Collections.emptyList();
        }

        if (psi instanceof RsEnumItem) {
            return new ArrayList<>(RsEnumItemUtil.getVariants((RsEnumItem) psi));
        }
        if (psi instanceof RsTraitOrImpl) {
            RsTraitOrImpl traitOrImpl = (RsTraitOrImpl) psi;
            if (expandMacros) {
                return new ArrayList<>(RsTraitOrImplUtil.getExpandedMembers(traitOrImpl));
            }
            else {
                return new ArrayList<>(RsTraitOrImplUtil.getExplicitMembers(traitOrImpl));
            }
        }
        if (psi instanceof RsMod) {
            return extractItems((RsItemsOwner) psi);
        }
        if (psi instanceof RsStructItem) {
            RsBlockFields blockFields = ((RsStructItem) psi).getBlockFields();
            if (blockFields != null) {
                return new ArrayList<>(blockFields.getNamedFieldDeclList());
            }
            return Collections.emptyList();
        }
        if (psi instanceof RsEnumVariant) {
            RsBlockFields blockFields = ((RsEnumVariant) psi).getBlockFields();
            if (blockFields != null) {
                return new ArrayList<>(blockFields.getNamedFieldDeclList());
            }
            return Collections.emptyList();
        }
        if (psi instanceof RsFunction) {
            RsBlock block = RsFunctionUtil.getBlock((RsFunction) psi);
            if (block != null) {
                return extractItems((RsItemsOwner) block);
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    @Nonnull
    private List<RsElement> extractItems(@Nonnull RsItemsOwner owner) {
        Iterable<RsElement> iterable = RsItemsOwnerUtil.getItemsAndMacros(owner);
        List<RsElement> items = new ArrayList<>();
        for (RsElement element : iterable) {
            items.add(element);
        }
        return extractItemsFromList(items);
    }

    @Nonnull
    private List<RsElement> extractItemsFromList(@Nonnull List<? extends RsElement> itemsAndMacros) {
        List<RsElement> result = new ArrayList<>();
        for (RsElement item : itemsAndMacros) {
            if (item instanceof RsForeignModItem) {
                List<RsElement> childItems = new ArrayList<>();
                for (PsiElement child : item.getChildren()) {
                    if (child instanceof RsElement) {
                        childItems.add((RsElement) child);
                    }
                }
                result.addAll(extractItemsFromList(childItems));
            }
            else if (item instanceof RsMacroCall) {
                if (expandMacros) {
                    result.addAll(extractItemsFromList(RsMacroCallUtil.getExpansionFlatten((RsMacroCall) item)));
                }
            }
            else if (item instanceof RsUseItem) {
                // skip
            }
            else if (item instanceof RsMacro || item instanceof RsItemElement) {
                result.add(item);
            }
        }
        return result;
    }
}
