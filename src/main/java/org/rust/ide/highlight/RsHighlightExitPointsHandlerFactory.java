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
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.dfa.ExitPoint;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.RsBlockExprUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;

public class RsHighlightExitPointsHandlerFactory extends HighlightUsagesHandlerFactoryBase {
    @Nullable
    @Override
    public HighlightUsagesHandlerBase<?> createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull PsiElement target) {
        if (!(file instanceof RsFile)) return null;

        RsHighlightExitPointsHandler handler = createHandler(editor, file, target);
        if (handler != null) return handler;
        PsiElement prevToken = PsiTreeUtil.prevLeaf(target);
        if (prevToken == null) return null;
        return createHandler(editor, file, prevToken);
    }

    @Nullable
    private static RsHighlightExitPointsHandler createHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull PsiElement element) {
        IElementType elementType = RsElementUtil.getElementType(element);
        boolean shouldHighlightExitPoints = elementType == RsElementTypes.RETURN
            || (elementType == RsElementTypes.Q && element.getParent() instanceof RsTryExpr)
            || elementType == RsElementTypes.BREAK
            || (elementType == RsElementTypes.FN && element.getParent() instanceof RsFunction)
            || (elementType == RsElementTypes.ARROW && element.getParent() instanceof RsRetType && element.getParent().getParent() instanceof RsFunctionOrLambda);
        if (shouldHighlightExitPoints) {
            return new RsHighlightExitPointsHandler(editor, file, element);
        }
        return null;
    }

    private static class RsHighlightExitPointsHandler extends HighlightUsagesHandlerBase<PsiElement> {
        private final PsiElement myTarget;

        RsHighlightExitPointsHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull PsiElement target) {
            super(editor, file);
            this.myTarget = target;
        }

        @NotNull
        @Override
        public List<PsiElement> getTargets() {
            return Collections.singletonList(myTarget);
        }

        @Override
        public void selectTargets(@NotNull List<? extends PsiElement> targets, @NotNull Consumer<? super List<? extends PsiElement>> selectionConsumer) {
            selectionConsumer.consume(targets);
        }

        @Override
        public void computeUsages(@NotNull List<? extends PsiElement> targets) {
            List<PsiElement> usages = new ArrayList<>();
            java.util.function.Consumer<ExitPoint> sink = exitPoint -> {
                PsiElement element = null;
                if (exitPoint instanceof ExitPoint.Return) {
                    element = ((ExitPoint.Return) exitPoint).e;
                } else if (exitPoint instanceof ExitPoint.TryExpr) {
                    PsiElement e = ((ExitPoint.TryExpr) exitPoint).e;
                    element = (e instanceof RsTryExpr) ? ((RsTryExpr) e).getQ() : e;
                } else if (exitPoint instanceof ExitPoint.DivergingExpr) {
                    element = ((ExitPoint.DivergingExpr) exitPoint).e;
                } else if (exitPoint instanceof ExitPoint.TailExpr) {
                    element = ((ExitPoint.TailExpr) exitPoint).e;
                }
                if (element != null && !RsExpandedElementUtil.isExpandedFromMacro(element)) {
                    usages.add(element);
                }
            };

            PsiElement current = myTarget;
            while (current != null) {
                if (current instanceof RsBlockExpr && RsBlockExprUtil.isTry((RsBlockExpr) current) && RsElementUtil.getElementType(myTarget) == RsElementTypes.Q) {
                    break;
                } else if (current instanceof RsBlockExpr && RsBlockExprUtil.isAsync((RsBlockExpr) current)) {
                    ExitPoint.process(((RsBlockExpr) current).getBlock(), sink);
                    break;
                } else if (current instanceof RsFunction) {
                    ExitPoint.process((RsFunction) current, sink);
                    break;
                } else if (current instanceof RsLambdaExpr) {
                    ExitPoint.process((RsLambdaExpr) current, sink);
                    break;
                }
                current = current.getParent();
            }

            // highlight only if target inside exit point
            Set<PsiElement> targetAncestors = new HashSet<>();
            PsiElement ancestor = myTarget;
            while (ancestor != null) {
                targetAncestors.add(ancestor);
                ancestor = ancestor.getParent();
            }

            boolean targetInExitPoint = false;
            for (PsiElement usage : usages) {
                if (targetAncestors.contains(usage)) {
                    targetInExitPoint = true;
                    break;
                }
            }

            IElementType targetType = RsElementUtil.getElementType(myTarget);
            if (targetInExitPoint || targetType == RsElementTypes.FN || targetType == RsElementTypes.ARROW) {
                for (PsiElement usage : usages) {
                    addOccurrence(usage);
                }
            }
        }
    }
}
