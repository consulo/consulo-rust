/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.refactoring.changeSignature.MethodDescriptor;
import consulo.language.editor.refactoring.changeSignature.ChangeSignatureDialogBase;
import consulo.language.editor.refactoring.changeSignature.CallerChooserBase;
import consulo.language.editor.refactoring.changeSignature.ParameterTableModelBase;
import consulo.language.editor.refactoring.changeSignature.ParameterTableModelItemBase;

import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.file.LanguageFileType;
import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;

import consulo.language.psi.PsiCodeFragment;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.editor.refactoring.changeSignature.ChangeSignatureHandler;
import consulo.language.editor.refactoring.changeSignature.ParameterInfo;
import consulo.language.editor.refactoring.ui.ComboBoxVisibilityPanel;
import javax.swing.JCheckBox;
import consulo.ui.ex.awt.tree.Tree;
import java.util.function.Consumer;
import consulo.ui.ex.awt.JBUI;
import net.miginfocom.swing.MigLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.RsBundle;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.ide.utils.imports.ImportUtils;
import org.rust.lang.RsFileType;
import org.rust.lang.core.completion.CompletionUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.openapiext.DocumentExtUtil;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;
import consulo.document.Document;

public final class RsChangeSignatureDialog {

    @Nullable
    private static java.util.function.Consumer<RsChangeFunctionSignatureConfig> MOCK = null;

    private RsChangeSignatureDialog() {
    }

    public static void showChangeFunctionSignatureDialog(
        @Nonnull Project project,
        @Nonnull RsChangeFunctionSignatureConfig config
    ) {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            java.util.function.Consumer<RsChangeFunctionSignatureConfig> mock = MOCK;
            if (mock == null) {
                throw new IllegalStateException("You should set mock UI via `withMockChangeFunctionSignature`");
            }
            mock.accept(config);
            RsChangeSignatureProcessor.runChangeSignatureRefactoring(config);
        } else {
            new ChangeSignatureDialogImpl(project, new SignatureDescriptor(config)).show();
        }
    }

    
    public static void withMockChangeFunctionSignature(
        @Nonnull java.util.function.Consumer<RsChangeFunctionSignatureConfig> mock,
        @Nonnull Runnable action
    ) {
        MOCK = mock;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }

    private static class SignatureParameter implements ParameterInfo {
        @Nonnull
        private final Parameter myParameter;

        SignatureParameter(@Nonnull Parameter parameter) {
            myParameter = parameter;
        }

        @Nonnull
        public Parameter getParameter() {
            return myParameter;
        }

        @Override
        public String getName() {
            return myParameter.getPatText();
        }

        @Override
        public int getOldIndex() {
            return myParameter.getIndex();
        }

        @Override
        public String getDefaultValue() {
            return myParameter.getDefaultValue().getText();
        }

        @Override
        public void setName(@Nullable String name) {
            if (name != null) {
                myParameter.setPatText(name);
            }
        }

        @Override
        public String getTypeText() {
            return myParameter.getType().getText();
        }

        @Override
        public boolean isUseAnySingleVariable() {
            return false;
        }

        @Override
        public void setUseAnySingleVariable(boolean b) {
        }
    }

    private static class SignatureDescriptor implements MethodDescriptor<SignatureParameter, String> {
        @Nonnull
        private final RsChangeFunctionSignatureConfig myConfig;
        @Nonnull
        private final RsFunction myFunction;

        SignatureDescriptor(@Nonnull RsChangeFunctionSignatureConfig config) {
            myConfig = config;
            myFunction = config.getFunction();
        }

        @Nonnull
        public RsChangeFunctionSignatureConfig getConfig() {
            return myConfig;
        }

        @Nonnull
        public RsFunction getFunction() {
            return myFunction;
        }

        @Override
        public String getName() {
            return myConfig.getName();
        }

        @Nonnull
        @Override
        public java.util.List<SignatureParameter> getParameters() {
            java.util.List<SignatureParameter> result = new java.util.ArrayList<>();
            for (Parameter p : myConfig.getParameters()) {
                result.add(new SignatureParameter(p));
            }
            return result;
        }

        @Override
        public int getParametersCount() {
            return myConfig.getParameters().size();
        }

        @Override
        public PsiElement getMethod() {
            return myConfig.getFunction();
        }

        @Override
        public String getVisibility() {
            return "";
        }

        @Override
        public boolean canChangeVisibility() {
            return false;
        }

        @Override
        public boolean canChangeParameters() {
            return true;
        }

        @Override
        public boolean canChangeName() {
            return true;
        }

        @Override
        public ReadWriteOption canChangeReturnType() {
            return ReadWriteOption.ReadWrite;
        }
    }

    private static class ModelItem extends ParameterTableModelItemBase<SignatureParameter> {
        ModelItem(@Nonnull RsMod importContext, @Nonnull SignatureParameter parameter) {
            super(
                parameter,
                createTypeCodeFragment(importContext, parameter.getParameter().parseTypeReference()),
                createExprCodeFragment(importContext)
            );
        }

        @Override
        public boolean isEllipsisType() {
            return false;
        }
    }

    private static class TableModel extends ParameterTableModelBase<SignatureParameter, ModelItem> {
        @Nonnull
        private final SignatureDescriptor myDescriptor;
        @Nonnull
        private final RsPsiFactory myFactory;
        @Nonnull
        private final RsMod myImportContext;

        TableModel(@Nonnull SignatureDescriptor descriptor, @Nonnull Runnable onUpdate) {
            super(
                descriptor.getFunction(),
                descriptor.getFunction(),
                new NameColumn<>(descriptor.getFunction().getProject(), RsBundle.message("column.name.pattern")),
                new SignatureTypeColumn(descriptor),
                new SignatureDefaultValueColumn(descriptor)
            );
            myDescriptor = descriptor;
            myFactory = new RsPsiFactory(descriptor.getFunction().getProject());
            myImportContext = ImportUtils.createVirtualImportContext(descriptor.getFunction());
            addTableModelListener(e -> onUpdate.run());
        }

        @Override
        protected ModelItem createRowItem(@Nullable SignatureParameter parameterInfo) {
            SignatureParameter parameter;
            if (parameterInfo == null) {
                Parameter newParameter = new Parameter(
                    myFactory,
                    "p" + myDescriptor.getParametersCount(),
                    new ParameterProperty.Empty<>()
                );
                myDescriptor.getConfig().getParameters().add(newParameter);
                parameter = new SignatureParameter(newParameter);
            } else {
                parameter = parameterInfo;
            }
            return new ModelItem(myImportContext, parameter);
        }

        @SuppressWarnings("UnstableApiUsage")
        @Override
        public void removeRow(int index) {
            myDescriptor.getConfig().getParameters().remove(index);
            super.removeRow(index);
        }

        @Override
        public void fireTableRowsUpdated(int firstRow, int lastRow) {
            java.util.List<Parameter> parameters = myDescriptor.getConfig().getParameters();
            Parameter tmp = parameters.get(firstRow);
            parameters.set(firstRow, parameters.get(lastRow));
            parameters.set(lastRow, tmp);
            super.fireTableRowsUpdated(firstRow, lastRow);
        }

        private static class SignatureTypeColumn extends TypeColumn<SignatureParameter, ModelItem> {
            SignatureTypeColumn(@Nonnull SignatureDescriptor descriptor) {
                super(descriptor.getFunction().getProject(), RsFileType.INSTANCE);
            }

            @Override
            public void setValue(ModelItem item, PsiCodeFragment value) {
                if (!(value instanceof RsTypeReferenceCodeFragment)) return;
                RsTypeReferenceCodeFragment fragment = (RsTypeReferenceCodeFragment) value;
                if (item != null) {
                    item.parameter.getParameter().setType(
                        ParameterProperty.fromText(fragment.getTypeReference(), fragment.getText())
                    );
                }
            }
        }

        private static class SignatureDefaultValueColumn extends DefaultValueColumn<SignatureParameter, ModelItem> {
            SignatureDefaultValueColumn(@Nonnull SignatureDescriptor descriptor) {
                super(descriptor.getFunction().getProject(), RsFileType.INSTANCE);
            }

            @Override
            public void setValue(ModelItem item, PsiCodeFragment value) {
                if (!(value instanceof RsExpressionCodeFragment)) return;
                RsExpressionCodeFragment fragment = (RsExpressionCodeFragment) value;
                if (item != null) {
                    item.parameter.getParameter().setDefaultValue(
                        ParameterProperty.fromText(fragment.getExpr(), fragment.getText())
                    );
                }
            }
        }
    }

    private static class ChangeSignatureDialogImpl extends ChangeSignatureDialogBase<
        SignatureParameter, RsFunction, String, SignatureDescriptor, ModelItem, TableModel> {

        private boolean myIsValid = true;
        @Nullable
        private VisibilityComboBox myVisibilityComboBox = null;

        ChangeSignatureDialogImpl(@Nonnull Project project, @Nonnull SignatureDescriptor descriptor) {
            super(project, descriptor, false, descriptor.getFunction());
        }

        @Nonnull
        private RsChangeFunctionSignatureConfig getConfig() {
            return myMethod.getConfig();
        }

        @Override
        protected LanguageFileType getFileType() {
            return RsFileType.INSTANCE;
        }

        // Note: placeReturnTypeBeforeName not overridable in this SDK version
        // protected boolean placeReturnTypeBeforeName() { return false; }

        @Nullable
        @Override
        protected JComponent createNorthPanel() {
            JComponent panel = super.createNorthPanel();
            if (panel == null) return null;
            myNameField.setPreferredWidth(-1);
            myReturnTypeField.setPreferredWidth(-1);

            if (getConfig().getAllowsVisibilityChange()) {
                JPanel visibilityPanel = new JPanel(new BorderLayout(0, 2));
                JLabel visibilityLabel = new JLabel(RsBundle.message("visibility"));
                visibilityPanel.add(visibilityLabel, BorderLayout.NORTH);

                VisibilityComboBox visibility = new VisibilityComboBox(getProject(), getConfig().getVisibility(), this::updateSignature);
                visibilityLabel.setLabelFor(visibility.getComponent());
                visibilityPanel.add(visibility.getComponent(), BorderLayout.SOUTH);
                myVisibilityComboBox = visibility;

                GridBagLayout layout = (GridBagLayout) panel.getLayout();
                GridBagConstraints nameConstraints = (GridBagConstraints) layout.getConstraints(myNamePanel).clone();
                nameConstraints.gridx = 1;
                layout.setConstraints(myNamePanel, nameConstraints);

                JComponent returnTypePanel = myReturnTypeField.getParent() instanceof JComponent ? (JComponent) myReturnTypeField.getParent() : null;
                if (returnTypePanel != null) {
                    GridBagConstraints returnTypeConstraints = (GridBagConstraints) layout.getConstraints(returnTypePanel).clone();
                    returnTypeConstraints.gridx = 2;
                    layout.setConstraints(returnTypePanel, returnTypeConstraints);
                }

                GridBagConstraints gbc = new GridBagConstraints(
                    0, 0, 1, 1, 1.0, 1.0,
                    GridBagConstraints.WEST,
                    GridBagConstraints.HORIZONTAL,
                    new Insets(0, 0, 0, 0),
                    0, 0
                );
                panel.add(visibilityPanel, gbc);
            }
            return panel;
        }

        @Nonnull
        protected JPanel createSouthAdditionalPanel() {
            JCheckBox asyncBox = new JCheckBox(RsBundle.message("checkbox.async"), getConfig().isAsync());
            asyncBox.addChangeListener(e -> {
                getConfig().setAsync(asyncBox.isSelected());
                updateSignature();
            });
            JCheckBox unsafeBox = new JCheckBox(RsBundle.message("checkbox.unsafe"), getConfig().isUnsafe());
            unsafeBox.addChangeListener(e -> {
                getConfig().setUnsafe(unsafeBox.isSelected());
                updateSignature();
            });

            JPanel p = new JPanel();
            p.setLayout(new MigLayout("align center center, insets 0 " + JBUI.scale(10) + " 0 0"));
            p.add(asyncBox);
            p.add(unsafeBox);
            return p;
        }

        @Nonnull
        @Override
        protected TableModel createParametersInfoModel(@Nonnull SignatureDescriptor descriptor) {
            return new TableModel(descriptor, this::updateSignature);
        }

        @Nonnull
        @Override
        protected BaseRefactoringProcessor createRefactoringProcessor() {
            return new RsChangeSignatureProcessor(getProject(), getConfig().createChangeInfo());
        }

        @Nonnull
        @Override
        protected PsiCodeFragment createReturnTypeCodeFragment() {
            return createTypeCodeFragment(
                ImportUtils.createVirtualImportContext(myMethod.getFunction()),
                myMethod.getFunction().getRetType() != null ? myMethod.getFunction().getRetType().getTypeReference() : null
            );
        }

        @Nullable
        @Override
        protected CallerChooserBase<RsFunction> createCallerChooser(
            String title,
            Tree treeToReuse,
            Consumer<Set<RsFunction>> callback
        ) {
            return null;
        }

        @Nullable
        @Override
        protected String validateAndCommitData() {
            getConfig().getFunction().getProject().getService(RsPsiManager.class).incRustStructureModificationCount();
            return validateAndUpdateData();
        }

        @Override
        protected boolean areButtonsValid() {
            return myIsValid;
        }

        @Override
        protected void updateSignature() {
            updateState();
            super.updateSignature();
        }

        @Override
        protected void updateSignatureAlarmFired() {
            super.updateSignatureAlarmFired();
            validateButtons();
        }

        @Override
        protected void canRun() throws ConfigurationException {
            String error = validateAndUpdateData();
            if (error != null) {
                throw new ConfigurationException(error);
            }
            super.canRun();
        }

        private void updateState() {
            myIsValid = validateAndUpdateData() == null;
        }

        @SuppressWarnings("UnstableApiUsage")
        
        @Nullable
        private String validateAndUpdateData() {
            RsPsiFactory factory = new RsPsiFactory(getConfig().getFunction().getProject());

            if (myNameField != null) {
                String functionName = myNameField.getText();
                if (validateName(functionName)) {
                    getConfig().setName(functionName);
                } else {
                    return RsBundle.message("dialog.message.function.name.must.be.valid.rust.identifier");
                }
            }

            if (myReturnTypeField != null) {
                String returnTypeText = myReturnTypeField.getText();
                RsTypeReference returnType;
                if (returnTypeText.isBlank()) {
                    returnType = factory.createType("()");
                } else {
                    returnType = myReturnTypeCodeFragment instanceof RsTypeReferenceCodeFragment
                        ? ((RsTypeReferenceCodeFragment) myReturnTypeCodeFragment).getTypeReference()
                        : null;
                }
                if (returnType != null) {
                    getConfig().setReturnTypeDisplay(returnType);
                } else {
                    return RsBundle.message("dialog.message.function.return.type.must.be.valid.rust.type");
                }
            }

            VisibilityComboBox visField = myVisibilityComboBox;
            if (visField != null) {
                if (visField.hasValidVisibility()) {
                    getConfig().setVisibility(visField.getVisibility());
                } else {
                    return RsBundle.message("dialog.message.function.visibility.must.be.valid.visibility.specifier");
                }
            }

            java.util.List<Parameter> params = getConfig().getParameters();
            for (int index = 0; index < params.size(); index++) {
                Parameter parameter = params.get(index);
                if (!parameter.hasValidPattern()) {
                    return RsBundle.message("dialog.message.parameter.has.invalid.pattern", index);
                }
                if (parameter.getType() instanceof ParameterProperty.Empty) {
                    return RsBundle.message("dialog.message.please.enter.type.for.parameter", index);
                }
                if (parameter.getType() instanceof ParameterProperty.Invalid) {
                    return RsBundle.message("dialog.message.type.entered.for.parameter.invalid", index);
                }
                if (parameter.getDefaultValue() instanceof ParameterProperty.Invalid) {
                    return RsBundle.message("dialog.message.default.value.entered.for.parameter.invalid", index);
                }
            }

            return null;
        }

        @Nonnull
        @Override
        protected String calculateSignature() {
            return getConfig().signature();
        }

        @Nonnull
        @Override
        protected ComboBoxVisibilityPanel<String> createVisibilityControl() {
            return new ComboBoxVisibilityPanel<String>(new String[0]) {};
        }
    }

    @Nonnull
    private static PsiCodeFragment createTypeCodeFragment(
        @Nonnull RsMod importContext,
        @Nullable RsTypeReference type
    ) {
        return createCodeFragment(importContext, importTarget ->
            new RsTypeReferenceCodeFragment(
                importContext.getProject(),
                type != null ? type.getText() : "",
                importTarget,
                importTarget
            )
        );
    }

    @Nonnull
    private static PsiCodeFragment createExprCodeFragment(@Nonnull RsMod importContext) {
        return createCodeFragment(importContext, importTarget -> {
            RsExpressionCodeFragment fragment = new RsExpressionCodeFragment(
                importContext.getProject(),
                "",
                importTarget,
                importTarget
            );
            // FORCE_OUT_OF_SCOPE_COMPLETION is not available in Consulo CompletionUtilCore
            return fragment;
        });
    }

    @Nonnull
    private static PsiCodeFragment createCodeFragment(
        @Nonnull RsMod importContext,
        @Nonnull java.util.function.Function<RsItemsOwner, RsCodeFragment> factory
    ) {
        RsCodeFragment fragment = factory.apply(importContext);
        consulo.document.Document document = fragment.getViewProvider().getDocument();
        if (document != null) {
            document.addDocumentListener(new DocumentListener() {
                @Override
                public void documentChanged(@Nonnull DocumentEvent event) {
                    PsiDocumentManager.getInstance(importContext.getProject()).commitDocument(document);
                }
            });
        }
        return (PsiCodeFragment) fragment;
    }

    private static boolean validateName(@Nonnull String name) {
        return !name.isBlank() && RsNamesValidator.isValidRustVariableIdentifier(name);
    }

    private static class VisibilityComboBox {
        @Nonnull
        private final ComboBox<String> myCombobox;
        @Nonnull
        private final RsPsiFactory myFactory;

        VisibilityComboBox(@Nonnull Project project, @Nullable RsVis initialVis, @Nonnull Runnable onChange) {
            myCombobox = new ComboBox<>(createVisibilityHints(initialVis), 80);
            myFactory = new RsPsiFactory(project);
            myCombobox.setEditable(true);
            myCombobox.setSelectedItem(initialVis != null ? initialVis.getText() : "");
            myCombobox.addActionListener(e -> onChange.run());
        }

        @Nonnull
        public JComponent getComponent() {
            return myCombobox;
        }

        public boolean hasValidVisibility() {
            String selected = (String) myCombobox.getSelectedItem();
            return selected != null && (selected.isBlank() || getVisibility() != null);
        }

        @Nullable
        public RsVis getVisibility() {
            String selected = (String) myCombobox.getSelectedItem();
            return selected != null ? myFactory.tryCreateVis(selected) : null;
        }

        @Nonnull
        private static String[] createVisibilityHints(@Nullable RsVis initialVis) {
            Set<String> hints = new LinkedHashSet<>();
            hints.add(initialVis != null ? initialVis.getText() : "");
            hints.add("");
            hints.add("pub");
            hints.add("pub(crate)");
            hints.add("pub(super)");
            return hints.toArray(new String[0]);
        }
    }
}
