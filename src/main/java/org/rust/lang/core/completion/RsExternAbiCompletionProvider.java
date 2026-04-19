/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.fixes.AddFeatureAttributeFix;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsExternAbi;
import org.rust.lang.utils.RsDiagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.rust.lang.core.PsiElementPatternExtUtil.or;
import static org.rust.lang.core.PsiElementPatternExtUtil.withSuperParent;

public class RsExternAbiCompletionProvider extends RsCompletionProvider {
    public static final RsExternAbiCompletionProvider INSTANCE = new RsExternAbiCompletionProvider();

    private RsExternAbiCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return withSuperParent(
            or(
                PlatformPatterns.psiElement(RsElementTypes.STRING_LITERAL),
                PlatformPatterns.psiElement(RsElementTypes.RAW_STRING_LITERAL)
            ),
            2,
            RsExternAbi.class
        );
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiFile file = parameters.getOriginalFile();
        List<LookupElementBuilder> lookups = new ArrayList<>();
        for (Map.Entry<String, CompilerFeature> entry : RsDiagnostic.SUPPORTED_CALLING_CONVENTIONS.entrySet()) {
            String conventionName = entry.getKey();
            CompilerFeature compilerFeature = entry.getValue();
            FeatureAvailability availability = compilerFeature != null ? compilerFeature.availability(file) : FeatureAvailability.AVAILABLE;
            if (availability != FeatureAvailability.AVAILABLE && availability != FeatureAvailability.CAN_BE_ADDED) continue;
            LookupElementBuilder builder = LookupElementBuilder.create(conventionName);
            if (compilerFeature != null) {
                CompilerFeature finalFeature = compilerFeature;
                builder = builder.withInsertHandler((ctx, item) -> {
                    if (finalFeature.availability(file) == FeatureAvailability.CAN_BE_ADDED) {
                        AddFeatureAttributeFix.addFeatureAttribute(file.getProject(), file, finalFeature.getName());
                    }
                });
            }
            lookups.add(builder);
        }
        result.addAllElements(lookups);
    }
}
