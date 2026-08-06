/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.stdext.Utils;

import java.util.Collections;
import java.util.List;

public class SingleItemTestConfig implements TestConfig {
    @Nonnull
    private final String commandName;
    @Nonnull
    private final String path;
    @Nonnull
    private final CargoWorkspace.Target target;
    @Nonnull
    private final RsElement sourceElement;
    @Nonnull
    private final RsElement originalElement;
    private final boolean isIgnored;

    public SingleItemTestConfig(
        @Nonnull String commandName,
        @Nonnull String path,
        @Nonnull CargoWorkspace.Target target,
        @Nonnull RsElement sourceElement,
        @Nonnull RsElement originalElement,
        boolean isIgnored
    ) {
        this.commandName = commandName;
        this.path = path;
        this.target = target;
        this.sourceElement = sourceElement;
        this.originalElement = originalElement;
        this.isIgnored = isIgnored;
    }

    @Nonnull
    @Override
    public String getCommandName() {
        return commandName;
    }

    @Nonnull
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean getExact() {
        return sourceElement instanceof RsFunction;
    }

    @Nonnull
    @Override
    public List<CargoWorkspace.Target> getTargets() {
        return Collections.singletonList(target);
    }

    @Nonnull
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

    @Nonnull
    @Override
    public PsiElement getSourceElement() {
        return sourceElement;
    }

    @Nonnull
    @Override
    public PsiElement getOriginalElement() {
        return originalElement;
    }

    @Override
    public boolean isIgnored() {
        return isIgnored;
    }
}
