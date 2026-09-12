/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeyValue;
import org.toml.lang.psi.TomlLiteral;
import org.toml.lang.psi.TomlPsiFactory;
import org.toml.lang.psi.TomlTable;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralExt;
import consulo.localize.LocalizeValue;

public class ExpandDependencySpecificationIntention extends RsTomlElementBaseIntentionAction<TomlKeyValue> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.expand.dependency.specification"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nullable
    @Override
    protected TomlKeyValue findApplicableContextInternal(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        if (!Util.isCargoToml(element.getContainingFile())) return null;

        TomlKeyValue keyValue = PsiTreeUtil.getParentOfType(element, TomlKeyValue.class);
        if (keyValue == null) return null;
        if (!(keyValue.getParent() instanceof TomlTable)) return null;
        TomlTable table = (TomlTable) keyValue.getParent();
        if (!Util.isDependencyListHeader(table.getHeader())) return null;

        PsiElement value = keyValue.getValue();
        if (!(value instanceof TomlLiteral)) return null;
        Object kind = TomlLiteralExt.getKind((TomlLiteral) value);
        if (!(kind instanceof TomlLiteralKind.StringKind)) return null;

        return keyValue;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull TomlKeyValue ctx) {
        String crateName = ctx.getKey().getText();
        PsiElement value = ctx.getValue();
        String crateVersion = value != null ? value.getText() : "\"\"";
        TomlPsiFactory factory = new TomlPsiFactory(project, false);
        TomlKeyValue newKeyValue = factory.createKeyValue(crateName + " = { version = " + crateVersion + " }");
        PsiElement replaced = ctx.replace(newKeyValue);
        editor.getCaretModel().moveToOffset(replaced.getTextRange().getEndOffset());
    }
}
