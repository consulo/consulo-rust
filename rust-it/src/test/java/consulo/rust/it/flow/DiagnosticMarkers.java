/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The marker comments of a {@code diagnostics/*.rs} fixture. A marker names the diagnostic reported at the code right
 * after it, by its rustc code:
 * <ul>
 * <li>{@code /*E0382*&#47;}, {@code /*unused_variables*&#47;} - rustc and the IDE both report it there</li>
 * <li>{@code /*missing:CODE*&#47;} - rustc reports it, the IDE does not (a known gap)</li>
 * <li>{@code /*false:CODE*&#47;} - the IDE reports it, rustc does not (a known false positive)</li>
 * </ul>
 * Only the codes in {@link #CODES} are compared; everything else either side reports is ignored.
 */
public final class DiagnosticMarkers {
    public static final List<String> CODES = List.of("unreachable_code", "unused_variables", "unused_assignments", "E0381", "E0382");

    private static final Pattern MARKER = Pattern.compile("/\\*(?:(missing|false):)?([a-z_]+|E\\d{4})\\*/");

    private DiagnosticMarkers() {
    }

    /** {@code CODE@line:column} of the code after the markers rustc must report */
    public static List<String> expectedByRustc(String source) {
        return collect(source, false);
    }

    /** {@code CODE@line:column} of the code after the markers the IDE must report */
    public static List<String> expectedByIde(String source) {
        return collect(source, true);
    }

    public static String at(String code, String source, int offset) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < offset; i++) {
            if (source.charAt(i) == '\n') {
                line++;
                column = 1;
            }
            else {
                column++;
            }
        }
        return code + "@" + line + ":" + column;
    }

    private static List<String> collect(String source, boolean ide) {
        List<String> result = new ArrayList<>();
        Matcher matcher = MARKER.matcher(source);
        while (matcher.find()) {
            String kind = matcher.group(1);
            String code = matcher.group(2);
            if (!CODES.contains(code)) {
                continue;
            }
            boolean expected = kind == null || (ide ? kind.equals("false") : kind.equals("missing"));
            if (expected) {
                result.add(at(code, source, matcher.end()));
            }
        }
        result.sort(null);
        return result;
    }
}
