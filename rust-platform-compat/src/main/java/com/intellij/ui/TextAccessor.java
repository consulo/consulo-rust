/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.intellij.ui;

/** Component whose text can be read and replaced. */
public interface TextAccessor {
    String getText();
    void setText(String text);
}
