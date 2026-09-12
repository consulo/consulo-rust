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
import org.rust.ide.intentions.visibility.ChangeVisibilityIntention;
import org.rust.lang.core.psi.ext.RsVisibilityOwner;
import consulo.localize.LocalizeValue;

public class MakePublicFix extends RsQuickFixBase<RsVisibilityOwner> {

    
    private final String _text;
    private final boolean withinOneCrate;

    private MakePublicFix(@Nonnull RsVisibilityOwner element, @Nullable String elementName, boolean withinOneCrate) {
        super(element);
        this._text = RsBundle.message("intention.name.make.public", elementName != null ? elementName : "");
        this.withinOneCrate = withinOneCrate;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.make.public"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(_text);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsVisibilityOwner element) {
        ChangeVisibilityIntention.makePublic(element, withinOneCrate);
    }

    @Nullable
    public static MakePublicFix createIfCompatible(
        @Nonnull RsVisibilityOwner visible,
        @Nullable String elementName,
        boolean crateRestricted
    ) {
        if (!ChangeVisibilityIntention.isValidVisibilityOwner(visible)) return null;
        return new MakePublicFix(visible, elementName, crateRestricted);
    }
}
