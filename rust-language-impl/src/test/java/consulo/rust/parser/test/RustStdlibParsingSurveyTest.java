package consulo.rust.parser.test;

import consulo.application.ReadAction;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.test.junit.impl.language.SimpleParsingTest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.rust.lang.RsFileType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Parses every {@code .rs} file under {@code -Drust.parse.survey=<dir>} (for example the standard library of a
 * toolchain with rust-src, {@code <sysroot>/lib/rustlib/src/rust/library}) and writes each parse error with the source
 * line it is on to {@code target/parse-survey.txt}. Real, compiling code has no syntax errors, so every entry is
 * syntax the parser does not know yet or gets wrong. {@code -Drust.parse.survey.tree=true} adds the PSI tree of
 * each such file, {@code =all} the tree of every file. Skipped unless the directory is set.
 */
public class RustStdlibParsingSurveyTest extends SimpleParsingTest<Object> {
    public RustStdlibParsingSurveyTest() {
        super("parsing/complete", "rs");
    }

    @Test
    public void survey(Context context) throws Exception {
        String root = System.getProperty("rust.parse.survey");
        Assumptions.assumeTrue(root != null && !root.isBlank(), "-Drust.parse.survey=<dir> is not set");

        List<Path> files;
        try (Stream<Path> walk = Files.walk(Path.of(root))) {
            files = walk.filter(p -> p.toString().endsWith(".rs")).sorted().toList();
        }

        List<String> report = new ArrayList<>();
        int filesWithErrors = 0;
        for (Path path : files) {
            String text = Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
            List<String> errors = ReadAction.compute(() -> {
                PsiFile file = createFile(context, null, path.getFileName().toString(), RsFileType.INSTANCE, text);
                List<String> result = new ArrayList<>();
                for (PsiErrorElement error : PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class)) {
                    int offset = error.getTextOffset();
                    int lineStart = text.lastIndexOf('\n', Math.max(0, offset - 1)) + 1;
                    int lineEnd = text.indexOf('\n', offset);
                    int line = (int) text.substring(0, offset).chars().filter(c -> c == '\n').count() + 1;
                    String source = text.substring(lineStart, lineEnd < 0 ? text.length() : lineEnd).strip();
                    result.add(line + ": " + error.getErrorDescription() + "    | " + source);
                }
                return result;
            });
            boolean dumpAll = "all".equals(System.getProperty("rust.parse.survey.tree"));
            if (!errors.isEmpty() || dumpAll) {
                if (!errors.isEmpty()) filesWithErrors++;
                report.add(Path.of(root).relativize(path).toString());
                errors.forEach(e -> report.add("  " + e));
                if (dumpAll || Boolean.getBoolean("rust.parse.survey.tree")) {
                    String tree = ReadAction.compute(() -> toParseTreeText(
                        createFile(context, null, path.getFileName().toString(), RsFileType.INSTANCE, text), false, false));
                    report.add(tree);
                }
            }
        }
        report.add(0, files.size() + " files, " + filesWithErrors + " with parse errors");
        Files.write(Path.of("target/parse-survey.txt"), report, StandardCharsets.UTF_8);
    }

    @Override
    protected LanguageFileType getFileType(Context context, Object o) {
        return RsFileType.INSTANCE;
    }
}
