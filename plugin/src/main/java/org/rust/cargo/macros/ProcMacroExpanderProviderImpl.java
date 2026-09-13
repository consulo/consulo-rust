package org.rust.cargo.macros;

import org.rust.cargo.toolchain.RsToolchainLocator;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.util.lang.SemVer;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.lang.core.crate.Crate;
import org.rust.cargo.macros.ProcMacroApplicationService;
import org.rust.lang.core.macros.proc.ProcMacroExpander;
import org.rust.lang.core.macros.proc.ProcMacroExpanderProvider;
import org.rust.cargo.macros.ProcMacroServerPool;

import java.nio.file.Path;

/**
 * Resolves the toolchain a crate is built with and hands the expander the pieces it needs.
 */
@Singleton
@ServiceImpl
public class ProcMacroExpanderProviderImpl implements ProcMacroExpanderProvider {
    private static final SemVer MIN_RUSTC_VERSION_WITH_EXPANDER_VERSION_CHECK = SemVer.parseFromText("1.64.0");

    @Nonnull
    @Override
    public ProcMacroExpander forCrate(@Nonnull Crate crate) {
        Project project = crate.getProject();
        RsToolchainBase toolchain = RsToolchainLocator.fromSettings(project);
        SemVer rustcVersion = crate.getCargoProject() != null && crate.getCargoProject().getRustcInfo() != null
            ? crate.getCargoProject().getRustcInfo().getRealVersion().getSemver()
            : null;
        Path expanderPath = crate.getCargoProject() != null
            ? crate.getCargoProject().getProcMacroExpanderPath()
            : null;

        ProcMacroServerPool server = null;
        if (toolchain != null && rustcVersion != null && expanderPath != null) {
            boolean needsVersionCheck = rustcVersion.compareTo(MIN_RUSTC_VERSION_WITH_EXPANDER_VERSION_CHECK) >= 0;
            server = ProcMacroApplicationService.getInstance().getServer(toolchain, needsVersionCheck, expanderPath);
        }
        return ProcMacroExpander.create(project, toolchain == null ? null : toolchain::toRemotePath, server);
    }
}
