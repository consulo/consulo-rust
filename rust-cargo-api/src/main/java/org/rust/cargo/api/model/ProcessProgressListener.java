/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.model;

import consulo.build.ui.event.BuildEventsNls;
import consulo.process.event.ProcessListener;
import jakarta.annotation.Nonnull;

@SuppressWarnings("UnstableApiUsage")
public interface ProcessProgressListener extends ProcessListener {
    void error(@BuildEventsNls.Title @Nonnull String title, @BuildEventsNls.Message @Nonnull String message);
    void warning(@BuildEventsNls.Title @Nonnull String title, @BuildEventsNls.Message @Nonnull String message);
}
