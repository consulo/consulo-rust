/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import com.intellij.util.text.EditDistance;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import consulo.localize.LocalizeValue;

/**
 * Changes the text of some element to the suggested name using the provided function.
 */
public class NameSuggestionFix<T extends PsiElement> extends RsQuickFixBase<T> {

    private final String newName;

    @SafeFieldForPreview
    private final Function<String, T> elementFactory;

    public NameSuggestionFix(@Nonnull T element, @Nonnull String newName, @Nonnull Function<String, T> elementFactory) {
        super(element);
        this.newName = newName;
        this.elementFactory = elementFactory;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.change.name.element"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.change.to", newName));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull T element) {
        T newElement = elementFactory.apply(newName);
        element.replace(newElement);
    }

    @Nonnull
    public static <T extends PsiElement> List<NameSuggestionFix<T>> createApplicable(
        @Nonnull T element,
        @Nonnull String name,
        @Nonnull List<String> validNames,
        int maxDistance,
        @Nonnull Function<String, T> elementFactory
    ) {
        List<NameSuggestionFix<T>> fixes = new ArrayList<>();
        for (String validName : validNames) {
            if (!name.equals(validName) && EditDistance.levenshtein(validName, name, true) <= maxDistance) {
                fixes.add(new NameSuggestionFix<>(element, validName, elementFactory));
            }
        }
        return fixes;
    }
}
