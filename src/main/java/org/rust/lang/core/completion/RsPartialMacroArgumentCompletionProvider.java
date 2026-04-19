/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.macros.MacroExpansionContext;
import org.rust.lang.core.macros.MacroExpansionContextUtil;
import org.rust.lang.core.macros.MacroExpansionMode;
import org.rust.lang.core.macros.RsMacroExpansionManagerUtil;
import org.rust.lang.core.macros.decl.FragmentKind;
import org.rust.lang.core.macros.decl.MGNodeData;
import org.rust.lang.core.macros.decl.MacroGraphWalker;
import org.rust.lang.utils.PresentableGraph;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBaseUtil;
import org.rust.openapiext.Testmark;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

/**
 * Provides completion inside a macro argument if the macro is NOT expanded successfully.
 * If macro is expanded successfully, {@link RsFullMacroArgumentCompletionProvider} is used.
 */
public class RsPartialMacroArgumentCompletionProvider extends RsCompletionProvider {
    public static final RsPartialMacroArgumentCompletionProvider INSTANCE = new RsPartialMacroArgumentCompletionProvider();

    private RsPartialMacroArgumentCompletionProvider() {
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
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

        com.intellij.openapi.util.TextRange bodyTextRange = RsMacroCallUtil.getBodyTextRange(macroCall);
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

    @NotNull
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
