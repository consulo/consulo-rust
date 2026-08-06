/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import org.rust.cargo.project.workspace.CargoWorkspaceData;

import java.util.Objects;

public final class ProjectDescription {

    private final CargoWorkspaceData myWorkspaceData;
    private final ProjectDescriptionStatus myStatus;

    public ProjectDescription(CargoWorkspaceData workspaceData, ProjectDescriptionStatus status) {
        myWorkspaceData = workspaceData;
        myStatus = status;
    }

    public CargoWorkspaceData getWorkspaceData() {
        return myWorkspaceData;
    }

    public ProjectDescriptionStatus getStatus() {
        return myStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectDescription)) return false;
        ProjectDescription that = (ProjectDescription) o;
        return Objects.equals(myWorkspaceData, that.myWorkspaceData) && myStatus == that.myStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myWorkspaceData, myStatus);
    }

    @Override
    public String toString() {
        return "ProjectDescription(workspaceData=" + myWorkspaceData + ", status=" + myStatus + ")";
    }
}
