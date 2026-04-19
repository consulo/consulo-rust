/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.intention.LowPriorityAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.List;

public class RunCargoExpandIntention extends RsElementBaseIntentionAction<RunCargoExpandIntention.Context> implements LowPriorityAction {

    private static final String PATH_SEPARATOR = "::";

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.show.result.macro.expansion.cargo.expand");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @NotNull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final CargoProject myCargoProject;
        private final CargoWorkspace.Target myCargoTarget;
        private final String myCrateRelativePath;

        public Context(@NotNull CargoProject cargoProject, @NotNull CargoWorkspace.Target cargoTarget, @NotNull String crateRelativePath) {
            myCargoProject = cargoProject;
            myCargoTarget = cargoTarget;
            myCrateRelativePath = crateRelativePath;
        }

        @NotNull
        public CargoProject getCargoProject() {
            return myCargoProject;
        }

        @NotNull
        public CargoWorkspace.Target getCargoTarget() {
            return myCargoTarget;
        }

        @NotNull
        public String getCrateRelativePath() {
            return myCrateRelativePath;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsItemsOwner itemsOwner = PsiElementExt.ancestorStrict(element, RsItemsOwner.class);
        if (!(itemsOwner instanceof RsMod)) return null;
        RsMod mod = (RsMod) itemsOwner;
        CargoProject cargoProject = RsModUtil.getCargoProject(mod);
        if (cargoProject == null) return null;
        CargoWorkspace.Target cargoTarget = RsModUtil.getContainingCargoTarget(mod);
        if (cargoTarget == null) return null;
        String crateRelativePath = RsModUtil.getCrateRelativePath(mod);
        if (crateRelativePath == null) return null;
        return new Context(cargoProject, cargoTarget, crateRelativePath);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        CargoProject cargoProject = ctx.getCargoProject();
        CargoWorkspace.Target cargoTarget = ctx.getCargoTarget();
        String crateRelativePath = ctx.getCrateRelativePath();

        if (Cargo.checkNeedInstallCargoExpand(cargoProject.getProject())) return;

        String theme = OpenApiUtil.isUnderDarkTheme() ? "Dracula" : "GitHub";
        List<String> additionalArguments = new ArrayList<>();
        additionalArguments.add("--color=always");
        additionalArguments.add("--theme=" + theme);
        additionalArguments.add("--tests");
        if (!crateRelativePath.isEmpty()) {
            String path = crateRelativePath;
            if (path.startsWith(PATH_SEPARATOR)) {
                path = path.substring(PATH_SEPARATOR.length());
            }
            additionalArguments.add(path);
        }

        CargoCommandLine.forTarget(
            cargoTarget,
            "expand",
            additionalArguments
        ).run(cargoProject, "Expand " + cargoTarget.getNormName() + crateRelativePath);
    }
}
