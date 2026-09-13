/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.macros.MacroExpansionContext;
import org.rust.lang.core.macros.MacroExpansionContextUtil;
import org.rust.lang.core.macros.MacroExpansionMode;
import org.rust.lang.core.macros.RsMacroExpansionManagerUtil;
import org.rust.lang.core.macros.decl.FragmentKind;
import org.rust.lang.core.macros.decl.MGNodeData;
import org.rust.lang.core.macros.decl.MacroGraphWalker;
import org.rust.common.graph.PresentableGraph;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.core.psi.ext.impl.RsMacroDefinitionBaseUtil;
import org.rust.openapiext.Testmark;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;
import consulo.document.util.TextRange;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.completion.RsCompletionProvider;
import org.rust.lang.core.completion.Utils;

/**
 * Provides completion inside a macro argument if the macro is NOT expanded successfully.
 * If macro is expanded successfully, {@link RsFullMacroArgumentCompletionProvider} is used.
 */
public class RsPartialMacroArgumentCompletionProvider extends RsCompletionProvider {
    public static final RsPartialMacroArgumentCompletionProvider INSTANCE = new RsPartialMacroArgumentCompletionProvider();

    private RsPartialMacroArgumentCompletionProvider() {
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        PsiElement position = parameters.getPosition();
        RsMacroArgument macroArg = RsElementUtil.ancestorStrict(position, RsMacroArgument.class);
        if (macroArg == null) return;
        RsMacroCall macroCall = RsElementUtil.ancestorStrict(macroArg, RsMacroCall.class);
        if (macroCall == null) return;

        boolean condition = RsMacroCallUtil.getExpansion(macroCall) != null
            && RsMacroExpansionManagerUtil.getMacroExpansionManager(parameters.getOriginalFile().getProject()).getMacroExpansionMode() instanceof MacroExpansionMode.New
            && MacroExpansionContextUtil.getExpansionContext(macroCall) != MacroExpansionContext.EXPR
            && MacroExpansionContextUtil.getExpansionContext(macroCall) != MacroExpansionContext.STMT;
        if (condition) return;

        consulo.document.util.TextRange bodyTextRange = RsMacroCallUtil.getBodyTextRange(macroCall);
        if (bodyTextRange == null) return;
        String macroCallBody = RsMacroCallUtil.getMacroBody(macroCall);
        if (macroCallBody == null) return;
        RsMacroDefinitionBase macro = RsMacroCallUtil.resolveToMacro(macroCall);
        if (macro == null) return;
        PresentableGraph<MGNodeData, Void> graph = RsMacroDefinitionBaseUtil.getGraph(macro);
        if (graph == null) return;
        int offsetInArgument = parameters.getOffset() - bodyTextRange.getStartOffset();

        Testmarks.Touched.hit();

        MacroGraphWalker walker = new MacroGraphWalker(parameters.getOriginalFile().getProject(), graph, macroCallBody, offsetInArgument);
        List<MacroGraphWalker.FragmentDescriptor> fragmentDescriptors = walker.run();
        if (fragmentDescriptors.isEmpty()) return;
        Set<FragmentKind> usedKinds = new HashSet<>();

        for (MacroGraphWalker.FragmentDescriptor descriptor : fragmentDescriptors) {
            FragmentKind kind = descriptor.getKind();
            if (usedKinds.contains(kind)) continue;

            RsCodeFragment codeFragment;
            if (kind == FragmentKind.Expr || kind == FragmentKind.Path) {
                codeFragment = new RsExpressionCodeFragment(parameters.getOriginalFile().getProject(), descriptor.getFragmentText(), macroCall);
            } else if (kind == FragmentKind.Stmt) {
                codeFragment = new RsStatementCodeFragment(parameters.getOriginalFile().getProject(), descriptor.getFragmentText(), macroCall);
            } else if (kind == FragmentKind.Ty) {
                codeFragment = new RsTypeReferenceCodeFragment(parameters.getOriginalFile().getProject(), descriptor.getFragmentText(), macroCall);
            } else {
                continue;
            }

            PsiElement element = codeFragment.findElementAt(descriptor.getCaretOffsetInFragment());
            if (element != null) {
                Utils.rerunCompletion(parameters.withPosition(element, descriptor.getCaretOffsetInFragment()), result);
            }
            usedKinds.add(kind);
        }
    }

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement()
            .withLanguage(RsLanguage.INSTANCE)
            .inside(psiElement(RsMacroArgument.class));
    }

    public static final class Testmarks {
        public static final Testmark Touched = new Testmark();
    }
}
