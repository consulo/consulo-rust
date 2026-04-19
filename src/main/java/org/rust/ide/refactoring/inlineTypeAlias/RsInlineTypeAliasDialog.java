/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineTypeAlias;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInlineDialog;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.resolve.ref.RsReference;

public class RsInlineTypeAliasDialog extends RsInlineDialog {
    @NotNull
    private final RsTypeAlias myTypeAlias;
    @Nullable
    private final RsReference myReference;

    public RsInlineTypeAliasDialog(@NotNull RsTypeAlias typeAlias, @Nullable RsReference reference) {
        super(typeAlias, reference, typeAlias.getProject());
        myTypeAlias = typeAlias;
        myReference = reference;
        init();
    }

    @NotNull
    @Override
    protected String getBorderTitle() {
        return RsBundle.message("border.title.inline.type.alias");
    }

    @NotNull
    @Override
    protected String getNameLabelText() {
        String name = myTypeAlias.getName() != null ? myTypeAlias.getName() : "";
        return RsBundle.message("label.type.alias", name);
    }

    @NotNull
    @Override
    protected String getInlineAllText() {
        return RsBundle.message("radio.inline.all.remove.type.alias");
    }

    @NotNull
    @Override
    protected String getInlineThisText() {
        return RsBundle.message("radio.inline.this.only.keep.type.alias");
    }

    @Override
    protected void doAction() {
        RsInlineTypeAliasProcessor processor = new RsInlineTypeAliasProcessor(
            myProject, myTypeAlias, myReference, isInlineThisOnly()
        );
        invokeRefactoring(processor);
    }
}
