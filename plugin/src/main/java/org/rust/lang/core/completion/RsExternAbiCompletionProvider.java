/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
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

    @Nonnull
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
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
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
