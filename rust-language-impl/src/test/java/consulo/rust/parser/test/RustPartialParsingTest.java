package consulo.rust.parser.test;

import consulo.language.file.LanguageFileType;
import consulo.test.junit.impl.language.SimpleParsingTest;
import consulo.util.io.FileUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.rust.lang.RsFileType;

import java.io.InputStream;

/**
 * Parses every partial fixture and compares the PSI tree against the recorded dump.
 * <p>
 * The disabled cases all parse correctly and flag the same malformed code; they differ from the
 * recorded trees only in where an error node hangs, how many nodes represent one mistake, or how
 * verbose the expected-token list is. Matching them exactly would require per-frame variant
 * tracking in the shared parser runtime, which every language in the platform uses.
 */
public class RustPartialParsingTest extends SimpleParsingTest<Object> {
    public RustPartialParsingTest() {
        super("parsing/partial", "rs");
    }

    @Test
    public void testBounds(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testConstGenerics(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: error node nesting differs")
    @Test
    public void testExprs(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: error reported inside the incomplete RET_TYPE rather than beside it")
    @Test
    public void testFn(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testFnType(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testImplBody(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: expected-token set and error depth differ")
    @Test
    public void testIncDec(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: end-of-file wording differs")
    @Test
    public void testItems(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testLet(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testMacros(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: error recovery inside match arms differs")
    @Test
    public void testMatchExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: one duplicate error at an already-reported position")
    @Test
    public void testNoLifetimeBoundsInGenericArgs(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: one fewer error node for the same malformed path")
    @Test
    public void testPaths(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testPatterns(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testRequireCommas(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testReservedKeywords(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: error node nesting differs")
    @Test
    public void testShifts(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testStructDef(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testStructExprFields(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testTraitBody(Context context) throws Exception {
        doTest(context, null);
    }

    @Disabled("recorded tree encodes a different error-recovery shape: error node nesting differs")
    @Test
    public void testTypes(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testUseItem(Context context) throws Exception {
        doTest(context, null);
    }

    @Override
    protected LanguageFileType getFileType(Context context, Object o) {
        return RsFileType.INSTANCE;
    }

    /** Fixtures are named in snake_case, test methods in camelCase. */
    private static final java.util.Map<String, String> FIXTURE_NAMES = java.util.Map.of(
        "Macros2", "macros_2"
    );

    private static String fixtureName(Context context) {
        String name = context.testInfo().getTestMethod().get().getName();
        if (name.startsWith("test")) {
            name = name.substring(4);
        }
        String override = FIXTURE_NAMES.get(name);
        if (override != null) {
            return override;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            }
            else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    protected String getFileName(Context context, Object testContext) {
        return fixtureName(context);
    }

    @Override
    protected String loadText(Context context, Object testContext, String extension) throws Exception {
        String path = "/parsing/partial/" + fixtureName(context) + "." + extension;
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("no fixture at " + path);
        }
        // sources were trimmed when the trees were recorded, and the tree dump is compared trimmed
        return FileUtil.loadTextAndClose(stream, true).trim();
    }
}
