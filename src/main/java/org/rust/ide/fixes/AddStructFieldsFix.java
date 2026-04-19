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
import org.rust.ide.utils.StructFieldsExpander;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsStructLiteral;

/**
 * Adds the given fields to the structure defined by {@code expr}
 */
public class AddStructFieldsFix extends RsQuickFixBase<RsStructLiteral> {
    private final boolean myRecursive;

    public AddStructFieldsFix(@NotNull RsStructLiteral structBody) {
        this(structBody, false);
    }

    public AddStructFieldsFix(@NotNull RsStructLiteral structBody, boolean recursive) {
        super(structBody);
        this.myRecursive = recursive;
    }

    @NotNull
    @Override
    public String getText() {
        if (myRecursive) {
            return RsBundle.message("intention.name.recursively.add.missing.fields");
        } else {
            return RsBundle.message("intention.name.add.missing.fields");
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsStructLiteral element) {
        StructFieldsExpander.addMissingFieldsToStructLiteral(new RsPsiFactory(project), editor, element, myRecursive);
    }
}
