/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    private final Project myProject;
    @NotNull
    private final RsPsiFactory myPsiFactory;

    public RsCodeFragmentFactory(@NotNull Project project) {
        myProject = project;
        myPsiFactory = new RsPsiFactory(project, false, false);
    }

    @Nullable
    public RsPath createCrateRelativePath(@NotNull String pathText, @NotNull CargoWorkspace.Target target) {
        if (pathText.startsWith("::")) {
            throw new IllegalArgumentException("Path must not start with '::'");
        }
        com.intellij.openapi.vfs.VirtualFile vFile = target.getCrateRoot();
        if (vFile == null) return null;
        RsFile crateRoot = OpenApiUtil.toPsiFile(vFile, myProject) instanceof RsFile
            ? (RsFile) OpenApiUtil.toPsiFile(vFile, myProject) : null;
        if (crateRoot == null) return null;
        return createPath(pathText, crateRoot, PathParsingMode.TYPE, Namespace.TYPES_N_VALUES_N_MACROS);
    }

    @Nullable
    public RsPath createPath(@NotNull String path, @NotNull RsElement context) {
        return createPath(path, context, PathParsingMode.TYPE, Namespace.TYPES_N_VALUES);
    }

    @Nullable
    public RsPath createPath(@NotNull String path, @NotNull RsElement context,
                             @NotNull PathParsingMode mode, @NotNull Set<Namespace> ns) {
        return new RsPathCodeFragment(myProject, path, false, context, mode, ns).getPath();
    }

    @Nullable
    public RsPath createPathInTmpMod(@NotNull String importingPathName, @NotNull RsMod context,
                                     @NotNull PathParsingMode mode, @NotNull Set<Namespace> ns,
                                     @NotNull String usePath, @Nullable String crateName) {
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
