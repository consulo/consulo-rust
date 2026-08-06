/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.highlight;

import consulo.language.editor.highlight.usage.HighlightUsagesHandlerBase;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerFactoryBase;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import java.util.function.Consumer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElementExtUtil;

public class RsHighlightAwaitHandlerFactory extends HighlightUsagesHandlerFactoryBase {
    @Nullable
    @Override
    public HighlightUsagesHandlerBase<?> createHighlightUsagesHandler(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull PsiElement target) {
        if (!(file instanceof RsFile)) return null;
        PsiElement parentAsyncFunctionOrBlock;
        if (isAsync(target)) {
            parentAsyncFunctionOrBlock = target.getParent();
        } else if (isAwait(target)) {
            parentAsyncFunctionOrBlock = parentAsyncFunctionOrBlock(target);
        } else {
            parentAsyncFunctionOrBlock = null;
        }
        if (parentAsyncFunctionOrBlock == null) return null;
        return new RsHighlightAsyncAwaitHandler(editor, file, parentAsyncFunctionOrBlock);
    }

    private static boolean isAsync(@Nonnull PsiElement element) {
        return RsElementUtil.getElementType(element) == RsElementTypes.ASYNC;
    }

    private static boolean isAwait(@Nonnull PsiElement element) {
        return RsElementUtil.getElementType(element) == RsElementTypes.IDENTIFIER && "await".equals(element.getText());
    }

    @Nullable
    private static PsiElement parentAsyncFunctionOrBlock(@Nonnull PsiElement element) {
        PsiElement current = element.getParent();
        while (current != null) {
            if ((current instanceof RsFunction || current instanceof RsBlockExpr)) {
                for (PsiElement child : RsElementExtUtil.getChildrenWithLeaves(current)) {
                    if (isAsync(child)) {
                        return current;
                    }
                }
            }
            current = current.getParent();
        }
        return null;
    }

    private static class RsHighlightAsyncAwaitHandler extends HighlightUsagesHandlerBase<PsiElement> {
        private final PsiElement myParentAsyncFunctionOrBlock;

        RsHighlightAsyncAwaitHandler(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull PsiElement parentAsyncFunctionOrBlock) {
            super(editor, file);
            this.myParentAsyncFunctionOrBlock = parentAsyncFunctionOrBlock;
        }

        @Nonnull
        @Override
        public List<PsiElement> getTargets() {
            return Collections.singletonList(myParentAsyncFunctionOrBlock);
        }

        @Override
        public void selectTargets(@Nonnull List<PsiElement> targets, @Nonnull java.util.function.Consumer<List<PsiElement>> selectionConsumer) {
            selectionConsumer.accept(targets);
        }

        @Override
        public void computeUsages(@Nonnull List<PsiElement> targets) {
            myParentAsyncFunctionOrBlock.accept(new RsRecursiveVisitor() {
                @Override
                public void visitDotExpr(@Nonnull RsDotExpr o) {
                    if (o.getFieldLookup() != null) {
                        PsiElement identifier = o.getFieldLookup().getIdentifier();
                        if (identifier != null && isAwait(identifier)) {
                            PsiElement parent = parentAsyncFunctionOrBlock(identifier);
                            if (parent == myParentAsyncFunctionOrBlock) {
                                addOccurrence(identifier);
                            }
                        }
                    }
                    o.getExpr().accept(this);
                }
            });
        }
    }
}
