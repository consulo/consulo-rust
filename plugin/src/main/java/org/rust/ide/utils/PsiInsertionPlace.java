/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.macros.RangeMap;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public interface PsiInsertionPlace {

    @Nonnull
    <T extends PsiElement> T insert(@Nonnull T psiToInsert);

    @Nonnull
    <T extends PsiElement> List<T> insertMultiple(@Nonnull List<T> psiToInsert);

    @Nonnull
    @SuppressWarnings("unchecked")
    default <T extends PsiElement> List<T> insertMultiple(@Nonnull T... psiToInsert) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, psiToInsert);
        return insertMultiple(list);
    }

    @Nullable
    static PsiInsertionPlace after(@Nonnull PsiElement anchor) {
        if (isEditableAt(anchor, PsiElementUtil.getEndOffset(anchor))) {
            return new After(anchor);
        }
        PsiElement nextSibling = PsiElementUtil.getNextNonCommentSibling(anchor);
        if (nextSibling != null && isEditableAt(nextSibling, PsiElementUtil.getStartOffset(nextSibling))) {
            return new Before(nextSibling);
        }
        return null;
    }

    @Nullable
    static PsiInsertionPlace before(@Nonnull PsiElement anchor) {
        if (isEditableAt(anchor, PsiElementUtil.getStartOffset(anchor))) {
            return new Before(anchor);
        }
        PsiElement prevSibling = PsiElementUtil.getPrevNonCommentSibling(anchor);
        if (prevSibling != null && isEditableAt(prevSibling, PsiElementUtil.getEndOffset(prevSibling))) {
            return new After(prevSibling);
        }
        return null;
    }

    @Nullable
    static PsiInsertionPlace afterLastChildIn(@Nonnull PsiElement parent) {
        PsiElement anchor = parent.getLastChild();
        if (anchor != null) {
            return after(anchor);
        }
        if (isEditableAt(parent, PsiElementUtil.getStartOffset(parent))) {
            return new Inside(parent);
        }
        return null;
    }

    @Nullable
    static PsiInsertionPlace forItemInModBefore(@Nonnull RsMod mod, @Nonnull RsElement context) {
        if (mod.equals(RsElementUtil.getContainingMod(context))) {
            return forItemBefore(context);
        }
        return forItemInMod(mod);
    }

    @Nullable
    static PsiInsertionPlace forItemInModAfter(@Nonnull RsMod mod, @Nonnull RsElement context) {
        if (mod.equals(RsElementUtil.getContainingMod(context))) {
            return forItemAfter(context);
        }
        return forItemInMod(mod);
    }

    @Nullable
    static PsiInsertionPlace forItemInMod(@Nonnull RsMod mod) {
        if (mod instanceof RsModItem) {
            PsiElement rbrace = ((RsModItem) mod).getRbrace();
            if (rbrace != null) {
                return before(rbrace);
            }
        }
        return afterLastChildIn((PsiElement) mod);
    }

    @Nullable
    static PsiInsertionPlace forItemBefore(@Nonnull RsElement context) {
        PsiElement topLevelItem = null;
        for (PsiElement ctx : RsElementUtil.getContexts(context)) {
            if (ctx instanceof RsItemElement) {
                if (!(ctx instanceof RsAbstractable) || RsAbstractableUtil.getOwner((RsAbstractable) ctx) == RsAbstractableOwner.Free) {
                    topLevelItem = ctx;
                    break;
                }
            }
        }
        if (topLevelItem == null) return null;
        return before(topLevelItem);
    }

    @Nullable
    static PsiInsertionPlace forItemAfter(@Nonnull RsElement context) {
        PsiElement nearestItem = null;
        for (PsiElement ctx : RsElementUtil.getContexts(context)) {
            if (ctx instanceof RsItemElement) {
                if (!(ctx instanceof RsAbstractable) || RsAbstractableUtil.getOwner((RsAbstractable) ctx) == RsAbstractableOwner.Free) {
                    nearestItem = ctx;
                    break;
                }
            }
        }
        if (nearestItem == null) return null;
        return after(nearestItem);
    }

    @Nullable
    static PsiInsertionPlace forTraitOrImplMember(@Nonnull RsTraitOrImpl traitOrImpl) {
        RsMembers members = traitOrImpl.getMembers();
        if (members == null) return null;
        PsiElement rbrace = members.getRbrace();
        if (rbrace != null) {
            return before(rbrace);
        }
        return afterLastChildIn(members);
    }

    @Nullable
    static PsiInsertionPlace forItemInTheScopeOf(@Nonnull RsElement context) {
        return forItemAfter(context);
    }

    static boolean isEditableAt(@Nonnull PsiElement element, int absoluteOffsetInFile) {
        if (!RsExpandedElementUtil.isExpandedFromMacro(element)) {
            return PsiModificationUtil.isWriteableRegardlessMacros(element);
        }
        if (!IntentionInMacroUtil.isMutableExpansionFile(element.getContainingFile())) {
            return false;
        }
        return false; // Editing inside a macro expansion is not supported
    }

    class Before implements PsiInsertionPlace {
        private final PsiElement myAnchor;

        public Before(@Nonnull PsiElement anchor) {
            this.myAnchor = anchor;
        }

        @Nonnull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends PsiElement> T insert(@Nonnull T psiToInsert) {
            return (T) myAnchor.getParent().addBefore(psiToInsert, myAnchor);
        }

        @Nonnull
        @Override
        public <T extends PsiElement> List<T> insertMultiple(@Nonnull List<T> psiToInsert) {
            List<T> result = new ArrayList<>();
            for (T item : psiToInsert) {
                result.add(insert(item));
            }
            return result;
        }

        @Override
        public String toString() {
            return "PsiInsertionPlace.Before(anchor = `" + myAnchor.getText() + "`)";
        }
    }

    class After implements PsiInsertionPlace {
        private final PsiElement myAnchor;

        public After(@Nonnull PsiElement anchor) {
            this.myAnchor = anchor;
        }

        @Nonnull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends PsiElement> T insert(@Nonnull T psiToInsert) {
            return (T) myAnchor.getParent().addAfter(psiToInsert, myAnchor);
        }

        @Nonnull
        @Override
        public <T extends PsiElement> List<T> insertMultiple(@Nonnull List<T> psiToInsert) {
            List<T> reversed = new ArrayList<>(psiToInsert);
            Collections.reverse(reversed);
            List<T> result = new ArrayList<>();
            for (T item : reversed) {
                result.add(insert(item));
            }
            Collections.reverse(result);
            return result;
        }

        @Override
        public String toString() {
            return "PsiInsertionPlace.After(anchor = `" + myAnchor.getText() + "`)";
        }
    }

    class Inside implements PsiInsertionPlace {
        private final PsiElement myParent;

        public Inside(@Nonnull PsiElement parent) {
            this.myParent = parent;
        }

        @Nonnull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends PsiElement> T insert(@Nonnull T psiToInsert) {
            return (T) myParent.add(psiToInsert);
        }

        @Nonnull
        @Override
        public <T extends PsiElement> List<T> insertMultiple(@Nonnull List<T> psiToInsert) {
            List<T> result = new ArrayList<>();
            for (T item : psiToInsert) {
                result.add(insert(item));
            }
            return result;
        }

        @Override
        public String toString() {
            return "PsiInsertionPlace.Inside(parent = `" + myParent.getText() + "`)";
        }
    }
}
