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
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.CustomOrderEntry;
import consulo.module.content.layer.orderEntry.ExportableOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.project.workspace.CargoLibrary;
import org.rust.cargo.project.workspace.orderEntry.CargoLibraryOrderEntryModel;
import org.rust.cargo.project.workspace.orderEntry.CargoLibraryOrderEntryType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Cargo order entry has to make its crate resolvable and keep the crate's tests out of the index.
 * <p>
 * The exclusion half is the reason the platform gained {@code CustomOrderEntryModel#getExcludedRoots}:
 * before it, only a {@code LibraryOrderEntry} could exclude, and a dependency's {@code tests/} would be
 * indexed along with everything else.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class CargoOrderEntryResolveTest {

    private static final String PACKAGE_ID = "dep 1.0.0 (registry+https://example)";

    @Test
    public void entryMakesItsCrateResolvableAndExcludesItsTests(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-rust-it-entry");

        // a dependency crate laid out the way cargo vendors one: a lib target plus a tests directory
        Path dep = directory.resolve("dep");
        Files.createDirectories(dep.resolve("src"));
        Files.createDirectories(dep.resolve("tests"));
        Files.writeString(dep.resolve("Cargo.toml"), "[package]\nname = \"dep\"\nversion = \"1.0.0\"\n");
        Files.writeString(dep.resolve("src/lib.rs"), "pub fn f() {}\n");
        Files.writeString(dep.resolve("tests/it.rs"), "#[test] fn t() {}\n");

        Path consumer = directory.resolve("consumer");
        Files.createDirectories(consumer.resolve("src"));
        Files.writeString(consumer.resolve("src/main.rs"), "fn main() {}\n");

        Project project = projects.open(directory);
        try {
            VirtualFile consumerRoot = refresh(consumer);
            VirtualFile depRoot = refresh(dep);
            VirtualFile depLib = refresh(dep.resolve("src/lib.rs"));
            VirtualFile depTestsDir = refresh(dep.resolve("tests"));
            VirtualFile depTest = refresh(dep.resolve("tests/it.rs"));
            assertThat(depLib).isNotNull();
            assertThat(depTest).isNotNull();

            Module module = HeadlessModules.createModule(project, "consumer", consumerRoot);
            addCargoEntry(module, depRoot, depTestsDir, directory.resolve("Cargo.toml").toString());

            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

            assertThat(ReadAction.compute(() -> fileIndex.isInLibrarySource(depLib)))
                .as("the crate's sources must be in the index, or nothing in it resolves")
                .isTrue();

            assertThat(ReadAction.compute(() -> fileIndex.isInLibrarySource(depTest)))
                .as("the crate's tests must be kept out of the library - this is what getExcludedRoots buys")
                .isFalse();

            OrderEntry found = ReadAction.compute(() -> {
                for (OrderEntry orderEntry : fileIndex.getOrderEntriesForFile(depLib)) {
                    if (orderEntry instanceof CustomOrderEntry<?> custom
                        && custom.getModel() instanceof CargoLibraryOrderEntryModel) {
                        return orderEntry;
                    }
                }
                return null;
            });
            assertThat(found)
                .as("the crate's files must be attributable back to the Cargo entry that owns them")
                .isNotNull();

            assertThat(found).isInstanceOf(ExportableOrderEntry.class);
            assertThat(((ExportableOrderEntry) found).isExported())
                .as("exported, so a workspace member depending on this module sees the crate too")
                .isTrue();
        }
        finally {
            projects.close(project);
        }
    }

    /**
     * The negative control: the identical fixture with no exclusion declared must put the tests in the
     * library. Without it the assertion above would pass even if exclusions did nothing at all.
     */
    @Test
    public void withoutAnExclusionTheTestsAreInTheLibrary(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-rust-it-entry-control");

        Path dep = directory.resolve("dep");
        Files.createDirectories(dep.resolve("src"));
        Files.createDirectories(dep.resolve("tests"));
        Files.writeString(dep.resolve("src/lib.rs"), "pub fn f() {}\n");
        Files.writeString(dep.resolve("tests/it.rs"), "#[test] fn t() {}\n");

        Path consumer = directory.resolve("consumer");
        Files.createDirectories(consumer.resolve("src"));
        Files.writeString(consumer.resolve("src/main.rs"), "fn main() {}\n");

        Project project = projects.open(directory);
        try {
            Module module = HeadlessModules.createModule(project, "consumer", refresh(consumer));
            VirtualFile depRoot = refresh(dep);
            VirtualFile depTest = refresh(dep.resolve("tests/it.rs"));

            // same entry, but nothing excluded
            addCargoEntry(module, depRoot, null, directory.resolve("Cargo.toml").toString());

            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
            assertThat(ReadAction.compute(() -> fileIndex.isInLibrarySource(depTest)))
                .as("with no exclusion the tests are part of the library, which is what the other test rules out")
                .isTrue();
        }
        finally {
            projects.close(project);
        }
    }

    private static void addCargoEntry(Module module, VirtualFile depRoot, VirtualFile depTests, String manifestPath) {
        Set<VirtualFile> sourceRoots = new LinkedHashSet<>();
        sourceRoots.add(depRoot);
        Set<VirtualFile> excludedRoots = new LinkedHashSet<>();
        if (depTests != null) {
            excludedRoots.add(depTests);
        }

        CargoLibrary library = new CargoLibrary(
            CargoLibrary.Kind.DEPENDENCY, PACKAGE_ID, "dep", "1.0.0", sourceRoots, excludedRoots);

        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            CustomOrderEntry<CargoLibraryOrderEntryModel> entry = rootModel.addCustomOderEntry(
                CargoLibraryOrderEntryType.getInstance(),
                new CargoLibraryOrderEntryModel(library, manifestPath));
            if (entry instanceof ExportableOrderEntry exportable) {
                exportable.setExported(true);
            }
            rootModel.commit();
        });
    }

    private static VirtualFile refresh(Path path) {
        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
    }
}
