/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import consulo.navigation.ItemPresentation;
import com.intellij.openapi.roots.SyntheticLibrary;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Set;

/**
 * IDEA side of Cargo package from crates.io
 */
public class CargoLibrary extends SyntheticLibrary implements ItemPresentation {

    private final String myName;
    private final Set<VirtualFile> mySourceRoots;
    private final Set<VirtualFile> myExcludedRoots;
    private final consulo.ui.image.Image myIcon;
    @Nullable
    private final String myVersion;

    public CargoLibrary(
        String name,
        Set<VirtualFile> sourceRoots,
        Set<VirtualFile> excludedRoots,
        consulo.ui.image.Image icon,
        @Nullable String version
    ) {
        myName = name;
        mySourceRoots = sourceRoots;
        myExcludedRoots = excludedRoots;
        myIcon = icon;
        myVersion = version;
    }

    @Override
    public Collection<VirtualFile> getSourceRoots() {
        return mySourceRoots;
    }

    @Override
    public Set<VirtualFile> getExcludedRoots() {
        return myExcludedRoots;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CargoLibrary && ((CargoLibrary) other).mySourceRoots.equals(mySourceRoots);
    }

    @Override
    public int hashCode() {
        return mySourceRoots.hashCode();
    }

    @Override
    @Nullable
    public String getLocationString() {
        return null;
    }

    @Override
    public consulo.ui.image.Image getIcon(boolean unused) {
        return myIcon;
    }

    @Override
    public String getPresentableText() {
        return myVersion != null ? myName + " " + myVersion : myName;
    }
}
