/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModUtil;
import org.rust.openapiext.VirtualFileExtUtil;

import java.nio.file.Path;

public final class RsMoveDirectoryUtils {

    private RsMoveDirectoryUtils() {
    }

    @Nullable
    public static RsMod getOwningMod(@NotNull PsiDirectory directory, @Nullable RsMod crateRoot) {
        if (crateRoot != null && RsModUtil.getOwnedDirectory(crateRoot) == directory) return crateRoot;

        RsFile atDefaultLocation = getOwningModAtDefaultLocation(directory);
        if (atDefaultLocation != null) return atDefaultLocation;

        if (crateRoot != null) {
            RsMod walkingFromCrateRoot = getOwningModWalkingFromCrateRoot(directory, crateRoot);
            if (walkingFromCrateRoot != null) return walkingFromCrateRoot;
        }

        if (crateRoot != null) {
            CargoWorkspace workspace = RsModUtil.getCargoWorkspace(crateRoot);
            if (workspace != null) {
                RsMod walkingFromAllRoots = getOwningModWalkingFromAllCrateRoots(directory, workspace);
                if (walkingFromAllRoots != null) return walkingFromAllRoots;
            }
        }

        return null;
    }

    @Nullable
    public static RsFile getOwningModAtDefaultLocation(@NotNull PsiDirectory directory) {
        RsFile file1 = directory.findFile(RsConstants.MOD_RS_FILE) instanceof RsFile
            ? (RsFile) directory.findFile(RsConstants.MOD_RS_FILE) : null;
        PsiDirectory parentDir = directory.getParentDirectory();
        RsFile file2 = parentDir != null && parentDir.findFile(directory.getName() + ".rs") instanceof RsFile
            ? (RsFile) parentDir.findFile(directory.getName() + ".rs") : null;

        if (file1 != null && file2 == null) return file1;
        if (file2 != null && file1 == null) return file2;
        return null;
    }

    @Nullable
    private static RsMod getOwningModWalkingFromCrateRoot(@NotNull PsiDirectory directory, @NotNull RsMod crateRoot) {
        PsiDirectory crateRootDirectory = RsModUtil.getOwnedDirectory(crateRoot);
        if (crateRootDirectory == null) return null;
        Path crateRootPath = VirtualFileExtUtil.getPathAsPath(crateRootDirectory.getVirtualFile());
        Path path = VirtualFileExtUtil.getPathAsPath(directory.getVirtualFile());
        if (!path.startsWith(crateRootPath)) return null;
        if (directory.equals(crateRootDirectory)) return crateRoot;

        RsMod mod = crateRoot;
        Path relativePath = crateRootPath.relativize(path);
        for (Path segmentPath : relativePath) {
            String segment = segmentPath.toString();
            mod = RsModUtil.getChildModule(mod, segment);
            if (mod == null) return null;
        }
        return mod;
    }

    @Nullable
    private static RsMod getOwningModWalkingFromAllCrateRoots(@NotNull PsiDirectory directory, @NotNull CargoWorkspace cargoWorkspace) {
        for (CargoWorkspace.Package cargoPackage : cargoWorkspace.getPackages()) {
            if (cargoPackage.getOrigin() != PackageOrigin.WORKSPACE) continue;
            for (CargoWorkspace.Target cargoTarget : cargoPackage.getTargets()) {
                if (cargoTarget.getCrateRoot() == null) continue;
                RsFile crateRoot = VirtualFileExtUtil.toPsiFile(cargoTarget.getCrateRoot(), directory.getProject()) instanceof RsFile
                    ? (RsFile) VirtualFileExtUtil.toPsiFile(cargoTarget.getCrateRoot(), directory.getProject()) : null;
                if (crateRoot == null) continue;
                RsMod owningMod = getOwningModWalkingFromCrateRoot(directory, crateRoot);
                if (owningMod != null) return owningMod;
            }
        }
        return null;
    }
}
