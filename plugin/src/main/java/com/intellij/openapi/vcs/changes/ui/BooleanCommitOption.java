package com.intellij.openapi.vcs.changes.ui;

import consulo.project.Project;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.ui.RefreshableOnComponent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** IntelliJ-compat stub — commit-dialog boolean option. */
public class BooleanCommitOption implements RefreshableOnComponent {
    private final JCheckBox checkBox;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    public BooleanCommitOption(Object panel, String text, boolean initialValue, Object propGetter, Object propSetter) {
        this.checkBox = new JCheckBox(text, initialValue);
        this.getter = () -> initialValue;
        this.setter = v -> {};
    }

    private BooleanCommitOption(String text, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.checkBox = new JCheckBox(text);
        this.getter = getter;
        this.setter = setter;
    }

    public static BooleanCommitOption create(Project project, CheckinHandler handler, boolean disableOnSession, String text,
                                             Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new BooleanCommitOption(text, getter, setter);
    }

    @Override public JComponent getComponent() { return checkBox; }
    @Override public void refresh() {}
    @Override public void saveState() { setter.accept(checkBox.isSelected()); }
    @Override public void restoreState() { checkBox.setSelected(Boolean.TRUE.equals(getter.get())); }
}
