/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import consulo.execution.ui.awt.ListTableWithButtons;
import consulo.project.Project;
import consulo.ui.ex.awt.ComponentValidator;
import consulo.ui.ex.awt.table.ComboBoxTableRenderer;
import consulo.ui.ex.awt.ValidationInfo;


import com.intellij.ui.components.fields.ExtendableTextField;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.ex.awt.table.ListTableModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.rust.settings.RsCodeInsightSettings;
import org.rust.settings.ExcludedPath;
import org.rust.settings.ExclusionType;

public class RsPathsExcludeTable extends ListTableWithButtons<RsPathsExcludeTable.Item> {

    private static final Pattern PATH_PATTERN = Pattern.compile("(\\w+::)*\\w+(::\\*)?");

    private final RsCodeInsightSettings globalSettings;
    private final RsProjectCodeInsightSettings projectSettings;

    public RsPathsExcludeTable(@Nonnull Project project) {
        globalSettings = RsCodeInsightSettings.getInstance();
        projectSettings = RsProjectCodeInsightSettings.getInstance(project);
    }

    @Nonnull
    private List<Item> getSettingsItems() {
        List<Item> result = new ArrayList<>();
        for (ExcludedPath p : globalSettings.getExcludedPaths()) {
            result.add(new Item(p.path, p.type, ExclusionScope.IDE));
        }
        for (ExcludedPath p : projectSettings.getState().getExcludedPaths()) {
            result.add(new Item(p.path, p.type, ExclusionScope.Project));
        }
        return result;
    }

    @Nonnull
    private List<Item> getCurrentItems() {
        return getTableView().getListTableModel().getItems();
    }

    @Nonnull
    private ExcludedPath[] getCurrentItems(@Nonnull ExclusionScope scope) {
        return getCurrentItems().stream()
            .filter(it -> it.scope == scope)
            .map(it -> new ExcludedPath(it.path, it.type))
            .toArray(ExcludedPath[]::new);
    }

    public boolean isModified() {
        return !getSettingsItems().equals(getCurrentItems());
    }

    public void apply() {
        globalSettings.setExcludedPaths(getCurrentItems(ExclusionScope.IDE));
        projectSettings.getState().setExcludedPaths(getCurrentItems(ExclusionScope.Project));
    }

    public void reset() {
        setValues(getSettingsItems());
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    protected ListTableModel<Item> createListModel() {
        return new ListTableModel<>(PATH_COLUMN, TYPE_COLUMN, SCOPE_COLUMN);
    }

    @Nonnull
    @Override
    protected Item createElement() {
        return new Item("", ExclusionType.ItemsAndMethods, ExclusionScope.IDE);
    }

    @Override
    protected boolean isEmpty(@Nonnull Item item) {
        return item.path.isEmpty();
    }

    @Override
    protected boolean canDeleteElement(@Nonnull Item item) {
        return true;
    }

    @Nonnull
    @Override
    protected Item cloneElement(@Nonnull Item item) {
        return new Item(item.path, item.type, item.scope);
    }

    public static class Item {
        public String path;
        public ExclusionType type;
        public ExclusionScope scope;

        public Item(@Nonnull String path, @Nonnull ExclusionType type, @Nonnull ExclusionScope scope) {
            this.path = path;
            this.type = type;
            this.scope = scope;
        }

        public Item copy() {
            return new Item(path, type, scope);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return Objects.equals(path, item.path) && type == item.type && scope == item.scope;
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, type, scope);
        }
    }

    public enum ExclusionScope {
        Project,
        IDE
    }

    @SuppressWarnings("DialogTitleCapitalization")
    private static final ColumnInfo<Item, String> PATH_COLUMN = new ColumnInfo<Item, String>(RsBundle.message("column.name.item.or.module")) {
        @Nullable
        @Override
        public String valueOf(Item item) {
            return item.path;
        }

        @Override
        public boolean isCellEditable(Item item) {
            return true;
        }

        @Override
        public void setValue(Item item, String value) {
            item.path = value;
        }

        @Nonnull
        @Override
        public TableCellEditor getEditor(Item item) {
            ExtendableTextField cellEditor = new ExtendableTextField();
            // COMPACT_PROPERTY not available in Consulo
            ComponentValidator validator = new ComponentValidator(RsCodeInsightSettings.getInstance());
            validator.withValidator((Supplier<ValidationInfo>) () -> getValidationInfo(cellEditor.getText(), cellEditor))
                .andRegisterOnDocumentListener(cellEditor).installOn(cellEditor);
            return new DefaultCellEditor(cellEditor);
        }

        @Nonnull
        @Override
        public TableCellRenderer getRenderer(Item item) {
            return new DefaultTableCellRenderer();
        }

        @Nullable
        private ValidationInfo getValidationInfo(@Nullable String path, @Nullable JComponent component) {
            if (path == null || path.isEmpty() || PATH_PATTERN.matcher(path).matches()) return null;
            String errorText = RsBundle.message("dialog.message.illegal.path", path);
            return new ValidationInfo(errorText, component);
        }
    };

    private static final ColumnInfo<Item, ExclusionType> TYPE_COLUMN = new ComboboxColumnInfo<>(ExclusionType.values(), RsBundle.message("column.name.apply.to")) {
        @Nonnull
        @Override
        protected String displayText(@Nonnull ExclusionType value) {
            return switch (value) {
                case ItemsAndMethods -> RsBundle.message("label.everything");
                case Methods -> RsBundle.message("label.methods.only");
            };
        }

        @Nullable
        @Override
        public ExclusionType valueOf(Item item) {
            return item.type;
        }

        @Override
        public void setValue(Item item, ExclusionType value) {
            item.type = value;
        }
    };

    private static final ColumnInfo<Item, ExclusionScope> SCOPE_COLUMN = new ComboboxColumnInfo<>(ExclusionScope.values(), RsBundle.message("column.name.scope")) {
        @Nullable
        @Override
        public ExclusionScope valueOf(Item item) {
            return item.scope;
        }

        @Override
        public void setValue(Item item, ExclusionScope value) {
            item.scope = value;
        }
    };

    private static abstract class ComboboxColumnInfo<T extends Enum<T>> extends ColumnInfo<Item, T> {
        private final T[] values;
        private final ComboBoxTableRenderer<T> renderer;

        ComboboxColumnInfo(@Nonnull T[] values,  @Nonnull String name) {
            super(name);
            this.values = values;
            this.renderer = new ComboBoxTableRenderer<>(values) {
                @Nonnull
                @Override
                protected String getTextFor(@Nonnull T value) {
                    return displayText(value);
                }
            };
        }

        
        @Nonnull
        protected String displayText(@Nonnull T value) {
             String text = value.toString();
            return text;
        }

        @Override
        public boolean isCellEditable(Item item) {
            return true;
        }

        @Nonnull
        @Override
        public TableCellRenderer getRenderer(Item pair) {
            return renderer;
        }

        @Nonnull
        @Override
        public TableCellEditor getEditor(Item pair) {
            return renderer;
        }

        @Nonnull
        @Override
        public String getMaxStringValue() {
            String max = "";
            for (T value : values) {
                String text = displayText(value);
                if (text.length() > max.length()) {
                    max = text;
                }
            }
            return max;
        }

        @Override
        public int getAdditionalWidth() {
            return JBUIScale.scale(12) + 16;
        }
    }
}
