/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;



public class RsReadWriteAccessDetector extends ReadWriteAccessDetector {

    @Override
    public boolean isReadWriteAccessible(PsiElement element) {
        return element instanceof RsPatBinding || element instanceof RsFieldDecl;
    }

    @Override
    public Access getReferenceAccess(PsiElement referencedElement, PsiReference reference) {
        return getExpressionAccess(reference.getElement());
    }

    @Override
    public Access getExpressionAccess(PsiElement element) {
        PsiElement expr;
        if (element instanceof RsExpr) {
            expr = element;
        } else if (element instanceof RsPath) {
            PsiElement parent = element.getParent();
            if (parent instanceof RsPathExpr) {
                expr = parent;
            } else {
                return Access.Read;
            }
        } else if (element instanceof RsFieldLookup) {
            expr = RsFieldLookupUtil.getParentDotExpr((RsFieldLookup) element);
        } else if (element instanceof RsStructLiteralField) {
            return Access.Write; // Struct literal `S { f: 0 }`
        } else {
            return Access.Read;
        }

        PsiElement context = expr.getContext();
        if (context instanceof RsBinaryExpr) {
            RsBinaryExpr binaryExpr = (RsBinaryExpr) context;
            if (binaryExpr.getLeft() == expr) {
                if (RsBinaryExprUtil.getOperatorType(binaryExpr) == AssignmentOp.EQ) {
                    return Access.Write;
                } else if (RsBinaryExprUtil.getOperatorType(binaryExpr) instanceof ArithmeticAssignmentOp) {
                    return Access.ReadWrite;
                } else {
                    return Access.Read;
                }
            } else {
                return Access.Read;
            }
        } else if (context instanceof RsUnaryExpr && RsUnaryExprUtil.isDereference((RsUnaryExpr) context)) {
            return getExpressionAccess(context);
        } else {
            return Access.Read;
        }
    }

    @Override
    public boolean isDeclarationWriteAccess(PsiElement element) {
        return false;
    }
}
