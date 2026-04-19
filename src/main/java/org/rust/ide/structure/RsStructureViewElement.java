/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.TreeAnchorizer;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
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
import org.rust.lang.core.psi.ext.RsEnumItemUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsItemsOwnerUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public class RsStructureViewElement implements StructureViewTreeElement, Queryable {

    public static final String NAME_KEY = "name";
    public static final String VISIBILITY_KEY = "visibility";

    @NotNull
    private final Object psiAnchor;
    private final boolean expandMacros;

    public RsStructureViewElement(@NotNull RsElement psiArg, boolean expandMacros) {
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
    @NotNull
    public ItemPresentation getPresentation() {
        RsElement psi = getPsi();
        if (psi != null) {
            return PresentationUtil.getPresentationForStructure(psi);
        }
        return new PresentationData("", null, null, null);
    }

    @Override
    @NotNull
    public TreeElement @NotNull [] getChildren() {
        List<RsElement> children = getChildElements();
        TreeElement[] result = new TreeElement[children.size()];
        for (int i = 0; i < children.size(); i++) {
            result[i] = new RsStructureViewElement(children.get(i), expandMacros);
        }
        return result;
    }

    @NotNull
    private List<RsElement> getChildElements() {
        RsElement psi = getPsi();
        if (psi == null) return Collections.emptyList();

        if (psi instanceof RsEnumItem) {
            return new ArrayList<>(RsEnumItemUtil.getVariants((RsEnumItem) psi));
        }
        if (psi instanceof RsTraitOrImpl) {
            RsTraitOrImpl traitOrImpl = (RsTraitOrImpl) psi;
            if (expandMacros) {
                return new ArrayList<>(RsTraitOrImplUtil.getExpandedMembers(traitOrImpl));
            } else {
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

    @NotNull
    private List<RsElement> extractItems(@NotNull RsItemsOwner owner) {
        Iterable<RsElement> iterable = RsItemsOwnerUtil.getItemsAndMacros(owner);
        List<RsElement> items = new ArrayList<>();
        for (RsElement element : iterable) {
            items.add(element);
        }
        return extractItemsFromList(items);
    }

    @NotNull
    private List<RsElement> extractItemsFromList(@NotNull List<? extends RsElement> itemsAndMacros) {
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
            } else if (item instanceof RsMacroCall) {
                if (expandMacros) {
                    result.addAll(extractItemsFromList(RsMacroCallUtil.getExpansionFlatten((RsMacroCall) item)));
                }
            } else if (item instanceof RsUseItem) {
                // skip
            } else if (item instanceof RsMacro || item instanceof RsItemElement) {
                result.add(item);
            }
        }
        return result;
    }

    // Used in `RsStructureViewTest`
    @Override
    public void putInfo(@NotNull Map<? super String, ? super String> info) {
        if (!org.rust.openapiext.OpenApiUtil.isUnitTestMode()) return;

        ItemPresentation presentation = getPresentation();
        info.put(NAME_KEY, presentation.getPresentableText() != null ? presentation.getPresentableText() : "");
        Icon icon = presentation.getIcon(false);
        Icon visibilityIcon = null;
        if (icon instanceof RowIcon) {
            java.util.List<Icon> allIcons = ((RowIcon) icon).getAllIcons();
            if (allIcons.size() > 1) {
                visibilityIcon = allIcons.get(1);
            }
        }

        String visibility;
        if (visibilityIcon == PlatformIcons.PUBLIC_ICON) {
            visibility = "public";
        } else if (visibilityIcon == PlatformIcons.PRIVATE_ICON) {
            visibility = "private";
        } else if (visibilityIcon == PlatformIcons.PROTECTED_ICON) {
            visibility = "restricted";
        } else if (visibilityIcon == null) {
            visibility = "none";
        } else {
            visibility = "unknown";
        }
        info.put(VISIBILITY_KEY, visibility);
    }
}
