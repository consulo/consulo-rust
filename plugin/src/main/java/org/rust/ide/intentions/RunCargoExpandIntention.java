/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.language.editor.intention.LowPriorityAction;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.show.result.macro.expansion.cargo.expand"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nonnull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final CargoProject myCargoProject;
        private final CargoWorkspace.Target myCargoTarget;
        private final String myCrateRelativePath;

        public Context(@Nonnull CargoProject cargoProject, @Nonnull CargoWorkspace.Target cargoTarget, @Nonnull String crateRelativePath) {
            myCargoProject = cargoProject;
            myCargoTarget = cargoTarget;
            myCrateRelativePath = crateRelativePath;
        }

        @Nonnull
        public CargoProject getCargoProject() {
            return myCargoProject;
        }

        @Nonnull
        public CargoWorkspace.Target getCargoTarget() {
            return myCargoTarget;
        }

        @Nonnull
        public String getCrateRelativePath() {
            return myCrateRelativePath;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
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
