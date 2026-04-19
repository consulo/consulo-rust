/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPath;

import java.util.Arrays;
import java.util.List;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

/** See also {@code RsCfgFeatureCompletionProvider} */
public class RsCfgAttributeCompletionProvider extends RsCompletionProvider {
    public static final RsCfgAttributeCompletionProvider INSTANCE = new RsCfgAttributeCompletionProvider();

    private static final List<String> NAME_OPTIONS = Arrays.asList(
        "unix",
        "windows",
        "test",
        "debug_assertions"
    );

    private static final List<String> NAME_VALUE_OPTIONS = Arrays.asList(
        "target_arch",
        "target_endian",
        "target_env",
        "target_family",
        "target_feature",
        "target_os",
        "target_pointer_width",
        "target_vendor",
        "feature",
        "panic"
    );

    private static final List<String> OPERATORS = Arrays.asList(
        "all",
        "any",
        "not"
    );

    private RsCfgAttributeCompletionProvider() {
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        for (String option : NAME_OPTIONS) {
            result.addElement(LookupElementBuilder.create(option));
        }

        for (String option : NAME_VALUE_OPTIONS) {
            result.addElement(
                LookupElementBuilder.create(option).withInsertHandler((InsertionContext ctx, com.intellij.codeInsight.lookup.LookupElement element) -> {
                    boolean alreadyHasValue = LookupElements.nextCharIs(ctx, '=');
                    if (!alreadyHasValue) {
                        ctx.getDocument().insertString(ctx.getSelectionEndOffset(), " = \"\"");
                    }
                    EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), 4);
                    if (!alreadyHasValue && "feature".equals(element.getLookupString())) {
                        AutoPopupController.getInstance(ctx.getProject()).scheduleAutoPopup(ctx.getEditor());
                    }
                })
            );
        }

        for (String operator : OPERATORS) {
            result.addElement(
                LookupElementBuilder.create(operator).withInsertHandler((ctx, item) -> {
                    if (!LookupElements.alreadyHasCallParens(ctx)) {
                        ctx.getDocument().insertString(ctx.getSelectionEndOffset(), "()");
                    }
                    EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), 1);
                })
            );
        }
    }

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement(RsElementTypes.IDENTIFIER)
            .withParent(
                psiElement(RsPath.class)
                    .inside(RsPsiPattern.anyCfgCondition)
            );
    }
}
