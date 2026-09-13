/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.impl.RsElementExtUtil;
import org.rust.lang.core.psi.impl.RsFile;


import consulo.language.psi.PsiDirectory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.io.FileUtil;
import org.rust.lang.RsConstants;
import org.rust.lang.RsFileType;
import org.rust.openapiext.VirtualFileExtUtil;

public interface RsMod extends RsQualifiedNamedElement, RsItemsOwner, RsVisible, RsDocAndAttributeOwner {
    /**
     * Returns a parent module ({@code super::} in paths).
     * The parent module may be in the same or other file.
     */
    @Nullable
    RsMod getSuper();

    /**
     * This might be different than {@link consulo.language.psi.PsiNamedElement#getName()}.
     */
    @Nullable
    String getModName();

    /**
     * Returns value of {@code path} attribute related to this module.
     * If module doesn't have a {@code path} attribute, returns null.
     */
    @Nullable
    String getPathAttribute();

    boolean getOwnsDirectory();

    /**
     * Returns directory where direct submodules should be located.
     */
    @Nullable
    default PsiDirectory getOwnedDirectory(boolean createIfNotExists) {
        PsiFile contextualFile = RsElementExtUtil.getContextualFile(this);
        if ((this instanceof RsFile && RsConstants.MOD_RS_FILE.equals(getName())) || isCrateRoot()) {
            return contextualFile.getOriginalFile().getParent();
        }

        String explicitPath = getPathAttribute();
        RsMod superMod = getSuper();

        PsiDirectory parentDirectory;
        String path;
        if (explicitPath != null) {
            if (this instanceof RsFile) {
                return contextualFile.getOriginalFile().getParent();
            }
            parentDirectory = superMod instanceof RsFile
                ? contextualFile.getOriginalFile().getParent()
                : (superMod != null ? superMod.getOwnedDirectory(createIfNotExists) : null);
            path = explicitPath;
        }
        else {
            parentDirectory = superMod != null ? superMod.getOwnedDirectory(createIfNotExists) : null;
            path = getName();
        }
        if (parentDirectory == null || path == null) {
            return null;
        }

        // a relative path like `./foo` must survive, so the extension is stripped by suffix rather than by
        // taking the name without extension
        String suffix = "." + RsFileType.INSTANCE.getDefaultExtension();
        String directoryPath = FileUtil.toSystemIndependentName(path);
        if (directoryPath.endsWith(suffix)) {
            directoryPath = directoryPath.substring(0, directoryPath.length() - suffix.length());
        }

        VirtualFile found = VirtualFileExtUtil.findFileByMaybeRelativePath(parentDirectory.getVirtualFile(), directoryPath);
        PsiDirectory directory = found != null ? parentDirectory.getManager().findDirectory(found) : null;
        if (directory == null && createIfNotExists) {
            return parentDirectory.createSubdirectory(directoryPath);
        }
        return directory;
    }

    @Nullable
    default PsiDirectory getOwnedDirectory() {
        return getOwnedDirectory(false);
    }

    boolean isCrateRoot();

    @Nonnull
    default java.util.List<RsMod> getSuperMods() {
        return RsPsiSupport.getInstance().superMods(this);
    }
}
