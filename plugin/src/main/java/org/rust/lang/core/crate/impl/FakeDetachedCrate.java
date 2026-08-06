/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate.impl;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;

import java.util.Collection;
import java.util.LinkedHashSet;

/** Fake crate for {@link RsFile} outside of module tree. */
public class FakeDetachedCrate extends FakeCrate {
    @Nonnull
    private final RsFile rootMod;
    private final int id;
    @Nonnull
    private final Collection<Crate.Dependency> dependencies;
    @Nonnull
    private final LinkedHashSet<Crate> flatDependencies;

    public FakeDetachedCrate(
        @Nonnull RsFile rootMod,
        int id,
        @Nonnull Collection<Crate.Dependency> dependencies
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
    public RsFile getRootMod() {
        return rootMod;
    }

    @Nullable
    @Override
    public VirtualFile getRootModFile() {
        return rootMod.getVirtualFile();
    }

    @Nonnull
    @Override
    public String getPresentableName() {
        VirtualFile vf = getRootModFile();
        return "Fake for " + (vf != null ? vf.getPath() : null);
    }

    @Nonnull
    @Override
    public Project getProject() {
        return rootMod.getProject();
    }
}
