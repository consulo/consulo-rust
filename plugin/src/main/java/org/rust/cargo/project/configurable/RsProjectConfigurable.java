/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ConfigurableAdapter;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.rust.localize.RustLocalize;

/**
 * The "Rust" node in the settings tree. It carries no settings of its own — the toolchain and the
 * standard library come from the Rust module extension's SDK, so the pages that do have settings
 * (Cargo, Rustfmt, external linters) hang off this node as children.
 */
@ExtensionImpl
public class RsProjectConfigurable extends ConfigurableAdapter implements ProjectConfigurable {
    /** Settings-tree id other Rust pages hang off as children. */
    public static final String ID = "language.rust";

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return RustLocalize.settingsRustToolchainName();
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }
}
