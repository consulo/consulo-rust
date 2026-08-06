/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;

import java.util.ArrayList;
import java.util.List;

public class CollectorContext {
    @Nonnull
    private final Crate crate;
    @Nonnull
    private final Project project;

    @Nonnull
    private final List<Import> imports = new ArrayList<>();
    @Nonnull
    private final List<MacroCallInfoBase> macroCalls = new ArrayList<>();

    @Nullable
    private final ModData hangingModData;
    private final boolean isHangingMode;

    public CollectorContext(@Nonnull Crate crate, @Nonnull Project project, @Nullable ModData hangingModData) {
        this.crate = crate;
        this.project = project;
        this.hangingModData = hangingModData;
        this.isHangingMode = hangingModData != null;
    }

    @Nonnull
    public Crate getCrate() {
        return crate;
    }

    @Nonnull
    public Project getProject() {
        return project;
    }

    @Nonnull
    public List<Import> getImports() {
        return imports;
    }

    @Nonnull
    public List<MacroCallInfoBase> getMacroCalls() {
        return macroCalls;
    }

    @Nullable
    public ModData getHangingModData() {
        return hangingModData;
    }

    public boolean isHangingMode() {
        return isHangingMode;
    }
}
