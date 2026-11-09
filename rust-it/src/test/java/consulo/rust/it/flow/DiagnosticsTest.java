/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.flow;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ReadAction;
import consulo.document.util.TextRange;
import consulo.it.AllowLogError;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsBorrowCheckerInspection;
import org.rust.ide.inspections.lints.RsLivenessInspection;
import org.rust.ide.inspections.lints.RsUnreachableCodeInspection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The flow diagnostics of the IDE on every {@code diagnostics/*.rs} against its {@link DiagnosticMarkers markers}: the
 * unreachable code and liveness lints, and the moves and initialization of the borrow checker. A diagnostic of these
 * inspections without a rustc code in {@link DiagnosticMarkers#CODES} fails the test.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class DiagnosticsTest {
    private static final String DIRECTORY = "diagnostics";

    static Stream<String> sources() throws Exception {
        return RsFlowTesting.sources(DIRECTORY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sources")
    // logged while the project closes, as in CargoSyncWithProxyToolchainTest
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void check(String fileName, HeadlessProjects projects) throws Exception {
        String text = RsFlowTesting.read(DIRECTORY, fileName);
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text)) {
            check(source.getFile(), text);
        }
    }

    private static void check(PsiFile file, String text) {
        Map<String, String> borrowCodes = Map.of(
            RsBundle.message("inspection.message.use.moved.value"), "E0382",
            RsBundle.message("inspection.message.use.possibly.uninitialized.variable"), "E0381"
        );

        List<String> reported = ReadAction.compute(() -> {
            List<String> result = new ArrayList<>();
            result.addAll(run(file, text, new RsUnreachableCodeInspection(), message -> "unreachable_code"));
            result.addAll(run(file, text, new RsLivenessInspection(), message -> "unused_variables"));
            result.addAll(run(file, text, new RsBorrowCheckerInspection(), message -> borrowCodes.getOrDefault(message, "other:" + message)));
            result.sort(null);
            return result;
        });

        assertThat(reported).isEqualTo(DiagnosticMarkers.expectedByIde(text));
    }

    @RequiredReadAction
    private static List<String> run(PsiFile file, String text, LocalInspectionTool inspection, Function<String, String> code) {
        ProblemsHolder holder = InspectionManager.getInstance(file.getProject()).createProblemsHolder(file, false);
        PsiElementVisitor visitor = inspection.buildVisitor(holder, false);
        PsiTreeUtil.processElements(file, element -> {
            element.accept(visitor);
            return true;
        });

        List<String> result = new ArrayList<>();
        for (ProblemDescriptor descriptor : holder.getResults()) {
            PsiElement element = descriptor.getPsiElement();
            TextRange range = descriptor.getTextRangeInElement();
            int offset = element.getTextRange().getStartOffset() + (range != null ? range.getStartOffset() : 0);
            result.add(DiagnosticMarkers.at(code.apply(descriptor.getDescriptionTemplate().get()), text, offset));
        }
        return result;
    }
}
