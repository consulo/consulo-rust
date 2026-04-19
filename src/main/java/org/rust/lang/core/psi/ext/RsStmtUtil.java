/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsExprStmtStub;

public final class RsStmtUtil {
    private RsStmtUtil() {
    }

    public static boolean getHasSemicolon(@NotNull RsExprStmt exprStmt) {
        RsExprStmtStub stub = RsPsiJavaUtil.getGreenStub(exprStmt);
        if (stub instanceof RsExprStmtStub) {
            return stub.getHasSemicolon();
        }
        return exprStmt.getSemicolon() != null;
    }

    public static boolean isTailStmt(@NotNull RsExprStmt exprStmt) {
        if (getHasSemicolon(exprStmt)) return false;
        com.intellij.psi.PsiElement parent = exprStmt.getParent();
        if (parent instanceof RsBlock) {
            return RsBlockUtil.getExpandedTailExpr((RsBlock) parent) == exprStmt.getExpr();
        }
        return false;
    }

    public static void addSemicolon(@NotNull RsExprStmt exprStmt) {
        if (getHasSemicolon(exprStmt)) return;
        exprStmt.add(new RsPsiFactory(exprStmt.getProject()).createSemicolon());
    }

    public static void addSemicolonIfNeeded(@NotNull RsExprStmt exprStmt) {
        if (needsSemicolon(exprStmt)) {
            addSemicolon(exprStmt);
        }
    }

    public static boolean needsSemicolon(@NotNull RsExprStmt exprStmt) {
        return exprNeedsSemicolon(exprStmt.getExpr());
    }

    private static boolean exprNeedsSemicolon(RsExpr expr) {
        return !(expr instanceof RsWhileExpr)
            && !(expr instanceof RsIfExpr)
            && !(expr instanceof RsForExpr)
            && !(expr instanceof RsLoopExpr)
            && !(expr instanceof RsMatchExpr);
    }
}
