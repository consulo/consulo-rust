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
import org.rust.lang.core.psi.RsPolybound;
import org.rust.lang.core.psi.ext.RsPolyboundUtil;

public class RemovePolyBoundFix extends RsQuickFixBase<RsPolybound> {

    private final String boundName;

    public RemovePolyBoundFix(@NotNull RsPolybound bound) {
        this(bound, "`" + bound.getText() + "`");
    }

    public RemovePolyBoundFix(@NotNull RsPolybound bound, @NotNull String boundName) {
        super(bound);
        this.boundName = boundName;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.bound", boundName);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.remove.bound");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsPolybound bound) {
        RsPolyboundUtil.deleteWithSurroundingPlus(bound);
    }
}
