/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeyValue;
import org.toml.lang.psi.TomlLiteral;
import org.toml.lang.psi.TomlPsiFactory;
import org.toml.lang.psi.TomlTable;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;

public class ExpandDependencySpecificationIntention extends RsTomlElementBaseIntentionAction<TomlKeyValue> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.expand.dependency.specification");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Nullable
    @Override
    protected TomlKeyValue findApplicableContextInternal(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (!Util.isCargoToml(element.getContainingFile())) return null;

        TomlKeyValue keyValue = PsiTreeUtil.getParentOfType(element, TomlKeyValue.class);
        if (keyValue == null) return null;
        if (!(keyValue.getParent() instanceof TomlTable)) return null;
        TomlTable table = (TomlTable) keyValue.getParent();
        if (!Util.isDependencyListHeader(table.getHeader())) return null;

        PsiElement value = keyValue.getValue();
        if (!(value instanceof TomlLiteral)) return null;
        Object kind = TomlLiteralKt.getKind((TomlLiteral) value);
        if (!(kind instanceof TomlLiteralKind.String)) return null;

        return keyValue;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull TomlKeyValue ctx) {
        String crateName = ctx.getKey().getText();
        PsiElement value = ctx.getValue();
        String crateVersion = value != null ? value.getText() : "\"\"";
        TomlPsiFactory factory = new TomlPsiFactory(project, false);
        TomlKeyValue newKeyValue = factory.createKeyValue(crateName + " = { version = " + crateVersion + " }");
        PsiElement replaced = ctx.replace(newKeyValue);
        editor.getCaretModel().moveToOffset(replaced.getTextRange().getEndOffset());
    }
}
