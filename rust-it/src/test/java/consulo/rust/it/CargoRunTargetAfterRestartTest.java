/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkTable;
import consulo.it.AllowLogError;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.rust.bundle.RustBundleType;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the run gutter marker needs, after a restart and with Cargo never invoked again.
 * <p>
 * {@code CargoExecutableRunConfigurationProducer.isMainFunction} asks the file's workspace for
 * {@code findTargetByCrateRoot(main.rs)} and requires a bin target back; with no target there is no
 * marker and no "run as application". Everything below the PSI is asserted here: the Cargo project
 * comes back, the file still maps to it, the workspace is rebuilt from the project model, and the
 * crate root of {@code src/main.rs} still resolves to the bin target the sync found.
 *
 * @see CargoSyncWithProxyToolchainTest for the sync this restarts on top of
 */
@ExtendWith(HeadlessProjectExtension.class)
public class CargoRunTargetAfterRestartTest {

    @Test
    // the first two are logged while the project closes. The third is the status-bar widget: its
    // factory builds a Swing panel out of an ide-impl internal on every cargoProjectsUpdated, so a
    // headless application cannot have one - unrelated to anything asserted here.
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil",
        "consulo.it.internal.HeadlessApplicationImpl"
    })
    public void theBinTargetIsStillFoundAfterReopen(Application application, HeadlessProjects projects) throws Exception {
        Path directory = CargoProxyToolchain.writeProject("consulo-rust-it-runmarker");
        Path home = CargoProxyToolchain.writeToolchain(directory);
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(home);

        Project first = projects.open(directory);
        SdkTable sdkTable = SdkTable.getInstance();
        Sdk sdk = WriteAction.compute(() -> {
            Sdk created = sdkTable.createSdk("proxy-rust", RustBundleType.getInstance());
            SdkModificator modificator = created.getSdkModificator();
            modificator.setHomePath(home.toString());
            modificator.commitChanges();
            sdkTable.addSdk(created);
            return created;
        });

        try {
            Module module = HeadlessModules.createModule(first, "proxied",
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory));
            WriteAction.run(() -> {
                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
                extension.setEnabled(true);
                extension.getInheritableSdk().set(null, sdk);
                rootModel.commit();
            });

            CargoProjectsService service = CargoProjectsService.getInstance(first);
            service.attachCargoProjectAsync(directory.resolve("Cargo.toml"))
                .thenCompose(ignored -> service.refreshAllProjects())
                .get(120, TimeUnit.SECONDS);

            assertBinTargetIsFound(first, directory, "after the sync");

            saveProject(first, application);
            projects.close(first);
        }
        finally {
            WriteAction.run(() -> sdkTable.removeSdk(sdk));
        }

        // no toolchain any more: whatever the reopened project knows came out of the project model
        Project second = projects.open(directory);
        try {
            assertBinTargetIsFound(second, directory, "after the reopen, with no toolchain and no sync");
        }
        finally {
            projects.close(second);
        }
    }

    private static void assertBinTargetIsFound(Project project, Path directory, String when) {
        VirtualFile mainFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory.resolve("src/main.rs"));
        assertThat(mainFile).as("src/main.rs must be in the virtual file system " + when).isNotNull();

        CargoProjectsService service = CargoProjectsService.getInstance(project);
        assertThat(service.getAllProjects()).as("the Cargo project must be registered " + when).isNotEmpty();

        CargoProject cargoProject = ReadAction.compute(() -> service.findProjectForFile(mainFile));
        assertThat(cargoProject).as("src/main.rs must map back to its Cargo project " + when).isNotNull();

        CargoWorkspace workspace = ReadAction.compute(cargoProject::getWorkspace);
        assertThat(workspace).as("the Cargo project must have a workspace " + when).isNotNull();

        CargoWorkspace.Target target = ReadAction.compute(() -> workspace.findTargetByCrateRoot(mainFile));
        assertThat(target).as("src/main.rs must be the crate root of a target " + when).isNotNull();
        assertThat(target.getKind().isBin())
            .as("the target of src/main.rs must be a bin, or there is no run marker " + when)
            .isTrue();
    }

    /** @see consulo.it.index.ScanningTestSupport in the platform, which this mirrors. */
    private static void saveProject(Project project, Application application) throws Exception {
        project.saveAsync(application.getLastUIAccess())
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture()
            .get(HeadlessProjects.TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
