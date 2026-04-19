/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.MacroExpansionHighlightingUtil;
import org.rust.lang.core.macros.PreparedProcMacroExpansion;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAttrProcMacroOwner;
import org.rust.lang.core.psi.ext.RsAttrProcMacroOwnerUtil;
import org.rust.stdext.CollectionExtUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a non-recursive element visitor, but if it faces a macro invocation, it accepts all elements
 * expanded from the macro. This visitor is intended to be used in {@link RsLocalInspectionTool} implementations.
 */
public abstract class RsWithMacrosInspectionVisitor extends RsVisitor {
    private final ThreadLocal<Boolean> myProcessingMacros = ThreadLocal.withInitial(() -> false);

    @Override
    public final void visitConstant(@NotNull RsConstant o) {
        visitConstant2(o);
        visitMacroExpansion(o);
    }

    public void visitConstant2(@NotNull RsConstant o) {
        super.visitConstant(o);
    }

    @Override
    public final void visitEnumItem(@NotNull RsEnumItem o) {
        visitEnumItem2(o);
        visitMacroExpansion(o);
    }

    public void visitEnumItem2(@NotNull RsEnumItem o) {
        super.visitEnumItem(o);
    }

    @Override
    public final void visitExternCrateItem(@NotNull RsExternCrateItem o) {
        visitExternCrateItem2(o);
        visitMacroExpansion(o);
    }

    public void visitExternCrateItem2(@NotNull RsExternCrateItem o) {
        super.visitExternCrateItem(o);
    }

    @Override
    public final void visitForeignModItem(@NotNull RsForeignModItem o) {
        visitForeignModItem2(o);
        visitMacroExpansion(o);
    }

    public void visitForeignModItem2(@NotNull RsForeignModItem o) {
        super.visitForeignModItem(o);
    }

    @Override
    public final void visitFunction(@NotNull RsFunction o) {
        visitFunction2(o);
        visitMacroExpansion(o);
    }

    public void visitFunction2(@NotNull RsFunction o) {
        super.visitFunction(o);
    }

    @Override
    public final void visitImplItem(@NotNull RsImplItem o) {
        visitImplItem2(o);
        visitMacroExpansion(o);
    }

    public void visitImplItem2(@NotNull RsImplItem o) {
        super.visitImplItem(o);
    }

    @Override
    public final void visitMacro(@NotNull RsMacro o) {
        visitMacro2(o);
        visitMacroExpansion(o);
    }

    public void visitMacro2(@NotNull RsMacro o) {
        super.visitMacro(o);
    }

    @Override
    public final void visitMacro2(@NotNull RsMacro2 o) {
        visitMacro22(o);
        visitMacroExpansion(o);
    }

    public void visitMacro22(@NotNull RsMacro2 o) {
        super.visitMacro2(o);
    }

    @Override
    public final void visitMacroCall(@NotNull RsMacroCall o) {
        visitMacroCall2(o);
        visitMacroExpansion(o);
    }

    public void visitMacroCall2(@NotNull RsMacroCall o) {
        super.visitMacroCall(o);
    }

    @Override
    public final void visitModItem(@NotNull RsModItem o) {
        visitModItem2(o);
        visitMacroExpansion(o);
    }

    public void visitModItem2(@NotNull RsModItem o) {
        super.visitModItem(o);
    }

    @Override
    public final void visitStructItem(@NotNull RsStructItem o) {
        visitStructItem2(o);
        visitMacroExpansion(o);
    }

    public void visitStructItem2(@NotNull RsStructItem o) {
        super.visitStructItem(o);
    }

    @Override
    public final void visitTraitAlias(@NotNull RsTraitAlias o) {
        visitTraitAlias2(o);
        visitMacroExpansion(o);
    }

    public void visitTraitAlias2(@NotNull RsTraitAlias o) {
        super.visitTraitAlias(o);
    }

    @Override
    public final void visitTraitItem(@NotNull RsTraitItem o) {
        visitTraitItem2(o);
        visitMacroExpansion(o);
    }

    public void visitTraitItem2(@NotNull RsTraitItem o) {
        super.visitTraitItem(o);
    }

    @Override
    public final void visitTypeAlias(@NotNull RsTypeAlias o) {
        visitTypeAlias2(o);
        visitMacroExpansion(o);
    }

    public void visitTypeAlias2(@NotNull RsTypeAlias o) {
        super.visitTypeAlias(o);
    }

    @Override
    public final void visitUseItem(@NotNull RsUseItem o) {
        visitUseItem2(o);
        visitMacroExpansion(o);
    }

    public void visitUseItem2(@NotNull RsUseItem o) {
        super.visitUseItem(o);
    }

    private void visitMacroExpansion(@NotNull RsAttrProcMacroOwner item) {
        if (myProcessingMacros.get()) return;

        ProcMacroAttribute<RsMetaItem> procMacroAttribute = RsAttrProcMacroOwnerUtil.getProcMacroAttribute(item);
        if (procMacroAttribute == null) return;
        if (procMacroAttribute.getAttr() == null) return;
        PreparedProcMacroExpansion preparedMacro = MacroExpansionHighlightingUtil.prepareForExpansionHighlighting(procMacroAttribute);
        if (preparedMacro == null) return;

        List<PreparedProcMacroExpansion> macros = new ArrayList<>();
        macros.add(preparedMacro);

        myProcessingMacros.set(true);
        System.out.println(Thread.currentThread().getName());

        while (!macros.isEmpty()) {
            PreparedProcMacroExpansion macro = macros.remove(macros.size() - 1);
            for (PsiElement element : macro.getElementsForErrorHighlighting()) {
                element.accept(this);
                if (element instanceof RsAttrProcMacroOwner) {
                    ProcMacroAttribute<?> pmAttr = RsAttrProcMacroOwnerUtil.getProcMacroAttribute((RsAttrProcMacroOwner) element);
                    if (pmAttr == null) continue;
                    if (pmAttr.getAttr() == null) continue;
                    PreparedProcMacroExpansion inner = MacroExpansionHighlightingUtil.prepareForExpansionHighlighting(pmAttr, macro);
                    if (inner != null) {
                        macros.add(inner);
                    }
                }
            }
        }

        myProcessingMacros.set(false);
    }
}
