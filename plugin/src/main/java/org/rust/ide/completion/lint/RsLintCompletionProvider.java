/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion.lint;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.icons.RsIcons;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.completion.RsCompletionProvider;
import org.rust.lang.core.completion.RsLookupElementProperties;
import org.rust.lang.core.completion.LookupElements;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.impl.RsPathUtil;

import javax.swing.*;
import java.util.List;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import consulo.ui.image.Image;

public abstract class RsLintCompletionProvider extends RsCompletionProvider {

    protected String getPrefix() {
        return "";
    }

    protected abstract List<Lint> getLints();

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        RsPath path = PsiTreeUtil.getParentOfType(parameters.getPosition(), RsPath.class);
        if (path == null) return;
        String currentPrefix = getPathPrefix(path);
        if (!currentPrefix.equals(getPrefix())) return;

        for (Lint lint : getLints()) {
            addLintToCompletion(result, lint, null);
        }
    }

    protected void addLintToCompletion(
        @Nonnull CompletionResultSet result,
        @Nonnull Lint lint,
        @Nullable String completionText
    ) {
        String text = completionText != null ? completionText : lint.getName();
        LookupElementBuilder element = LookupElementBuilder.create(text)
            .withPresentableText(lint.getName())
            .withIcon(getIcon(lint));
        result.addElement(LookupElements.toRsLookupElement(element,
            new RsLookupElementProperties(getElementKind(lint))));
    }

    @Nonnull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement()
            .withLanguage(RsLanguage.INSTANCE)
            .withParent(RsPath.class)
            .inside(
                psiElement(RsMetaItem.class).withSuperParent(2, RsPsiPattern.lintAttributeMetaItem)
            );
    }

    private consulo.ui.image.Image getIcon(Lint lint) {
        if (lint.isGroup()) {
            return RsIcons.multiple(RsIcons.ATTRIBUTE);
        } else {
            return RsIcons.ATTRIBUTE;
        }
    }

    private RsLookupElementProperties.ElementKind getElementKind(Lint lint) {
        if (lint.isGroup()) {
            return RsLookupElementProperties.ElementKind.LINT_GROUP;
        } else {
            return RsLookupElementProperties.ElementKind.LINT;
        }
    }

    protected String getPathPrefix(RsPath path) {
        RsPath qualifier = RsPathUtil.getQualifier(path);
        if (qualifier == null) {
            PsiElement coloncolon = path.getColoncolon();
            return coloncolon != null ? coloncolon.getText() : "";
        }
        String refName = qualifier.getReferenceName();
        return getPathPrefix(qualifier) + (refName != null ? refName : "") + "::";
    }
}
