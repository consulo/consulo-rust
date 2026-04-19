/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.CargoWorkspaceData;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsFileUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;

/**
 * {@link #equals}/{@link #hashCode} are based on {@link #cargoTarget} and {@link #id} fields
 * because these fields equality guarantee other fields equality.
 */
public class CargoBasedCrate extends UserDataHolderBase implements Crate {
    @NotNull
    private CargoProject cargoProject;
    @NotNull
    private CargoWorkspace.Target cargoTarget;
    @NotNull
    private final Collection<Crate.Dependency> dependencies;
    @NotNull
    private final LinkedHashSet<Crate> flatDependencies;
    @Nullable
    private CargoWorkspaceData.ProcMacroArtifact procMacroArtifact;

    @NotNull
    private final List<CargoBasedCrate> reverseDependencies = new ArrayList<>();
    @NotNull
    private Map<String, FeatureState> features;

    // These properties are fields (not just delegates to cargoTarget) because Crate must be immutable
    @Nullable
    private final VirtualFile rootModFile;
    @Nullable
    private final Integer id;

    /** See docs for {@link org.rust.lang.core.crate.CrateGraphService} */
    @NotNull
    private List<Crate.Dependency> cyclicDevDeps = Collections.emptyList();

    public CargoBasedCrate(
        @NotNull CargoProject cargoProject,
        @NotNull CargoWorkspace.Target cargoTarget,
        @NotNull Collection<Crate.Dependency> dependencies,
        @NotNull LinkedHashSet<Crate> flatDependencies,
        @Nullable CargoWorkspaceData.ProcMacroArtifact procMacroArtifact
    ) {
        this.cargoProject = cargoProject;
        this.cargoTarget = cargoTarget;
        this.dependencies = dependencies;
        this.flatDependencies = flatDependencies;
        this.procMacroArtifact = procMacroArtifact;
        this.features = cargoTarget.getPkg().getFeatureState();
        this.rootModFile = cargoTarget.getCrateRoot();
        this.id = rootModFile != null ? org.rust.openapiext.OpenApiUtil.getFileId(rootModFile) : null;

        for (Dependency dependency : dependencies) {
            ((CargoBasedCrate) dependency.getCrate()).reverseDependencies.add(this);
        }
    }

    public CargoBasedCrate(
        @NotNull CargoProject cargoProject,
        @NotNull CargoWorkspace.Target cargoTarget,
        @NotNull Collection<Crate.Dependency> dependencies,
        @NotNull LinkedHashSet<Crate> flatDependencies
    ) {
        this(cargoProject, cargoTarget, dependencies, flatDependencies, null);
    }

    @NotNull
    @Override
    public Collection<Dependency> getDependenciesWithCyclic() {
        List<Dependency> result = new ArrayList<>(dependencies);
        result.addAll(cyclicDevDeps);
        return result;
    }

    @NotNull
    public List<Dependency> getCyclicDevDeps() {
        return cyclicDevDeps;
    }

    public void setCyclicDevDeps(@NotNull List<Dependency> cyclicDevDeps) {
        this.cyclicDevDeps = cyclicDevDeps;
    }

    @Nullable
    @Override
    public Integer getId() {
        return id;
    }

    @NotNull
    @Override
    public CargoWorkspace.Edition getEdition() {
        return cargoTarget.getEdition();
    }

    @NotNull
    @Override
    public CargoProject getCargoProject() {
        return cargoProject;
    }

    public void setCargoProject(@NotNull CargoProject cargoProject) {
        this.cargoProject = cargoProject;
    }

    @NotNull
    @Override
    public CargoWorkspace getCargoWorkspace() {
        return cargoTarget.getPkg().getWorkspace();
    }

    @NotNull
    @Override
    public CargoWorkspace.Target getCargoTarget() {
        return cargoTarget;
    }

    public void setCargoTarget(@NotNull CargoWorkspace.Target cargoTarget) {
        this.cargoTarget = cargoTarget;
    }

    @NotNull
    @Override
    public CargoWorkspace.TargetKind getKind() {
        return cargoTarget.getKind();
    }

    @NotNull
    @Override
    public PackageOrigin getOrigin() {
        return cargoTarget.getPkg().getOrigin();
    }

    @NotNull
    @Override
    public CfgOptions getCfgOptions() {
        return cargoTarget.getCfgOptions();
    }

    @NotNull
    @Override
    public Map<String, FeatureState> getFeatures() {
        return features;
    }

    public void setFeatures(@NotNull Map<String, FeatureState> features) {
        this.features = features;
    }

    @Override
    public boolean getEvaluateUnknownCfgToFalse() {
        return getOrigin() == PackageOrigin.STDLIB ||
            cargoTarget.getPkg().getCfgOptions() != null ||
            !cargoTarget.getPkg().getHasCustomBuildScript();
    }

    @NotNull
    @Override
    public Map<String, String> getEnv() {
        return cargoTarget.getPkg().getEnv();
    }

    @Nullable
    @Override
    public VirtualFile getOutDir() {
        return cargoTarget.getPkg().getOutDir();
    }

    @NotNull
    @Override
    public Collection<Dependency> getDependencies() {
        return dependencies;
    }

    @NotNull
    @Override
    public LinkedHashSet<Crate> getFlatDependencies() {
        return flatDependencies;
    }

    @NotNull
    @Override
    public List<Crate> getReverseDependencies() {
        return Collections.unmodifiableList(reverseDependencies);
    }

    @NotNull
    public List<CargoBasedCrate> getMutableReverseDependencies() {
        return reverseDependencies;
    }

    @Override
    public boolean getHasCyclicDevDependencies() {
        return !cyclicDevDeps.isEmpty();
    }

    @Nullable
    @Override
    public VirtualFile getRootModFile() {
        return rootModFile;
    }

    @Nullable
    @Override
    public RsFile getRootMod() {
        if (rootModFile == null) return null;
        com.intellij.psi.PsiFile psiFile = org.rust.openapiext.OpenApiUtil.toPsiFile(rootModFile, getProject());
        return psiFile != null ? RsFileUtil.getRustFile(psiFile) : null;
    }

    @NotNull
    @Override
    public Project getProject() {
        return cargoProject.getProject();
    }

    @Override
    public boolean getAreDoctestsEnabled() {
        return cargoTarget.getDoctest() && isDoctestable(cargoTarget);
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return cargoTarget.getName();
    }

    @NotNull
    @Override
    public String getNormName() {
        return cargoTarget.getNormName();
    }

    @Nullable
    @Override
    public CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact() {
        return procMacroArtifact;
    }

    public void setProcMacroArtifact(@Nullable CargoWorkspaceData.ProcMacroArtifact procMacroArtifact) {
        this.procMacroArtifact = procMacroArtifact;
    }

    @Override
    public String toString() {
        return cargoTarget.getName() + "(" + getKind().toString() + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        CargoBasedCrate that = (CargoBasedCrate) other;

        if (!cargoTarget.equals(that.cargoTarget)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        int result = cargoTarget.hashCode();
        result = 31 * result + (id != null ? id : 0);
        return result;
    }

    // See https://github.com/rust-lang/cargo/blob/5a0c31d81/src/cargo/core/manifest.rs#L775
    private static boolean isDoctestable(@NotNull CargoWorkspace.Target target) {
        CargoWorkspace.TargetKind kind = target.getKind();
        if (!(kind instanceof CargoWorkspace.TargetKind.Lib)) return false;
        CargoWorkspace.TargetKind.Lib lib = (CargoWorkspace.TargetKind.Lib) kind;
        return lib.getKinds().contains(CargoWorkspace.LibKind.LIB) ||
            lib.getKinds().contains(CargoWorkspace.LibKind.RLIB) ||
            lib.getKinds().contains(CargoWorkspace.LibKind.PROC_MACRO);
    }
}
