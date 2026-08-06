/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspaceData;
import org.rust.lang.core.psi.KnownProcMacroKind;
import org.rust.lang.core.psi.RsProcMacroKind;

public class ProcMacroDefInfo extends MacroDefInfo {
    private final int crate;
    @Nonnull
    private final ModPath path;
    @Nonnull
    private final RsProcMacroKind procMacroKind;
    @Nullable
    private final CargoWorkspaceData.ProcMacroArtifact procMacroArtifact;
    @Nonnull
    private final KnownProcMacroKind kind;

    public ProcMacroDefInfo(
        int crate,
        @Nonnull ModPath path,
        @Nonnull RsProcMacroKind procMacroKind,
        @Nullable CargoWorkspaceData.ProcMacroArtifact procMacroArtifact,
        @Nonnull KnownProcMacroKind kind
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
    @Nonnull
    public ModPath getPath() {
        return path;
    }

    @Override
    @Nonnull
    public RsProcMacroKind getProcMacroKind() {
        return procMacroKind;
    }

    @Nullable
    public CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact() {
        return procMacroArtifact;
    }

    @Nonnull
    public KnownProcMacroKind getKind() {
        return kind;
    }
}
