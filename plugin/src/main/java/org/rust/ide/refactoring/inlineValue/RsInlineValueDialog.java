/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import consulo.project.Project;
import consulo.language.editor.refactoring.RefactoringBundle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInlineDialog;

public class RsInlineValueDialog extends RsInlineDialog {
    @Nonnull
    private final InlineValueContext myContext;
    private final int myOccurrencesNumber;

    public RsInlineValueDialog(@Nonnull InlineValueContext context) {
        this(context, context.getElement().getProject());
    }

    public RsInlineValueDialog(@Nonnull InlineValueContext context, @Nonnull Project project) {
        super(context.getElement(), context.getReference(), project);
        myContext = context;
        myOccurrencesNumber = initOccurrencesNumber(context.getElement());
        init();
    }

    @Override
    protected void doAction() {
        InlineValueMode mode;
        if (isInlineThisOnly()) {
            mode = InlineValueMode.INLINE_THIS_ONLY;
        } else {
            // isKeepTheDeclaration() isn't exposed on Consulo's InlineOptionsDialog
            mode = InlineValueMode.INLINE_ALL_AND_REMOVE_ORIGINAL;
        }
        RsInlineValueProcessor processor = new RsInlineValueProcessor(myProject, myContext, mode);
        invokeRefactoring(processor);
    }

    @Nonnull
    @Override
    protected String getBorderTitle() {
        return RefactoringBundle.message("inline.field.border.title");
    }

    @Nonnull
    @Override
    protected String getNameLabelText() {
        String type = myContext.getType();
        String capitalizedType = type.substring(0, 1).toUpperCase() + type.substring(1);
        return RsBundle.message("label.", capitalizedType, myContext.getName(), getOccurrencesText(myOccurrencesNumber));
    }

    @Nonnull
    @Override
    protected String getInlineAllText() {
        String text = myContext.getElement().isWritable()
            ? "all.references.and.remove.the.local"
            : "all.invocations.in.project";
        return RefactoringBundle.message(text);
    }

    @Nonnull
    @Override
    protected String getInlineThisText() {
        return RsBundle.message("radio.inline.this.only.keep", myContext.getType());
    }

    // getKeepTheDeclarationText isn't exposed on Consulo InlineOptionsDialog

    @Nonnull
    @Override
    protected String getHelpId() {
        return "refactoring.inlineVariable";
    }
}
