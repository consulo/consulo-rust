/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix.editable;

import org.rust.ide.template.postfix.RsPostfixTemplateProvider;

import javax.swing.*;

/** Editor UI for a user-defined Rust postfix template; currently an empty panel. */
public class RsPostfixTemplateEditor {
    private final RsPostfixTemplateProvider provider;
    private final JPanel panel = new JPanel();

    public RsPostfixTemplateEditor(RsPostfixTemplateProvider provider) {
        this.provider = provider;
    }

    public JComponent getComponent() {
        return panel;
    }
}
