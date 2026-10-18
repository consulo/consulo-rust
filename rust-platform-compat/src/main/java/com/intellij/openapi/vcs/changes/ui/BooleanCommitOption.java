package com.intellij.openapi.vcs.changes.ui;

import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.versionControlSystem.checkin.CheckinHandler;
import consulo.versionControlSystem.ui.RefreshableOnComponent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.lang.classfile.instruction.LocalVariable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Boolean option shown in the commit dialog, backed by a getter/setter pair. */
public class BooleanCommitOption implements RefreshableOnComponent {
    private final CheckBox checkBox;
    private final Supplier<Boolean> getter;
    private final Consumer<Boolean> setter;

    private BooleanCommitOption(String text, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        this.checkBox = CheckBox.create(text);
        this.getter = getter;
        this.setter = setter;
    }

    public static BooleanCommitOption create(Project project, CheckinHandler handler, boolean disableOnSession, String text,
                                             Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new BooleanCommitOption(text, getter, setter);
    }

    @Override public Component getUIComponent() { return checkBox; }
    @Override public void refresh() {}
    @Override public void saveState() { setter.accept(checkBox.getValueOrError()); }
    @Override public void restoreState() { checkBox.setValue(Boolean.TRUE.equals(getter.get())); }
}
