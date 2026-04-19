/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.Command;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class RsInPlaceVariableIntroducer extends InplaceVariableIntroducer<PsiElement> {

    @NotNull
    private final List<PsiElement> additionalElementsToRename;

    public RsInPlaceVariableIntroducer(
        @NotNull PsiNamedElement elementToRename,
        @NotNull Editor editor,
        @NotNull Project project,
        @Command @NotNull String title,
        @NotNull List<PsiElement> additionalElementsToRename
    ) {
        super(elementToRename, editor, project, title, new PsiElement[0], null);
        this.additionalElementsToRename = additionalElementsToRename;
    }

    public RsInPlaceVariableIntroducer(
        @NotNull PsiNamedElement elementToRename,
        @NotNull Editor editor,
        @NotNull Project project,
        @Command @NotNull String title
    ) {
        this(elementToRename, editor, project, title, Collections.emptyList());
    }

    @Override
    protected void collectAdditionalElementsToRename(@NotNull List<? super Pair<PsiElement, TextRange>> stringUsages) {
        for (PsiElement element : additionalElementsToRename) {
            if (element.isValid()) {
                stringUsages.add(Pair.create(element, new TextRange(0, element.getTextLength())));
            }
        }
    }
}
