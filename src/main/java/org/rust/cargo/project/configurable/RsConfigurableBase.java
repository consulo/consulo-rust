/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import com.intellij.openapi.options.BoundConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.ConfigurableName;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public abstract class RsConfigurableBase extends BoundConfigurable {

    protected final Project project;

    protected RsConfigurableBase(@NotNull Project project, @ConfigurableName @NotNull String displayName) {
        super(displayName, null);
        this.project = project;
    }

    // Currently, we have help page only for CLion
    @Nullable
    @Override
    public String getHelpTopic() {
        return PlatformUtils.isCLion() ? "rustsupport" : null;
    }
}
