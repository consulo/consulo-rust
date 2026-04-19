/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.util.messages.Topic;

public interface MacroExpansionTaskListener {
    void onMacroExpansionTaskFinished();

    Topic<MacroExpansionTaskListener> MACRO_EXPANSION_TASK_TOPIC = new Topic<>(
        "rust macro expansion task",
        MacroExpansionTaskListener.class
    );
}
