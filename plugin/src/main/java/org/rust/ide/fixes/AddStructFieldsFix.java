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
import org.rust.ide.utils.StructFieldsExpander;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.RsStructLiteral;
import consulo.localize.LocalizeValue;

/**
 * Adds the given fields to the structure defined by {@code expr}
 */
public class AddStructFieldsFix extends RsQuickFixBase<RsStructLiteral> {
    private final boolean myRecursive;

    public AddStructFieldsFix(@Nonnull RsStructLiteral structBody) {
        this(structBody, false);
    }

    public AddStructFieldsFix(@Nonnull RsStructLiteral structBody, boolean recursive) {
        super(structBody);
        this.myRecursive = recursive;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        if (myRecursive) {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.recursively.add.missing.fields"));
        } else {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.missing.fields"));
        }
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsStructLiteral element) {
        StructFieldsExpander.addMissingFieldsToStructLiteral(new RsPsiFactory(project), editor, element, myRecursive);
    }
}
