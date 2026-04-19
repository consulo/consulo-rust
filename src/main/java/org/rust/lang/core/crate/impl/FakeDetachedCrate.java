/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;

import java.util.Collection;
import java.util.LinkedHashSet;

/** Fake crate for {@link RsFile} outside of module tree. */
public class FakeDetachedCrate extends FakeCrate {
    @NotNull
    private final RsFile rootMod;
    private final int id;
    @NotNull
    private final Collection<Crate.Dependency> dependencies;
    @NotNull
    private final LinkedHashSet<Crate> flatDependencies;

    public FakeDetachedCrate(
        @NotNull RsFile rootMod,
        int id,
        @NotNull Collection<Crate.Dependency> dependencies
    ) {
        this.rootMod = rootMod;
        this.id = id;
        this.dependencies = dependencies;
        this.flatDependencies = Util.flattenTopSortedDeps(dependencies);
    }

    @Nullable
    @Override
    public Integer getId() {
        return id;
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
    public RsFile getRootMod() {
        return rootMod;
    }

    @Nullable
    @Override
    public VirtualFile getRootModFile() {
        return rootMod.getVirtualFile();
    }

    @NotNull
    @Override
    public String getPresentableName() {
        VirtualFile vf = getRootModFile();
        return "Fake for " + (vf != null ? vf.getPath() : null);
    }

    @NotNull
    @Override
    public Project getProject() {
        return rootMod.getProject();
    }
}
