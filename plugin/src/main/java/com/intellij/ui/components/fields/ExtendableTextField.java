/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.ui.components.fields;

import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;

/** IntelliJ-compat stub for Swing text field with extension icons. */
public class ExtendableTextField extends JTextField {
    private final List<ExtendableTextComponent.Extension> extensions = new ArrayList<>();
    public ExtendableTextField() {}
    public ExtendableTextField(int columns) { super(columns); }
    public ExtendableTextField(String text) { super(text); }
    public void addExtension(ExtendableTextComponent.Extension extension) { extensions.add(extension); }
    public void removeExtension(ExtendableTextComponent.Extension extension) { extensions.remove(extension); }
}
