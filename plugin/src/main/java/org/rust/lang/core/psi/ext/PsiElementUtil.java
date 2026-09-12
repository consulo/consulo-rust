/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.stub.StubElement;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.stubs.RsFileStub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTokenSets;

/**
 * Delegates to {@link PsiElementExt} where possible, and provides additional methods.
 */
public final class PsiElementUtil {

    private PsiElementUtil() {
    }

    @Nullable
    public static IElementType getElementType(@Nullable PsiElement element) {
        return getElementTypeOrNull(element);
    }

    /**
     * Element type of {@code element}, read from the stub layer whenever one is available so that the
     * AST is not parsed. Returns {@code null} for an element that has neither a stub nor a node.
     */
    @Nullable
    public static IElementType getElementTypeOrNull(@Nullable PsiElement element) {
        if (element == null) return null;
        if (element instanceof RsFile) return RsFileStub.Type;
        if (element instanceof StubBasedPsiElement) {
            StubElement<?> stub = ((StubBasedPsiElement<?>) element).getGreenStub();
            if (stub != null) {
                IElementType stubType = stub.getStubType();
                if (stubType != null) return stubType;
            }
        }
        if (element instanceof PsiFile) {
            IElementType fileType = ((PsiFile) element).getFileElementType();
            if (fileType != null) return fileType;
        }
        return PsiUtilCore.getElementType(element);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    @Nullable
    public static <T extends PsiElement> T stubAncestorStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
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
    public static <T extends PsiElement> T contextStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getContextOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T contextOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getContextOfType(element, clazz, false);
    }

    @Nullable
    public static <T extends PsiElement> T childOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz);
    }

    @Nonnull
    public static <T extends PsiElement> List<T> childrenOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
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

    @Nonnull
    public static <T extends PsiElement> List<T> stubChildrenOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        StubElement<?> stub = getGreenStubOf(element);
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
        return childrenOfType(element, clazz);
    }

    @Nonnull
    public static <T extends PsiElement> List<T> descendantsOfType(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return new ArrayList<>(PsiTreeUtil.findChildrenOfType(element, clazz));
    }

    @Nullable
    public static PsiElement stubChildOfElementType(@Nonnull PsiElement element,
                                                     @Nonnull TokenSet tokenSet,
                                                     @Nonnull Class<? extends PsiElement> clazz) {
        StubElement<?> stub = getGreenStubOf(element);
        if (stub != null) {
            for (StubElement<?> childStub : stub.getChildrenStubs()) {
                if (!tokenSet.contains(childStub.getStubType())) continue;
                PsiElement childPsi = childStub.getPsi();
                if (clazz.isInstance(childPsi)) {
                    return childPsi;
                }
            }
            return null;
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (tokenSet.contains(PsiUtilCore.getElementType(child)) && clazz.isInstance(child)) {
                return child;
            }
        }
        return null;
    }

    @Nullable
    public static PsiElement stubChildOfElementType(@Nonnull PsiElement element,
                                                     @Nonnull IElementType type) {
        StubElement<?> stub = getGreenStubOf(element);
        if (stub != null) {
            for (StubElement<?> childStub : stub.getChildrenStubs()) {
                if (childStub.getStubType() == type) {
                    return childStub.getPsi();
                }
            }
            return null;
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == type) {
                return child;
            }
        }
        return null;
    }

    /**
     * Stub backing {@code element}, or {@code null} when the element is not stub-based or its AST is
     * already loaded. Never parses the file.
     */
    @Nullable
    public static StubElement<?> getGreenStubOf(@Nonnull PsiElement element) {
        if (element instanceof PsiFileImpl) {
            return ((PsiFileImpl) element).getGreenStub();
        }
        if (element instanceof StubBasedPsiElementBase) {
            return ((StubBasedPsiElementBase<?>) element).getGreenStub();
        }
        return null;
    }

    @Nullable
    public static <T extends PsiElement> List<T> getStubDescendantsOfType(@Nonnull PsiElement element,
                                                                           @Nonnull Class<T> clazz) {
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

    @Nonnull
    public static Iterable<PsiElement> getLeftSiblings(@Nonnull PsiElement element) {
        return () -> new Iterator<PsiElement>() {
            PsiElement current = element.getPrevSibling();
            public boolean hasNext() { return current != null; }
            public PsiElement next() { PsiElement r = current; current = current.getPrevSibling(); return r; }
        };
    }

    @Nonnull
    public static Iterable<PsiElement> getRightSiblings(@Nonnull PsiElement element) {
        return () -> new Iterator<PsiElement>() {
            PsiElement current = element.getNextSibling();
            public boolean hasNext() { return current != null; }
            public PsiElement next() { PsiElement r = current; current = current.getNextSibling(); return r; }
        };
    }

    @Nullable
    public static PsiElement getStubParent(@Nonnull PsiElement element) {
        if (element instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) element).getGreenStub();
            if (stub != null) {
                StubElement<?> parentStub = stub.getParentStub();
                return parentStub != null ? parentStub.getPsi() : null;
            }
        }
        return element.getParent();
    }

    public static int getStartOffset(@Nonnull PsiElement element) {
        return element.getTextRange().getStartOffset();
    }

    public static int getEndOffset(@Nonnull PsiElement element) {
        return element.getTextRange().getEndOffset();
    }

    @Nonnull
    public static TextRange getRangeWithPrevSpace(@Nonnull PsiElement element) {
        TextRange range = element.getTextRange();
        PsiElement prev = element.getPrevSibling();
        if (prev instanceof PsiWhiteSpace) {
            return range.union(prev.getTextRange());
        }
        return range;
    }

    @Nonnull
    public static TextRange getRangeWithPrevSpace(@Nonnull PsiElement element, @Nullable PsiElement prevSibling) {
        TextRange range = element.getTextRange();
        if (prevSibling instanceof PsiWhiteSpace) {
            return range.union(prevSibling.getTextRange());
        }
        return range;
    }

    public static boolean isMultiLine(@Nonnull PsiElement element) {
        return element.getText().contains("\n");
    }

    public static boolean isKeywordLike(@Nonnull PsiElement element) {
        IElementType type = PsiUtilCore.getElementType(element);
        return RsTokenSets.RS_KEYWORDS.contains(type);
    }

    @Nullable
    public static RsFile getContainingRsFileSkippingCodeFragments(@Nonnull PsiElement element) {
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
    public static PsiFile getContextualFile(@Nonnull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) return null;
        PsiElement context = file.getContext();
        return context != null ? context.getContainingFile() : file;
    }

    public static void deleteWithSurroundingCommaAndWhitespace(@Nonnull PsiElement element) {
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
    public static RsMod commonParentMod(@Nonnull PsiElement element1, @Nonnull PsiElement element2) {
        PsiElement common = PsiTreeUtil.findCommonParent(element1, element2);
        if (common instanceof RsMod) return (RsMod) common;
        return PsiTreeUtil.getParentOfType(common, RsMod.class, false);
    }
}
