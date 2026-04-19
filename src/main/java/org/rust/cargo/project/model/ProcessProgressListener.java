/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.build.events.BuildEventsNls;
import com.intellij.execution.process.ProcessListener;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public interface ProcessProgressListener extends ProcessListener {
    void error(@BuildEventsNls.Title @NotNull String title, @BuildEventsNls.Message @NotNull String message);
    void warning(@BuildEventsNls.Title @NotNull String title, @BuildEventsNls.Message @NotNull String message);
}
