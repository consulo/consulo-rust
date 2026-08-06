/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineTypeAlias;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInlineDialog;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.resolve.ref.RsReference;

public class RsInlineTypeAliasDialog extends RsInlineDialog {
    @Nonnull
    private final RsTypeAlias myTypeAlias;
    @Nullable
    private final RsReference myReference;

    public RsInlineTypeAliasDialog(@Nonnull RsTypeAlias typeAlias, @Nullable RsReference reference) {
        super(typeAlias, reference, typeAlias.getProject());
        myTypeAlias = typeAlias;
        myReference = reference;
        init();
    }

    @Nonnull
    @Override
    protected String getBorderTitle() {
        return RsBundle.message("border.title.inline.type.alias");
    }

    @Nonnull
    @Override
    protected String getNameLabelText() {
        String name = myTypeAlias.getName() != null ? myTypeAlias.getName() : "";
        return RsBundle.message("label.type.alias", name);
    }

    @Nonnull
    @Override
    protected String getInlineAllText() {
        return RsBundle.message("radio.inline.all.remove.type.alias");
    }

    @Nonnull
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
