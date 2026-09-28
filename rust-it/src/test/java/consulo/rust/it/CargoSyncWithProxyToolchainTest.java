/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

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
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one path the other tests skip: a real sync. A stub toolchain stands in for cargo and rustc, so
 * the plugin runs its actual command lines, parses real {@code cargo metadata} output and builds the
 * workspace from it - without a Rust installation.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class CargoSyncWithProxyToolchainTest {

    @Test
    // both are logged while the project closes, after every assertion below has already passed: the
    // external-system auto-import tracker times out disposing its background task, and a listener
    // outlives the bus it was connected to. Neither is reached by the sync itself.
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void syncBuildsTheWorkspaceFromProxiedCargoOutput(HeadlessProjects projects) throws Exception {
        Path directory = CargoProxyToolchain.writeProject("consulo-rust-it-sync");
        Path home = CargoProxyToolchain.writeToolchain(directory);
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(home);

        Project project = projects.open(directory);
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
            Module module = HeadlessModules.createModule(project, "proxied",
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory));
            WriteAction.run(() -> {
                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
                extension.setEnabled(true);
                extension.getInheritableSdk().set(null, sdk);
                rootModel.commit();
            });

            CargoProjectsService service = CargoProjectsService.getInstance(project);
            List<CargoProject> attached = List.copyOf(
                service.attachCargoProjectAsync(directory.resolve("Cargo.toml"))
                    .thenCompose(ignored -> service.refreshAllProjects())
                    .get(120, TimeUnit.SECONDS));

            assertThat(attached).as("the sync must produce a Cargo project").isNotEmpty();

            CargoWorkspace workspace = ReadAction.compute(() -> attached.get(0).getWorkspace());
            assertThat(workspace)
                .as("the workspace must be built from the proxied cargo metadata")
                .isNotNull();
            assertThat(workspace.getPackages())
                .as("the package the stub reported must be in the workspace")
                .anyMatch(pkg -> pkg.getName().equals("proxied"));
        }
        finally {
            WriteAction.run(() -> sdkTable.removeSdk(sdk));
            projects.close(project);
        }
    }
}
