/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractTrait;

import consulo.language.Language;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import com.intellij.refactoring.actions.ExtractSuperActionBase;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;

public class RsExtractTraitAction extends ExtractSuperActionBase {

    public RsExtractTraitAction() {
        setInjectedContext(true);
    }

    @Override
    protected boolean isAvailableForLanguage(@Nonnull Language language) {
        return language == RsLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    protected RsExtractTraitHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider) {
        return new RsExtractTraitHandler();
    }

    @Override
    public void actionPerformed(@Nonnull consulo.ui.ex.action.AnActionEvent e) {
        // TODO: wire extract-super dispatch via RefactoringActionHandler
    }
}
