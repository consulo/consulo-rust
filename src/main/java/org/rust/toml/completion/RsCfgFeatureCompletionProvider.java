/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.completion.CompletionUtilsUtil;
import org.rust.lang.core.completion.RsCompletionProvider;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.toml.Util;
import org.rust.toml.resolve.CargoTomlNameResolution;
import org.toml.lang.psi.TomlFile;
import org.toml.lang.psi.TomlKeySegment;

/**
 * Provides completion for cargo features in Rust cfg attributes.
 */
public class RsCfgFeatureCompletionProvider extends RsCompletionProvider {
    public static final RsCfgFeatureCompletionProvider INSTANCE = new RsCfgFeatureCompletionProvider();

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
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

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return RsPsiPattern.insideAnyCfgFlagValue("feature");
    }

    @NotNull
    private static LookupElementBuilder rustLookupElementForFeature(@NotNull TomlKeySegment feature) {
        return LookupElementBuilder
            .createWithSmartPointer(feature.getText(), feature)
            .withInsertHandler(new RustStringLiteralInsertionHandler());
    }

    public static class RustStringLiteralInsertionHandler implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            PsiElement leaf = CompletionUtilsUtil.getElementOfType(context, PsiElement.class);
            if (leaf == null) return;
            boolean hasQuotes = leaf.getParent() instanceof RsLitExpr
                && RsTokenType.RS_ALL_STRING_LITERALS.contains(RsElementUtil.getElementType(leaf));

            if (!hasQuotes) {
                context.getDocument().insertString(context.getStartOffset(), "\"");
                context.getDocument().insertString(context.getSelectionEndOffset(), "\"");
            }
        }
    }
}
