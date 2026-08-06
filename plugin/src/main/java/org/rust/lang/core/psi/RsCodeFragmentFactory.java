/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.parser.RustParserUtil.PathParsingMode;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.resolve.Namespace;
import org.rust.openapiext.OpenApiUtil;

import java.util.Set;

public class RsCodeFragmentFactory {
    public static final String TMP_MOD_NAME = "__tmp__";

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final RsPsiFactory myPsiFactory;

    public RsCodeFragmentFactory(@Nonnull Project project) {
        myProject = project;
        myPsiFactory = new RsPsiFactory(project, false, false);
    }

    @Nullable
    public RsPath createCrateRelativePath(@Nonnull String pathText, @Nonnull CargoWorkspace.Target target) {
        if (pathText.startsWith("::")) {
            throw new IllegalArgumentException("Path must not start with '::'");
        }
        consulo.virtualFileSystem.VirtualFile vFile = target.getCrateRoot();
        if (vFile == null) return null;
        RsFile crateRoot = OpenApiUtil.toPsiFile(vFile, myProject) instanceof RsFile
            ? (RsFile) OpenApiUtil.toPsiFile(vFile, myProject) : null;
        if (crateRoot == null) return null;
        return createPath(pathText, crateRoot, PathParsingMode.TYPE, Namespace.TYPES_N_VALUES_N_MACROS);
    }

    @Nullable
    public RsPath createPath(@Nonnull String path, @Nonnull RsElement context) {
        return createPath(path, context, PathParsingMode.TYPE, Namespace.TYPES_N_VALUES);
    }

    @Nullable
    public RsPath createPath(@Nonnull String path, @Nonnull RsElement context,
                             @Nonnull PathParsingMode mode, @Nonnull Set<Namespace> ns) {
        return new RsPathCodeFragment(myProject, path, false, context, mode, ns).getPath();
    }

    @Nullable
    public RsPath createPathInTmpMod(@Nonnull String importingPathName, @Nonnull RsMod context,
                                     @Nonnull PathParsingMode mode, @Nonnull Set<Namespace> ns,
                                     @Nonnull String usePath, @Nullable String crateName) {
        String externCrateItem;
        String useItem;
        if (crateName != null) {
            externCrateItem = "extern crate " + crateName + ";";
            useItem = "use self::" + usePath + ";";
        } else {
            externCrateItem = "";
            useItem = "use " + usePath + ";";
        }
        RsModItem mod = myPsiFactory.createModItem(TMP_MOD_NAME,
            externCrateItem + "\n" +
            "use super::*;\n" +
            useItem + "\n");
        RsExpandedElementUtil.setContext(mod, context);
        return createPath(importingPathName, mod, mode, ns);
    }
}
