/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.highlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerBase;
import consulo.language.editor.highlight.usage.HighlightUsagesHandlerFactoryBase;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import java.util.function.Consumer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.dfa.ExitPoint;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.impl.RsBlockExprUtil;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;
import org.rust.lang.core.psi.impl.*;

@ExtensionImpl
public class RsHighlightExitPointsHandlerFactory extends HighlightUsagesHandlerFactoryBase {
    @Nullable
    @Override
    public HighlightUsagesHandlerBase<?> createHighlightUsagesHandler(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull PsiElement target) {
        if (!(file instanceof RsFile)) return null;

        RsHighlightExitPointsHandler handler = createHandler(editor, file, target);
        if (handler != null) return handler;
        PsiElement prevToken = PsiTreeUtil.prevLeaf(target);
        if (prevToken == null) return null;
        return createHandler(editor, file, prevToken);
    }

    @Nullable
    private static RsHighlightExitPointsHandler createHandler(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull PsiElement element) {
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

        RsHighlightExitPointsHandler(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull PsiElement target) {
            super(editor, file);
            this.myTarget = target;
        }

        @Nonnull
        @Override
        public List<PsiElement> getTargets() {
            return Collections.singletonList(myTarget);
        }

        @Override
        public void selectTargets(@Nonnull List<PsiElement> targets, @Nonnull java.util.function.Consumer<List<PsiElement>> selectionConsumer) {
            selectionConsumer.accept(targets);
        }

        @Override
        public void computeUsages(@Nonnull List<PsiElement> targets) {
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
