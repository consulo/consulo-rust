/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.newProject.state.RsUserTemplate;
import org.rust.ide.newProject.state.RsUserTemplatesState;
import org.rust.openapiext.UiUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;
import org.rust.stdext.BuilderUtil;

public class AddUserTemplateDialog extends DialogWrapper {

    private static final List<String> KNOWN_URL_PREFIXES = List.of("http://", "https://");

    private final JBTextField repoUrlField;
    private final JBTextField nameField;

    public AddUserTemplateDialog() {
        super((com.intellij.openapi.project.Project) null);
        repoUrlField = new JBTextField();
        repoUrlField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                suggestName(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        nameField = new JBTextField();

        setTitle(RsBundle.message("dialog.create.project.custom.add.template.title"));
        setOKButtonText(RsBundle.message("dialog.create.project.custom.add.template.action.add"));
        init();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return repoUrlField;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return BuilderUtil.panel(builder -> {
            builder.row(RsBundle.message("dialog.create.project.custom.add.template.url"), row -> {
                UiUtil.fullWidthCell(row, repoUrlField)
                    .comment(RsBundle.message("dialog.create.project.custom.add.template.url.description"), 70, com.intellij.ui.dsl.builder.HyperlinkEventAction.HTML_HYPERLINK_INSTANCE);
                return null;
            });
            builder.row(RsBundle.message("dialog.create.project.custom.add.template.name"), row -> {
                UiUtil.fullWidthCell(row, nameField);
                return null;
            });
            return null;
        });
    }

    @Override
    protected void doOKAction() {
        // TODO: Find a better way to handle dialog form validation
        String name = UiUtil.trimmedText(nameField);
        String repoUrl = UiUtil.trimmedText(repoUrlField);

        if (name.isBlank()) return;
        if (RsUserTemplatesState.getInstance().templates.stream().anyMatch(t -> t.name.equals(name))) return;

        RsUserTemplatesState.getInstance().templates.add(new RsUserTemplate(name, repoUrl));

        super.doOKAction();
    }

    private void suggestName(@NotNull DocumentEvent event) {
        // Suggest name only if the whole URL was inserted
        if (event.getType() == DocumentEvent.EventType.INSERT && event.getLength() == event.getDocument().getLength()) {
            if (!nameField.getText().isBlank()) return;

            String repoUrl = UiUtil.trimmedText(repoUrlField);
            boolean startsWithKnownPrefix = false;
            for (String prefix : KNOWN_URL_PREFIXES) {
                if (repoUrl.startsWith(prefix)) {
                    startsWithKnownPrefix = true;
                    break;
                }
            }
            if (!startsWithKnownPrefix) return;

            String url = repoUrl;
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            if (url.endsWith(".git")) {
                url = url.substring(0, url.length() - 4);
            }
            int lastSlash = url.lastIndexOf('/');
            nameField.setText(lastSlash >= 0 ? url.substring(lastSlash + 1) : url);
        }
    }
}
