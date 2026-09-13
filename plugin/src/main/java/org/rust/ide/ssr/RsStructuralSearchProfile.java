/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.ssr;

import consulo.language.editor.template.context.TemplateContextType;
import consulo.language.duplicateAnalysis.util.NodeFilter;
import consulo.language.Language;
import consulo.language.impl.parser.GeneratedParserUtilBase.DummyBlock;
import consulo.language.file.LanguageFileType;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiErrorElement;
import consulo.language.impl.psi.LeafPsiElement;
import com.intellij.structuralsearch.MalformedPatternException;
import com.intellij.structuralsearch.StructuralSearchProfile;
import com.intellij.structuralsearch.impl.matcher.CompiledPattern;
import com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor;
import com.intellij.structuralsearch.impl.matcher.PatternTreeContext;
import com.intellij.structuralsearch.impl.matcher.compiler.GlobalCompilingVisitor;
import com.intellij.structuralsearch.impl.matcher.strategies.MatchingStrategy;
import com.intellij.structuralsearch.plugin.replace.ReplaceOptions;
import com.intellij.structuralsearch.plugin.ui.Configuration;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.experiments.RsExperiments;
import org.rust.ide.template.RsContextType;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.openapiext.OpenApiUtil;
import consulo.language.impl.parser.GeneratedParserUtilBase;
import org.rust.lang.core.psi.impl.*;

public class RsStructuralSearchProfile extends StructuralSearchProfile {

    public static final String TYPED_VAR_PREFIX = "_____";

    
    public boolean isMyLanguage(@Nonnull Language language) {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.SSR)) return false;
        return language == RsLanguage.INSTANCE;
    }

    
    @Nullable
    public LanguageFileType getDefaultFileType(@Nullable LanguageFileType fileType) {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.SSR)) return null;
        return fileType != null ? fileType : RsFileType.INSTANCE;
    }

    
    @Nonnull
    public Class<? extends TemplateContextType> getTemplateContextTypeClass() {
        return RsContextType.class;
    }

    
    public void compile(PsiElement[] elements, @Nonnull GlobalCompilingVisitor globalVisitor) {
        new RsCompilingVisitor(globalVisitor).compile(elements);
    }

    
    @Nonnull
    public PsiElementVisitor createMatchingVisitor(@Nonnull GlobalMatchingVisitor globalVisitor) {
        return new RsMatchingVisitor(globalVisitor);
    }

    
    @Nonnull
    public Configuration [] getPredefinedTemplates() {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.SSR)) return new Configuration[0];
        return RsPredefinedConfigurations.createPredefinedTemplates();
    }

    
    public boolean isIdentifier(@Nullable PsiElement element) {
        return element != null && element.getNode() != null
            && element.getNode().getElementType() == RsElementTypes.IDENTIFIER;
    }

    
    @Nonnull
    public CompiledPattern createCompiledPattern() {
        return new RsCompiledPattern();
    }

    
    @Nonnull
    public PsiElement [] createPatternTree(
        @Nonnull String text,
        @Nonnull PatternTreeContext context,
        @Nonnull LanguageFileType fileType,
        @Nonnull Language language,
        @Nullable String contextId,
        @Nonnull Project project,
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

    
    public void checkSearchPattern(@Nonnull CompiledPattern pattern) {
        RustValidator visitor = new RustValidator();
        var nodes = pattern.getNodes();
        while (nodes.hasNext()) {
            nodes.current().accept(visitor);
            nodes.advance();
        }
        nodes.reset();
    }

    
    public void checkReplacementPattern(@Nonnull Project project, @Nonnull ReplaceOptions options) {
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

        
        public void visitElement(@Nonnull PsiElement element) {
            if (invalid.accepts(element)) {
                throw new MalformedPatternException(RsBundle.message("ssr.unsupported.search.template"));
            }
            super.visitElement(element);
        }
    }

    private static class RsCompiledPattern extends CompiledPattern {
        RsCompiledPattern() {
            setStrategy(new MatchingStrategy() {
                
                public boolean continueMatching(@Nullable PsiElement start) {
                    return start != null && start.getLanguage() == RsLanguage.INSTANCE;
                }

                
                public boolean shouldSkip(@Nullable PsiElement element, @Nullable PsiElement elementToMatchWith) {
                    return false;
                }
            });
        }

        
        @Nonnull
        public String [] getTypedVarPrefixes() {
            return new String[]{TYPED_VAR_PREFIX};
        }

        
        public boolean isTypedVar(@Nonnull String str) {
            if (str.isEmpty()) return false;
            if (str.charAt(0) == '@') {
                return str.substring(1).startsWith(TYPED_VAR_PREFIX);
            } else {
                return str.startsWith(TYPED_VAR_PREFIX);
            }
        }

        
        @Nonnull
        public String getTypedVarString(@Nonnull PsiElement element) {
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
