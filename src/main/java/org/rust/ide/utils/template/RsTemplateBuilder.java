/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.template;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.template.*;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.codeInsight.template.impl.VariableNode;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;
import org.rust.lang.core.macros.RangeMap;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.*;

/**
 * A wrapper for {@link com.intellij.codeInsight.template.TemplateBuilder}.
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class RsTemplateBuilder {
    @NotNull
    private final PsiFile hostPsiFile;
    @NotNull
    private final Editor editor;
    @NotNull
    private final Editor hostEditor;
    @NotNull
    private final List<RsTemplateElement> elementsToReplace = new ArrayList<>();
    @NotNull
    private final Map<String, TemplateVariable> variables = new LinkedHashMap<>();
    @NotNull
    private final Map<String, String> usageToVar = new HashMap<>();
    private int variableCounter = 0;
    private boolean highlightExpressions = false;
    private boolean disableDaemonHighlighting = false;
    @NotNull
    private final List<TemplateEditingListener> listeners = new ArrayList<>();

    public RsTemplateBuilder(@NotNull PsiFile hostPsiFile, @NotNull Editor editor, @NotNull Editor hostEditor) {
        this.hostPsiFile = hostPsiFile;
        this.editor = editor;
        this.hostEditor = hostEditor;
    }

    @NotNull
    private Document getHostDocument() {
        return hostEditor.getDocument();
    }

    private void replaceElement(
        @Nullable RangeMarker range,
        @Nullable Expression expression,
        @Nullable String variableName,
        boolean alwaysStopAt
    ) {
        if (range != null) {
            elementsToReplace.add(new RsTemplateElement(range, expression, variableName, alwaysStopAt));
        }
    }

    @Nullable
    private RangeMarker psiToRangeMarker(@NotNull PsiElement element, @NotNull TextRange rangeWithinElement) {
        TextRange absoluteRange = rangeWithinElement.shiftRight(element.getTextRange().getStartOffset());
        TextRange range;
        if (editor instanceof RsIntentionInsideMacroExpansionEditor
            && element.getContainingFile() == ((RsIntentionInsideMacroExpansionEditor) editor).getPsiFileCopy()) {
            // handle macro expansion mapping
            var macroEditor = (RsIntentionInsideMacroExpansionEditor) editor;
            var mutableContext = macroEditor.getContext();
            if (mutableContext == null) return null;
            var mapped = mutableContext.getRangeMap().mapTextRangeFromExpansionToCallBody(absoluteRange);
            if (mapped.isEmpty()) return null;
            var srcRange = mapped.get(0).getSrcRange();
            if (srcRange == null) return null;
            range = srcRange.shiftRight(mutableContext.getRootMacroCallBodyOffset());
        } else {
            range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, absoluteRange);
        }
        return getHostDocument().createRangeMarker(range);
    }

    @Nullable
    private RangeMarker psiToRangeMarker(@NotNull PsiElement element) {
        return psiToRangeMarker(element, new TextRange(0, element.getTextLength()));
    }

    @NotNull
    public RsTemplateBuilder replaceElement(@NotNull PsiElement element, @Nullable String replacementText) {
        replaceElement(
            psiToRangeMarker(element),
            replacementText != null ? new ConstantNode(replacementText) : null,
            null,
            true
        );
        return this;
    }

    @NotNull
    public RsTemplateBuilder replaceElement(@NotNull PsiElement element, @NotNull TextRange rangeWithinElement, @Nullable String replacementText) {
        replaceElement(
            psiToRangeMarker(element, rangeWithinElement),
            replacementText != null ? new ConstantNode(replacementText) : null,
            null,
            true
        );
        return this;
    }

    @NotNull
    public RsTemplateBuilder replaceElement(@NotNull PsiElement element, @NotNull Expression expression) {
        replaceElement(psiToRangeMarker(element), expression, null, true);
        return this;
    }

    @NotNull
    public RsTemplateBuilder replaceElement(@NotNull PsiElement element, @NotNull TextRange rangeWithinElement, @NotNull Expression expression) {
        replaceElement(psiToRangeMarker(element, rangeWithinElement), expression, null, true);
        return this;
    }

    @NotNull
    private RsTemplateBuilder replaceElement(@NotNull PsiElement element, @NotNull TemplateVariable variable, @Nullable String replacementText) {
        replaceElement(
            psiToRangeMarker(element),
            replacementText != null ? new ConstantNode(replacementText) : null,
            variable.getName(),
            true
        );
        return this;
    }

    @NotNull
    public TemplateVariable introduceVariable(@NotNull PsiElement element, @Nullable String replacementText) {
        TemplateVariable variable = newVariable();
        replaceElement(element, variable, replacementText);
        return variable;
    }

    @NotNull
    public TemplateVariable introduceVariable(@NotNull PsiElement element) {
        return introduceVariable(element, null);
    }

    @NotNull
    private TemplateVariable newVariable() {
        String name;
        do {
            variableCounter++;
            name = "variable" + variableCounter;
        } while (variables.containsKey(name));
        return newVariable(name);
    }

    @NotNull
    private TemplateVariable newVariable(@NotNull String name) {
        if (variables.containsKey(name)) {
            throw new IllegalStateException("The variable `" + name + "` is already defined");
        }
        TemplateVariable variable = new TemplateVariable(name);
        variables.put(name, variable);
        return variable;
    }

    @NotNull
    public RsTemplateBuilder withExpressionsHighlighting() {
        highlightExpressions = true;
        return this;
    }

    @NotNull
    public RsTemplateBuilder withDisabledDaemonHighlighting() {
        disableDaemonHighlighting = true;
        return this;
    }

    @NotNull
    public RsTemplateBuilder withListener(@NotNull TemplateEditingListener listener) {
        listeners.add(listener);
        return this;
    }

    @NotNull
    public RsTemplateBuilder withResultListener(@NotNull TemplateEditingListener listener) {
        return withListener(listener);
    }

    @NotNull
    private RsTemplateBuilder withFinishResultListener(@NotNull Runnable onFinish) {
        return withListener(new TemplateEditingAdapter() {
            @Override
            public void templateFinished(@NotNull Template template, boolean brokenOff) {
                if (!brokenOff) {
                    onFinish.run();
                }
            }
        });
    }

    private void doPostponedOperationsAndCommit(@NotNull Editor ed) {
        PsiDocumentManager manager = PsiDocumentManager.getInstance(hostPsiFile.getProject());
        manager.doPostponedOperationsAndUnblockDocument(ed.getDocument());
        manager.commitDocument(ed.getDocument());
    }

    public void runInline() {
        var project = hostPsiFile.getProject();

        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            var macroEditor = (RsIntentionInsideMacroExpansionEditor) editor;
            if (macroEditor.getContext() != null && macroEditor.getContext().isBroken()) return;
            IntentionInMacroUtil.finishActionInMacroExpansionCopy(editor);
        }

        doPostponedOperationsAndCommit(editor);
        if (editor != hostEditor) {
            doPostponedOperationsAndCommit(hostEditor);
        }

        if (elementsToReplace.isEmpty()) {
            return;
        }

        TextRange commonTextRange = elementsToReplace.get(0).range.getTextRange();

        List<RsUnwrappedTemplateElement> elements = new ArrayList<>();
        for (RsTemplateElement it : elementsToReplace) {
            TextRange range = it.range.getTextRange();
            it.range.dispose();
            commonTextRange = commonTextRange.union(range);
            elements.add(new RsUnwrappedTemplateElement(range, it.expression, it.variableName, it.alwaysStopAt));
        }

        PsiElement hostOwner = hostPsiFile.findElementAt(commonTextRange.getStartOffset());
        if (hostOwner == null) return;
        while (hostOwner != null && !hostOwner.getTextRange().contains(commonTextRange)) {
            hostOwner = hostOwner.getParent();
        }
        if (hostOwner == null) return;

        TemplateBuilderImpl delegate = (TemplateBuilderImpl) TemplateBuilderFactory.getInstance().createTemplateBuilder(hostOwner);
        Document hostDoc = getHostDocument();

        for (RsUnwrappedTemplateElement element : elements) {
            Expression expression = element.expression;
            if (expression == null) {
                expression = new ConstantNode(element.range.subSequence(hostDoc.getImmutableCharSequence()).toString());
            }
            TextRange relRange = element.range.shiftLeft(hostOwner.getTextRange().getStartOffset());
            if (element.variableName != null) {
                delegate.replaceElement(hostOwner, relRange, element.variableName, expression, element.alwaysStopAt);
            } else {
                delegate.replaceElement(hostOwner, relRange, expression);
            }
        }

        // From TemplateBuilderImpl.run()
        Template template = delegate.buildInlineTemplate();
        hostEditor.getCaretModel().moveToOffset(hostOwner.getTextRange().getStartOffset());
        TemplateState templateState = (TemplateState) TemplateManager.getInstance(project).runTemplate(hostEditor, template);

        boolean isAlreadyFinished = templateState.isFinished(); // Can be true in unit tests
        for (TemplateEditingListener listener : listeners) {
            if (isAlreadyFinished) {
                listener.templateFinished(template, false);
            } else {
                templateState.addTemplateStateListener(listener);
            }
        }

        if (isAlreadyFinished) return;

        if (highlightExpressions) {
            setupUsageHighlighting(templateState, template);
        }

        if (disableDaemonHighlighting) {
            DaemonCodeAnalyzer.getInstance(project).disableUpdateByTimer(templateState);
        }
    }

    public void runInline(@NotNull Runnable onFinish) {
        withFinishResultListener(onFinish);
        runInline();
    }

    private void setupUsageHighlighting(@NotNull TemplateState templateState, @NotNull Template template) {
        Map<String, Set<Integer>> varToUsages = new HashMap<>();
        for (int i = 0; i < templateState.getSegmentsCount(); i++) {
            String variableName = template.getSegmentName(i);
            if (!variables.containsKey(variableName)) {
                String parentVarName = usageToVar.get(variableName);
                if (parentVarName != null) {
                    varToUsages.computeIfAbsent(parentVarName, k -> new HashSet<>()).add(i);
                }
            }
        }

        if (!varToUsages.isEmpty()) {
            RsTemplateHighlighting h = new RsTemplateHighlighting(hostEditor, HighlightManager.getInstance(hostPsiFile.getProject()), varToUsages);
            h.highlightVariablesAt(templateState, template, 0);
            templateState.addTemplateStateListener(h);
            Disposer.register(templateState, h);
        }
    }

    private static class RsTemplateElement {
        @NotNull
        final RangeMarker range;
        @Nullable
        final Expression expression;
        @Nullable
        final String variableName;
        final boolean alwaysStopAt;

        RsTemplateElement(@NotNull RangeMarker range, @Nullable Expression expression, @Nullable String variableName, boolean alwaysStopAt) {
            this.range = range;
            this.expression = expression;
            this.variableName = variableName;
            this.alwaysStopAt = alwaysStopAt;
        }
    }

    private static class RsUnwrappedTemplateElement {
        @NotNull
        final TextRange range;
        @Nullable
        final Expression expression;
        @Nullable
        final String variableName;
        final boolean alwaysStopAt;

        RsUnwrappedTemplateElement(@NotNull TextRange range, @Nullable Expression expression, @Nullable String variableName, boolean alwaysStopAt) {
            this.range = range;
            this.expression = expression;
            this.variableName = variableName;
            this.alwaysStopAt = alwaysStopAt;
        }
    }

    public final class TemplateVariable {
        @NotNull
        private final String name;
        private int dependentVarCounter = 0;

        TemplateVariable(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        public String getName() {
            return name;
        }

        public void replaceElementWithVariable(@NotNull PsiElement element) {
            RsTemplateBuilder.this.replaceElement(
                psiToRangeMarker(element),
                new VariableNode(name, null),
                newSubsequentVariable(),
                false
            );
        }

        @NotNull
        private String newSubsequentVariable() {
            String dependentVar = name + "_" + dependentVarCounter;
            dependentVarCounter++;
            usageToVar.put(dependentVar, name);
            return dependentVar;
        }
    }

    public static class RsTemplateHighlighting extends TemplateEditingAdapter implements Disposable {
        @NotNull
        private final Editor hostEditor;
        @NotNull
        private final HighlightManager highlightManager;
        @NotNull
        private final Map<String, Set<Integer>> varToUsages;
        @NotNull
        private final List<RangeHighlighter> highlighters = new ArrayList<>();

        public RsTemplateHighlighting(
            @NotNull Editor hostEditor,
            @NotNull HighlightManager highlightManager,
            @NotNull Map<String, Set<Integer>> varToUsages
        ) {
            this.hostEditor = hostEditor;
            this.highlightManager = highlightManager;
            this.varToUsages = varToUsages;
        }

        public void highlightVariablesAt(@NotNull TemplateState templateState, @NotNull Template template, int index) {
            releaseHighlighters();
            var key = EditorColors.SEARCH_RESULT_ATTRIBUTES;
            String name = ((TemplateImpl) template).getVariableNameAt(index);
            Set<Integer> usages = varToUsages.get(name);
            if (usages != null) {
                for (int i : usages) {
                    TextRange range = templateState.getSegmentRange(i);
                    highlightManager.addOccurrenceHighlight(hostEditor, range.getStartOffset(), range.getEndOffset(), key, 0, highlighters);
                }
            }
        }

        @Override
        public void currentVariableChanged(@NotNull TemplateState templateState, Template template, int oldIndex, int newIndex) {
            if (newIndex >= 0) {
                highlightVariablesAt(templateState, template, newIndex);
            }
        }

        @Override
        public void dispose() {
            releaseHighlighters();
        }

        private void releaseHighlighters() {
            for (RangeHighlighter highlighter : highlighters) {
                highlightManager.removeSegmentHighlighter(hostEditor, highlighter);
            }
            highlighters.clear();
        }
    }
}
