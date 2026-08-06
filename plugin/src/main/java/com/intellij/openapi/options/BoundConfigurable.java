package com.intellij.openapi.options;

import com.intellij.openapi.ui.DialogPanel;
import consulo.configurable.Configurable;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;

/** IntelliJ-compat stub. Consulo: write configurable directly with FormBuilder. */
public abstract class BoundConfigurable implements Configurable {
    protected final String displayName;
    protected final String helpTopic;

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
    @Override
    public JComponent createComponent() { return createPanel(); }

    @Override
    public boolean isModified() { return false; }

    @Override
    public void apply() {}
}
