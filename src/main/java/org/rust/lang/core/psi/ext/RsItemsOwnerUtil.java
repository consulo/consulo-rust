/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.doc.psi.RsDocComment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public final class RsItemsOwnerUtil {
    private RsItemsOwnerUtil() {
    }

    @NotNull
    public static Iterable<RsElement> getItemsAndMacros(@NotNull RsItemsOwner owner) {
        // Try stub children first
        @SuppressWarnings("rawtypes")
        java.util.List stubChildren = null;
        if (owner instanceof RsFile) {
            Object stub = ((RsFile) owner).getGreenStub();
            if (stub != null) {
                stubChildren = ((StubElement<?>) stub).getChildrenStubs();
            }
        } else if (owner instanceof StubBasedPsiElementBase<?>) {
            Object stub = ((StubBasedPsiElementBase<?>) owner).getGreenStub();
            if (stub != null) {
                stubChildren = ((StubElement<?>) stub).getChildrenStubs();
            }
        }
        if (stubChildren != null) {
            @SuppressWarnings("unchecked")
            final List<StubElement<?>> sc = stubChildren;
            List<RsElement> result = new ArrayList<>();
            for (StubElement<?> s : sc) {
                PsiElement psi = s.getPsi();
                if (psi instanceof RsElement) {
                    result.add((RsElement) psi);
                }
            }
            return result;
        }
        // Fall back to PSI children - return lazy iterable
        return () -> new Iterator<RsElement>() {
            private PsiElement current = ((PsiElement) owner).getFirstChild();
            private RsElement next = advance();

            private RsElement advance() {
                while (current != null) {
                    PsiElement c = current;
                    current = current.getNextSibling();
                    if (c instanceof RsElement) {
                        return (RsElement) c;
                    }
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public RsElement next() {
                if (next == null) throw new NoSuchElementException();
                RsElement result = next;
                next = advance();
                return result;
            }
        };
    }

    public static boolean processExpandedItemsExceptImplsAndUses(@NotNull RsItemsOwner owner,
                                                                  @NotNull Predicate<RsItemElement> processor) {
        for (RsItemElement element : getExpandedItemsExceptImplsAndUses(owner)) {
            if (processor.test(element)) return true;
        }
        return false;
    }

    @NotNull
    public static List<RsItemElement> getExpandedItemsExceptImplsAndUses(@NotNull RsItemsOwner owner) {
        return getExpandedItemsCached(owner).getCfgEnabledNamedItems();
    }

    @NotNull
    public static RsCachedItems getExpandedItemsCached(@NotNull RsItemsOwner owner) {
        // Delegates to CachedValuesManager - the full implementation is complex
        return RsItemsOwnerCacheUtil.getExpandedItemsCached(owner);
    }

    @Nullable
    public static RsElement getFirstItem(@NotNull RsItemsOwner owner) {
        for (RsElement element : getItemsAndMacros(owner)) {
            if (element instanceof RsAttr) continue;
            if (element instanceof RsVis) continue;
            if (element instanceof RsDocComment) continue;
            return element;
        }
        return null;
    }
}
