/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.visibility;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsVisibility;
import org.rust.lang.core.psi.ext.RsVisibilityOwner;

public class MakePrivateIntention extends ChangeVisibilityIntention {
    @Nonnull
    @Override
    public String getVisibility() {
        return "private";
    }

    @Override
    public boolean isApplicable(@Nonnull RsVisibilityOwner element) {
        return element.getVisibility() != RsVisibility.Private.INSTANCE;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        makePrivate(ctx.getElement());
    }
}
