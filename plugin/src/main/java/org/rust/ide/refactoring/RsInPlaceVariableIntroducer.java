/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.codeEditor.Editor;
import consulo.project.Project;

import consulo.util.lang.Pair;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.editor.refactoring.introduce.inplace.InplaceVariableIntroducer;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public class RsInPlaceVariableIntroducer extends InplaceVariableIntroducer<PsiElement> {

    @Nonnull
    private final List<PsiElement> additionalElementsToRename;

    public RsInPlaceVariableIntroducer(
        @Nonnull PsiNamedElement elementToRename,
        @Nonnull Editor editor,
        @Nonnull Project project,
         @Nonnull String title,
        @Nonnull List<PsiElement> additionalElementsToRename
    ) {
        super(elementToRename, editor, project, title, new PsiElement[0], null);
        this.additionalElementsToRename = additionalElementsToRename;
    }

    public RsInPlaceVariableIntroducer(
        @Nonnull PsiNamedElement elementToRename,
        @Nonnull Editor editor,
        @Nonnull Project project,
         @Nonnull String title
    ) {
        this(elementToRename, editor, project, title, Collections.emptyList());
    }

    @Override
    protected void collectAdditionalElementsToRename(@Nonnull List<Pair<PsiElement, TextRange>> stringUsages) {
        for (PsiElement element : additionalElementsToRename) {
            if (element.isValid()) {
                stringUsages.add(Pair.create(element, new TextRange(0, element.getTextLength())));
            }
        }
    }
}
