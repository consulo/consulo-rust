/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate.impl;

import consulo.project.Project;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.CargoWorkspaceData;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;

import java.util.*;

/** Fake crate for cases when something is very wrong or for {@link RsFile} outside of module tree. */
public abstract class FakeCrate extends UserDataHolderBase implements Crate {

    @Nonnull
    @Override
    public List<Crate> getReverseDependencies() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public CargoProject getCargoProject() {
        return null;
    }

    @Nullable
    @Override
    public CargoWorkspace.Target getCargoTarget() {
        return null;
    }

    @Nullable
    @Override
    public CargoWorkspace getCargoWorkspace() {
        return null;
    }

    @Nonnull
    @Override
    public CargoWorkspace.TargetKind getKind() {
        return CargoWorkspace.TargetKind.Test.INSTANCE;
    }

    @Nonnull
    @Override
    public CfgOptions getCfgOptions() {
        return CfgOptions.EMPTY;
    }

    @Nonnull
    @Override
    public Map<String, FeatureState> getFeatures() {
        return Collections.emptyMap();
    }

    @Override
    public boolean getEvaluateUnknownCfgToFalse() {
        return true;
    }

    @Nonnull
    @Override
    public Map<String, String> getEnv() {
        return Collections.emptyMap();
    }

    @Nullable
    @Override
    public VirtualFile getOutDir() {
        return null;
    }

    @Nonnull
    @Override
    public PackageOrigin getOrigin() {
        return PackageOrigin.WORKSPACE;
    }

    @Nonnull
    @Override
    public CargoWorkspace.Edition getEdition() {
        return CargoWorkspace.Edition.DEFAULT;
    }

    @Override
    public boolean getAreDoctestsEnabled() {
        return false;
    }

    @Nonnull
    @Override
    public String getNormName() {
        return "__fake__";
    }

    @Nullable
    @Override
    public CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact() {
        return null;
    }

    @Override
    public String toString() {
        return getPresentableName();
    }
}
