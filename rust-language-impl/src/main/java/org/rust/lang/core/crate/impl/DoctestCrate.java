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
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.api.workspace.FeatureState;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.resolve2.DefMapService;

import java.util.*;
import java.util.stream.Collectors;

public class DoctestCrate extends UserDataHolderBase implements Crate {
    @Nonnull
    private final Crate parentCrate;
    @Nonnull
    private final RsFile rootMod;
    @Nonnull
    private final Collection<Crate.Dependency> dependencies;
    @Nonnull
    private final LinkedHashSet<Crate> flatDependencies;
    private final int id;

    public DoctestCrate(
        @Nonnull Crate parentCrate,
        @Nonnull RsFile rootMod,
        @Nonnull Collection<Crate.Dependency> dependencies
    ) {
        this.parentCrate = parentCrate;
        this.rootMod = rootMod;
        this.dependencies = dependencies;
        this.flatDependencies = Util.flattenTopSortedDeps(dependencies);
        this.id = DefMapService.getNextNonCargoCrateId();
    }

    @Nonnull
    @Override
    public List<Crate> getReverseDependencies() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    @Override
    public CargoProject getCargoProject() {
        return parentCrate.getCargoProject();
    }

    @Nullable
    @Override
    public CargoWorkspace.Target getCargoTarget() {
        return null;
    }

    @Nullable
    @Override
    public CargoWorkspace getCargoWorkspace() {
        return parentCrate.getCargoWorkspace();
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
    public Collection<Dependency> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public LinkedHashSet<Crate> getFlatDependencies() {
        return flatDependencies;
    }

    @Nullable
    @Override
    public VirtualFile getRootModFile() {
        return rootMod.getVirtualFile();
    }

    @Nonnull
    @Override
    public RsFile getRootMod() {
        return rootMod;
    }

    @Nonnull
    @Override
    public PackageOrigin getOrigin() {
        return parentCrate.getOrigin();
    }

    @Nonnull
    @Override
    public CargoWorkspace.Edition getEdition() {
        return parentCrate.getEdition();
    }

    @Override
    public boolean getAreDoctestsEnabled() {
        return false;
    }

    @Nonnull
    @Override
    public String getPresentableName() {
        return parentCrate.getPresentableName() + "-doctest";
    }

    @Nonnull
    @Override
    public String getNormName() {
        return parentCrate.getNormName() + "_doctest";
    }

    @Nonnull
    @Override
    public Project getProject() {
        return parentCrate.getProject();
    }

    @Nullable
    @Override
    public CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact() {
        return null;
    }

    @Override
    public String toString() {
        CargoWorkspace.Target target = parentCrate.getCargoTarget();
        return "Doctest in " + (target != null ? target.getName() : null);
    }

    @Nonnull
    public static DoctestCrate inCrate(@Nonnull Crate parentCrate, @Nonnull RsFile doctestModule) {
        if (parentCrate.getOrigin() != PackageOrigin.STDLIB) {
            List<Dependency> dependencies = new ArrayList<>(parentCrate.getDependenciesWithCyclic());
            dependencies.add(new Dependency(parentCrate.getNormName(), parentCrate));
            return new DoctestCrate(parentCrate, doctestModule, dependencies);
        } else {
            // A doctest located in the stdlib is depending on all stdlib crates
            CrateGraphService crateGraph = CrateGraphService.crateGraph(parentCrate.getProject());
            List<Dependency> stdCrates = crateGraph.getTopSortedCrates().stream()
                .filter(c -> c.getOrigin() == PackageOrigin.STDLIB)
                .map(c -> new Dependency(c.getNormName(), c))
                .collect(Collectors.toList());
            // Remove duplicates by normName
            LinkedHashMap<String, Dependency> uniqueByName = new LinkedHashMap<>();
            for (Dependency dep : stdCrates) {
                uniqueByName.putIfAbsent(dep.getNormName(), dep);
            }
            return new DoctestCrate(parentCrate, doctestModule, new ArrayList<>(uniqueByName.values()));
        }
    }
}
