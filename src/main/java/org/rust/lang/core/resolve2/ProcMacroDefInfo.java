/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspaceData;
import org.rust.lang.core.psi.KnownProcMacroKind;
import org.rust.lang.core.psi.RsProcMacroKind;

public class ProcMacroDefInfo extends MacroDefInfo {
    private final int crate;
    @NotNull
    private final ModPath path;
    @NotNull
    private final RsProcMacroKind procMacroKind;
    @Nullable
    private final CargoWorkspaceData.ProcMacroArtifact procMacroArtifact;
    @NotNull
    private final KnownProcMacroKind kind;

    public ProcMacroDefInfo(
        int crate,
        @NotNull ModPath path,
        @NotNull RsProcMacroKind procMacroKind,
        @Nullable CargoWorkspaceData.ProcMacroArtifact procMacroArtifact,
        @NotNull KnownProcMacroKind kind
    ) {
        this.crate = crate;
        this.path = path;
        this.procMacroKind = procMacroKind;
        this.procMacroArtifact = procMacroArtifact;
        this.kind = kind;
    }

    @Override
    public int getCrate() {
        return crate;
    }

    @Override
    @NotNull
    public ModPath getPath() {
        return path;
    }

    @Override
    @NotNull
    public RsProcMacroKind getProcMacroKind() {
        return procMacroKind;
    }

    @Nullable
    public CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact() {
        return procMacroArtifact;
    }

    @NotNull
    public KnownProcMacroKind getKind() {
        return kind;
    }
}
