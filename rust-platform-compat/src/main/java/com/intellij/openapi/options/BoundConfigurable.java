package com.intellij.openapi.options;

import com.intellij.openapi.ui.DialogPanel;
import consulo.configurable.Configurable;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;
import consulo.configurable.ConfigurationException;

/** Configurable whose UI is a {@link DialogPanel} that carries its own settings bindings. */
public abstract class BoundConfigurable implements Configurable {
    protected final String displayName;
    protected final String helpTopic;

    @Nullable
    private DialogPanel panel;

    protected BoundConfigurable(@Nonnull String displayName) { this(displayName, null); }
    protected BoundConfigurable(@Nonnull String displayName, @Nullable String helpTopic) {
        this.displayName = displayName;
        this.helpTopic = helpTopic;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() { return LocalizeValue.of(displayName); }

    @Nullable
    @Override
    public String getHelpTopic() { return helpTopic; }

    protected abstract DialogPanel createPanel();

    @Nullable
    protected final DialogPanel getPanel() { return panel; }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (panel == null) {
            panel = createPanel();
            panel.reset();
        }
        return panel;
    }

    @Override
    public boolean isModified() {
        return panel != null && panel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        if (panel != null) {
            panel.apply();
        }
    }

    @Override
    public void reset() {
        if (panel != null) {
            panel.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        panel = null;
    }
}
