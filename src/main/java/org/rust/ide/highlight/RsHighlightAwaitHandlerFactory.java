/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.highlight;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactoryBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElementExtUtil;

public class RsHighlightAwaitHandlerFactory extends HighlightUsagesHandlerFactoryBase {
    @Nullable
    @Override
    public HighlightUsagesHandlerBase<?> createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull PsiElement target) {
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

    private static boolean isAsync(@NotNull PsiElement element) {
        return RsElementUtil.getElementType(element) == RsElementTypes.ASYNC;
    }

    private static boolean isAwait(@NotNull PsiElement element) {
        return RsElementUtil.getElementType(element) == RsElementTypes.IDENTIFIER && "await".equals(element.getText());
    }

    @Nullable
    private static PsiElement parentAsyncFunctionOrBlock(@NotNull PsiElement element) {
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

        RsHighlightAsyncAwaitHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull PsiElement parentAsyncFunctionOrBlock) {
            super(editor, file);
            this.myParentAsyncFunctionOrBlock = parentAsyncFunctionOrBlock;
        }

        @NotNull
        @Override
        public List<PsiElement> getTargets() {
            return Collections.singletonList(myParentAsyncFunctionOrBlock);
        }

        @Override
        public void selectTargets(@NotNull List<? extends PsiElement> targets, @NotNull Consumer<? super List<? extends PsiElement>> selectionConsumer) {
            selectionConsumer.consume(targets);
        }

        @Override
        public void computeUsages(@NotNull List<? extends PsiElement> targets) {
            myParentAsyncFunctionOrBlock.accept(new RsRecursiveVisitor() {
                @Override
                public void visitDotExpr(@NotNull RsDotExpr o) {
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
