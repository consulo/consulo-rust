/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.StubElement;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.ext.RsAttrProcMacroOwner;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.impl.RsMetaItemUtil;
import org.rust.lang.core.psi.impl.ProcMacroAttribute;
import org.rust.lang.core.psi.impl.RsCodeFragment;
import org.rust.lang.core.stubs.RsAttrProcMacroOwnerStub;
import org.rust.lang.core.stubs.RsModDeclItemStub;

public class RsModInfo {
    @Nonnull
    private final Project project;
    @Nonnull
    private final CrateDefMap defMap;
    @Nonnull
    private final ModData modData;
    @Nullable
    private final Crate crate;
    @Nullable
    private final DataPsiHelper dataPsiHelper;

    public RsModInfo(
        @Nonnull Project project,
        @Nonnull CrateDefMap defMap,
        @Nonnull ModData modData,
        @Nullable Crate crate,
        @Nullable DataPsiHelper dataPsiHelper
    ) {
        this.project = project;
        this.defMap = defMap;
        this.modData = modData;
        this.crate = crate;
        this.dataPsiHelper = dataPsiHelper;
    }

    @Nonnull
    public Project getProject() {
        return project;
    }

    @Nonnull
    public CrateDefMap getDefMap() {
        return defMap;
    }

    @Nonnull
    public ModData getModData() {
        return modData;
    }

    @Nullable
    public Crate getCrate() {
        return crate;
    }

    @Nullable
    public DataPsiHelper getDataPsiHelper() {
        return dataPsiHelper;
    }

    /**
     * The position of {@code element} among the macro definitions and calls of its crate: the index of its parent
     * (a module, a block with items, the call it was expanded from or a code fragment) followed by the number of
     * siblings before it that shift the index - module declarations and possible macro calls.
     */
    @Nullable
    public MacroIndex getMacroIndex(@Nonnull PsiElement element, @Nullable Crate elementCrate) {
        Crate crate = elementCrate != null ? elementCrate : this.crate;
        if (crate == null) return null;

        if (element instanceof RsMetaItem metaItem) {
            return getMetaItemMacroIndex(metaItem, crate);
        }

        PsiElement current = element;
        for (PsiElement parent = current.getContext(); parent != null; current = parent, parent = parent.getContext()) {
            RsPossibleMacroCall expandedOrIncludedFrom = current instanceof RsExpandedElement expanded
                ? RsExpandedElementUtil.getExpandedOrIncludedFrom(expanded)
                : null;
            MacroIndex parentIndex;
            if (expandedOrIncludedFrom != null) {
                parentIndex = getMacroIndex(expandedOrIncludedFrom, crate);
            } else if (parent instanceof RsCodeFragment) {
                parentIndex = new MacroIndex(new int[]{Integer.MAX_VALUE});
            } else if (parent instanceof RsBlock block) {
                ModData parentData = dataPsiHelper != null ? dataPsiHelper.psiToData(block) : null;
                if (parentData == null) continue;
                parentIndex = parentData.getMacroIndex();
            } else if (parent instanceof RsMod mod) {
                Integer crateId = crate.getId();
                if (crateId == null) return null;
                ModData modData = getModData(mod, crateId);
                parentIndex = modData != null ? modData.getMacroIndex() : null;
            } else {
                continue;
            }
            if (parentIndex == null) return null;
            return parentIndex.append(getMacroIndexInParent(current, parent));
        }
        return null;
    }

    @Nullable
    private MacroIndex getMetaItemMacroIndex(@Nonnull RsMetaItem metaItem, @Nonnull Crate crate) {
        if (!(RsMetaItemUtil.getOwner(metaItem) instanceof RsAttrProcMacroOwner owner)) return null;
        MacroIndex ownerIndex = getMacroIndex(owner, crate);
        if (ownerIndex == null) return null;
        for (ProcMacroAttribute<RsMetaItem> attr : ProcMacroAttribute.getProcMacroAttributeWithoutResolve(owner, null, this.crate, true, null, false)) {
            if (attr instanceof ProcMacroAttribute.Derive<RsMetaItem> derive) {
                int index = 0;
                for (RsMetaItem derived : derive.getDerives()) {
                    if (derived == metaItem) return ownerIndex.append(index);
                    index++;
                }
                return null;
            }
            if (attr instanceof ProcMacroAttribute.Attr<RsMetaItem> attribute && attribute.getAttr() == metaItem) {
                return ownerIndex;
            }
        }
        return null;
    }

    @Nullable
    private ModData getModData(@Nonnull RsMod mod, int modCrate) {
        if (dataPsiHelper != null) {
            ModData data = dataPsiHelper.psiToData(mod);
            if (data != null) return data;
        }
        CrateDefMap crateDefMap = defMap.getDefMap(modCrate);
        return crateDefMap != null ? crateDefMap.getModData(mod) : null;
    }

    private static int getMacroIndexInParent(@Nonnull PsiElement item, @Nonnull PsiElement parent) {
        StubElement<?> itemStub = item instanceof StubBasedPsiElementBase<?> based ? based.getGreenStub() : null;
        StubElement<?> parentStub = parent instanceof PsiFileImpl file ? file.getGreenStub()
            : parent instanceof StubBasedPsiElementBase<?> based ? based.getGreenStub() : null;
        int count = 0;
        if (itemStub != null && parentStub != null) {
            for (StubElement<?> child : parentStub.getChildrenStubs()) {
                if (child == itemStub) break;
                if (child instanceof RsModDeclItemStub || child instanceof RsAttrProcMacroOwnerStub) count++;
            }
        } else {
            for (PsiElement child = parent.getFirstChild(); child != null && child != item; child = child.getNextSibling()) {
                if (child instanceof RsModDeclItem || child instanceof RsAttrProcMacroOwner) count++;
            }
        }
        return count;
    }
}
