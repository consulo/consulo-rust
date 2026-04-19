/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import com.intellij.openapi.project.Project;
import com.intellij.refactoring.RefactoringBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInlineDialog;

public class RsInlineValueDialog extends RsInlineDialog {
    @NotNull
    private final InlineValueContext myContext;
    private final int myOccurrencesNumber;

    public RsInlineValueDialog(@NotNull InlineValueContext context) {
        this(context, context.getElement().getProject());
    }

    public RsInlineValueDialog(@NotNull InlineValueContext context, @NotNull Project project) {
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
        } else if (isKeepTheDeclaration()) {
            mode = InlineValueMode.INLINE_ALL_AND_KEEP_ORIGINAL;
        } else {
            mode = InlineValueMode.INLINE_ALL_AND_REMOVE_ORIGINAL;
        }
        RsInlineValueProcessor processor = new RsInlineValueProcessor(myProject, myContext, mode);
        invokeRefactoring(processor);
    }

    @NotNull
    @Override
    protected String getBorderTitle() {
        return RefactoringBundle.message("inline.field.border.title");
    }

    @NotNull
    @Override
    protected String getNameLabelText() {
        String type = myContext.getType();
        String capitalizedType = type.substring(0, 1).toUpperCase() + type.substring(1);
        return RsBundle.message("label.", capitalizedType, myContext.getName(), getOccurrencesText(myOccurrencesNumber));
    }

    @NotNull
    @Override
    protected String getInlineAllText() {
        String text = myContext.getElement().isWritable()
            ? "all.references.and.remove.the.local"
            : "all.invocations.in.project";
        return RefactoringBundle.message(text);
    }

    @NotNull
    @Override
    protected String getInlineThisText() {
        return RsBundle.message("radio.inline.this.only.keep", myContext.getType());
    }

    @Nullable
    @Override
    protected String getKeepTheDeclarationText() {
        if (myContext.getElement().isWritable()) {
            return RsBundle.message("radio.inline.all.references.keep", myContext.getType());
        }
        return super.getKeepTheDeclarationText();
    }

    @NotNull
    @Override
    protected String getHelpId() {
        return "refactoring.inlineVariable";
    }
}
