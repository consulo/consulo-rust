/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.settings;

import com.intellij.application.options.GenerationCodeStylePanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.rust.lang.RsLanguage;

public class RsCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {

    public RsCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
        super(RsLanguage.INSTANCE, currentSettings, settings);
    }

    @Override
    protected void initTabs(CodeStyleSettings settings) {
        addIndentOptionsTab(settings);
        addSpacesTab(settings);
        addWrappingAndBracesTab(settings);
        addBlankLinesTab(settings);
        addTab(new GenerationCodeStylePanel(settings, RsLanguage.INSTANCE));
    }
}
