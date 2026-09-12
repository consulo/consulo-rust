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
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;
import consulo.localize.LocalizeValue;

public class RemoveSelfFix extends RsQuickFixBase<RsFunction> {

    private final String elementName;

    public RemoveSelfFix(@Nonnull RsFunction function) {
        super(function);
        this.elementName = RsAbstractableUtil.getOwner(function) instanceof RsAbstractableOwner.Impl
            ? "function" : "trait";
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.remove.self.from", elementName));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.remove.self.from", elementName));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsFunction element) {
        RsSelfParameter selfParam = element.getSelfParameter();
        if (selfParam != null) {
            selfParam.delete();
        }
    }
}
