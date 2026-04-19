/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.text.EditDistance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Changes the text of some element to the suggested name using the provided function.
 */
public class NameSuggestionFix<T extends PsiElement> extends RsQuickFixBase<T> {

    private final String newName;

    @SafeFieldForPreview
    private final Function<String, T> elementFactory;

    public NameSuggestionFix(@NotNull T element, @NotNull String newName, @NotNull Function<String, T> elementFactory) {
        super(element);
        this.newName = newName;
        this.elementFactory = elementFactory;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.change.name.element");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.change.to", newName);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull T element) {
        T newElement = elementFactory.apply(newName);
        element.replace(newElement);
    }

    @NotNull
    public static <T extends PsiElement> List<NameSuggestionFix<T>> createApplicable(
        @NotNull T element,
        @NotNull String name,
        @NotNull List<String> validNames,
        int maxDistance,
        @NotNull Function<String, T> elementFactory
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
