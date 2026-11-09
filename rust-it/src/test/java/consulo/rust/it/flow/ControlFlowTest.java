/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.flow;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.controlFlow.Instruction;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.rust.lang.core.dfa.CFGInstruction;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.RsTypesUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

/**
 * The control-flow graphs of the functions of every {@code flow/<name>.rs}, in the order of the file, against
 * {@code flow/<name>.txt}: a header per function, then a node per line as {@code index(successors) text}, nodes not
 * reachable from the entry marked with {@code !}, then the elements the unreachable code lint reports. A missing
 * {@code .txt} is written to {@code src/test/resources/flow} and the test fails.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class ControlFlowTest {
    private static final String DIRECTORY = "flow";

    static Stream<String> sources() throws Exception {
        return RsFlowTesting.sources(DIRECTORY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sources")
    public void check(String fileName, HeadlessProjects projects) throws Exception {
        PsiFile file = RsFlowTesting.openSource(projects, fileName, RsFlowTesting.read(DIRECTORY, fileName));
        String actual = ReadAction.compute(() -> dump(file));
        RsFlowTesting.assertGolden(DIRECTORY, fileName.substring(0, fileName.length() - ".rs".length()) + ".txt", actual);
    }

    @RequiredReadAction
    private static String dump(PsiFile file) {
        StringBuilder builder = new StringBuilder();
        for (RsFunction function : PsiTreeUtil.findChildrenOfType(file, RsFunction.class)) {
            ControlFlowGraph cfg = RsTypesUtil.getControlFlowGraph(function);
            if (cfg == null) {
                continue;
            }
            builder.append("# fn ").append(function.getName()).append('\n');

            for (CFGInstruction instruction : cfg.getInstructions()) {
                List<Integer> successors = new ArrayList<>();
                for (Instruction succ : instruction.allSucc()) {
                    successors.add(succ.num());
                }
                successors.sort(Comparator.naturalOrder());
                StringJoiner joiner = new StringJoiner(",", "(", ")");
                successors.forEach(index -> joiner.add(String.valueOf(index)));
                builder.append(cfg.isReachable(instruction) ? "  " : "! ")
                    .append(instruction.num()).append(joiner).append(' ').append(oneLine(instruction.getData().getText())).append('\n');
            }

            List<RsElement> unreachable = new ArrayList<>(cfg.unreachableElements);
            unreachable.sort(Comparator.comparingInt(RsElement::getTextOffset));
            for (RsElement element : unreachable) {
                builder.append("unreachable: ").append(oneLine(element.getText())).append('\n');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private static String oneLine(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
