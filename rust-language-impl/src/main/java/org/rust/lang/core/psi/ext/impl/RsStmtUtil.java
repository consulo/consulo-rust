/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsExprStmtStub;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.*;

public final class RsStmtUtil {
    private RsStmtUtil() {
    }

    public static boolean getHasSemicolon(@Nonnull RsExprStmt exprStmt) {
        RsExprStmtStub stub = RsPsiJavaUtil.getGreenStub(exprStmt);
        if (stub instanceof RsExprStmtStub) {
            return stub.getHasSemicolon();
        }
        return exprStmt.getSemicolon() != null;
    }

    public static boolean isTailStmt(@Nonnull RsExprStmt exprStmt) {
        if (getHasSemicolon(exprStmt)) return false;
        consulo.language.psi.PsiElement parent = exprStmt.getParent();
        if (parent instanceof RsBlock) {
            return RsBlockUtil.getExpandedTailExpr((RsBlock) parent) == exprStmt.getExpr();
        }
        return false;
    }

    public static void addSemicolon(@Nonnull RsExprStmt exprStmt) {
        if (getHasSemicolon(exprStmt)) return;
        exprStmt.add(new RsPsiFactory(exprStmt.getProject()).createSemicolon());
    }

    public static void addSemicolonIfNeeded(@Nonnull RsExprStmt exprStmt) {
        if (needsSemicolon(exprStmt)) {
            addSemicolon(exprStmt);
        }
    }

    public static boolean needsSemicolon(@Nonnull RsExprStmt exprStmt) {
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
