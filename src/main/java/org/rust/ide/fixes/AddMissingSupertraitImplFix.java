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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.*;

public class AddMissingSupertraitImplFix extends RsQuickFixBase<RsImplItem> {

    public AddMissingSupertraitImplFix(@NotNull RsImplItem implItem) {
        super(implItem);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.implement.missing.supertrait.s");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsImplItem element) {
        // This is a simplified stub; full implementation requires deep type system integration
    }
}
