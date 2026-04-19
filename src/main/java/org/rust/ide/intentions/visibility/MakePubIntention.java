/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.visibility;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsVisibility;
import org.rust.lang.core.psi.ext.RsVisibilityOwner;

public class MakePubIntention extends ChangeVisibilityIntention {
    @NotNull
    @Override
    public String getVisibility() {
        return "pub";
    }

    @Override
    public boolean isApplicable(@NotNull RsVisibilityOwner element) {
        return element.getVisibility() != RsVisibility.Public.INSTANCE;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        makePublic(ctx.getElement(), false);
    }
}
