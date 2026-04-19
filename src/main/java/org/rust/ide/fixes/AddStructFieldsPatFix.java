/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.StructFieldsExpander;
import org.rust.lang.core.psi.RsPatStruct;
import org.rust.lang.core.psi.RsPatTupleStruct;
import org.rust.lang.core.psi.RsPsiFactory;

public class AddStructFieldsPatFix extends RsQuickFixBase<PsiElement> {

    public AddStructFieldsPatFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.missing.fields");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        RsPsiFactory factory = new RsPsiFactory(project);
        if (element instanceof RsPatStruct) {
            StructFieldsExpander.expandStructFields(factory, (RsPatStruct) element);
        } else if (element instanceof RsPatTupleStruct) {
            StructFieldsExpander.expandTupleStructFields(factory, editor, (RsPatTupleStruct) element);
        }
    }
}
