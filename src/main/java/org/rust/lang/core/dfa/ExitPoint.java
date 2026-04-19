/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.TyNever;

import java.util.function.Consumer;

public abstract class ExitPoint {

    private ExitPoint() {
    }

    public static class Return extends ExitPoint {
        @NotNull
        public final RsRetExpr e;

        public Return(@NotNull RsRetExpr e) {
            this.e = e;
        }
    }

    /** {@code ?} or {@code try!} */
    public static class TryExpr extends ExitPoint {
        @NotNull
        public final RsExpr e;

        public TryExpr(@NotNull RsExpr e) {
            this.e = e;
        }
    }

    public static class DivergingExpr extends ExitPoint {
        @NotNull
        public final RsExpr e;

        public DivergingExpr(@NotNull RsExpr e) {
            this.e = e;
        }
    }

    public static class TailExpr extends ExitPoint {
        @NotNull
        public final RsExpr e;

        public TailExpr(@NotNull RsExpr e) {
            this.e = e;
        }
    }

    /**
     * This is not a real exit point. Used in {@code org.rust.ide.inspections.RsExtraSemicolonInspection}
     */
    public static class InvalidTailStatement extends ExitPoint {
        @NotNull
        public final RsExprStmt stmt;

        public InvalidTailStatement(@NotNull RsExprStmt stmt) {
            this.stmt = stmt;
        }
    }

    public static void process(@NotNull RsFunctionOrLambda fn, @NotNull Consumer<ExitPoint> sink) {
        if (fn instanceof RsFunction) {
            RsBlock block = RsFunctionUtil.getBlock((RsFunction) fn);
            if (block == null) return;
            process(block, sink);
        } else if (fn instanceof RsLambdaExpr) {
            RsExpr expr = ((RsLambdaExpr) fn).getExpr();
            if (expr == null) return;
            if (expr instanceof RsBlockExpr) {
                process(((RsBlockExpr) expr).getBlock(), sink);
            } else {
                processTailExpr(expr, sink);
            }
        }
    }

    public static void process(@NotNull RsBlock block, @NotNull Consumer<ExitPoint> sink) {
        block.acceptChildren(new ExitPointVisitor(sink));
        processBlockTailExprs(block, sink);
    }

    private static void processBlockTailExprs(@NotNull RsBlock block, @NotNull Consumer<ExitPoint> sink) {
        RsBlockUtil.ExpandedStmtsAndTailExpr expanded = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
        RsExpr tailExpr = expanded.getTailExpr();
        if (tailExpr != null) {
            processTailExpr(tailExpr, sink);
        } else {
            java.util.List<? extends RsStmt> stmts = expanded.getStmts();
            if (!stmts.isEmpty()) {
                RsStmt lastStmt = stmts.get(stmts.size() - 1);
                if (lastStmt instanceof RsExprStmt) {
                    RsExprStmt exprStmt = (RsExprStmt) lastStmt;
                    if (RsStmtUtil.getHasSemicolon(exprStmt) && !(RsExprUtil.getType(exprStmt.getExpr()) instanceof TyNever)) {
                        sink.accept(new InvalidTailStatement(exprStmt));
                    }
                }
            }
        }
    }

    private static void processTailExpr(@NotNull RsExpr expr, @NotNull Consumer<ExitPoint> sink) {
        if (expr instanceof RsBlockExpr) {
            RsBlockExpr blockExpr = (RsBlockExpr) expr;
            if (RsBlockExprUtil.isTry(blockExpr) || RsBlockExprUtil.isAsync(blockExpr)) {
                sink.accept(new TailExpr(expr));
                return;
            }
            RsLabelDecl labelDecl = blockExpr.getLabelDecl();
            String label = labelDecl != null ? labelDecl.getName() : null;
            if (label != null) {
                RsExprUtil.processBreakExprs(blockExpr, label, true, it -> {
                    sink.accept(new TailExpr(it));
                });
            }
            processBlockTailExprs(blockExpr.getBlock(), sink);
        } else if (expr instanceof RsIfExpr) {
            RsIfExpr ifExpr = (RsIfExpr) expr;
            RsBlock ifBlock = ifExpr.getBlock();
            if (ifBlock != null) {
                processBlockTailExprs(ifBlock, sink);
            }
            RsElseBranch elseBranch = ifExpr.getElseBranch();
            if (elseBranch == null) return;
            RsBlock elseBranchBlock = elseBranch.getBlock();
            if (elseBranchBlock != null) {
                processBlockTailExprs(elseBranchBlock, sink);
            }
            RsIfExpr elseIf = elseBranch.getIfExpr();
            if (elseIf != null) {
                processTailExpr(elseIf, sink);
            }
        } else if (expr instanceof RsMatchExpr) {
            RsMatchExpr matchExpr = (RsMatchExpr) expr;
            RsMatchBody matchBody = matchExpr.getMatchBody();
            if (matchBody != null) {
                for (RsMatchArm arm : matchBody.getMatchArmList()) {
                    RsExpr armExpr = arm.getExpr();
                    if (armExpr != null) {
                        processTailExpr(armExpr, sink);
                    }
                }
            }
        } else if (expr instanceof RsLoopExpr) {
            RsLoopExpr loopExpr = (RsLoopExpr) expr;
            RsLabelDecl labelDecl = loopExpr.getLabelDecl();
            String label = labelDecl != null ? labelDecl.getName() : null;
            RsExprUtil.processBreakExprs(loopExpr, label, false, it -> {
                sink.accept(new TailExpr(it));
            });
        } else {
            sink.accept(new TailExpr(expr));
        }
    }

    private static void markNeverTypeAsExit(@NotNull RsExpr expr, @NotNull Consumer<ExitPoint> sink) {
        if (RsExprUtil.getType(expr) instanceof TyNever) {
            sink.accept(new DivergingExpr(expr));
        }
    }

    private static class ExitPointVisitor extends RsRecursiveVisitor {
        @NotNull
        private final Consumer<ExitPoint> sink;
        private int inTry = 0;

        ExitPointVisitor(@NotNull Consumer<ExitPoint> sink) {
            this.sink = sink;
        }

        @Override
        public void visitLambdaExpr(@NotNull RsLambdaExpr lambdaExpr) {
            // do not recurse into lambdas
        }

        @Override
        public void visitFunction(@NotNull RsFunction function) {
            // do not recurse into functions
        }

        @Override
        public void visitBlockExpr(@NotNull RsBlockExpr blockExpr) {
            if (RsBlockExprUtil.isTry(blockExpr)) {
                inTry++;
                super.visitBlockExpr(blockExpr);
                inTry--;
            } else if (!RsBlockExprUtil.isAsync(blockExpr)) {
                super.visitBlockExpr(blockExpr);
            }
        }

        @Override
        public void visitRetExpr(@NotNull RsRetExpr retExpr) {
            sink.accept(new Return(retExpr));
        }

        @Override
        public void visitTryExpr(@NotNull RsTryExpr tryExpr) {
            tryExpr.getExpr().acceptChildren(this);
            if (inTry == 0) {
                sink.accept(new ExitPoint.TryExpr(tryExpr));
            }
        }

        @Override
        public void visitMacroExpr(@NotNull RsMacroExpr macroExpr) {
            super.visitMacroExpr(macroExpr);
            RsMacroCall macroCall = macroExpr.getMacroCall();
            if (RsMacroCallUtil.isStdTryMacro(macroCall) && inTry == 0) {
                sink.accept(new ExitPoint.TryExpr(macroExpr));
            }
            markNeverTypeAsExit(macroExpr, sink);
        }

        @Override
        public void visitCallExpr(@NotNull RsCallExpr callExpr) {
            super.visitCallExpr(callExpr);
            markNeverTypeAsExit(callExpr, sink);
        }

        @Override
        public void visitDotExpr(@NotNull RsDotExpr dotExpr) {
            super.visitDotExpr(dotExpr);
            markNeverTypeAsExit(dotExpr, sink);
        }
    }
}
