/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GeneratedCodeFakeLibrary extends SyntheticLibrary {

    private final Set<VirtualFile> mySourceRoots;

    public GeneratedCodeFakeLibrary(Set<VirtualFile> sourceRoots) {
        mySourceRoots = sourceRoots;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GeneratedCodeFakeLibrary &&
            ((GeneratedCodeFakeLibrary) other).mySourceRoots.equals(mySourceRoots);
    }

    @Override
    public int hashCode() {
        return mySourceRoots.hashCode();
    }

    @Override
    public Collection<VirtualFile> getSourceRoots() {
        return mySourceRoots;
    }

    @Override
    public boolean isShowInExternalLibrariesNode() {
        return false;
    }

    @Nullable
    public static GeneratedCodeFakeLibrary create(CargoProject cargoProject) {
        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) return null;
        Set<VirtualFile> generatedRoots = new HashSet<>();
        for (CargoWorkspace.Package pkg : workspace.getPackages()) {
            VirtualFile outDir = pkg.getOutDir();
            if (outDir != null) {
                generatedRoots.add(outDir);
            }
        }
        if (generatedRoots.isEmpty()) return null;
        return new GeneratedCodeFakeLibrary(generatedRoots);
    }
}
