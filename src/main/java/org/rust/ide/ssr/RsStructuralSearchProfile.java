/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.ssr;

import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.dupLocator.util.NodeFilter;
import com.intellij.lang.Language;
import com.intellij.lang.parser.GeneratedParserUtilBase.DummyBlock;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.structuralsearch.MalformedPatternException;
import com.intellij.structuralsearch.StructuralSearchProfile;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor;
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext;
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor;
import com.intellij.structuralsearch.impl.matcher.strategies.MatchingStrategy;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.ui.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.experiments.RsExperiments;
import org.rust.ide.template.RsContextType;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.openapiext.OpenApiUtil;

public class RsStructuralSearchProfile extends StructuralSearchProfile {

    public static final String TYPED_VAR_PREFIX = "_____";

    @Override
    public boolean isMyLanguage(@NotNull Language language) {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.SSR)) return false;
        return language == RsLanguage.INSTANCE;
    }

    @Override
    @Nullable
    public LanguageFileType getDefaultFileType(@Nullable LanguageFileType fileType) {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.SSR)) return null;
        return fileType != null ? fileType : RsFileType.INSTANCE;
    }

    @Override
    @NotNull
    public Class<? extends TemplateContextType> getTemplateContextTypeClass() {
        return RsContextType.class;
    }

    @Override
    public void compile(@NotNull PsiElement @NotNull [] elements, @NotNull GlobalCompilingVisitor globalVisitor) {
        new RsCompilingVisitor(globalVisitor).compile(elements);
    }

    @Override
    @NotNull
    public PsiElementVisitor createMatchingVisitor(@NotNull GlobalMatchingVisitor globalVisitor) {
        return new RsMatchingVisitor(globalVisitor);
    }

    @Override
    @NotNull
    public Configuration @NotNull [] getPredefinedTemplates() {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.SSR)) return new Configuration[0];
        return RsPredefinedConfigurations.createPredefinedTemplates();
    }

    @Override
    public boolean isIdentifier(@Nullable PsiElement element) {
        return element != null && element.getNode() != null
            && element.getNode().getElementType() == RsElementTypes.IDENTIFIER;
    }

    @Override
    @NotNull
    public CompiledPattern createCompiledPattern() {
        return new RsCompiledPattern();
    }

    @Override
    @NotNull
    public PsiElement @NotNull [] createPatternTree(
        @NotNull String text,
        @NotNull PatternTreeContext context,
        @NotNull LanguageFileType fileType,
        @NotNull Language language,
        @Nullable String contextId,
        @NotNull Project project,
        boolean physical
    ) {
        PsiElement[] patternTree = super.createPatternTree(text, context, fileType, language, contextId, project, physical);
        if ((patternTree.length > 0 && patternTree[0] instanceof PsiErrorElement)
            || (patternTree.length > 1 && patternTree[0] instanceof LeafPsiElement && patternTree[1] instanceof PsiErrorElement)
            || (patternTree.length > 0 && patternTree[0] instanceof DummyBlock)) {
            RsPsiFactory factory = new RsPsiFactory(project);
            PsiElement blockExpr = factory.createBlockExpr(text);
            PsiElement firstChild = blockExpr.getFirstChild();
            patternTree = firstChild.getChildren();
        }
        return patternTree;
    }

    @Override
    public void checkSearchPattern(@NotNull CompiledPattern pattern) {
        RustValidator visitor = new RustValidator();
        var nodes = pattern.getNodes();
        while (nodes.hasNext()) {
            nodes.current().accept(visitor);
            nodes.advance();
        }
        nodes.reset();
    }

    @Override
    public void checkReplacementPattern(@NotNull Project project, @NotNull ReplaceOptions options) {
        throw new MalformedPatternException(RsBundle.message("ssr.unsupported.replace.template"));
    }

    private class RustValidator extends RsRecursiveVisitor {
        private final NodeFilter invalid = element ->
            (element instanceof RsGenericDeclaration && !(element instanceof RsStructItem))
                || element instanceof RsMacro
                || element instanceof RsAlias
                || element instanceof RsStmt
                || (element instanceof RsExpr && !(element instanceof RsLitExpr))
                || element instanceof RsBlock
                || element instanceof RsReference
                || element instanceof RsPat;

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (invalid.accepts(element)) {
                throw new MalformedPatternException(RsBundle.message("ssr.unsupported.search.template"));
            }
            super.visitElement(element);
        }
    }

    private static class RsCompiledPattern extends CompiledPattern {
        RsCompiledPattern() {
            setStrategy(new MatchingStrategy() {
                @Override
                public boolean continueMatching(@Nullable PsiElement start) {
                    return start != null && start.getLanguage() == RsLanguage.INSTANCE;
                }

                @Override
                public boolean shouldSkip(@Nullable PsiElement element, @Nullable PsiElement elementToMatchWith) {
                    return false;
                }
            });
        }

        @Override
        @NotNull
        public String @NotNull [] getTypedVarPrefixes() {
            return new String[]{TYPED_VAR_PREFIX};
        }

        @Override
        public boolean isTypedVar(@NotNull String str) {
            if (str.isEmpty()) return false;
            if (str.charAt(0) == '@') {
                return str.substring(1).startsWith(TYPED_VAR_PREFIX);
            } else {
                return str.startsWith(TYPED_VAR_PREFIX);
            }
        }

        @Override
        @NotNull
        public String getTypedVarString(@NotNull PsiElement element) {
            String typedVarString = super.getTypedVarString(element);
            // TODO: implement lifetime identifier properly
            String modifiedString;
            if (element instanceof RsLifetime) {
                modifiedString = typedVarString.substring(1);
            } else {
                modifiedString = typedVarString;
            }
            if (modifiedString.startsWith("@")) {
                return modifiedString.substring(1);
            }
            return modifiedString;
        }
    }
}
