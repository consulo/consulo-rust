/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Describes the project model in terms of <i>crates</i>. Should be preferred to
 * {@code org.rust.cargo.project.model.CargoProjectsService} in Rust analysis code
 * (name resolution, type inference, most of the inspections, etc)
 *
 * Crate Graph is <a href="https://en.wikipedia.org/wiki/Directed_acyclic_graph">DAG</a>,
 * i.e. it doesn't have cycles, hence we can do
 * <a href="https://en.wikipedia.org/wiki/Topological_sorting">topological sorting</a> of crates.
 *
 * Use {@link #crateGraph(Project)} to get an instance of the service.
 *
 * <h2>Relations to the Cargo project model</h2>
 *
 * {@link Crate} is usually a wrapper around {@code org.rust.cargo.project.workspace.CargoWorkspace.Target}.
 *
 * <h3>Duplicated packages</h3>
 *
 * Multiple {@code org.rust.cargo.project.model.CargoProject}'s can refer to the <i>same</i> package, but
 * since cargo projects are different, there will be 2 copies of the packages. Each copy can have
 * specific <i>features</i> and <i>dependencies</i> (because of different {@code Cargo.lock} files in different
 * cargo projects). We trying to merge these packages into a single crate, but we can't merge
 * different dependencies, so only one variant is preferred for now.
 *
 * <h3>Cyclic dependencies</h3>
 *
 * A Cargo package can have a cycle dependency on itself through {@code [dev-dependencies]}. It works in
 * Cargo because Cargo package basically consists of two crates: one "production" crate and one
 * {@code --cfg=test} crate, so "test" crate can depends on "production" one.
 * We need to avoid cyclic dependencies because we need DAG in order to do topological sorting
 * of crates, so we just remove cyclic {@code [dev-dependencies]} from the graph for now.
 */
public interface CrateGraphService {
    /**
     * <a href="https://en.wikipedia.org/wiki/Topological_sorting">Topological sorted</a>
     * list of all crates in the project
     */
    @NotNull
    List<Crate> getTopSortedCrates();

    /** See {@link Crate#getId()} */
    @Nullable
    Crate findCrateById(int id);

    @Nullable
    Crate findCrateByRootMod(@NotNull VirtualFile rootModFile);

    @NotNull
    static CrateGraphService crateGraph(@NotNull Project project) {
        return project.getService(CrateGraphService.class);
    }
}
