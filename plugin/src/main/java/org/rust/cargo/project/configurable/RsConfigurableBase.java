/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import com.intellij.openapi.options.BoundConfigurable;
import consulo.project.Project;

import com.intellij.util.PlatformUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@SuppressWarnings("UnstableApiUsage")
public abstract class RsConfigurableBase extends BoundConfigurable {

    protected final Project project;
    private final String displayName;

    protected RsConfigurableBase(@Nonnull Project project,  @Nonnull String displayName) {
        super(displayName, null);
        this.project = project;
        this.displayName = displayName;
    }

    // Currently, we have help page only for CLion
    @Nullable
    @Override
    public String getHelpTopic() {
        return PlatformUtils.isCLion() ? "rustsupport" : null;
    }
}
