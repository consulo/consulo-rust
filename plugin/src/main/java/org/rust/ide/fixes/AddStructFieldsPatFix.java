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
import org.rust.ide.utils.StructFieldsExpander;
import org.rust.lang.core.psi.RsPatStruct;
import org.rust.lang.core.psi.RsPatTupleStruct;
import org.rust.lang.core.psi.RsPsiFactory;

public class AddStructFieldsPatFix extends RsQuickFixBase<PsiElement> {

    public AddStructFieldsPatFix(@Nonnull PsiElement element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.missing.fields"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        RsPsiFactory factory = new RsPsiFactory(project);
        if (element instanceof RsPatStruct) {
            StructFieldsExpander.expandStructFields(factory, (RsPatStruct) element);
        } else if (element instanceof RsPatTupleStruct) {
            StructFieldsExpander.expandTupleStructFields(factory, editor, (RsPatTupleStruct) element);
        }
    }
}
