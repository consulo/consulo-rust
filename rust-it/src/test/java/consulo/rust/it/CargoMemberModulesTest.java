/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.project.workspace.CargoWorkspaceFactory;
import org.rust.cargo.project.workspace.orderEntry.CargoWorkspaceModules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every workspace member gets a module, so that {@code getModuleForFile} can say which member a file
 * belongs to. The member sitting on the owner's content root keeps the owner's module, because two
 * modules may not share a content root.
 * <p>
 * The disposal half is the dangerous one: a module may only be removed when this plugin created it for
 * a member that has since left the workspace. Anything the user made must survive.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class CargoMemberModulesTest {

    @Test
    public void createsAModulePerMemberAndDisposesOnlyItsOwn(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-rust-it-members");
        writeWorkspaceTree(directory);

        Project project = projects.open(directory);
        try {
            VirtualFile root = refresh(directory);
            Module owner = HeadlessModules.createModule(project, "ws", root);
            Module userModule = HeadlessModules.createModule(project, "hand-made", refresh(directory.resolve("unrelated")));

            ModuleManager moduleManager = ModuleManager.getInstance(project);
            Path manifest = directory.resolve("Cargo.toml");

            CargoWorkspace workspace = workspaceOf(directory, manifest, List.of("root-pkg", "alpha", "beta"));
            Map<Module, List<CargoWorkspaceModules.Member>> byOwner =
                Map.of(owner, membersOf(workspace, directory, manifest));

            Map<String, Module> modules = WriteAction.compute(
                () -> CargoWorkspaceModules.reconcile(project, moduleManager, byOwner));

            assertThat(modules.keySet())
                .as("every member must end up with a module")
                .hasSize(3);

            Module rootModule = modules.get(packageId("root-pkg"));
            assertThat(rootModule)
                .as("the member on the owner's content root must reuse the owner module")
                .isSameAs(owner);

            Module alpha = modules.get(packageId("alpha"));
            assertThat(alpha).as("a nested member must get a module of its own").isNotNull();
            assertThat(alpha).isNotSameAs(owner);

            // configure marks the module as ours, which is what makes disposal safe
            configure(alpha, workspace, directory, manifest, "alpha", owner);

            assertThat(ReadAction.compute(() -> RustModuleExtension.findExtension(alpha).getCargoPackageId()))
                .as("the module must carry the package id that marks it as created from a member")
                .isEqualTo(packageId("alpha"));

            // alpha leaves the workspace; beta stays
            CargoWorkspace smaller = workspaceOf(directory, manifest, List.of("root-pkg", "beta"));
            Map<Module, List<CargoWorkspaceModules.Member>> shrunk =
                Map.of(owner, membersOf(smaller, directory, manifest));

            WriteAction.run(() -> CargoWorkspaceModules.reconcile(project, moduleManager, shrunk));

            List<String> names = new ArrayList<>();
            for (Module module : moduleManager.getModules()) {
                names.add(module.getName());
            }

            assertThat(names)
                .as("the module of a member that left must be disposed")
                .doesNotContain("alpha");
            assertThat(names)
                .as("a module the user made must never be disposed")
                .contains("hand-made");
            assertThat(names)
                .as("the owner module must survive, it owns the manifest")
                .contains("ws");
        }
        finally {
            projects.close(project);
        }
    }

    private static void configure(Module module, CargoWorkspace workspace, Path directory, Path manifest,
                                  String name, Module owner) {
        CargoWorkspaceModules.Member member = member(workspace, directory, manifest, name);
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            CargoWorkspaceModules.configure(rootModel, member, owner);
            rootModel.commit();
        });
    }

    private static List<CargoWorkspaceModules.Member> membersOf(CargoWorkspace workspace, Path directory, Path manifest) {
        List<CargoWorkspaceModules.Member> members = new ArrayList<>();
        for (CargoWorkspace.Package pkg : workspace.getPackages()) {
            VirtualFile contentRoot = pkg.getContentRoot();
            if (contentRoot != null) {
                members.add(new CargoWorkspaceModules.Member(pkg, contentRoot, manifest.toString()));
            }
        }
        return members;
    }

    private static CargoWorkspaceModules.Member member(CargoWorkspace workspace, Path directory, Path manifest, String name) {
        for (CargoWorkspaceModules.Member member : membersOf(workspace, directory, manifest)) {
            if (member.pkg().getName().equals(name)) return member;
        }
        throw new IllegalStateException("no member " + name);
    }

    private static CargoWorkspace workspaceOf(Path directory, Path manifest, List<String> names) {
        List<CargoWorkspaceData.Package> packages = new ArrayList<>();
        for (String name : names) {
            Path dir = name.equals("root-pkg") ? directory : directory.resolve("crates/" + name);
            String url = "file://" + dir.toString().replace('\\', '/');
            CargoWorkspaceData.Target target = new CargoWorkspaceData.Target(
                url + "/src/lib.rs", name,
                new CargoWorkspace.TargetKind.Lib(EnumSet.of(CargoWorkspace.LibKind.LIB)),
                CargoWorkspace.Edition.EDITION_2021, false, List.of());
            packages.add(new CargoWorkspaceData.Package(
                packageId(name), url, name, "0.1.0", List.of(target), null,
                PackageOrigin.WORKSPACE, CargoWorkspace.Edition.EDITION_2021,
                Map.of(), Set.of(), CfgOptions.DEFAULT, Map.of(), null, null));
        }
        CargoWorkspaceData data = new CargoWorkspaceData(
            packages, new LinkedHashMap<>(), new LinkedHashMap<>(),
            "file://" + directory.toString().replace('\\', '/'));
        return CargoWorkspaceFactory.deserialize(manifest, data);
    }

    private static String packageId(String name) {
        return name + " 0.1.0 (path+file:///" + name + ")";
    }

    private static void writeWorkspaceTree(Path directory) throws Exception {
        Files.writeString(directory.resolve("Cargo.toml"),
            "[workspace]\nmembers = [\"crates/alpha\", \"crates/beta\"]\n");
        Files.createDirectories(directory.resolve("src"));
        Files.writeString(directory.resolve("src/lib.rs"), "pub fn r() {}\n");
        for (String crate : new String[]{"alpha", "beta"}) {
            Files.createDirectories(directory.resolve("crates/" + crate + "/src"));
            Files.writeString(directory.resolve("crates/" + crate + "/src/lib.rs"), "pub fn f() {}\n");
        }
        Files.createDirectories(directory.resolve("unrelated"));
    }

    private static VirtualFile refresh(Path path) {
        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
    }
}
