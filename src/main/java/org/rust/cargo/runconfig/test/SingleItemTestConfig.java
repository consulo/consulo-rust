/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.stdext.Utils;

import java.util.Collections;
import java.util.List;

public class SingleItemTestConfig implements TestConfig {
    @NotNull
    private final String commandName;
    @NotNull
    private final String path;
    @NotNull
    private final CargoWorkspace.Target target;
    @NotNull
    private final RsElement sourceElement;
    @NotNull
    private final RsElement originalElement;
    private final boolean isIgnored;

    public SingleItemTestConfig(
        @NotNull String commandName,
        @NotNull String path,
        @NotNull CargoWorkspace.Target target,
        @NotNull RsElement sourceElement,
        @NotNull RsElement originalElement,
        boolean isIgnored
    ) {
        this.commandName = commandName;
        this.path = path;
        this.target = target;
        this.sourceElement = sourceElement;
        this.originalElement = originalElement;
        this.isIgnored = isIgnored;
    }

    @NotNull
    @Override
    public String getCommandName() {
        return commandName;
    }

    @NotNull
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean getExact() {
        return sourceElement instanceof RsFunction;
    }

    @NotNull
    @Override
    public List<CargoWorkspace.Target> getTargets() {
        return Collections.singletonList(target);
    }

    @NotNull
    @Override
    public String getConfigurationName() {
        StringBuilder sb = new StringBuilder();
        sb.append(Utils.capitalized(commandName));
        sb.append(" ");

        if (!(sourceElement instanceof RsMod)) {
            sb.append(path);
            return sb.toString();
        }

        RsMod mod = (RsMod) sourceElement;
        String modName = mod.getModName();
        if ("test".equals(modName) || "tests".equals(modName)) {
            RsMod superMod = mod.getSuper();
            sb.append(superMod != null ? superMod.getModName() : "");
            sb.append("::");
        }
        sb.append(modName);
        return sb.toString();
    }

    @NotNull
    @Override
    public PsiElement getSourceElement() {
        return sourceElement;
    }

    @NotNull
    @Override
    public PsiElement getOriginalElement() {
        return originalElement;
    }

    @Override
    public boolean isIgnored() {
        return isIgnored;
    }
}
