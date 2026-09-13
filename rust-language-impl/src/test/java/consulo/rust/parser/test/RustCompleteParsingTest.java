package consulo.rust.parser.test;

import consulo.language.file.LanguageFileType;
import consulo.test.junit.impl.language.SimpleParsingTest;
import consulo.util.io.FileUtil;
import org.junit.jupiter.api.Test;
import org.rust.lang.RsFileType;

import java.io.InputStream;

/**
 * Parses every complete fixture and compares the PSI tree against the recorded dump.
 */
public class RustCompleteParsingTest extends SimpleParsingTest<Object> {
    public RustCompleteParsingTest() {
        super("parsing/complete", "rs");
    }

    @Test
    public void testAndand(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testAssociatedTypes(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testAsyncAwait(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testAttributes(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testAttrsInExprs(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testAttrsInParams(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockAssignment(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockBinExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockCallExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockCastExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockDotExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockFullRangeExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockFullRangeExprDeprecated(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockIndexExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockLambdaExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockOpenRangeExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockReturnExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockTryExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlockUnaryExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBlocks(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testBreakWithLabelInCondition(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testCommentBinding(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testConditions(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testConstGenerics(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testConstants(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testDefaultParameterValues(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testDieselMacros(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testDocCommentWhitespace(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testDocComments(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testEmptyGenerics(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testEnumVis(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testExpr(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testExternBlock(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testExternCrates(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testExternFns(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testFn(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testImplDynTypeBound(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testImpls(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testIncDec(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testIssue320(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testLastBlockIsExpression(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testLoops(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testMacros(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testMacros2(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testMatch(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testMatchPatternAmbiguity(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testMod(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testNumbers(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testOror(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testPatterns(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testPolybounds(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testPrecedence(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testRanges(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testRawOperator(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testReservedKeywords(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testShifts(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testStructInheritance(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testStructLiterals(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testStructs(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testTraitAliases(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testTraits(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testTry(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testTryOperator(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testTurbo(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testType(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testUseItem(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testVisibility(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testWayTooManyBraces(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testWayTooManyGenerics(Context context) throws Exception {
        doTest(context, null);
    }

    @Test
    public void testWayTooManyParens(Context context) throws Exception {
        doTest(context, null);
    }

    /** No tree is recorded upstream for this one; it only asserts the parser terminates. */
    @Test
    public void testWayTooManyTypeQuals(Context context) throws Exception {
        ensureParsed(loadPsiFile(context, null));
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
        String path = "/parsing/complete/" + fixtureName(context) + "." + extension;
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("no fixture at " + path);
        }
        // sources were trimmed when the trees were recorded, and the tree dump is compared trimmed
        return FileUtil.loadTextAndClose(stream, true).trim();
    }
}
