/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.component.util.ModificationTracker;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsElement;

public final class RsPsiManagerUtil {
    private RsPsiManagerUtil() {
    }

    private static final Class<RustStructureChangeListener> RUST_STRUCTURE_CHANGE_TOPIC =
        RustStructureChangeListener.class;

    private static final Class<RustPsiChangeListener> RUST_PSI_CHANGE_TOPIC =
        RustPsiChangeListener.class;

    @Nonnull
    public static Class<RustStructureChangeListener> getRUST_STRUCTURE_CHANGE_TOPIC() {
        return RUST_STRUCTURE_CHANGE_TOPIC;
    }

    @Nonnull
    public static Class<RustPsiChangeListener> getRUST_PSI_CHANGE_TOPIC() {
        return RUST_PSI_CHANGE_TOPIC;
    }

    @Nonnull
    public static RsPsiManager getRustPsiManager(@Nonnull Project project) {
        return project.getService(RsPsiManager.class);
    }

    @Nonnull
    public static ModificationTracker getRustStructureModificationTracker(@Nonnull Project project) {
        return getRustPsiManager(project).getRustStructureModificationTracker();
    }

    @Nonnull
    public static ModificationTracker getRustStructureModificationTracker(@Nonnull Crate crate) {
        return getRustStructureModificationTracker(crate.getProject());
    }

    /**
     * The tracker a cached value derived from {@code element} must depend on.
     * <p>
     * Injected PSI has no event system of its own - Rust injected into another language's string
     * literal only ever hears that the literal changed - so anything inside it has to invalidate on
     * any PSI change at all. A workspace file follows the Rust structure, and everything else follows
     * the structure of the dependencies, which changes far less often.
     */
    @Nonnull
    public static ModificationTracker getRustStructureOrAnyPsiModificationTracker(@Nonnull RsElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) {
            return getRustPsiManager(element.getProject()).getRustStructureModificationTracker();
        }

        Project project = containingFile.getProject();
        if (containingFile.getVirtualFile() instanceof VirtualFileWindow) {
            return PsiManager.getInstance(project).getModificationTracker();
        }

        if (containingFile instanceof org.rust.lang.core.psi.RsFile) {
            org.rust.lang.core.crate.Crate crate = ((org.rust.lang.core.psi.RsFile) containingFile).getCrate();
            if (crate != null && crate.getOrigin() == org.rust.cargo.project.workspace.PackageOrigin.WORKSPACE) {
                return getRustPsiManager(project).getRustStructureModificationTracker();
            }
        }

        return getRustPsiManager(project).getRustStructureModificationTrackerInDependencies();
    }
}
