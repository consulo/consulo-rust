/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import com.intellij.execution.util.ListTableWithButtons;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.cellvalidators.ValidatingTableCellRendererWrapper;
import com.intellij.openapi.ui.cellvalidators.ValidationUtils;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

public class RsPathsExcludeTable extends ListTableWithButtons<RsPathsExcludeTable.Item> {

    private static final Pattern PATH_PATTERN = Pattern.compile("(\\w+::)*\\w+(::\\*)?");

    private final RsCodeInsightSettings globalSettings;
    private final RsProjectCodeInsightSettings projectSettings;

    public RsPathsExcludeTable(@NotNull Project project) {
        globalSettings = RsCodeInsightSettings.getInstance();
        projectSettings = RsProjectCodeInsightSettings.getInstance(project);
    }

    @NotNull
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

    @NotNull
    private List<Item> getCurrentItems() {
        return getTableView().getListTableModel().getItems();
    }

    @NotNull
    private ExcludedPath[] getCurrentItems(@NotNull ExclusionScope scope) {
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
    @NotNull
    @Override
    protected ListTableModel<Item> createListModel() {
        return new ListTableModel<>(PATH_COLUMN, TYPE_COLUMN, SCOPE_COLUMN);
    }

    @NotNull
    @Override
    protected Item createElement() {
        return new Item("", ExclusionType.ItemsAndMethods, ExclusionScope.IDE);
    }

    @Override
    protected boolean isEmpty(@NotNull Item item) {
        return item.path.isEmpty();
    }

    @Override
    protected boolean canDeleteElement(@NotNull Item item) {
        return true;
    }

    @NotNull
    @Override
    protected Item cloneElement(@NotNull Item item) {
        return new Item(item.path, item.type, item.scope);
    }

    public static class Item {
        public String path;
        public ExclusionType type;
        public ExclusionScope scope;

        public Item(@NotNull String path, @NotNull ExclusionType type, @NotNull ExclusionScope scope) {
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
    private static final ColumnInfo<Item, String> PATH_COLUMN = new ColumnInfo<>(RsBundle.message("column.name.item.or.module")) {
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

        @NotNull
        @Override
        public TableCellEditor getEditor(Item item) {
            ExtendableTextField cellEditor = new ExtendableTextField();
            cellEditor.putClientProperty(DarculaUIUtil.COMPACT_PROPERTY, true);
            ComponentValidator validator = new ComponentValidator(RsCodeInsightSettings.getInstance());
            validator.withValidator((Supplier<ValidationInfo>) () -> {
                ValidationInfo error = getValidationInfo(cellEditor.getText(), cellEditor);
                ValidationUtils.setExtension(cellEditor, ValidationUtils.ERROR_EXTENSION, error != null);
                return error;
            }).andRegisterOnDocumentListener(cellEditor).installOn(cellEditor);
            return new DefaultCellEditor(cellEditor);
        }

        @NotNull
        @Override
        public TableCellRenderer getRenderer(Item item) {
            JTextField cellEditor = new JTextField();
            cellEditor.putClientProperty(DarculaUIUtil.COMPACT_PROPERTY, true);
            @SuppressWarnings("UnstableApiUsage")
            ValidatingTableCellRendererWrapper wrapper = new ValidatingTableCellRendererWrapper(new DefaultTableCellRenderer())
                .withCellValidator((value, row, column) -> getValidationInfo(value != null ? value.toString() : null, null))
                .bindToEditorSize(cellEditor::getPreferredSize);
            return wrapper;
        }

        @Nullable
        private ValidationInfo getValidationInfo(@Nullable String path, @Nullable JComponent component) {
            if (path == null || path.isEmpty() || PATH_PATTERN.matcher(path).matches()) return null;
            String errorText = RsBundle.message("dialog.message.illegal.path", path);
            return new ValidationInfo(errorText, component);
        }
    };

    private static final ColumnInfo<Item, ExclusionType> TYPE_COLUMN = new ComboboxColumnInfo<>(ExclusionType.values(), RsBundle.message("column.name.apply.to")) {
        @NotNull
        @Override
        protected String displayText(@NotNull ExclusionType value) {
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

        ComboboxColumnInfo(@NotNull T[] values, @NlsContexts.ColumnName @NotNull String name) {
            super(name);
            this.values = values;
            this.renderer = new ComboBoxTableRenderer<>(values) {
                @NotNull
                @Override
                protected String getTextFor(@NotNull T value) {
                    return displayText(value);
                }
            };
        }

        @NlsContexts.Label
        @NotNull
        protected String displayText(@NotNull T value) {
            @NlsSafe String text = value.toString();
            return text;
        }

        @Override
        public boolean isCellEditable(Item item) {
            return true;
        }

        @NotNull
        @Override
        public TableCellRenderer getRenderer(Item pair) {
            return renderer;
        }

        @NotNull
        @Override
        public TableCellEditor getEditor(Item pair) {
            return renderer;
        }

        @NotNull
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
            return JBUIScale.scale(12) + AllIcons.General.ArrowDown.getIconWidth();
        }
    }
}
