/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.util.macros;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.intellij.psi.PsiFile;

public final class IntentionInMacroUtil {
    public static final IntentionInMacroUtil INSTANCE = new IntentionInMacroUtil();

    private static final ThreadLocal<PsiFile> MUTABLE_EXPANSION_FILE_COPY = new ThreadLocal<>();

    private IntentionInMacroUtil() {
    }

    public static Editor unwrapEditor(Editor editor) {
        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            return ((RsIntentionInsideMacroExpansionEditor) editor).getOriginalEditor();
        }
        return editor;
    }

    public static boolean isMutableExpansionFile(PsiFile file) {
        return MUTABLE_EXPANSION_FILE_COPY.get() == file;
    }

    private static <T> T withMutableExpansionFile(PsiFile file, java.util.function.Supplier<T> action) {
        MUTABLE_EXPANSION_FILE_COPY.set(file);
        try {
            return action.get();
        } finally {
            MUTABLE_EXPANSION_FILE_COPY.remove();
        }
    }

    public static boolean doActionAvailabilityCheckInsideMacroExpansion(
        Editor originalEditor,
        PsiFile originalFile,
        PsiElement expandedElement,
        int caretOffset,
        Function<Editor, Boolean> action
    ) {
        PsiFile expansionFile = expandedElement.getContainingFile();
        RsIntentionInsideMacroExpansionEditor fakeEditor = new RsIntentionInsideMacroExpansionEditor(
            expansionFile,
            originalFile,
            originalEditor,
            caretOffset,
            null
        );
        return withMutableExpansionFile(expansionFile, () -> action.apply(fakeEditor));
    }

    /**
     * Creates a copy of the macro expansion, runs the given action on the copy,
     * and applies changes back to the original file.
     */
    public static void runActionInsideMacroExpansionCopy(
        @NotNull Project project,
        @Nullable Editor originalEditor,
        @NotNull PsiFile originalFile,
        @NotNull PsiElement expandedElement,
        @NotNull java.util.function.BiFunction<Editor, PsiElement, Boolean> action
    ) {
        PsiFile expansionFile = expandedElement.getContainingFile();
        int offset = expandedElement.getTextOffset();
        RsIntentionInsideMacroExpansionEditor fakeEditor = new RsIntentionInsideMacroExpansionEditor(
            expansionFile,
            originalFile,
            originalEditor,
            offset,
            null
        );
        withMutableExpansionFile(expansionFile, () -> {
            PsiElement elementInCopy = expansionFile.findElementAt(offset);
            if (elementInCopy != null) {
                // Find the element of matching type at the same offset
                PsiElement current = elementInCopy;
                while (current != null && current.getTextOffset() == offset) {
                    if (current.getClass().equals(expandedElement.getClass())) {
                        action.apply(fakeEditor, current);
                        break;
                    }
                    current = current.getParent();
                }
            }
            return null;
        });
        finishActionInMacroExpansionCopy(fakeEditor);
    }

    public static void finishActionInMacroExpansionCopy(Editor editorCopy) {
        if (editorCopy instanceof RsIntentionInsideMacroExpansionEditor) {
            finishActionInMacroExpansionCopy((RsIntentionInsideMacroExpansionEditor) editorCopy);
        }
    }

    public static void finishActionInMacroExpansionCopy(RsIntentionInsideMacroExpansionEditor editorCopy) {
        RsIntentionInsideMacroExpansionContext mutableContext = editorCopy.getContext();
        if (mutableContext == null) return;
        finishActionInMacroExpansionCopy(mutableContext, editorCopy);
    }

    private static void finishActionInMacroExpansionCopy(
        RsIntentionInsideMacroExpansionContext mutableContext,
        @Nullable RsIntentionInsideMacroExpansionEditor editorCopy
    ) {
        if (mutableContext.isFinished() || mutableContext.isBroken()) {
            return;
        }
        mutableContext.setFinished(true);
        PsiFile originalFile = mutableContext.getOriginalFile();
        Project project = originalFile.getProject();
        Document originalDoc = originalFile.getViewProvider().getDocument();

        mutableContext.setApplyChangesToOriginalDoc(false);
        PsiDocumentManager psiDocMgr = PsiDocumentManager.getInstance(project);
        psiDocMgr.doPostponedOperationsAndUnblockDocument(originalDoc);
        psiDocMgr.commitDocument(originalDoc);
        psiDocMgr.doPostponedOperationsAndUnblockDocument(mutableContext.getDocumentCopy());
        psiDocMgr.commitDocument(mutableContext.getDocumentCopy());
        mutableContext.setApplyChangesToOriginalDoc(true);

        if (editorCopy != null) {
            int macroBodyStartOffset = mutableContext.getRootMacroBodyRange().getStartOffset();

            Integer initialMappedOffset = editorCopy.getInitialMappedOffset();
            com.intellij.openapi.editor.Caret editorCopyCaret = editorCopy.getCaretModel().getCurrentCaret();
            if (initialMappedOffset != null && initialMappedOffset != editorCopyCaret.getOffset()) {
                Integer backMappedOffset = mutableContext.getRangeMap().mapOffsetFromExpansionToCallBody(editorCopyCaret.getOffset(), false);
                if (backMappedOffset != null) {
                    editorCopy.getOriginalEditor().getCaretModel().moveToOffset(macroBodyStartOffset + backMappedOffset);
                }
            }

            if (editorCopyCaret.hasSelection()) {
                TextRange selectionRange = new TextRange(editorCopyCaret.getSelectionStart(), editorCopyCaret.getSelectionEnd());
                List<MappedTextRange> mappedRanges = mutableContext.getRangeMap().mapTextRangeFromExpansionToCallBody(selectionRange);
                if (mappedRanges.size() == 1) {
                    TextRange srcRange = mappedRanges.get(0).getSrcRange();
                    TextRange backMappedRange = srcRange.shiftRight(macroBodyStartOffset);
                    editorCopy.getOriginalEditor().getCaretModel().getCurrentCaret().setSelection(
                        backMappedRange.getStartOffset(),
                        backMappedRange.getEndOffset()
                    );
                }
            }
        }

        if (mutableContext.getChangedRanges().isEmpty()) {
            return;
        }

        markRangesToReformat(mutableContext.getChangedRanges(), originalFile);
        mutableContext.getChangedRanges().clear();
    }

    private static void markRangesToReformat(List<RangeMarker> mappedChanges, PsiFile originalFile) {
        PsiElement someLeaf = null;
        for (RangeMarker rangeMarker : mappedChanges) {
            TextRange range = rangeMarker.getTextRange();
            rangeMarker.dispose();
            PsiElement leaf = originalFile.findElementAt(range.getStartOffset());
            if (leaf != null && leaf.getTextOffset() == range.getStartOffset()) {
                if (!(someLeaf instanceof PsiWhiteSpace)) {
                    someLeaf = leaf;
                }
                PsiElement prevLeaf = PsiTreeUtil.prevLeaf(leaf);
                if (prevLeaf instanceof PsiWhiteSpace) {
                    leaf = prevLeaf;
                } else {
                    CodeEditUtil.markToReformatBefore(leaf.getNode(), true);
                    leaf = PsiTreeUtil.nextLeaf(leaf);
                }
            }
            while (leaf != null && leaf.getTextOffset() <= range.getEndOffset()) {
                if (!(someLeaf instanceof PsiWhiteSpace)) {
                    someLeaf = leaf;
                }
                CodeEditUtil.markToReformatBefore(leaf.getNode(), false);
                CodeEditUtil.markToReformat(leaf.getNode(), true);
                leaf = PsiTreeUtil.nextLeaf(leaf);
            }
        }

        if (someLeaf != null) {
            if (!(someLeaf instanceof PsiWhiteSpace)) {
                PsiElement nextLeaf = PsiTreeUtil.nextLeaf(someLeaf);
                if (nextLeaf instanceof PsiWhiteSpace) {
                    someLeaf = nextLeaf;
                }
            }
            if (someLeaf instanceof PsiWhiteSpaceImpl) {
                PsiElement ws = PsiParserFacade.getInstance(originalFile.getProject()).createWhiteSpaceFromText(someLeaf.getText());
                CodeEditUtil.setNodeGenerated(ws.getNode(), false);
                PsiElement prevLeaf = PsiTreeUtil.prevLeaf(someLeaf);
                boolean isMarkedToReformat = CodeEditUtil.isMarkedToReformat(someLeaf.getNode());
                boolean isMarkedToReformatBefore = someLeaf.getNode() instanceof com.intellij.psi.impl.source.tree.TreeElement
                    && CodeEditUtil.isMarkedToReformatBefore((com.intellij.psi.impl.source.tree.TreeElement) someLeaf.getNode());
                someLeaf.replace(ws);
                if (prevLeaf != null) {
                    PsiElement recoveredWs = PsiTreeUtil.nextLeaf(prevLeaf);
                    if (recoveredWs != null) {
                        CodeEditUtil.setNodeGenerated(recoveredWs.getNode(), false);
                        if (isMarkedToReformat) {
                            CodeEditUtil.markToReformat(recoveredWs.getNode(), true);
                        }
                        if (isMarkedToReformatBefore) {
                            CodeEditUtil.markToReformatBefore(recoveredWs.getNode(), true);
                        }
                    }
                }
            } else {
                PsiElement parent = someLeaf.getParent();
                PsiElement ws = PsiParserFacade.getInstance(originalFile.getProject()).createWhiteSpaceFromText(" ");
                CodeEditUtil.setNodeGenerated(ws.getNode(), false);
                parent.addAfter(ws, someLeaf);
                someLeaf.getNextSibling().delete();
            }
        }
    }
}
