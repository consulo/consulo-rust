/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.template;
import consulo.language.editor.template.*;
import consulo.language.editor.template.event.TemplateEditingAdapter;
import consulo.language.editor.template.event.TemplateEditingListener;

import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.inject.InjectedLanguageManager;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposer;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;

import java.util.*;

/**
 * A wrapper for {@link TemplateBuilder}.
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class RsTemplateBuilder {
    @Nonnull
    private final PsiFile hostPsiFile;
    @Nonnull
    private final Editor editor;
    @Nonnull
    private final Editor hostEditor;
    @Nonnull
    private final List<RsTemplateElement> elementsToReplace = new ArrayList<>();
    @Nonnull
    private final Map<String, TemplateVariable> variables = new LinkedHashMap<>();
    @Nonnull
    private final Map<String, String> usageToVar = new HashMap<>();
    private int variableCounter = 0;
    private boolean highlightExpressions = false;
    private boolean disableDaemonHighlighting = false;
    @Nonnull
    private final List<TemplateEditingListener> listeners = new ArrayList<>();

    public RsTemplateBuilder(@Nonnull PsiFile hostPsiFile, @Nonnull Editor editor, @Nonnull Editor hostEditor) {
        this.hostPsiFile = hostPsiFile;
        this.editor = editor;
        this.hostEditor = hostEditor;
    }

    @Nonnull
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
    private RangeMarker psiToRangeMarker(@Nonnull PsiElement element, @Nonnull TextRange rangeWithinElement) {
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
    private RangeMarker psiToRangeMarker(@Nonnull PsiElement element) {
        return psiToRangeMarker(element, new TextRange(0, element.getTextLength()));
    }

    @Nonnull
    public RsTemplateBuilder replaceElement(@Nonnull PsiElement element, @Nullable String replacementText) {
        replaceElement(
            psiToRangeMarker(element),
            replacementText != null ? new ConstantNode(replacementText) : null,
            null,
            true
        );
        return this;
    }

    @Nonnull
    public RsTemplateBuilder replaceElement(@Nonnull PsiElement element, @Nonnull TextRange rangeWithinElement, @Nullable String replacementText) {
        replaceElement(
            psiToRangeMarker(element, rangeWithinElement),
            replacementText != null ? new ConstantNode(replacementText) : null,
            null,
            true
        );
        return this;
    }

    @Nonnull
    public RsTemplateBuilder replaceElement(@Nonnull PsiElement element, @Nonnull Expression expression) {
        replaceElement(psiToRangeMarker(element), expression, null, true);
        return this;
    }

    @Nonnull
    public RsTemplateBuilder replaceElement(@Nonnull PsiElement element, @Nonnull TextRange rangeWithinElement, @Nonnull Expression expression) {
        replaceElement(psiToRangeMarker(element, rangeWithinElement), expression, null, true);
        return this;
    }

    @Nonnull
    private RsTemplateBuilder replaceElement(@Nonnull PsiElement element, @Nonnull TemplateVariable variable, @Nullable String replacementText) {
        replaceElement(
            psiToRangeMarker(element),
            replacementText != null ? new ConstantNode(replacementText) : null,
            variable.getName(),
            true
        );
        return this;
    }

    @Nonnull
    public TemplateVariable introduceVariable(@Nonnull PsiElement element, @Nullable String replacementText) {
        TemplateVariable variable = newVariable();
        replaceElement(element, variable, replacementText);
        return variable;
    }

    @Nonnull
    public TemplateVariable introduceVariable(@Nonnull PsiElement element) {
        return introduceVariable(element, null);
    }

    @Nonnull
    private TemplateVariable newVariable() {
        String name;
        do {
            variableCounter++;
            name = "variable" + variableCounter;
        } while (variables.containsKey(name));
        return newVariable(name);
    }

    @Nonnull
    private TemplateVariable newVariable(@Nonnull String name) {
        if (variables.containsKey(name)) {
            throw new IllegalStateException("The variable `" + name + "` is already defined");
        }
        TemplateVariable variable = new TemplateVariable(name);
        variables.put(name, variable);
        return variable;
    }

    @Nonnull
    public RsTemplateBuilder withExpressionsHighlighting() {
        highlightExpressions = true;
        return this;
    }

    @Nonnull
    public RsTemplateBuilder withDisabledDaemonHighlighting() {
        disableDaemonHighlighting = true;
        return this;
    }

    @Nonnull
    public RsTemplateBuilder withListener(@Nonnull TemplateEditingListener listener) {
        listeners.add(listener);
        return this;
    }

    @Nonnull
    public RsTemplateBuilder withResultListener(@Nonnull TemplateEditingListener listener) {
        return withListener(listener);
    }

    @Nonnull
    private RsTemplateBuilder withFinishResultListener(@Nonnull Runnable onFinish) {
        return withListener(new TemplateEditingAdapter() {
            @Override
            public void templateFinished(@Nonnull Template template, boolean brokenOff) {
                if (!brokenOff) {
                    onFinish.run();
                }
            }
        });
    }

    private void doPostponedOperationsAndCommit(@Nonnull Editor ed) {
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

        TemplateBuilder delegate = TemplateBuilderFactory.getInstance().createTemplateBuilder(hostOwner);
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
        TemplateManager.getInstance(project).startTemplate(hostEditor, template);
        TemplateState templateState = TemplateManager.getInstance(project).getTemplateState(hostEditor);

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

    public void runInline(@Nonnull Runnable onFinish) {
        withFinishResultListener(onFinish);
        runInline();
    }

    private void setupUsageHighlighting(@Nonnull TemplateState templateState, @Nonnull Template template) {
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
        @Nonnull
        final RangeMarker range;
        @Nullable
        final Expression expression;
        @Nullable
        final String variableName;
        final boolean alwaysStopAt;

        RsTemplateElement(@Nonnull RangeMarker range, @Nullable Expression expression, @Nullable String variableName, boolean alwaysStopAt) {
            this.range = range;
            this.expression = expression;
            this.variableName = variableName;
            this.alwaysStopAt = alwaysStopAt;
        }
    }

    private static class RsUnwrappedTemplateElement {
        @Nonnull
        final TextRange range;
        @Nullable
        final Expression expression;
        @Nullable
        final String variableName;
        final boolean alwaysStopAt;

        RsUnwrappedTemplateElement(@Nonnull TextRange range, @Nullable Expression expression, @Nullable String variableName, boolean alwaysStopAt) {
            this.range = range;
            this.expression = expression;
            this.variableName = variableName;
            this.alwaysStopAt = alwaysStopAt;
        }
    }

    public final class TemplateVariable {
        @Nonnull
        private final String name;
        private int dependentVarCounter = 0;

        TemplateVariable(@Nonnull String name) {
            this.name = name;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        public void replaceElementWithVariable(@Nonnull PsiElement element) {
            RsTemplateBuilder.this.replaceElement(
                psiToRangeMarker(element),
                new VariableNode(name, null),
                newSubsequentVariable(),
                false
            );
        }

        @Nonnull
        private String newSubsequentVariable() {
            String dependentVar = name + "_" + dependentVarCounter;
            dependentVarCounter++;
            usageToVar.put(dependentVar, name);
            return dependentVar;
        }
    }

    public static class RsTemplateHighlighting extends TemplateEditingAdapter implements Disposable {
        @Nonnull
        private final Editor hostEditor;
        @Nonnull
        private final HighlightManager highlightManager;
        @Nonnull
        private final Map<String, Set<Integer>> varToUsages;
        @Nonnull
        private final List<RangeHighlighter> highlighters = new ArrayList<>();

        public RsTemplateHighlighting(
            @Nonnull Editor hostEditor,
            @Nonnull HighlightManager highlightManager,
            @Nonnull Map<String, Set<Integer>> varToUsages
        ) {
            this.hostEditor = hostEditor;
            this.highlightManager = highlightManager;
            this.varToUsages = varToUsages;
        }

        public void highlightVariablesAt(@Nonnull TemplateState templateState, @Nonnull Template template, int index) {
            releaseHighlighters();
            var key = EditorColors.SEARCH_RESULT_ATTRIBUTES;
            String name = template.getVariableNameAt(index);
            Set<Integer> usages = varToUsages.get(name);
            if (usages != null) {
                for (int i : usages) {
                    TextRange range = templateState.getSegmentRange(i);
                    highlightManager.addOccurrenceHighlight(hostEditor, range.getStartOffset(), range.getEndOffset(), key, 0, highlighters);
                }
            }
        }

        @Override
        public void currentVariableChanged(@Nonnull TemplateState templateState, Template template, int oldIndex, int newIndex) {
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
