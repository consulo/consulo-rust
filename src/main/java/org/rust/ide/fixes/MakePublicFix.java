/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.visibility.ChangeVisibilityIntention;
import org.rust.lang.core.psi.ext.RsVisibilityOwner;

public class MakePublicFix extends RsQuickFixBase<RsVisibilityOwner> {

    @IntentionName
    private final String _text;
    private final boolean withinOneCrate;

    private MakePublicFix(@NotNull RsVisibilityOwner element, @Nullable String elementName, boolean withinOneCrate) {
        super(element);
        this._text = RsBundle.message("intention.name.make.public", elementName != null ? elementName : "");
        this.withinOneCrate = withinOneCrate;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.make.public");
    }

    @NotNull
    @Override
    public String getText() {
        return _text;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsVisibilityOwner element) {
        ChangeVisibilityIntention.makePublic(element, withinOneCrate);
    }

    @Nullable
    public static MakePublicFix createIfCompatible(
        @NotNull RsVisibilityOwner visible,
        @Nullable String elementName,
        boolean crateRestricted
    ) {
        if (!ChangeVisibilityIntention.isValidVisibilityOwner(visible)) return null;
        return new MakePublicFix(visible, elementName, crateRestricted);
    }
}
