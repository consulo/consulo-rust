/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.macros;

import consulo.application.ReadAction;
import consulo.it.AllowLogError;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.rust.it.flow.RsFlowTesting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallUtil;

import org.rust.stdext.RsResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Function-like macro calls of a Cargo package expand to PSI that knows the call it came from and resolves in the
 * scope of the call.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class MacroExpansionTest {
    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void expressionMacroExpandsToAnExpression(HeadlessProjects projects) throws Exception {
        String text = """
            macro_rules! add {
                ($a:expr, $b:expr) => { $a + $b };
            }

            pub fn sum() -> i32 {
                add!(1, 2)
            }
            """;
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text)) {
            ReadAction.run(() -> {
                RsMacroCall call = onlyCall(source.getFile());
                MacroExpansion expansion = expand(call);
                assertThat(expansion).isInstanceOf(MacroExpansion.Expr.class);
                assertThat(text(expansion)).containsExactly("1+2");
                RsExpandedElement expr = expansion.getElements().get(0);
                assertThat(RsExpandedElementUtil.getExpandedFrom(expr)).isSameAs(call);
            });
        }
    }

    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void expandedElementsResolveInTheScopeOfTheCall(HeadlessProjects projects) throws Exception {
        String text = """
            pub struct Answer;

            macro_rules! answer {
                () => { Answer };
            }

            pub fn get() -> Answer {
                answer!()
            }
            """;
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text)) {
            ReadAction.run(() -> {
                MacroExpansion expansion = expand(onlyCall(source.getFile()));
                assertThat(expansion).isNotNull();
                RsPathExpr path = (RsPathExpr) expansion.getElements().get(0);
                PsiElement target = path.getPath().getReference().resolve();
                assertThat(target).isInstanceOf(RsStructItem.class);
                assertThat(target.getContainingFile()).isSameAs(source.getFile());
            });
        }
    }

    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void itemMacroExpandsNestedCallsRecursively(HeadlessProjects projects) throws Exception {
        String text = """
            macro_rules! unit {
                ($name:ident) => { pub struct $name; };
            }

            macro_rules! units {
                ($($name:ident),*) => { $(unit!($name);)* };
            }

            units!(First, Second);
            """;
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text)) {
            ReadAction.run(() -> {
                RsMacroCall call = PsiTreeUtil.findChildrenOfType(source.getFile(), RsMacroCall.class).stream()
                    .filter(c -> c.getPath().getText().equals("units"))
                    .findFirst().orElseThrow();
                MacroExpansion expansion = expand(call);
                assertThat(expansion).isInstanceOf(MacroExpansion.Items.class);
                assertThat(text(expansion)).containsExactly("unit!(First);", "unit!(Second);");

                String recursive = RsPossibleMacroCallUtil.expandMacrosRecursively(
                    call, Integer.MAX_VALUE, true, RsPossibleMacroCallUtil::getExpansion
                );
                assertThat(recursive.replaceAll("\\s+", " ").trim()).isEqualTo("pub struct First ; pub struct Second ;");
            });
        }
    }

    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void dollarCrateIsTheCrateOfTheMacro(HeadlessProjects projects) throws Exception {
        String text = """
            pub struct Answer;

            macro_rules! answer {
                () => { $crate::Answer };
            }

            pub fn get() -> Answer {
                answer!()
            }
            """;
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text)) {
            ReadAction.run(() -> {
                RsMacroCall call = onlyCall(source.getFile());
                RsPathExpr path = (RsPathExpr) expand(call).getElements().get(0);
                PsiElement target = path.getPath().getReference().resolve();
                assertThat(target).isInstanceOf(RsStructItem.class);

                String crateName = RsElementUtil.getContainingCrate(call).getNormName();
                String shown = RsPossibleMacroCallUtil.expandMacrosRecursively(call, Integer.MAX_VALUE, true);
                assertThat(compact(shown)).isEqualTo("::" + crateName + "::Answer");
            });
        }
    }

    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void dollarCrateIsShownInANestedCallThatDoesNotExpand(HeadlessProjects projects) throws Exception {
        String text = """
            macro_rules! outer {
                () => { $crate::missing!() };
            }

            pub fn get() {
                outer!();
            }
            """;
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text)) {
            ReadAction.run(() -> {
                RsMacroCall call = onlyCall(source.getFile());
                String crateName = RsElementUtil.getContainingCrate(call).getNormName();
                String shown = RsPossibleMacroCallUtil.expandMacrosRecursively(call, Integer.MAX_VALUE, true);
                assertThat(compact(shown)).isEqualTo("::" + crateName + "::missing!()");
            });
        }
    }

    private static MacroExpansion expand(RsMacroCall call) {
        RsResult<MacroExpansion, GetMacroExpansionError> result = RsPossibleMacroCallUtil.getExpansionResult(call);
        assertThat(result.isOk()).as("expansion of %s: %s", call.getText(), result.err()).isTrue();
        return result.ok();
    }

    private static RsMacroCall onlyCall(PsiFile file) {
        List<RsMacroCall> calls = List.copyOf(PsiTreeUtil.findChildrenOfType(file, RsMacroCall.class));
        assertThat(calls).hasSize(1);
        return calls.get(0);
    }

    private static List<String> text(MacroExpansion expansion) {
        return expansion.getElements().stream().map(element -> compact(element.getText())).toList();
    }

    /**
     * The text without the whitespace that the expansion puts between the tokens of the macro definition.
     */
    private static String compact(String text) {
        return text.replaceAll("\\s+", "");
    }
}
