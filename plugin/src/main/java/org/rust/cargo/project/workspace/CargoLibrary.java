/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A set of Cargo roots that the IDE treats as a single dependency of a Rust module: sources that must
 * be indexed, navigable and searchable without belonging to a module of their own.
 */
public final class CargoLibrary {

    public enum Kind {
        /** A single package resolved from a registry, a git checkout or a path dependency. */
        DEPENDENCY,
        /** Code written into {@code OUT_DIR} by build scripts. */
        GENERATED
    }

    private final Kind myKind;
    private final String myId;
    private final String myName;
    @Nullable
    private final String myVersion;
    private final Set<VirtualFile> mySourceRoots;
    private final Set<VirtualFile> myExcludedRoots;

    public CargoLibrary(
        @Nonnull Kind kind,
        @Nonnull String id,
        @Nonnull String name,
        @Nullable String version,
        @Nonnull Set<VirtualFile> sourceRoots,
        @Nonnull Set<VirtualFile> excludedRoots
    ) {
        myKind = kind;
        myId = id;
        myName = name;
        myVersion = version;
        mySourceRoots = Collections.unmodifiableSet(new LinkedHashSet<>(sourceRoots));
        myExcludedRoots = Collections.unmodifiableSet(new LinkedHashSet<>(excludedRoots));
    }

    @Nonnull
    public Kind getKind() {
        return myKind;
    }

    /**
     * Identifies the library inside its Cargo project: the package id for {@link Kind#DEPENDENCY},
     * the manifest path of the owning Cargo project otherwise.
     */
    @Nonnull
    public String getId() {
        return myId;
    }

    @Nonnull
    public String getName() {
        return myName;
    }

    @Nullable
    public String getVersion() {
        return myVersion;
    }

    @Nonnull
    public Set<VirtualFile> getSourceRoots() {
        return mySourceRoots;
    }

    /**
     * Directories and crate roots below {@link #getSourceRoots()} that carry no code the rest of the
     * project can refer to, such as the test, example and benchmark targets of a dependency. They are
     * taken out of the index along with everything below them.
     */
    @Nonnull
    public Set<VirtualFile> getExcludedRoots() {
        return myExcludedRoots;
    }

    @Nonnull
    public String getPresentableText() {
        return myVersion != null ? myName + " " + myVersion : myName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CargoLibrary)) return false;
        CargoLibrary other = (CargoLibrary) o;
        return myKind == other.myKind
            && myId.equals(other.myId)
            && mySourceRoots.equals(other.mySourceRoots)
            && myExcludedRoots.equals(other.myExcludedRoots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myKind, myId, mySourceRoots, myExcludedRoots);
    }

    @Override
    public String toString() {
        return "CargoLibrary(" + myKind + ", " + getPresentableText() + ")";
    }
}
