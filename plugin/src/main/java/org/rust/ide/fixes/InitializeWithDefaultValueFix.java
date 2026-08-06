/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.DeclarationUtil;
import org.rust.lang.core.types.RsTypesUtil;

import java.util.Collections;

public class InitializeWithDefaultValueFix extends RsQuickFixBase<RsElement> {

    public InitializeWithDefaultValueFix(@Nonnull RsElement element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.initialize.with.default.value"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getName();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsElement element) {
        RsExpr variable = PsiTreeUtil.getParentOfType(element, RsExpr.class, false);
        if (variable == null) return;
        PsiElement decl = RsTypesUtil.getDeclaration(variable);
        if (!(decl instanceof RsPatBinding)) return;
        RsPatBinding patBinding = (RsPatBinding) decl;
        RsLetDecl declaration = PsiTreeUtil.getParentOfType(patBinding, RsLetDecl.class, false);
        if (declaration == null) return;
        PsiElement semicolon = declaration.getSemicolon();
        if (semicolon == null) return;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsExpr initExpr = new RsDefaultValueBuilder(
            KnownItems.knownItems(declaration),
            RsElementUtil.getContainingMod(declaration),
            psiFactory,
            true
        ).buildFor(RsTypesUtil.getType(patBinding), RsElementUtil.getLocalVariableVisibleBindings(element));

        if (declaration.getEq() == null) {
            declaration.addBefore(psiFactory.createEq(), semicolon);
        }
        PsiElement addedInitExpr = declaration.addBefore(initExpr, semicolon);
        if (editor != null) {
            EditorExtUtil.buildAndRunTemplate(editor, declaration, Collections.singletonList(addedInitExpr));
        }
    }

    @Nullable
    public static InitializeWithDefaultValueFix createIfCompatible(@Nonnull RsElement element) {
        RsExpr variable = PsiTreeUtil.getParentOfType(element, RsExpr.class, false);
        if (variable == null) return null;
        PsiElement decl = RsTypesUtil.getDeclaration(variable);
        if (!(decl instanceof RsPatBinding)) return null;
        RsLetDecl declaration = PsiTreeUtil.getParentOfType((RsPatBinding) decl, RsLetDecl.class, false);
        if (declaration == null) return null;
        if (!(declaration.getPat() instanceof RsPatIdent) || declaration.getSemicolon() == null) return null;
        return new InitializeWithDefaultValueFix(element);
    }
}
