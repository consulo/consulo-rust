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
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsVisRestriction;

public class FixVisRestriction extends RsQuickFixBase<RsVisRestriction> {

    public FixVisRestriction(@NotNull RsVisRestriction visRestriction) {
        super(visRestriction);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.fix.visibility.restriction");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsVisRestriction element) {
        element.addBefore(new RsPsiFactory(project).createIn(), element.getPath());
    }
}
