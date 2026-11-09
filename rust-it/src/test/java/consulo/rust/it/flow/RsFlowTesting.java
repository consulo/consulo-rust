/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.flow;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkTable;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.rust.bundle.RustBundleType;
import consulo.rust.it.CargoProxyToolchain;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixtures of the flow tests, each in a fresh project: a {@code .rs} file of a test resource directory opened either as a
 * detached file (its own crate, no Cargo project, no standard library) or as the library of a Cargo package with a mock
 * standard library.
 */
public final class RsFlowTesting {
    private RsFlowTesting() {
    }

    public static Path resource(String path) throws Exception {
        URL url = RsFlowTesting.class.getClassLoader().getResource(path);
        assertThat(url).as(path).isNotNull();
        return Path.of(url.toURI());
    }

    public static Stream<String> sources(String directory) throws Exception {
        try (Stream<Path> list = Files.list(resource(directory))) {
            return list.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".rs")).sorted().toList().stream();
        }
    }

    public static String read(String directory, String fileName) throws Exception {
        return Files.readString(resource(directory + "/" + fileName), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    /**
     * A project with one module whose content root holds {@code src/<fileName>}, indexed, with no parse errors in it.
     */
    public static PsiFile openSource(HeadlessProjects projects, String fileName, String text) throws Exception {
        Path directory = projects.newDirectory();
        Path srcDirectory = Files.createDirectories(directory.resolve("src"));
        Files.writeString(srcDirectory.resolve(fileName), text, StandardCharsets.UTF_8);

        Project project = projects.open(directory);

        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(root).isNotNull();
        Module module = HeadlessModules.createModule(project, "main", root);
        VirtualFile srcRoot = root.findChild("src");
        assertThat(srcRoot).isNotNull();
        HeadlessModules.addSourceRoot(module, srcRoot);

        HeadlessProjects.awaitIdle(project);

        VirtualFile file = srcRoot.findChild(fileName);
        assertThat(file).isNotNull();
        PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(file));
        assertThat(psiFile).isNotNull();

        List<String> errors = ReadAction.compute(() -> {
            List<String> result = new ArrayList<>();
            for (PsiErrorElement error : PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement.class)) {
                result.add(error.getErrorDescription() + "@" + error.getTextOffset());
            }
            return result;
        });
        assertThat(errors).as("parse errors").isEmpty();
        return psiFile;
    }

    /**
     * A Cargo project of one package whose {@code src/lib.rs} is {@code text}, synced against a stub toolchain whose
     * standard library is {@code mockstd/} (the lang items and the prelude, nothing else), indexed. Close it to drop the
     * toolchain bundle.
     */
    public static CargoSource openCargoSource(HeadlessProjects projects, String text) throws Exception {
        CargoSource source = openCargoSource(projects, text, false);
        assertThat(ReadAction.compute(() -> PsiTreeUtil.findChildrenOfType(source.file, PsiErrorElement.class))).as("parse errors").isEmpty();
        return source;
    }

    /** The same, for a {@code text} that may not parse. */
    public static CargoSource openCargoSource(HeadlessProjects projects, String text, boolean parseErrors) throws Exception {
        Path directory = CargoProxyToolchain.writeProject("consulo-rust-it-flow");
        Files.writeString(directory.resolve("src/lib.rs"), text, StandardCharsets.UTF_8);
        Path home = CargoProxyToolchain.writeToolchain(directory, Map.of(
            "core", read("mockstd/core", "lib.rs"),
            "std", read("mockstd/std", "lib.rs")
        ));
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(home);

        Project project = projects.open(directory);
        SdkTable sdkTable = SdkTable.getInstance();
        Sdk sdk = WriteAction.compute(() -> {
            Sdk created = sdkTable.createSdk("flow-rust-" + home.getFileName(), RustBundleType.getInstance());
            SdkModificator modificator = created.getSdkModificator();
            modificator.setHomePath(home.toString());
            modificator.commitChanges();
            sdkTable.addSdk(created);
            return created;
        });
        CargoSource source = new CargoSource(sdk);

        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(root).isNotNull();
        Module module = HeadlessModules.createModule(project, "flow", root);
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
            extension.setEnabled(true);
            extension.getInheritableSdk().set(null, sdk);
            rootModel.commit();
        });

        CargoProjectsService service = CargoProjectsService.getInstance(project);
        List<CargoProject> attached = List.copyOf(service.attachCargoProjectAsync(directory.resolve("Cargo.toml"))
            .thenCompose(ignored -> service.refreshAllProjects())
            .get(120, TimeUnit.SECONDS));
        assertThat(attached).as("the sync must produce a Cargo project").isNotEmpty();
        HeadlessProjects.awaitIdle(project);

        VirtualFile file = root.findFileByRelativePath("src/lib.rs");
        assertThat(file).isNotNull();
        source.file = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(file));
        assertThat(source.file).isNotNull();
        return source;
    }

    /** A source of {@link #openCargoSource}: closing it removes its toolchain bundle. */
    public static final class CargoSource implements AutoCloseable {
        private final Sdk sdk;
        private PsiFile file;

        private CargoSource(Sdk sdk) {
            this.sdk = sdk;
        }

        public PsiFile getFile() {
            return file;
        }

        @Override
        public void close() {
            WriteAction.run(() -> SdkTable.getInstance().removeSdk(sdk));
        }
    }

    /**
     * Compares {@code actual} with {@code <directory>/<expectedName>}; a missing expectation is written to
     * {@code src/test/resources/<directory>} and the test fails.
     */
    public static void assertGolden(String directory, String expectedName, String actual) throws Exception {
        URL expectedUrl = RsFlowTesting.class.getClassLoader().getResource(directory + "/" + expectedName);
        if (expectedUrl == null) {
            Path path = Path.of("src/test/resources", directory, expectedName);
            Files.writeString(path, actual, StandardCharsets.UTF_8);
            throw new AssertionError("written " + path.toAbsolutePath());
        }
        String expected = Files.readString(Path.of(expectedUrl.toURI()), StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertThat(actual).isEqualTo(expected);
    }
}
