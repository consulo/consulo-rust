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
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.resolve2.DefMapService;

import java.util.*;
import java.util.stream.Collectors;

public class DoctestCrate extends UserDataHolderBase implements Crate {
    @NotNull
    private final Crate parentCrate;
    @NotNull
    private final RsFile rootMod;
    @NotNull
    private final Collection<Crate.Dependency> dependencies;
    @NotNull
    private final LinkedHashSet<Crate> flatDependencies;
    private final int id;

    public DoctestCrate(
        @NotNull Crate parentCrate,
        @NotNull RsFile rootMod,
        @NotNull Collection<Crate.Dependency> dependencies
    ) {
        this.parentCrate = parentCrate;
        this.rootMod = rootMod;
        this.dependencies = dependencies;
        this.flatDependencies = Util.flattenTopSortedDeps(dependencies);
        this.id = DefMapService.getNextNonCargoCrateId();
    }

    @NotNull
    @Override
    public List<Crate> getReverseDependencies() {
        return Collections.emptyList();
    }

    @NotNull
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

    @NotNull
    @Override
    public CargoWorkspace.TargetKind getKind() {
        return CargoWorkspace.TargetKind.Test.INSTANCE;
    }

    @NotNull
    @Override
    public CfgOptions getCfgOptions() {
        return CfgOptions.EMPTY;
    }

    @NotNull
    @Override
    public Map<String, FeatureState> getFeatures() {
        return Collections.emptyMap();
    }

    @Override
    public boolean getEvaluateUnknownCfgToFalse() {
        return true;
    }

    @NotNull
    @Override
    public Map<String, String> getEnv() {
        return Collections.emptyMap();
    }

    @Nullable
    @Override
    public VirtualFile getOutDir() {
        return null;
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

    @Nullable
    @Override
    public VirtualFile getRootModFile() {
        return rootMod.getVirtualFile();
    }

    @NotNull
    @Override
    public RsFile getRootMod() {
        return rootMod;
    }

    @NotNull
    @Override
    public PackageOrigin getOrigin() {
        return parentCrate.getOrigin();
    }

    @NotNull
    @Override
    public CargoWorkspace.Edition getEdition() {
        return parentCrate.getEdition();
    }

    @Override
    public boolean getAreDoctestsEnabled() {
        return false;
    }

    @NotNull
    @Override
    public String getPresentableName() {
        return parentCrate.getPresentableName() + "-doctest";
    }

    @NotNull
    @Override
    public String getNormName() {
        return parentCrate.getNormName() + "_doctest";
    }

    @NotNull
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

    @NotNull
    public static DoctestCrate inCrate(@NotNull Crate parentCrate, @NotNull RsFile doctestModule) {
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
