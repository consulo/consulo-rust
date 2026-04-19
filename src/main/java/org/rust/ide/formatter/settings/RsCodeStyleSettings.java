/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.settings;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class RsCodeStyleSettings extends CustomCodeStyleSettings {

    public boolean ALIGN_RET_TYPE = true;
    public boolean ALIGN_WHERE_CLAUSE = false;
    public boolean ALIGN_TYPE_PARAMS = false;
    public boolean ALIGN_WHERE_BOUNDS = true;
    public boolean INDENT_WHERE_CLAUSE = true;
    public boolean ALLOW_ONE_LINE_MATCH = false;
    public int MIN_NUMBER_OF_BLANKS_BETWEEN_ITEMS = 1;
    public boolean PRESERVE_PUNCTUATION = false;
    public boolean SPACE_AROUND_ASSOC_TYPE_BINDING = false;

    public RsCodeStyleSettings(CodeStyleSettings container) {
        super(RsCodeStyleSettings.class.getSimpleName(), container);
    }
}
