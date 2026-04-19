/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.RustcInfo;
import org.rust.cargo.toolchain.impl.RustcVersion;
import com.intellij.util.text.SemVer;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for StandardLibrary extension functions.
 */
public final class StandardLibraryHelper {

    private StandardLibraryHelper() {}

    public static List<CargoWorkspaceData.Package> asPackageData(StandardLibrary stdlib, @Nullable RustcInfo rustcInfo) {
        if (!stdlib.isHardcoded()) return stdlib.getCrates();
        List<CargoWorkspaceData.Package> result = new ArrayList<>();
        for (CargoWorkspaceData.Package crate : stdlib.getCrates()) {
            result.add(withProperEdition(crate, rustcInfo));
        }
        return result;
    }

    private static CargoWorkspaceData.Package withProperEdition(CargoWorkspaceData.Package pkg, @Nullable RustcInfo rustcInfo) {
        SemVer currentRustcVersion = null;
        if (rustcInfo != null && rustcInfo.getVersion() != null) {
            currentRustcVersion = rustcInfo.getVersion().getSemver();
        }
        CargoWorkspace.Edition edition;
        if (currentRustcVersion == null) {
            edition = CargoWorkspace.Edition.EDITION_2015;
        } else {
            edition = CargoWorkspace.Edition.EDITION_2018;
        }

        List<CargoWorkspaceData.Target> newTargets = new ArrayList<>();
        for (CargoWorkspaceData.Target target : pkg.getTargets()) {
            newTargets.add(target.copyWithEdition(edition));
        }
        return pkg.copyWithTargetsAndEdition(newTargets, edition);
    }
}
