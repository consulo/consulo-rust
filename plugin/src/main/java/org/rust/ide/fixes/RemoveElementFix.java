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
import consulo.localize.LocalizeValue;

public class RemoveElementFix extends RsQuickFixBase<PsiElement> {

    private final String removingElementName;

    public RemoveElementFix(@Nonnull PsiElement element) {
        this(element, "`" + element.getText() + "`");
    }

    public RemoveElementFix(@Nonnull PsiElement element, @Nonnull String removingElementName) {
        super(element);
        this.removingElementName = removingElementName;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.remove"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove", removingElementName));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        element.delete();
    }
}
