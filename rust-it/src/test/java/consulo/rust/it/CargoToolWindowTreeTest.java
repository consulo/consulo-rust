/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.AllowLogError;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.it.TreeTester;
import consulo.ui.internal.UIThreadTreeExecutor;
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
import org.rust.cargo.project.toolwindow.CargoTreeModel;
import org.rust.cargo.project.toolwindow.CargoTreeNode;
import org.rust.cargo.project.workspace.state.CargoWorkspaceStates;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the user actually sees: the Cargo tool window shows the project and its targets, built from a
 * workspace that was restored from the project model rather than resolved by Cargo.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class CargoToolWindowTreeTest {

    private static final String PACKAGE_ID = "treed 0.1.0 (path+file:///treed)";

    @Test
    @AllowLogError("consulo.component.impl.internal.messagebus.MessageBusConnectionImpl")
    public void treeShowsTheRestoredProjectAndItsTargets(Application application, HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-rust-it-tree");
        Files.writeString(directory.resolve("Cargo.toml"),
            "[package]\nname = \"treed\"\nversion = \"0.1.0\"\nedition = \"2021\"\n");
        Files.createDirectories(directory.resolve("src"));
        Files.writeString(directory.resolve("src/lib.rs"), "pub fn f() {}\n");

        Project project = projects.open(directory);
        Disposable disposable = Disposable.newDisposable();
        try {
            VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
            Module module = HeadlessModules.createModule(project, "treed", root);
            Path manifest = directory.resolve("Cargo.toml");
            writeWorkspaceOnModule(module, directory, manifest);

            CargoProjectsServiceImpl service = (CargoProjectsServiceImpl) CargoProjectsService.getInstance(project);
            Element state = new Element("state");
            state.addContent(new Element("cargoProject").setAttribute("FILE", manifest.toString().replace('\\', '/')));
            service.loadState(state);

            List<CargoProject> cargoProjects = List.copyOf(service.getAllProjects());
            assertThat(cargoProjects).hasSize(1);
            assertThat(ReadAction.compute(() -> cargoProjects.get(0).getWorkspace()))
                .as("the tree must be built from a workspace restored without Cargo")
                .isNotNull();

            CargoTreeModel model = new CargoTreeModel();
            model.setCargoProjects(cargoProjects);

            // show() is bind().settle(), so the executor has to actually run the build - a manual one
            // never reaches idle
            TreeTester<CargoTreeNode> tester =
                TreeTester.create(null, model, UIThreadTreeExecutor.INSTANCE);
            Disposer.register(disposable, tester.getTree().destroyHook());
            tester.show();

            CargoTreeNode.ProjectNode projectNode = new CargoTreeNode.ProjectNode(cargoProjects.get(0));
            assertThat(tester.node(projectNode))
                .as("the tool window must show the restored Cargo project")
                .isNotNull();
        }
        finally {
            Disposer.dispose(disposable);
            projects.close(project);
        }
    }

    private static void writeWorkspaceOnModule(Module module, Path directory, Path manifest) {
        String url = "file://" + directory.toString().replace('\\', '/');

        CargoWorkspaceData.Target target = new CargoWorkspaceData.Target(
            url + "/src/lib.rs", "treed",
            new CargoWorkspace.TargetKind.Lib(EnumSet.of(CargoWorkspace.LibKind.LIB)),
            CargoWorkspace.Edition.EDITION_2021, false, List.of());

        CargoWorkspaceData.Package pkg = new CargoWorkspaceData.Package(
            PACKAGE_ID, url, "treed", "0.1.0", List.of(target), null,
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
