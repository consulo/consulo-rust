/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.completion.CompletionUtilsUtil;
import org.rust.lang.core.completion.RsCompletionProvider;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import org.rust.toml.Util;
import org.rust.toml.resolve.CargoTomlNameResolution;
import org.toml.lang.psi.TomlFile;
import org.toml.lang.psi.TomlKeySegment;
import org.rust.lang.core.psi.impl.RsTokenSets;

/**
 * Provides completion for cargo features in Rust cfg attributes.
 */
public class RsCfgFeatureCompletionProvider extends RsCompletionProvider {
    public static final RsCfgFeatureCompletionProvider INSTANCE = new RsCfgFeatureCompletionProvider();

    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        RsElement rsElement = PsiElementExt.ancestorOrSelf(parameters.getPosition(), RsElement.class);
        if (rsElement == null) return;
        CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(rsElement);
        if (pkg == null) return;
        TomlFile pkgToml = Util.getPackageCargoTomlFile(pkg, parameters.getOriginalFile().getProject());
        if (pkgToml == null) return;

        for (TomlKeySegment feature : CargoTomlNameResolution.allFeatures(pkgToml, false)) {
            result.addElement(rustLookupElementForFeature(feature));
        }
    }

    @Nonnull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return RsPsiPattern.insideAnyCfgFlagValue("feature");
    }

    @Nonnull
    private static LookupElementBuilder rustLookupElementForFeature(@Nonnull TomlKeySegment feature) {
        return LookupElementBuilder
            .createWithSmartPointer(feature.getText(), feature)
            .withInsertHandler(new RustStringLiteralInsertionHandler());
    }

    public static class RustStringLiteralInsertionHandler implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(@Nonnull InsertionContext context, @Nonnull LookupElement item) {
            PsiElement leaf = CompletionUtilsUtil.getElementOfType(context, PsiElement.class);
            if (leaf == null) return;
            boolean hasQuotes = leaf.getParent() instanceof RsLitExpr
                && RsTokenSets.RS_ALL_STRING_LITERALS.contains(RsElementUtil.getElementType(leaf));

            if (!hasQuotes) {
                context.getDocument().insertString(context.getStartOffset(), "\"");
                context.getDocument().insertString(context.getSelectionEndOffset(), "\"");
            }
        }
    }
}
