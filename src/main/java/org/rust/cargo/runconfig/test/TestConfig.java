/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.toolchain.CargoCommandLine;

import java.util.List;

public interface TestConfig {
    @NotNull
    String getCommandName();

    @NotNull
    String getPath();

    boolean getExact();

    @NotNull
    List<CargoWorkspace.Target> getTargets();

    @NotNull
    String getConfigurationName();

    @NotNull
    PsiElement getSourceElement();

    @NotNull
    default PsiElement getOriginalElement() {
        return getSourceElement();
    }

    default boolean isIgnored() {
        return false;
    }

    default boolean isDoctest() {
        return false;
    }

    @NotNull
    default CargoCommandLine cargoCommandLine() {
        CargoCommandLine commandLine = CargoCommandLine.forTargets(getTargets(), getCommandName(), List.of(getPath()), isDoctest());
        if (getExact()) {
            commandLine = commandLine.withPositionalArgument("--exact");
        }
        if (isIgnored()) {
            commandLine = commandLine.withPositionalArgument("--ignored");
        }
        return commandLine;
    }
}
