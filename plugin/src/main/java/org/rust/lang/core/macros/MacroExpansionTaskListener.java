/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;


@TopicAPI(value = ComponentScope.PROJECT)
public interface MacroExpansionTaskListener {
    void onMacroExpansionTaskFinished();

    Class<MacroExpansionTaskListener> MACRO_EXPANSION_TASK_TOPIC = MacroExpansionTaskListener.class;
}
