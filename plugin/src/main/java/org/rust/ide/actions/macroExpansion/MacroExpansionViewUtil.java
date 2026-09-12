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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsFileType;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.RsProcMacroPsiUtil;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiManager;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.stream.Collectors;
import consulo.undoRedo.util.UndoUtil;

/**
 * Utilities for expanding macros and displaying the expansion results.
 */
public final class MacroExpansionViewUtil {

    private MacroExpansionViewUtil() {
    }

    /**
     * Expands a macro in background thread with progress bar.
     */
    @Nonnull
    public static RsResult<MacroExpansionViewDetails, GetMacroExpansionError> expandMacroForViewWithProgress(
        @Nonnull Project project,
        @Nonnull RsPossibleMacroCall ctx,
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

    /**
     * Shows macro expansion in floating popup.
     */
    public static void showMacroExpansionPopup(
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull MacroExpansionViewDetails expansionDetails
    ) {
        if (expansionDetails.expansion().getElements().isEmpty()) return;

        MacroExpansion formattedExpansion = reformatMacroExpansion(
            expansionDetails.macroToExpand(),
            expansionDetails.expansion()
        );

        JPanel component = createMacroExpansionViewComponent(project, formattedExpansion);

        var popup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, component)
            .setProject(project)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setTitle(expansionDetails.title())
            .createPopup();

        PopupPositionManager.positionPopupInBestPosition(popup, editor, null);
    }

    @Nonnull
    public static EditorHighlighter createRustHighlighter(@Nonnull Project project) {
        return HighlighterFactory.createHighlighter(project, RsFileType.INSTANCE);
    }

    @Nonnull
    private static RsResult<MacroExpansionViewDetails, GetMacroExpansionError> expandMacroForView(
        @Nonnull RsPossibleMacroCall macroToExpand,
        boolean expandRecursively
    ) {
        RsResult<MacroExpansion, GetMacroExpansionError> singleStepExpansion = RsPossibleMacroCallUtil.getExpansionResult(macroToExpand);
        if (singleStepExpansion instanceof RsResult.Err) {
            return (RsResult.Err<MacroExpansionViewDetails, GetMacroExpansionError>) (RsResult<?, GetMacroExpansionError>) singleStepExpansion;
        }

        int depthLimit = expandRecursively ? Integer.MAX_VALUE : 1;
        String expansionText = RsPossibleMacroCallUtil.expandMacrosRecursively(macroToExpand, depthLimit, true);

        MacroExpansion parseResult = RsExpandedElementUtil.parseExpandedTextWithContext(
            RsPossibleMacroCallUtil.getExpansionContext(macroToExpand),
            new RsPsiFactory(macroToExpand.getProject(), false, true),
            expansionText
        );

        if (parseResult != null) {
            return new RsResult.Ok<>(new MacroExpansionViewDetails(
                macroToExpand,
                getMacroExpansionViewTitle(macroToExpand, expandRecursively),
                parseResult
            ));
        } else {
            return new RsResult.Err<>(new GetMacroExpansionError.MemExpParsingError(
                expansionText, RsPossibleMacroCallUtil.getExpansionContext(macroToExpand)
            ));
        }
    }

    @Nonnull
    private static String getMacroExpansionViewTitle(
        @Nonnull RsPossibleMacroCall macroToExpand,
        boolean expandRecursively
    ) {
        String path = macroToExpand.getPath() != null ? macroToExpand.getPath().getText() : "";
        RsPossibleMacroCallKind kind = RsPossibleMacroCallUtil.getKind(macroToExpand);
        String name;
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            name = RsBundle.message("popup.title.macro", path);
        } else if (kind instanceof RsPossibleMacroCallKind.MetaItem) {
            if (RsProcMacroPsiUtil.canBeCustomDerive(((RsPossibleMacroCallKind.MetaItem) kind).meta)) {
                name = "#[derive(" + path + ")]";
            } else {
                name = "#[" + path + "]";
            }
        } else {
            name = path;
        }

        return expandRecursively
            ? RsBundle.message("popup.title.recursive.expansion", name)
            : RsBundle.message("popup.title.first.level.expansion", name);
    }

    @Nonnull
    private static MacroExpansion reformatMacroExpansion(
        @Nonnull RsPossibleMacroCall macroToExpand,
        @Nonnull MacroExpansion expansion
    ) {
        PsiFile file = expansion.getFile();
        if (file.getVirtualFile() != null) {
            file = new RsPsiFactory(expansion.getFile().getProject(), false, true).createFile(expansion.getFile().getText());
        }

        PsiFile finalFile = file;
        RsPsiManager.withIgnoredPsiEvents(finalFile, () -> {
            consulo.undoRedo.util.UndoUtil.writeInRunUndoTransparentAction(() -> {
                CodeStyleManager.getInstance(finalFile.getProject())
                    .reformatText(finalFile, finalFile.getTextRange().getStartOffset(), finalFile.getTextRange().getEndOffset());
            });
        });

        MacroExpansion result = RsExpandedElementUtil.getExpansionFromExpandedFile(RsPossibleMacroCallUtil.getExpansionContext(macroToExpand), file);
        if (result == null) {
            throw new IllegalStateException("Can't recover macro expansion after reformat");
        }
        return result;
    }

    @Nonnull
    private static JPanel createMacroExpansionViewComponent(@Nonnull Project project, @Nonnull MacroExpansion expansion) {
        EditorFactory factory = EditorFactory.getInstance();
        String text = expansion.getElements().stream()
            .map(PsiElement::getText)
            .collect(Collectors.joining("\n"));
        var doc = factory.createDocument(text);
        doc.setReadOnly(true);
        EditorEx editor = (EditorEx) factory.createEditor(doc, project);

        editor.getSettings().setAdditionalLinesCount(1);
        editor.getSettings().setAdditionalColumnsCount(1);
        editor.getSettings().setLineMarkerAreaShown(false);
        editor.getSettings().setIndentGuidesShown(false);
        editor.getSettings().setLineNumbersShown(false);
        editor.getSettings().setFoldingOutlineShown(false);
        editor.setHighlighter(createRustHighlighter(project));

        JPanel panel = new JPanel(new BorderLayout()) {
            private boolean isEditorReleased = false;

            @Override
            public void removeNotify() {
                super.removeNotify();
                if (ScreenUtil.isStandardAddRemoveNotify(this) && !isEditorReleased) {
                    isEditorReleased = true;
                    EditorFactory.getInstance().releaseEditor(editor);
                }
            }
        };
        panel.add(editor.getComponent());
        return panel;
    }
}
