/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.ConvertFunctionToClosureIntention;
import org.rust.lang.core.psi.RsFunction;

public class ConvertFunctionToClosureFix extends RsQuickFixBase<RsFunction> {

    public ConvertFunctionToClosureFix(@NotNull RsFunction function) {
        super(function);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.function.to.closure");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsFunction element) {
        new ConvertFunctionToClosureIntention().doInvoke(project, editor, element);
    }
}
