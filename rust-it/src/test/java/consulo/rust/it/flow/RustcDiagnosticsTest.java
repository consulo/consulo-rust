/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.flow;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The markers of every {@code diagnostics/*.rs} against what {@code rustc} on the {@code PATH} reports for it, compiled
 * as a 2021 edition library; skipped without a {@code rustc}. Only the codes of {@link DiagnosticMarkers#CODES} are
 * compared, and the fixture must have no other errors.
 */
public class RustcDiagnosticsTest {
    private static final String DIRECTORY = "diagnostics";

    static Stream<String> sources() throws Exception {
        return RsFlowTesting.sources(DIRECTORY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sources")
    public void check(String fileName) throws Exception {
        String rustc = findRustc();
        Assumptions.assumeTrue(rustc != null, "no rustc on the PATH");

        Path source = RsFlowTesting.resource(DIRECTORY + "/" + fileName);
        Path output = Files.createTempDirectory("consulo-rust-it-rustc");
        Process process = new ProcessBuilder(rustc, "--edition", "2021", "--crate-type", "lib", "--error-format=json",
            "--emit=metadata", "--out-dir", output.toString(), source.toString())
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor(60, TimeUnit.SECONDS)).isTrue();

        String text = Files.readString(source, StandardCharsets.UTF_8);
        List<String> reported = new ArrayList<>();
        List<String> otherErrors = new ArrayList<>();
        for (String line : stderr.split("\n")) {
            if (!line.startsWith("{")) {
                continue;
            }
            JsonObject diagnostic = JsonParser.parseString(line).getAsJsonObject();
            JsonElement codeElement = diagnostic.get("code");
            String code = codeElement == null || codeElement.isJsonNull() ? null : codeElement.getAsJsonObject().get("code").getAsString();
            JsonObject primary = null;
            for (JsonElement span : diagnostic.getAsJsonArray("spans")) {
                if (span.getAsJsonObject().get("is_primary").getAsBoolean()) {
                    primary = span.getAsJsonObject();
                    break;
                }
            }
            if (code != null && DiagnosticMarkers.CODES.contains(code) && primary != null) {
                reported.add(code + "@" + primary.get("line_start").getAsInt() + ":" + primary.get("column_start").getAsInt());
            }
            else if ("error".equals(diagnostic.get("level").getAsString()) && !diagnostic.get("message").getAsString().startsWith("aborting")) {
                otherErrors.add(diagnostic.get("message").getAsString());
            }
        }
        reported.sort(null);

        assertThat(otherErrors).as("other errors").isEmpty();
        assertThat(reported).isEqualTo(DiagnosticMarkers.expectedByRustc(text));
    }

    private static String findRustc() {
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        for (String directory : path.split(File.pathSeparator)) {
            Path candidate = Path.of(directory, "rustc");
            if (Files.isExecutable(candidate)) {
                return candidate.toString();
            }
        }
        return null;
    }
}
