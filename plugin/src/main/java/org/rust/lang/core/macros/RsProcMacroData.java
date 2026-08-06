/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import jakarta.annotation.Nonnull;
import org.rust.cargo.project.workspace.CargoWorkspaceData;

import java.util.Objects;

public class RsProcMacroData extends RsMacroData {
    private final String myName;
    private final CargoWorkspaceData.ProcMacroArtifact myArtifact;

    public RsProcMacroData(@Nonnull String name, @Nonnull CargoWorkspaceData.ProcMacroArtifact artifact) {
        myName = name;
        myArtifact = artifact;
    }

    @Nonnull
    public String getName() {
        return myName;
    }

    @Nonnull
    public CargoWorkspaceData.ProcMacroArtifact getArtifact() {
        return myArtifact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RsProcMacroData that = (RsProcMacroData) o;
        return myName.equals(that.myName) && myArtifact.equals(that.myArtifact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myArtifact);
    }

    @Override
    public String toString() {
        return "RsProcMacroData(name=" + myName + ", artifact=" + myArtifact + ")";
    }
}
