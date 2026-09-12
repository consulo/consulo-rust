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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.*;
import consulo.localize.LocalizeValue;

public class AddMissingSupertraitImplFix extends RsQuickFixBase<RsImplItem> {

    public AddMissingSupertraitImplFix(@Nonnull RsImplItem implItem) {
        super(implItem);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.implement.missing.supertrait.s"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsImplItem element) {
        // This is a simplified stub; full implementation requires deep type system integration
    }
}
