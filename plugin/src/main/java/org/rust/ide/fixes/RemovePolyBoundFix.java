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
import org.rust.lang.core.psi.RsPolybound;
import org.rust.lang.core.psi.ext.RsPolyboundUtil;

public class RemovePolyBoundFix extends RsQuickFixBase<RsPolybound> {

    private final String boundName;

    public RemovePolyBoundFix(@Nonnull RsPolybound bound) {
        this(bound, "`" + bound.getText() + "`");
    }

    public RemovePolyBoundFix(@Nonnull RsPolybound bound, @Nonnull String boundName) {
        super(bound);
        this.boundName = boundName;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.bound", boundName));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.remove.bound"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsPolybound bound) {
        RsPolyboundUtil.deleteWithSurroundingPlus(bound);
    }
}
