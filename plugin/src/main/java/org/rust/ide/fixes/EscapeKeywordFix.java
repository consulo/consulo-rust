/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import consulo.localize.LocalizeValue;

public class EscapeKeywordFix extends RsQuickFixBase<PsiElement> {

    private final boolean isKeyword;

    public EscapeKeywordFix(@Nonnull PsiElement element, boolean isKeyword) {
        super(element);
        this.isKeyword = isKeyword;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.escape.keyword"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(isKeyword
            ? RsBundle.message("intention.name.escape.keyword")
            : RsBundle.message("intention.name.escape.reserved.keyword"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        String name = element.getText();
        element.replace(new RsPsiFactory(project).createIdentifier("r#" + name));
    }
}
