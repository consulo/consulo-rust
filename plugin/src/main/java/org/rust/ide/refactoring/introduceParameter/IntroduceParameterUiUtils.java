/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceParameter;

import consulo.navigation.NavigationUtil;
import consulo.language.editor.refactoring.unwrap.ScopeHighlighter;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.refactoring.ExtraxtExpressionUiUtils;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class IntroduceParameterUiUtils {

    private IntroduceParameterUiUtils() {
    }

    public static void showEnclosingFunctionsChooser(
        @Nonnull Editor editor,
        @Nonnull List<RsFunction> methods,
        @Nonnull Consumer<RsFunction> callback
    ) {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode() && methods.size() > 1) {
            callback.accept(ExtraxtExpressionUiUtils.MOCK.chooseMethod(methods));
            return;
        }
        AtomicReference<ScopeHighlighter> highlighter = new AtomicReference<>(new ScopeHighlighter(editor));
        String title = RsBundle.message("introduce.parameter.to.method");
        var popup = JBPopupFactory.getInstance().createPopupChooserBuilder(methods)
            .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            .setSelectedValue(methods.get(0), true)
            .setAccessibleName(title)
            .setTitle(title)
            .setMovable(false)
            .setResizable(false)
            .setRequestFocus(true)
            .setItemChosenCallback(callback::accept)
            .addListener(new JBPopupListener() {
                @Override
                public void onClosed(@Nonnull LightweightWindowEvent event) {
                    ScopeHighlighter h = highlighter.getAndSet(null);
                    if (h != null) {
                        h.dropHighlight();
                    }
                }
            })
            .setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
                ) {
                    Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    setText(RsFunctionUtil.getTitle((RsFunction) value));
                    return rendererComponent;
                }
            }).createPopup();
        popup.showInBestPositionFor(consulo.dataContext.DataManager.getInstance().getDataContext(editor.getContentComponent()));
        Project project = editor.getProject();
        if (project != null) {
            consulo.language.editor.ui.PopupNavigationUtil.hidePopupIfDumbModeStarts(popup, project);
        }
    }
}
