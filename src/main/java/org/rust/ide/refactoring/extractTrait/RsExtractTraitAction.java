/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractTrait;

import com.intellij.lang.Language;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.refactoring.actions.ExtractSuperActionBase;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;

public class RsExtractTraitAction extends ExtractSuperActionBase {

    public RsExtractTraitAction() {
        setInjectedContext(true);
    }

    @Override
    protected boolean isAvailableForLanguage(@NotNull Language language) {
        return language == RsLanguage.INSTANCE;
    }

    @NotNull
    @Override
    protected RsExtractTraitHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
        return new RsExtractTraitHandler();
    }
}
