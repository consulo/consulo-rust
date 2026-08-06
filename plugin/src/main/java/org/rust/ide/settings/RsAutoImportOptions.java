/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import consulo.configurable.UnnamedConfigurable;
import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.VerticalFlowLayout;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Auto-import settings panel for Rust. Simplified port — the IntelliJ version used
 * UiDslUnnamedConfigurable.Simple and the kotlin-ui-dsl `bind` API; here we
 * build the same checkboxes + exclude-table with plain Swing.
 */
public class RsAutoImportOptions implements UnnamedConfigurable {

    private final RsPathsExcludeTable excludeTable;
    private JBCheckBox showImportPopup;
    private JBCheckBox importOutOfScopeItems;
    private JBCheckBox importOnPaste;
    private JBCheckBox addUnambiguousImportsOnTheFly;

    public RsAutoImportOptions(@Nonnull Project project) {
        this.excludeTable = new RsPathsExcludeTable(project);
    }

    @Override
    public JComponent createComponent() {
        showImportPopup = new JBCheckBox(RsBundle.message("settings.rust.auto.import.show.popup"));
        importOutOfScopeItems = new JBCheckBox(RsBundle.message("settings.rust.auto.import.on.completion"));
        importOnPaste = new JBCheckBox(RsBundle.message("settings.rust.auto.import.on.paste"));
        addUnambiguousImportsOnTheFly = new JBCheckBox(RsBundle.message("settings.rust.auto.import.on.the.fly", "Add unambiguous imports"));
        JPanel panel = new JPanel(new VerticalFlowLayout());
        panel.add(showImportPopup);
        panel.add(importOutOfScopeItems);
        panel.add(importOnPaste);
        panel.add(addUnambiguousImportsOnTheFly);
        panel.add(Box.createVerticalStrut(8));
        panel.add(excludeTable.getComponent());
        return panel;
    }

    @Override
    public boolean isModified() {
        RsCodeInsightSettings s = RsCodeInsightSettings.getInstance();
        return showImportPopup.isSelected() != s.showImportPopup
            || importOutOfScopeItems.isSelected() != s.importOutOfScopeItems
            || importOnPaste.isSelected() != s.importOnPaste
            || addUnambiguousImportsOnTheFly.isSelected() != s.addUnambiguousImportsOnTheFly
            || excludeTable.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        RsCodeInsightSettings s = RsCodeInsightSettings.getInstance();
        s.showImportPopup = showImportPopup.isSelected();
        s.importOutOfScopeItems = importOutOfScopeItems.isSelected();
        s.importOnPaste = importOnPaste.isSelected();
        s.addUnambiguousImportsOnTheFly = addUnambiguousImportsOnTheFly.isSelected();
        excludeTable.apply();
    }

    @Override
    public void reset() {
        RsCodeInsightSettings s = RsCodeInsightSettings.getInstance();
        showImportPopup.setSelected(s.showImportPopup);
        importOutOfScopeItems.setSelected(s.importOutOfScopeItems);
        importOnPaste.setSelected(s.importOnPaste);
        addUnambiguousImportsOnTheFly.setSelected(s.addUnambiguousImportsOnTheFly);
        excludeTable.reset();
    }
}
