/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import consulo.language.editor.highlight.HighlighterFactory;
import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorHighlighter;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ide.impl.idea.ui.popup.PopupPositionManager;
import consulo.document.util.DocumentUtil;
import org.rust.RsBundle;
import org.rust.lang.RsFileType;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.impl.RsProcMacroPsiUtil;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiManager;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.stream.Collectors;
import consulo.codeEditor.EditorSettings;
import consulo.document.Document;
import consulo.ui.ex.popup.JBPopup;
import consulo.undoRedo.util.UndoUtil;
import org.rust.lang.core.psi.ext.impl.*;

/** Utility class for macro expansion view operations. */
public final class MacroExpansionViewUtils {

    private MacroExpansionViewUtils() {}

    /**
     * This method expands macro in background thread with progress bar showing on,
     * allowing user to close it if expansion takes too long.
     */
    public static RsResult<MacroExpansionViewDetails, GetMacroExpansionError> expandMacroForViewWithProgress(
        Project project,
        RsPossibleMacroCall ctx,
        boolean expandRecursively
    ) {
        String progressTitle = RsBundle.message(
            "progress.title.choice.recursive.single.step.expansion.progress",
            expandRecursively ? 0 : 1
        );
        return OpenApiUtil.computeWithCancelableProgress(project, progressTitle, () ->
            ReadAction.compute(() -> expandMacroForView(ctx, expandRecursively))
        );
    }

    /** This function shows macro expansion in floating popup. */
    public static void showMacroExpansionPopup(Project project, Editor editor, MacroExpansionViewDetails expansionDetails) {
        if (expansionDetails.expansion().getElements().isEmpty()) return;

        MacroExpansion formattedExpansion = reformatMacroExpansion(expansionDetails.macroToExpand(), expansionDetails.expansion());

        MacroExpansionViewComponent component = new MacroExpansionViewComponent(formattedExpansion);

        consulo.ui.ex.popup.JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, component)
            .setProject(project)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setTitle(expansionDetails.title())
            .createPopup();

        PopupPositionManager.positionPopupInBestPosition(popup, editor, null);
    }

    private static RsResult<MacroExpansionViewDetails, GetMacroExpansionError> expandMacroForView(
        RsPossibleMacroCall macroToExpand,
        boolean expandRecursively
    ) {
        RsResult<MacroExpansion, GetMacroExpansionError> expansions = getMacroExpansions(macroToExpand, expandRecursively);
        if (expansions instanceof RsResult.Err) {
            return new RsResult.Err<>(((RsResult.Err<MacroExpansion, GetMacroExpansionError>) expansions).getErr());
        }
        MacroExpansion expansion = ((RsResult.Ok<MacroExpansion, GetMacroExpansionError>) expansions).getOk();
        return new RsResult.Ok<>(new MacroExpansionViewDetails(
            macroToExpand,
            getMacroExpansionViewTitle(macroToExpand, expandRecursively),
            expansion
        ));
    }

    private static String getMacroExpansionViewTitle(RsPossibleMacroCall macroToExpand, boolean expandRecursively) {
        String path = macroToExpand.getPath() != null ? macroToExpand.getPath().getText() : null;
        RsPossibleMacroCallKind kind = RsPossibleMacroCallUtil.getKind(macroToExpand);
        String name;
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            name = RsBundle.message("popup.title.macro", path != null ? path : "");
        } else if (kind instanceof RsPossibleMacroCallKind.MetaItem metaItem) {
            if (RsProcMacroPsiUtil.canBeCustomDerive(metaItem.meta)) {
                name = "#[derive(" + path + ")]";
            } else {
                name = "#[" + path + "]";
            }
        } else {
            name = "";
        }
        return expandRecursively
            ? RsBundle.message("popup.title.recursive.expansion", name)
            : RsBundle.message("popup.title.first.level.expansion", name);
    }

    private static RsResult<MacroExpansion, GetMacroExpansionError> getMacroExpansions(
        RsPossibleMacroCall macroToExpand,
        boolean expandRecursively
    ) {
        RsResult<MacroExpansion, GetMacroExpansionError> singleStepExpansion = RsPossibleMacroCallUtil.getExpansionResult(macroToExpand);
        if (singleStepExpansion instanceof RsResult.Err) {
            return singleStepExpansion;
        }

        int depthLimit = expandRecursively ? Integer.MAX_VALUE : 1;
        String expansionText = RsPossibleMacroCallUtil.expandMacrosRecursively(macroToExpand, depthLimit, true, call -> RsPossibleMacroCallUtil.getExpansion(call));

        MacroExpansion parsedResult = RsExpandedElementUtil.parseExpandedTextWithContext(
            RsExpandedElementUtil.getExpansionContext(macroToExpand),
            new RsPsiFactory(macroToExpand.getProject(), false, true),
            expansionText
        );
        if (parsedResult == null) {
            return new RsResult.Err<>(new GetMacroExpansionError.MemExpParsingError(
                expansionText,
                RsExpandedElementUtil.getExpansionContext(macroToExpand)
            ));
        }
        return new RsResult.Ok<>(parsedResult);
    }

    private static MacroExpansion reformatMacroExpansion(
        RsPossibleMacroCall macroToExpand,
        MacroExpansion expansion
    ) {
        PsiFile file = expansion.getFile();
        if (file.getVirtualFile() != null) {
            file = new RsPsiFactory(expansion.getFile().getProject(), false, true)
                .createFile(expansion.getFile().getText());
        }

        PsiFile finalFile = file;
        RsPsiManager.withIgnoredPsiEvents(finalFile, () -> {
            consulo.undoRedo.util.UndoUtil.writeInRunUndoTransparentAction(() -> formatPsiFile(finalFile));
            return null;
        });

        MacroExpansion result = RsExpandedElementUtil.getExpansionFromExpandedFile(
            RsExpandedElementUtil.getExpansionContext(macroToExpand), (RsFile) finalFile
        );
        if (result == null) {
            throw new IllegalStateException("Can't recover macro expansion after reformat");
        }
        return result;
    }

    private static void formatPsiFile(PsiFile element) {
        CodeStyleManager.getInstance(element.getProject()).reformatText(element, element.getTextOffset(), element.getTextLength());
    }

    public static EditorEx createReadOnlyEditorWithElements(Project project, Collection<PsiElement> expansions) {
        EditorFactory factory = EditorFactory.getInstance();
        String text = expansions.stream().map(PsiElement::getText).collect(Collectors.joining("\n"));
        consulo.document.Document doc = factory.createDocument(text);
        doc.setReadOnly(true);
        return (EditorEx) factory.createEditor(doc, project);
    }

    public static EditorHighlighter createRustHighlighter(Project project) {
        return HighlighterFactory.createHighlighter(project, RsFileType.INSTANCE);
    }

    /** Simple view to show some code. */
    private static class MacroExpansionViewComponent extends JPanel {
        private final EditorEx editor;
        private boolean isEditorReleased = false;

        MacroExpansionViewComponent(MacroExpansion expansion) {
            super(new BorderLayout());
            if (expansion.getElements().isEmpty()) {
                throw new IllegalArgumentException("Must be at least one expansion!");
            }
            Project project = expansion.getFile().getProject();

            editor = createReadOnlyEditorWithElements(project, new java.util.ArrayList<>(expansion.getElements()));
            setupSimpleEditorLook(editor);
            editor.setHighlighter(createRustHighlighter(project));

            add(editor.getComponent());
        }

        private void setupSimpleEditorLook(EditorEx editor) {
            consulo.codeEditor.EditorSettings settings = editor.getSettings();
            settings.setAdditionalLinesCount(1);
            settings.setAdditionalColumnsCount(1);
            settings.setLineMarkerAreaShown(false);
            settings.setIndentGuidesShown(false);
            settings.setLineNumbersShown(false);
            settings.setFoldingOutlineShown(false);
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            if (ScreenUtil.isStandardAddRemoveNotify(this) && !isEditorReleased) {
                isEditorReleased = true;
                EditorFactory.getInstance().releaseEditor(editor);
            }
        }
    }
}
