/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceParameter;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import org.jetbrains.annotations.NotNull;
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
        @NotNull Editor editor,
        @NotNull List<RsFunction> methods,
        @NotNull Consumer<RsFunction> callback
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
                public void onClosed(@NotNull LightweightWindowEvent event) {
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
        popup.showInBestPositionFor(editor);
        Project project = editor.getProject();
        if (project != null) {
            NavigationUtil.hidePopupIfDumbModeStarts(popup, project);
        }
    }
}
