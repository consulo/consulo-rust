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
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;

public class RemoveSelfFix extends RsQuickFixBase<RsFunction> {

    private final String elementName;

    public RemoveSelfFix(@NotNull RsFunction function) {
        super(function);
        this.elementName = RsAbstractableUtil.getOwner(function) instanceof RsAbstractableOwner.Impl
            ? "function" : "trait";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.remove.self.from", elementName);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.family.name.remove.self.from", elementName);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsFunction element) {
        RsSelfParameter selfParam = element.getSelfParameter();
        if (selfParam != null) {
            selfParam.delete();
        }
    }
}
