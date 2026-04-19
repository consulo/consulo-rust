/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Delegates to {@link PsiElementExt} where possible, and provides additional methods.
 */
public final class PsiElementUtil {

    private PsiElementUtil() {
    }

    @NotNull
    public static IElementType getElementType(@NotNull PsiElement element) {
        return PsiUtilCore.getElementType(element);
    }

    @Nullable
    public static IElementType getElementTypeOrNull(@Nullable PsiElement element) {
        return element != null ? PsiUtilCore.getElementType(element) : null;
    }

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    @Nullable
    public static <T extends PsiElement> T stubAncestorStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        PsiElement parent = getStubParent(element);
        while (parent != null) {
            if (clazz.isInstance(parent)) {
                return clazz.cast(parent);
            }
            parent = getStubParent(parent);
        }
        return null;
    }

    @Nullable
    public static <T extends PsiElement> T contextStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getContextOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T contextOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getContextOfType(element, clazz, false);
    }

    @Nullable
    public static <T extends PsiElement> T childOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    @NotNull
    public static <T extends PsiElement> List<T> childrenOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        Collection<T> found = PsiTreeUtil.findChildrenOfType(element, clazz);
        // Only direct children
        List<T> result = new ArrayList<>();
        for (T child : found) {
            if (child.getParent() == element) {
                result.add(child);
            }
        }
        return result;
    }

    @NotNull
    public static <T extends PsiElement> List<T> stubChildrenOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        if (element instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) element).getGreenStub();
            if (stub != null) {
                List<T> result = new ArrayList<>();
                for (StubElement<?> childStub : stub.getChildrenStubs()) {
                    PsiElement childPsi = childStub.getPsi();
                    if (clazz.isInstance(childPsi)) {
                        result.add(clazz.cast(childPsi));
                    }
                }
                return result;
            }
        }
        return childrenOfType(element, clazz);
    }

    @NotNull
    public static <T extends PsiElement> List<T> descendantsOfType(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, clazz));
    }

    @Nullable
    public static PsiElement stubChildOfElementType(@NotNull PsiElement element,
                                                     @NotNull TokenSet tokenSet,
                                                     @NotNull Class<? extends PsiElement> clazz) {
        if (element instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) element).getGreenStub();
            if (stub != null) {
                for (StubElement<?> childStub : stub.getChildrenStubs()) {
                    PsiElement childPsi = childStub.getPsi();
                    if (clazz.isInstance(childPsi)) {
                        return childPsi;
                    }
                }
                return null;
            }
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (tokenSet.contains(PsiUtilCore.getElementType(child)) && clazz.isInstance(child)) {
                return child;
            }
        }
        return null;
    }

    @Nullable
    public static PsiElement stubChildOfElementType(@NotNull PsiElement element,
                                                     @NotNull IElementType type) {
        if (element instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) element).getGreenStub();
            if (stub != null) {
                for (StubElement<?> childStub : stub.getChildrenStubs()) {
                    if (childStub.getStubType() == type) {
                        return childStub.getPsi();
                    }
                }
                return null;
            }
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == type) {
                return child;
            }
        }
        return null;
    }

    @Nullable
    public static <T extends PsiElement> List<T> getStubDescendantsOfType(@NotNull PsiElement element,
                                                                           @NotNull Class<T> clazz) {
        return stubChildrenOfType(element, clazz);
    }

    @Nullable
    public static PsiElement getPrevNonCommentSibling(@Nullable PsiElement element) {
        return PsiTreeUtil.skipWhitespacesAndCommentsBackward(element);
    }

    @Nullable
    public static PsiElement getNextNonCommentSibling(@Nullable PsiElement element) {
        return PsiTreeUtil.skipWhitespacesAndCommentsForward(element);
    }

    @NotNull
    public static Iterable<PsiElement> getLeftSiblings(@NotNull PsiElement element) {
        return () -> new Iterator<PsiElement>() {
            PsiElement current = element.getPrevSibling();
            public boolean hasNext() { return current != null; }
            public PsiElement next() { PsiElement r = current; current = current.getPrevSibling(); return r; }
        };
    }

    @NotNull
    public static Iterable<PsiElement> getRightSiblings(@NotNull PsiElement element) {
        return () -> new Iterator<PsiElement>() {
            PsiElement current = element.getNextSibling();
            public boolean hasNext() { return current != null; }
            public PsiElement next() { PsiElement r = current; current = current.getNextSibling(); return r; }
        };
    }

    @Nullable
    public static PsiElement getStubParent(@NotNull PsiElement element) {
        if (element instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) element).getGreenStub();
            if (stub != null) {
                StubElement<?> parentStub = stub.getParentStub();
                return parentStub != null ? parentStub.getPsi() : null;
            }
        }
        return element.getParent();
    }

    public static int getStartOffset(@NotNull PsiElement element) {
        return element.getTextRange().getStartOffset();
    }

    public static int getEndOffset(@NotNull PsiElement element) {
        return element.getTextRange().getEndOffset();
    }

    @NotNull
    public static TextRange getRangeWithPrevSpace(@NotNull PsiElement element) {
        TextRange range = element.getTextRange();
        PsiElement prev = element.getPrevSibling();
        if (prev instanceof PsiWhiteSpace) {
            return range.union(prev.getTextRange());
        }
        return range;
    }

    @NotNull
    public static TextRange getRangeWithPrevSpace(@NotNull PsiElement element, @Nullable PsiElement prevSibling) {
        TextRange range = element.getTextRange();
        if (prevSibling instanceof PsiWhiteSpace) {
            return range.union(prevSibling.getTextRange());
        }
        return range;
    }

    public static boolean isMultiLine(@NotNull PsiElement element) {
        return element.getText().contains("\n");
    }

    public static boolean isKeywordLike(@NotNull PsiElement element) {
        IElementType type = PsiUtilCore.getElementType(element);
        return org.rust.lang.core.psi.RsTokenType.RS_KEYWORDS.contains(type);
    }

    @Nullable
    public static RsFile getContainingRsFileSkippingCodeFragments(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        while (file != null) {
            if (file instanceof RsFile) {
                return (RsFile) file;
            }
            PsiElement context = file.getContext();
            file = context != null ? context.getContainingFile() : null;
        }
        return null;
    }

    @Nullable
    public static PsiFile getContextualFile(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) return null;
        PsiElement context = file.getContext();
        return context != null ? context.getContainingFile() : file;
    }

    public static void deleteWithSurroundingCommaAndWhitespace(@NotNull PsiElement element) {
        PsiElement next = element.getNextSibling();
        if (next instanceof PsiWhiteSpace) {
            PsiElement afterWhitespace = next.getNextSibling();
            if (afterWhitespace != null && PsiUtilCore.getElementType(afterWhitespace) == org.rust.lang.core.psi.RsElementTypes.COMMA) {
                afterWhitespace.delete();
            }
            next.delete();
        } else if (next != null && PsiUtilCore.getElementType(next) == org.rust.lang.core.psi.RsElementTypes.COMMA) {
            PsiElement afterComma = next.getNextSibling();
            if (afterComma instanceof PsiWhiteSpace) {
                afterComma.delete();
            }
            next.delete();
        } else {
            PsiElement prev = element.getPrevSibling();
            if (prev instanceof PsiWhiteSpace) {
                PsiElement beforeWhitespace = prev.getPrevSibling();
                if (beforeWhitespace != null && PsiUtilCore.getElementType(beforeWhitespace) == org.rust.lang.core.psi.RsElementTypes.COMMA) {
                    beforeWhitespace.delete();
                }
                prev.delete();
            } else if (prev != null && PsiUtilCore.getElementType(prev) == org.rust.lang.core.psi.RsElementTypes.COMMA) {
                PsiElement beforeComma = prev.getPrevSibling();
                if (beforeComma instanceof PsiWhiteSpace) {
                    beforeComma.delete();
                }
                prev.delete();
            }
        }
        element.delete();
    }

    @Nullable
    public static RsMod commonParentMod(@NotNull PsiElement element1, @NotNull PsiElement element2) {
        PsiElement common = PsiTreeUtil.findCommonParent(element1, element2);
        if (common instanceof RsMod) return (RsMod) common;
        return PsiTreeUtil.getParentOfType(common, RsMod.class, false);
    }
}
