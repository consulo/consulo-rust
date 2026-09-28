/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.it.AllowLogError;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.project.workspace.state.CargoWorkspaceStates;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A workspace asked for before the module model carries it must not be remembered as absent.
 * <p>
 * The restored workspace is read out of the module extensions, and the first caller routinely arrives
 * before those are loaded - the publish {@code loadState} schedules wakes the tool window, the status
 * bar and the editor notifications, and every one of them asks. Since opening a project stopped
 * running Cargo, a cached "no workspace" is what the project keeps for the rest of the session: no
 * crate graph, no run line marker on {@code fn main}, and no runnable configuration. That is the
 * regression this pins down.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class CargoWorkspaceAskedTooEarlyTest {

    private static final String PACKAGE_ID = "early 0.1.0 (path+file:///early)";

    @Test
    // both are logged while the project closes, after every assertion has already passed
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void anEarlyMissThenStillFindsTheWorkspace(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-rust-it-early");
        Files.writeString(directory.resolve("Cargo.toml"),
            "[package]\nname = \"early\"\nversion = \"0.1.0\"\nedition = \"2021\"\n");
        Files.createDirectories(directory.resolve("src"));
        Files.writeString(directory.resolve("src/main.rs"), "fn main() {}\n");

        Project project = projects.open(directory);
        try {
            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
            assertThat(root).as("the project directory must be in the virtual file system").isNotNull();

            CargoProjectsService service = CargoProjectsService.getInstance(project);
            service.attachCargoProjectAsync(directory.resolve("Cargo.toml"))
                .get(HeadlessProjects.TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CargoProject cargoProject = service.getAllProjects().iterator().next();

            // nothing carries the workspace yet, exactly as during project open
            assertThat(ReadAction.compute(cargoProject::getWorkspace))
                .as("there is nothing to restore from before the module model is loaded")
                .isNull();

            Module module = HeadlessModules.createModule(project, "early", root);
            writeWorkspace(module, directory);

            CargoWorkspace workspace = ReadAction.compute(cargoProject::getWorkspace);
            assertThat(workspace)
                .as("the earlier miss must not have been cached - the workspace is there now")
                .isNotNull();
            assertThat(workspace.getPackages())
                .as("and it must be the one the module carries")
                .anyMatch(pkg -> pkg.getName().equals("early"));
        }
        finally {
            projects.close(project);
        }
    }

    /** What a finished sync leaves behind, without going near Cargo. */
    private static void writeWorkspace(Module module, Path directory) {
        String rootUrl = "file://" + directory.toString().replace('\\', '/');

        CargoWorkspaceData.Target target = new CargoWorkspaceData.Target(
            rootUrl + "/src/main.rs", "early",
            CargoWorkspace.TargetKind.Bin.INSTANCE,
            CargoWorkspace.Edition.EDITION_2021, false, List.of());

        CargoWorkspaceData.Package pkg = new CargoWorkspaceData.Package(
            PACKAGE_ID, rootUrl, "early", "0.1.0", List.of(target), null,
            PackageOrigin.WORKSPACE, CargoWorkspace.Edition.EDITION_2021,
            Map.of(), Set.of(), CfgOptions.DEFAULT, Map.of(), null, null);

        CargoWorkspaceData data = new CargoWorkspaceData(List.of(pkg), Map.of(), Map.of(), rootUrl);

        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
            extension.setEnabled(true);
            extension.setCargoPackageId(PACKAGE_ID);
            extension.setCargoManifestPath(directory.resolve("Cargo.toml").toString());
            extension.setCargoWorkspaceState(CargoWorkspaceStates.toState(data, CfgOptions.DEFAULT));
            rootModel.commit();
        });
    }
}
