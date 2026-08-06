/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject.ui;

import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.configurable.ConfigurationException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ToolbarDecorator;
import consulo.ui.ex.awt.JBList;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.ui.dsl.gridLayout.VerticalAlign;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.ui.RustProjectSettingsPanel;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.ide.newProject.*;
import org.rust.ide.newProject.state.RsUserTemplatesState;
import org.rust.openapiext.UiDebouncer;
import org.rust.openapiext.UiUtil;
import org.rust.stdext.RsResult;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class RsNewProjectPanel implements Disposable {

    private final boolean showProjectTypeSelection;
    @Nullable
    private final Runnable updateListener;

    private final RustProjectSettingsPanel rustProjectSettings;

    private final List<RsProjectTemplate> defaultTemplates;

    private final DefaultListModel<RsProjectTemplate> templateListModel;
    private final JBList<RsProjectTemplate> templateList;
    private final ToolbarDecorator templateToolbar;

    private boolean needInstallCargoGenerate = false;

    @SuppressWarnings("DialogTitleCapitalization")
    private final ActionLink downloadCargoGenerateLink;

    private final UiDebouncer updateDebouncer;

    public RsNewProjectPanel(boolean showProjectTypeSelection) {
        this(showProjectTypeSelection, Paths.get("."), null);
    }

    public RsNewProjectPanel(boolean showProjectTypeSelection, @Nonnull Path cargoProjectDir, @Nullable Runnable updateListener) {
        this.showProjectTypeSelection = showProjectTypeSelection;
        this.updateListener = updateListener;
        this.rustProjectSettings = new RustProjectSettingsPanel(cargoProjectDir, updateListener);

        this.defaultTemplates = List.of(
            RsGenericTemplate.CargoBinaryTemplate,
            RsGenericTemplate.CargoLibraryTemplate,
            RsCustomTemplate.ProcMacroTemplate,
            RsCustomTemplate.WasmPackTemplate
        );

        List<RsProjectTemplate> allTemplates = new ArrayList<>(defaultTemplates);
        allTemplates.addAll(getUserTemplates());
        this.templateListModel = JBList.createDefaultListModel(allTemplates);

        this.templateList = new JBList<>(templateListModel);
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateList.setSelectedIndex(0);
        templateList.addListSelectionListener(e -> update());
        templateList.setCellRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(
                @Nonnull JList<? extends RsProjectTemplate> list,
                RsProjectTemplate value,
                int index,
                boolean selected,
                boolean hasFocus
            ) {
                setIcon(value.getIcon());
                append(value.getName());
                if (value instanceof RsCustomTemplate customTemplate) {
                    append(" ");
                    append(customTemplate.getShortLink(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                }
            }
        });

        this.templateToolbar = ToolbarDecorator.createDecorator(templateList)
            .setToolbarPosition(ActionToolbarPosition.BOTTOM)
            .setPreferredSize(JBUI.size(0, 125))
            .disableUpDownActions()
            .setAddAction(button -> {
                new AddUserTemplateDialog().show();
                updateTemplatesList();
            })
            .setRemoveAction(button -> {
                RsProjectTemplate selected = getSelectedTemplate();
                if (!(selected instanceof RsCustomTemplate customTemplate)) return;
                RsUserTemplatesState.getInstance().templates
                    .removeIf(t -> t.name.equals(customTemplate.getName()));
                updateTemplatesList();
            })
            .setRemoveActionUpdater(e -> !defaultTemplates.contains(getSelectedTemplate()));

        this.downloadCargoGenerateLink = new ActionLink(
            RsBundle.message("label.install.cargo.generate.using.cargo"),
            e -> {
                Cargo cargo = getCargo();
                if (cargo == null) return;

                new Task.Modal(null, RsBundle.message("dialog.title.installing.cargo.generate"), true) {
                    int exitCode = Integer.MIN_VALUE;

                    @Override
                    public void onFinished() {
                        if (exitCode != 0) {
                            // Show error notification
                            consulo.ui.ex.popup.JBPopupFactory.getInstance()
                                .createHtmlTextBalloonBuilder(
                                    RsBundle.message("notification.content.failed.to.install.cargo.generate"),
                                    consulo.ui.NotificationType.ERROR, null)
                                .createBalloon()
                                .showInCenterOf(templateList);
                        }
                        update();
                    }

                    @Override
                    public void run(@Nonnull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        RsResult.unwrapOrThrow(cargo.installCargoGenerate(RsNewProjectPanel.this, new ProcessAdapter() {
                            @Override
                            public void onTextAvailable(@Nonnull ProcessEvent event, @Nonnull Key outputType) {
                                indicator.setText(RsBundle.message("progress.text.installing.using.cargo"));
                                indicator.setText2(event.getText().trim());
                            }

                            @Override
                            public void processTerminated(@Nonnull ProcessEvent event) {
                                exitCode = event.getExitCode();
                            }
                        }));
                    }
                }.queue();
            }
        );
        downloadCargoGenerateLink.setVisible(false);

        this.updateDebouncer = new UiDebouncer(this);
    }

    @Nullable
    private Cargo getCargo() {
        var toolchain = rustProjectSettings.getData().getToolchain();
        return toolchain != null ? Cargo.cargo(toolchain) : null;
    }

    @Nonnull
    private List<RsCustomTemplate> getUserTemplates() {
        return RsUserTemplatesState.getInstance().templates.stream()
            .map(t -> new RsCustomTemplate(t.name, t.url))
            .collect(Collectors.toList());
    }

    @Nonnull
    private RsProjectTemplate getSelectedTemplate() {
        return templateList.getSelectedValue();
    }

    @Nonnull
    public ConfigurationData getData() {
        return new ConfigurationData(rustProjectSettings.getData(), getSelectedTemplate());
    }

    public void attachTo(@Nonnull Panel panel) {
        rustProjectSettings.attachTo(panel);

        if (showProjectTypeSelection) {
            panel.groupRowsRange(RsBundle.message("border.title.project.template"), false, null, null, builder -> {
                builder.row((String) null, row -> {
                    row.resizableRow();
                    UiUtil.fullWidthCell(row, templateToolbar.createPanel())
                        .verticalAlign(VerticalAlign.FILL);
                    return null;
                });
                builder.row((String) null, row -> {
                    row.cell(downloadCargoGenerateLink);
                    return null;
                });
                return null;
            });
        }

        update();
    }

    public void update() {
        updateDebouncer.run(
            () -> {
                RsProjectTemplate selected = getSelectedTemplate();
                if (selected instanceof RsGenericTemplate) {
                    return false;
                } else {
                    Cargo cargo = getCargo();
                    return cargo != null && cargo.checkNeedInstallCargoGenerate();
                }
            },
            needInstall -> {
                downloadCargoGenerateLink.setVisible(needInstall);
                needInstallCargoGenerate = needInstall;
                if (updateListener != null) {
                    updateListener.run();
                }
            }
        );
    }

    private void updateTemplatesList() {
        int index = templateList.getSelectedIndex();

        templateListModel.removeAllElements();
        for (RsProjectTemplate t : defaultTemplates) {
            templateListModel.addElement(t);
        }
        for (RsCustomTemplate t : getUserTemplates()) {
            templateListModel.addElement(t);
        }

        templateList.setSelectedIndex(Math.min(index, templateList.getModel().getSize() - 1));
    }

    public void validateSettings() throws ConfigurationException {
        rustProjectSettings.validateSettings();

        if (needInstallCargoGenerate) {
            //noinspection DialogTitleCapitalization
            throw new ConfigurationException(RsBundle.message("dialog.message.cargo.generate.needed.to.create.project.from.custom.template"));
        }
    }

    @Override
    public void dispose() {
        Disposer.dispose(rustProjectSettings);
    }
}
