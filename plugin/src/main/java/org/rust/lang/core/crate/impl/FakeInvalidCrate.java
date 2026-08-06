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
import java.util.Collections;
import java.util.LinkedHashSet;

/** Fake crate for cases when something is very wrong */
public class FakeInvalidCrate extends FakeCrate {
    @Nonnull
    private final Project project;

    public FakeInvalidCrate(@Nonnull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public Integer getId() {
        return null;
    }

    @Nonnull
    @Override
    public Collection<Dependency> getDependencies() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public LinkedHashSet<Crate> getFlatDependencies() {
        return new LinkedHashSet<>();
    }

    @Nullable
    @Override
    public VirtualFile getRootModFile() {
        return null;
    }

    @Nullable
    @Override
    public RsFile getRootMod() {
        return null;
    }

    @Nonnull
    @Override
    public String getPresentableName() {
        return "Fake";
    }

    @Nonnull
    @Override
    public Project getProject() {
        return project;
    }
}
