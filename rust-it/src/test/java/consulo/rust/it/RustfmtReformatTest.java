/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.it.AllowWriteLockUnderUIThread;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.it.HeadlessUIThread;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.codeStyle.service.AsyncDocumentFormattingService;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.rust.it.flow.RsFlowTesting;
import consulo.undoRedo.CommandProcessor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reformat Code of a Rust file goes through rustfmt when the project uses it, and through the built-in formatter
 * otherwise. Needs a real rustfmt - {@code -Drust.it.rustfmt=<executable>}, or the first one of the rustup toolchains -
 * linked into the stub toolchain of the project; skipped without one. rustfmt sorts the imports, the built-in formatter
 * leaves them as they are: that tells which one formatted the file.
 */
@ExtendWith(HeadlessProjectExtension.class)
@AllowWriteLockUnderUIThread
public class RustfmtReformatTest {
    private static final String TEXT = "use std::fmt;\nuse std::cmp;\nfn main(){let x=1;}\n";

    @Test
    public void reformatCodeRunsRustfmt(HeadlessProjects projects) throws Exception {
        assertThat(reformat(projects, true, FormatterUtil.REFORMAT_COMMAND_NAME))
            .isEqualTo("use std::cmp;\nuse std::fmt;\nfn main() {\n    let x = 1;\n}\n");
    }

    @Test
    public void builtInFormatterWhenTheProjectDoesNotUseRustfmt(HeadlessProjects projects) throws Exception {
        assertBuiltInFormatted(reformat(projects, false, FormatterUtil.REFORMAT_COMMAND_NAME));
    }

    @Test
    public void builtInFormatterForAFormattingThatIsNotReformatCode(HeadlessProjects projects) throws Exception {
        assertBuiltInFormatted(reformat(projects, true, LocalizeValue.of("Some Refactoring")));
    }

    @Test
    public void codeRustfmtCannotParseIsLeftAsItIs(HeadlessProjects projects) throws Exception {
        String broken = "fn main(){let x=;}\n";
        assertThat(reformat(projects, broken, true, FormatterUtil.REFORMAT_COMMAND_NAME)).isEqualTo(broken);
    }

    /** Formatted, but the imports in the order they were written. */
    private static void assertBuiltInFormatted(String text) {
        assertThat(text).isNotEqualTo(TEXT).startsWith("use std::fmt;\nuse std::cmp;\n");
    }

    private static String reformat(HeadlessProjects projects, boolean useRustfmt, LocalizeValue commandName) throws Exception {
        return reformat(projects, TEXT, useRustfmt, commandName);
    }

    /** {@code text} reformatted whole in a command named {@code commandName}. */
    private static String reformat(HeadlessProjects projects, String text, boolean useRustfmt, LocalizeValue commandName) throws Exception {
        Path rustfmt = findRustfmt();
        Assumptions.assumeTrue(rustfmt != null, "no rustfmt: set -Drust.it.rustfmt or install a rustup toolchain");
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text, true)) {
            Files.createSymbolicLink(source.getToolchainHome().resolve("bin/rustfmt"), rustfmt);
            PsiFile file = source.getFile();
            Project project = file.getProject();
            // formatted in the background otherwise, and the text read before rustfmt is done
            Document document = ReadAction.compute(() -> PsiDocumentManager.getInstance(project).getDocument(file));
            document.putUserData(AsyncDocumentFormattingService.FORMAT_DOCUMENT_SYNCHRONOUSLY, true);
            Disposable settings = Disposable.newDisposable("rustfmt settings");
            try {
                RsProjectSettingsServiceUtil.getRustfmtSettings(project).modifyTemporary(settings, state -> state.useRustfmt = useRustfmt);
                HeadlessUIThread.run(() -> CommandProcessor.getInstance().newCommand()
                    .project(project)
                    .name(commandName)
                    .run(() -> WriteAction.run(() -> CodeStyleManager.getInstance(project).reformat(file))));
            }
            finally {
                Disposer.dispose(settings);
            }
            return ReadAction.compute(document::getText);
        }
    }

    private static Path findRustfmt() throws IOException {
        String property = System.getProperty("rust.it.rustfmt");
        if (property != null) return Files.isExecutable(Path.of(property)) ? Path.of(property) : null;
        Path toolchains = Path.of(System.getProperty("user.home"), ".rustup", "toolchains");
        if (!Files.isDirectory(toolchains)) return null;
        try (Stream<Path> list = Files.list(toolchains)) {
            // the stable toolchains first: the expected text is what a stable rustfmt writes
            return list.sorted((a, b) -> Boolean.compare(!a.getFileName().toString().startsWith("stable"), !b.getFileName().toString().startsWith("stable")))
                .map(toolchain -> toolchain.resolve("bin/rustfmt"))
                .filter(Files::isExecutable)
                .findFirst()
                .orElse(null);
        }
    }
}
