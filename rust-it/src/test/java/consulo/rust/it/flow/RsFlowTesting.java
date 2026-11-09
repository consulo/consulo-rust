/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.flow;

import consulo.application.ReadAction;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixtures of the flow tests: a {@code .rs} file of a test resource directory opened as a detached file (its own
 * crate, no Cargo project, no standard library) in a fresh project.
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
