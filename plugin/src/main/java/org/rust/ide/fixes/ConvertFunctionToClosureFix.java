/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.ConvertFunctionToClosureIntention;
import org.rust.lang.core.psi.RsFunction;

public class ConvertFunctionToClosureFix extends RsQuickFixBase<RsFunction> {

    public ConvertFunctionToClosureFix(@Nonnull RsFunction function) {
        super(function);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.convert.function.to.closure"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsFunction element) {
        new ConvertFunctionToClosureIntention().doInvoke(project, editor, element);
    }
}
