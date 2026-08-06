/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.openapi.ui;

import javax.swing.JPanel;

/**
 * IntelliJ-compat stub. Consulo has no Kotlin-DSL {@code DialogPanel};
 * DSL UI is IntelliJ-specific. Real UI will be rewritten with Consulo's
 * FormBuilder in a later pass.
 */
public class DialogPanel extends JPanel {
    public DialogPanel() { super(); }
    public DialogPanel(java.awt.LayoutManager layout) { super(layout); }
    public void apply() {}
    public void reset() {}
    public boolean isModified() { return false; }
    public DialogPanel registerValidators(Object parentDisposable) { return this; }
    public DialogPanel registerValidators(Object parentDisposable, Object callback) { return this; }
}
