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
import java.util.Collections;
import java.util.LinkedHashSet;

/** Fake crate for cases when something is very wrong */
public class FakeInvalidCrate extends FakeCrate {
    @NotNull
    private final Project project;

    public FakeInvalidCrate(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public Integer getId() {
        return null;
    }

    @NotNull
    @Override
    public Collection<Dependency> getDependencies() {
        return Collections.emptyList();
    }

    @NotNull
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

    @NotNull
    @Override
    public String getPresentableName() {
        return "Fake";
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }
}
