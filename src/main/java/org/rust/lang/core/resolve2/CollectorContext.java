/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;

import java.util.ArrayList;
import java.util.List;

public class CollectorContext {
    @NotNull
    private final Crate crate;
    @NotNull
    private final Project project;

    @NotNull
    private final List<Import> imports = new ArrayList<>();
    @NotNull
    private final List<MacroCallInfoBase> macroCalls = new ArrayList<>();

    @Nullable
    private final ModData hangingModData;
    private final boolean isHangingMode;

    public CollectorContext(@NotNull Crate crate, @NotNull Project project, @Nullable ModData hangingModData) {
        this.crate = crate;
        this.project = project;
        this.hangingModData = hangingModData;
        this.isHangingMode = hangingModData != null;
    }

    @NotNull
    public Crate getCrate() {
        return crate;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public List<Import> getImports() {
        return imports;
    }

    @NotNull
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
