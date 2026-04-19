/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import com.intellij.refactoring.util.AbstractParameterTablePanel;
import com.intellij.refactoring.util.AbstractVariableData;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExtractFunctionParameterTablePanel extends AbstractParameterTablePanel<ExtractFunctionParameterTablePanel.ParameterDataHolder> {
    private static final int WIDTH = 40;
    @NotNull
    private final RsExtractFunctionConfig myConfig;
    @NotNull
    private final Runnable myOnChange;

    @SuppressWarnings("unchecked")
    public ExtractFunctionParameterTablePanel(
        @NotNull Predicate<String> nameValidator,
        @NotNull RsExtractFunctionConfig config,
        @NotNull Runnable onChange
    ) {
        super(
            new ChooseColumn(),
            new NameColumn(nameValidator),
            new TypeColumn(),
            new MutabilityColumn()
        );
        myConfig = config;
        myOnChange = onChange;
        myTable.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
        myTable.setDefaultEditor(Boolean.class, new BooleanTableCellEditor());
        myTable.getColumnModel().getColumn(0).setPreferredWidth(WIDTH);
        myTable.getColumnModel().getColumn(0).setMaxWidth(WIDTH);
        ParameterDataHolder[] holders = config.getParameters().stream()
            .map(p -> new ParameterDataHolder(p, this::updateSignature))
            .toArray(ParameterDataHolder[]::new);
        init(holders);
    }

    @Override
    protected void doEnterAction() {
    }

    @Override
    protected void doCancelAction() {
    }

    @Override
    protected void updateSignature() {
        myConfig.setParameters(
            Arrays.stream(getVariableData())
                .map(h -> h.myParameter)
                .collect(Collectors.toList())
        );
        myOnChange.run();
    }

    public static class ParameterDataHolder extends AbstractVariableData {
        @NotNull
        final Parameter myParameter;
        @NotNull
        final Runnable myOnChange;

        ParameterDataHolder(@NotNull Parameter parameter, @NotNull Runnable onChange) {
            myParameter = parameter;
            myOnChange = onChange;
        }

        void changeName(@NotNull String name) {
            myParameter.setName(name);
            myOnChange.run();
        }

        void changeMutability(boolean mutable) {
            myParameter.setMutable(mutable);
            myOnChange.run();
        }
    }

    private static class ChooseColumn extends ColumnInfo<ParameterDataHolder, Boolean> {
        ChooseColumn() {
            super(null);
        }

        @Nullable
        @Override
        public Boolean valueOf(ParameterDataHolder item) {
            return item.myParameter.isSelected();
        }

        @Override
        public void setValue(ParameterDataHolder item, Boolean value) {
            item.myParameter.setSelected(value);
        }

        @Override
        public Class<?> getColumnClass() {
            return Boolean.class;
        }

        @Override
        public boolean isCellEditable(ParameterDataHolder item) {
            return true;
        }
    }

    private static class NameColumn extends ColumnInfo<ParameterDataHolder, String> {
        @NotNull
        private final Predicate<String> myNameValidator;

        NameColumn(@NotNull Predicate<String> nameValidator) {
            super(RsBundle.message("name"));
            myNameValidator = nameValidator;
        }

        @Nullable
        @Override
        public String valueOf(ParameterDataHolder item) {
            return item.myParameter.getName();
        }

        @Override
        public void setValue(ParameterDataHolder item, String value) {
            if (myNameValidator.test(value)) {
                item.changeName(value);
            }
        }

        @Override
        public boolean isCellEditable(ParameterDataHolder item) {
            return true;
        }
    }

    private static class TypeColumn extends ColumnInfo<ParameterDataHolder, String> {
        TypeColumn() {
            super(RsBundle.message("type"));
        }

        @Nullable
        @Override
        public String valueOf(ParameterDataHolder item) {
            return item.myParameter.getType() != null ? item.myParameter.getType().toString() : "_";
        }
    }

    private static class MutabilityColumn extends ColumnInfo<ParameterDataHolder, Boolean> {
        MutabilityColumn() {
            super(RsBundle.message("column.name.mutable"));
        }

        @Nullable
        @Override
        public Boolean valueOf(ParameterDataHolder item) {
            return item.myParameter.isMutable();
        }

        @Override
        public void setValue(ParameterDataHolder item, Boolean value) {
            item.changeMutability(value);
        }

        @Override
        public boolean isCellEditable(ParameterDataHolder item) {
            return true;
        }

        @Override
        public Class<?> getColumnClass() {
            return Boolean.class;
        }
    }
}
