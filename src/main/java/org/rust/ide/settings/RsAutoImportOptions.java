/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import com.intellij.application.options.editor.AutoImportOptionsProvider;
import com.intellij.openapi.options.UiDslUnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.dsl.builder.Cell;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.ui.dsl.builder.MutableProperty;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;

import com.intellij.ui.components.JBCheckBox;

/**
 * Auto-import settings panel for Rust.
 *
 */
public class RsAutoImportOptions extends UiDslUnnamedConfigurable.Simple implements AutoImportOptionsProvider {

    private final RsPathsExcludeTable excludeTable;

    public RsAutoImportOptions(@NotNull Project project) {
        this.excludeTable = new RsPathsExcludeTable(project);
    }

    @Override
    public void createContent(@NotNull Panel panel) {
        RsCodeInsightSettings settings = RsCodeInsightSettings.getInstance();
        panel.group(RsBundle.message("settings.rust.auto.import.title"), false, builder -> {
            builder.row((String) null, row -> {
                Cell<JBCheckBox> cell = row.checkBox(RsBundle.message("settings.rust.auto.import.show.popup"));
                cell.bind(JBCheckBox::isSelected, (cb, v) -> { cb.setSelected(v); return Unit.INSTANCE; },
                    new MutableProperty<>() {
                        @Override public Boolean get() { return settings.showImportPopup; }
                        @Override public void set(Boolean v) { settings.showImportPopup = v; }
                    });
                return Unit.INSTANCE;
            });
            builder.row((String) null, row -> {
                Cell<JBCheckBox> cell = row.checkBox(RsBundle.message("settings.rust.auto.import.on.completion"));
                cell.bind(JBCheckBox::isSelected, (cb, v) -> { cb.setSelected(v); return Unit.INSTANCE; },
                    new MutableProperty<>() {
                        @Override public Boolean get() { return settings.importOutOfScopeItems; }
                        @Override public void set(Boolean v) { settings.importOutOfScopeItems = v; }
                    });
                return Unit.INSTANCE;
            });
            builder.row((String) null, row -> {
                Cell<JBCheckBox> cell = row.checkBox(RsBundle.message("settings.rust.auto.import.on.paste"));
                cell.bind(JBCheckBox::isSelected, (cb, v) -> { cb.setSelected(v); return Unit.INSTANCE; },
                    new MutableProperty<>() {
                        @Override public Boolean get() { return settings.importOnPaste; }
                        @Override public void set(Boolean v) { settings.importOnPaste = v; }
                    });
                return Unit.INSTANCE;
            });
            builder.row((String) null, row -> {
                Cell<JBCheckBox> cell = row.checkBox(com.intellij.openapi.application.ApplicationBundle.message("checkbox.add.unambiguous.imports.on.the.fly"));
                cell.bind(JBCheckBox::isSelected, (cb, v) -> { cb.setSelected(v); return Unit.INSTANCE; },
                    new MutableProperty<>() {
                        @Override public Boolean get() { return settings.addUnambiguousImportsOnTheFly; }
                        @Override public void set(Boolean v) { settings.addUnambiguousImportsOnTheFly = v; }
                    });
                return Unit.INSTANCE;
            });
            builder.row((String) null, row -> {
                org.rust.openapiext.UiUtil.fullWidthCell(row, excludeTable.getComponent())
                    .label(RsBundle.message("settings.rust.auto.import.exclude.label"), com.intellij.ui.dsl.builder.LabelPosition.TOP)
                    .comment(RsBundle.message("settings.rust.auto.import.exclude.comment"), 100, com.intellij.ui.dsl.builder.HyperlinkEventAction.HTML_HYPERLINK_INSTANCE)
                    .onApply(() -> { excludeTable.apply(); return Unit.INSTANCE; })
                    .onReset(() -> { excludeTable.reset(); return Unit.INSTANCE; })
                    .onIsModified(() -> excludeTable.isModified());
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        });
    }
}
