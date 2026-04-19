/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.popup.PopupPositionManager;
import com.intellij.util.DocumentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

/**
 * Utilities for expanding macros and displaying the expansion results.
 */
public final class MacroExpansionViewUtil {

    private MacroExpansionViewUtil() {
    }

    /**
     * Expands a macro in background thread with progress bar.
     */
    @NotNull
    public static RsResult<MacroExpansionViewDetails, GetMacroExpansionError> expandMacroForViewWithProgress(
        @NotNull Project project,
        @NotNull RsPossibleMacroCall ctx,
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
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull MacroExpansionViewDetails expansionDetails
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

    @NotNull
    public static EditorHighlighter createRustHighlighter(@NotNull Project project) {
        return HighlighterFactory.createHighlighter(project, RsFileType.INSTANCE);
    }

    @NotNull
    private static RsResult<MacroExpansionViewDetails, GetMacroExpansionError> expandMacroForView(
        @NotNull RsPossibleMacroCall macroToExpand,
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

    @NotNull
    private static String getMacroExpansionViewTitle(
        @NotNull RsPossibleMacroCall macroToExpand,
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

    @NotNull
    private static MacroExpansion reformatMacroExpansion(
        @NotNull RsPossibleMacroCall macroToExpand,
        @NotNull MacroExpansion expansion
    ) {
        PsiFile file = expansion.getFile();
        if (file.getVirtualFile() != null) {
            file = new RsPsiFactory(expansion.getFile().getProject(), false, true).createFile(expansion.getFile().getText());
        }

        PsiFile finalFile = file;
        RsPsiManager.withIgnoredPsiEvents(finalFile, () -> {
            DocumentUtil.writeInRunUndoTransparentAction(() -> {
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

    @NotNull
    private static JPanel createMacroExpansionViewComponent(@NotNull Project project, @NotNull MacroExpansion expansion) {
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
