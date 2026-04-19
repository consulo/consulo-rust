/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project;

import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComboBoxWithWidePopup;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import org.jetbrains.annotations.Nullable;
import org.rust.stdext.Utils;

import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A combobox with browse button for choosing a path to a toolchain, also capable of showing progress indicator.
 * To toggle progress indicator visibility use {@link #setBusy} method.
 */
public class RsToolchainPathChoosingComboBox extends ComponentWithBrowseButton<ComboBoxWithWidePopup<Path>> {
    private final BasicComboBoxEditor editor = new BasicComboBoxEditor() {
        @Override
        protected ExtendableTextField createEditorComponent() {
            return new ExtendableTextField();
        }
    };

    private final ExtendableTextComponent.Extension busyIconExtension =
        ExtendableTextComponent.Extension.create(AnimatedIcon.Default.INSTANCE, null, null);

    public RsToolchainPathChoosingComboBox(Runnable onTextChanged) {
        super(new ComboBoxWithWidePopup<>(), null);

        new ComboboxSpeedSearch(getChildComponent());
        getChildComponent().setEditor(editor);
        getChildComponent().setEditable(true);

        addActionListener(e -> {
            com.intellij.openapi.fileChooser.FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            FileChooser.chooseFile(descriptor, null, null, file ->
                getChildComponent().setSelectedItem(org.rust.openapiext.OpenApiUtil.getPathAsPath(file))
            );
        });

        getPathTextField().getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { onTextChanged.run(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { onTextChanged.run(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onTextChanged.run(); }
        });
    }

    public RsToolchainPathChoosingComboBox() {
        this(() -> {});
    }

    private ExtendableTextField getPathTextField() {
        return (ExtendableTextField) getChildComponent().getEditor().getEditorComponent();
    }

    @Nullable
    public Path getSelectedPath() {
        String text = getPathTextField().getText();
        return text != null ? org.rust.stdext.PathUtil.toPathOrNull(text) : null;
    }

    public void setSelectedPath(@Nullable Path value) {
        getPathTextField().setText(value != null ? value.toString() : "");
    }

    private void setBusy(boolean busy) {
        if (busy) {
            getPathTextField().addExtension(busyIconExtension);
        } else {
            getPathTextField().removeExtension(busyIconExtension);
        }
        repaint();
    }

    /**
     * Obtains a list of toolchains on a pool using toolchainObtainer, then fills the combobox and calls callback on the EDT.
     */
    @SuppressWarnings({"UnstableApiUsage", "MemberVisibilityCanBePrivate"})
    public void addToolchainsAsync(Supplier<List<Path>> toolchainObtainer, Runnable callback) {
        setBusy(true);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<Path> toolchains = Collections.emptyList();
            try {
                toolchains = toolchainObtainer.get();
            } finally {
                List<Path> finalToolchains = toolchains;
                AppUIExecutor.onUiThread(ModalityState.any()).expireWith(this)
                    .execute(() -> {
                        setBusy(false);
                        Path oldSelectedPath = getSelectedPath();
                        getChildComponent().removeAllItems();
                        for (Path path : finalToolchains) {
                            getChildComponent().addItem(path);
                        }
                        setSelectedPath(oldSelectedPath);
                        callback.run();
                    });
            }
        });
    }

    public void addToolchainsAsync(Supplier<List<Path>> toolchainObtainer) {
        addToolchainsAsync(toolchainObtainer, () -> {});
    }
}
