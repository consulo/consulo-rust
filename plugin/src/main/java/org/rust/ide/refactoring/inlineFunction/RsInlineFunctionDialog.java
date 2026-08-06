/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineFunction;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.project.Project;
import consulo.language.psi.PsiReference;
import consulo.language.editor.refactoring.RefactoringBundle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInlineDialog;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.resolve.ref.RsReference;

public class RsInlineFunctionDialog extends RsInlineDialog {
    @Nonnull
    private final RsFunction myFunction;
    @Nullable
    private final RsReference myRefElement;
    private final boolean myAllowInlineThisOnly;
    private final int myOccurrencesNumber;
    @Nonnull
    private final String myCallableType;

    public RsInlineFunctionDialog(
        @Nonnull RsFunction function,
        @Nullable RsReference refElement,
        boolean allowInlineThisOnly
    ) {
        this(function, refElement, allowInlineThisOnly, function.getProject());
    }

    public RsInlineFunctionDialog(
        @Nonnull RsFunction function,
        @Nullable RsReference refElement,
        boolean allowInlineThisOnly,
        @Nonnull Project project
    ) {
        super(function, refElement, project);
        myFunction = function;
        myRefElement = refElement;
        myAllowInlineThisOnly = allowInlineThisOnly;
        myOccurrencesNumber = 0;
        myCallableType = RsFunctionUtil.isMethod(function) ? "Method" : "Function";
        init();
    }

    @Override
    protected boolean canInlineThisOnly() {
        return myAllowInlineThisOnly;
    }

    protected boolean allowInlineAll() {
        return true;
    }

    protected boolean ignoreOccurrence(@Nonnull PsiReference reference) {
        return RsElementUtil.ancestorStrict(reference.getElement(), RsUseItem.class) == null;
    }

    @Override
    protected void doAction() {
        boolean inlineThisOnly = myAllowInlineThisOnly || isInlineThisOnly();
        boolean removeDefinition = myRbInlineAll.isSelected() && myFunction.isWritable();
        RsInlineFunctionProcessor processor = new RsInlineFunctionProcessor(
            myProject, myFunction, myRefElement, inlineThisOnly, removeDefinition
        );
        invokeRefactoring(processor);
    }

    @Nonnull
    @Override
    protected String getBorderTitle() {
        return RsBundle.message("border.title.inline", myCallableType);
    }

    @Nonnull
    @Override
    protected String getNameLabelText() {
        String name = myFunction.getName() != null ? myFunction.getName() : "";
        return RsBundle.message("label.2", myCallableType, name, getOccurrencesText(myOccurrencesNumber));
    }

    @Nonnull
    @Override
    protected String getInlineAllText() {
        String text = myFunction.isWritable()
            ? "all.invocations.and.remove.the.method"
            : "all.invocations.in.project";
        return RefactoringBundle.message(text);
    }

    @Nonnull
    @Override
    protected String getInlineThisText() {
        return RefactoringBundle.message("this.invocation.only.and.keep.the.method");
    }

    @Nullable
    protected String getKeepTheDeclarationText() {
        boolean mightHaveMultipleOccurrences = myOccurrencesNumber < 0 || myOccurrencesNumber > 1;
        if (myFunction.isWritable() && (mightHaveMultipleOccurrences || !myInvokedOnReference)) {
            return RsBundle.message("radio.inline.all.keep.method");
        }
        return null;
    }

    @Nonnull
    @Override
    protected String getHelpId() {
        return "refactoring.inlineMethod";
    }
}
