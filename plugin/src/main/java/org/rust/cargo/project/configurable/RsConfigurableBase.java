/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import com.intellij.openapi.options.BoundConfigurable;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

@SuppressWarnings("UnstableApiUsage")
public abstract class RsConfigurableBase extends BoundConfigurable {

    protected final Project project;

    protected RsConfigurableBase(@Nonnull Project project,  @Nonnull String displayName) {
        super(displayName, null);
        this.project = project;
    }
}
