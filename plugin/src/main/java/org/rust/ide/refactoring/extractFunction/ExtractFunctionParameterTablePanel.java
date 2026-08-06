/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import consulo.language.editor.refactoring.ui.AbstractParameterTablePanel;
import consulo.language.editor.refactoring.extractMethod.AbstractVariableData;
import consulo.ui.ex.awt.table.BooleanTableCellEditor;
import consulo.ui.ex.awt.BooleanTableCellRenderer;
import consulo.ui.ex.awt.ColumnInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExtractFunctionParameterTablePanel extends AbstractParameterTablePanel<ExtractFunctionParameterTablePanel.ParameterDataHolder> {
    private static final int WIDTH = 40;
    @Nonnull
    private final RsExtractFunctionConfig myConfig;
    @Nonnull
    private final Runnable myOnChange;

    @SuppressWarnings("unchecked")
    public ExtractFunctionParameterTablePanel(
        @Nonnull Predicate<String> nameValidator,
        @Nonnull RsExtractFunctionConfig config,
        @Nonnull Runnable onChange
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
        @Nonnull
        final Parameter myParameter;
        @Nonnull
        final Runnable myOnChange;

        ParameterDataHolder(@Nonnull Parameter parameter, @Nonnull Runnable onChange) {
            myParameter = parameter;
            myOnChange = onChange;
        }

        void changeName(@Nonnull String name) {
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
            super((String) null);
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
        @Nonnull
        private final Predicate<String> myNameValidator;

        NameColumn(@Nonnull Predicate<String> nameValidator) {
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
