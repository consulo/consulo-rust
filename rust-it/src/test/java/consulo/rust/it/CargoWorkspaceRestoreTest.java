/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.Application;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.project.workspace.state.CargoWorkspaceState;
import org.rust.cargo.project.workspace.state.CargoWorkspaceStates;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The point of persisting the resolved workspace into the project model: a project that is closed and
 * reopened gets its Cargo workspace back without anyone running Cargo.
 * <p>
 * Cargo is never invoked here - the workspace is written straight onto the module extension, which is
 * what a finished sync does - so a restored workspace can only have come from the project model.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class CargoWorkspaceRestoreTest {

    private static final String PACKAGE_ID = "restored 0.1.0 (path+file:///restored)";

    @Test
    public void workspaceSurvivesCloseAndReopen(Application application, HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-rust-it-restore");
        Files.writeString(directory.resolve("Cargo.toml"),
            "[package]\nname = \"restored\"\nversion = \"0.1.0\"\nedition = \"2021\"\n");
        Files.createDirectories(directory.resolve("src"));
        Files.writeString(directory.resolve("src/main.rs"), "fn main() {}\n");

        Project first = projects.open(directory);
        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(root).as("the project directory must be in the virtual file system").isNotNull();

        Module module = HeadlessModules.createModule(first, "restored", root);
        writeWorkspace(module, directory);

        RustModuleExtension written = RustModuleExtension.findExtension(module);
        assertThat(written).as("the module must carry a Rust extension after the write").isNotNull();
        assertThat(written.getCargoWorkspaceState())
            .as("the sync must leave the resolved workspace on the module")
            .isNotNull();

        saveProject(first, application);
        projects.close(first);

        Project second = projects.open(directory);
        try {
            Module[] modules = consulo.module.ModuleManager.getInstance(second).getModules();
            assertThat(modules)
                .as("the reopened project must load the module of the first session")
                .isNotEmpty();

            RustModuleExtension restored = null;
            for (Module reopened : modules) {
                RustModuleExtension extension = RustModuleExtension.findExtension(reopened);
                if (extension != null && extension.getCargoWorkspaceState() != null) {
                    restored = extension;
                }
            }
            assertThat(restored)
                .as("the workspace written before the close must be readable after the reopen")
                .isNotNull();

            assertThat(restored.getCargoManifestPath())
                .as("the manifest is what ties the restored workspace back to its Cargo project")
                .isEqualTo(directory.resolve("Cargo.toml").toString());

            CargoWorkspaceData data = CargoWorkspaceStates.fromState(restored.getCargoWorkspaceState());
            assertThat(data.getPackages())
                .as("the restored workspace must still describe its package")
                .hasSize(1);

            CargoWorkspaceData.Package pkg = data.getPackages().get(0);
            assertThat(pkg.getId()).isEqualTo(PACKAGE_ID);
            assertThat(pkg.getName()).isEqualTo("restored");
            assertThat(pkg.getOrigin()).isEqualTo(PackageOrigin.WORKSPACE);
            assertThat(pkg.getTargets()).hasSize(1);
            assertThat(pkg.getTargets().iterator().next().getKind().isLib()).isTrue();
        }
        finally {
            projects.close(second);
        }
    }

    /** What a finished sync leaves behind, without going near Cargo. */
    private static void writeWorkspace(Module module, Path directory) {
        CargoWorkspaceData.Target target = new CargoWorkspaceData.Target(
            directoryUrl(directory) + "/src/main.rs", "restored",
            new CargoWorkspace.TargetKind.Lib(EnumSet.of(CargoWorkspace.LibKind.LIB)),
            CargoWorkspace.Edition.EDITION_2021, false, List.of());

        CargoWorkspaceData.Package pkg = new CargoWorkspaceData.Package(
            PACKAGE_ID, directoryUrl(directory), "restored", "0.1.0", List.of(target), null,
            PackageOrigin.WORKSPACE, CargoWorkspace.Edition.EDITION_2021,
            Map.of(), Set.of(), CfgOptions.DEFAULT, Map.of(), null, null);

        CargoWorkspaceData data = new CargoWorkspaceData(
            List.of(pkg), Map.of(), Map.of(), directoryUrl(directory));
        CargoWorkspaceState state = CargoWorkspaceStates.toState(data, CfgOptions.DEFAULT);

        consulo.application.WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
            extension.setEnabled(true);
            extension.setCargoPackageId(PACKAGE_ID);
            extension.setCargoManifestPath(directory.resolve("Cargo.toml").toString());
            extension.setCargoWorkspaceState(state);
            rootModel.commit();
        });
    }

    private static String directoryUrl(Path directory) {
        return "file://" + directory.toString().replace('\\', '/');
    }

    /** @see consulo.it.index.ScanningTestSupport in the platform, which this mirrors. */
    private static void saveProject(Project project, Application application) throws Exception {
        project.saveAsync(application.getLastUIAccess())
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture()
            .get(HeadlessProjects.TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
