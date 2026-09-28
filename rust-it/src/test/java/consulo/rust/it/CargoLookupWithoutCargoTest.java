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
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.project.model.impl.CargoProjectsServiceImpl;
import org.rust.cargo.project.workspace.state.CargoWorkspaceStates;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The goal of the whole exercise: a project restored from the model answers "which Cargo project and
 * package owns this file" without Cargo ever running.
 * <p>
 * The setup is what a reopen looks like - the service restores its project list from the manifest path
 * it persisted, and the module carries the workspace the last sync wrote. Nothing here invokes Cargo,
 * and no toolchain is configured, so a correct answer can only come from the project model. This is
 * what {@code LightDirectoryIndex} used to answer and {@code ProjectFileIndex} answers now.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class CargoLookupWithoutCargoTest {

    private static final String PACKAGE_ID = "lookup 0.1.0 (path+file:///lookup)";

    /**
     * The allowed category is a status-bar widget reacting to the projects-changed topic: its UI class
     * is not on a headless classpath, which has nothing to do with the lookup under test. Scoped to
     * that one logger, so any other logged error still fails.
     */
    @Test
    @AllowLogError("consulo.component.impl.internal.messagebus.MessageBusConnectionImpl")
    public void findsProjectAndPackageWithoutRunningCargo(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-rust-it-lookup");
        Files.writeString(directory.resolve("Cargo.toml"),
            "[package]\nname = \"lookup\"\nversion = \"0.1.0\"\nedition = \"2021\"\n");
        Files.createDirectories(directory.resolve("src"));
        Path mainRs = directory.resolve("src/main.rs");
        Files.writeString(mainRs, "fn main() {}\n");

        Project project = projects.open(directory);
        try {
            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
            assertThat(root).isNotNull();

            Module module = HeadlessModules.createModule(project, "lookup", root);
            Path manifest = directory.resolve("Cargo.toml");
            writeWorkspaceOnModule(module, directory, manifest);

            // what loadState does on a reopen: the project list comes back from the manifest alone
            CargoProjectsServiceImpl service = (CargoProjectsServiceImpl) CargoProjectsService.getInstance(project);
            Element state = new Element("state");
            state.addContent(new Element("cargoProject")
                .setAttribute("FILE", manifest.toString().replace('\\', '/')));
            service.loadState(state);

            assertThat(service.getAllProjects())
                .as("the restored state must register the Cargo project")
                .hasSize(1);

            CargoProject restored = service.getAllProjects().iterator().next();
            assertThat(ReadAction.compute(restored::getWorkspace))
                .as("the workspace must be rebuilt from the module, with no Cargo run")
                .isNotNull();

            VirtualFile mainFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(mainRs);
            assertThat(mainFile).isNotNull();

            assertThat(ReadAction.compute(() -> service.findProjectForFile(mainFile)))
                .as("findProjectForFile must answer from the project model")
                .isNotNull();

            CargoWorkspace.Package pkg = ReadAction.compute(() -> service.findPackageForFile(mainFile));
            assertThat(pkg)
                .as("findPackageForFile must answer from the project model")
                .isNotNull();
            assertThat(pkg.getId()).isEqualTo(PACKAGE_ID);
            assertThat(pkg.getName()).isEqualTo("lookup");
        }
        finally {
            projects.close(project);
        }
    }

    private static void writeWorkspaceOnModule(Module module, Path directory, Path manifest) {
        String url = "file://" + directory.toString().replace('\\', '/');

        CargoWorkspaceData.Target target = new CargoWorkspaceData.Target(
            url + "/src/main.rs", "lookup",
            new CargoWorkspace.TargetKind.Lib(EnumSet.of(CargoWorkspace.LibKind.LIB)),
            CargoWorkspace.Edition.EDITION_2021, false, List.of());

        CargoWorkspaceData.Package pkg = new CargoWorkspaceData.Package(
            PACKAGE_ID, url, "lookup", "0.1.0", List.of(target), null,
            PackageOrigin.WORKSPACE, CargoWorkspace.Edition.EDITION_2021,
            Map.of(), Set.of(), CfgOptions.DEFAULT, Map.of(), null, null);

        CargoWorkspaceData data = new CargoWorkspaceData(List.of(pkg), Map.of(), Map.of(), url);

        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
            extension.setEnabled(true);
            extension.setCargoPackageId(PACKAGE_ID);
            extension.setCargoManifestPath(manifest.toString());
            extension.setCargoWorkspaceState(CargoWorkspaceStates.toState(data, CfgOptions.DEFAULT));
            rootModel.commit();
        });
    }
}
