/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.resolve;

import consulo.application.ReadAction;
import consulo.it.AllowLogError;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.rust.it.flow.RsFlowTesting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code Self} and {@code self} in {@code resolve/SelfPaths.rs}: in inherent and trait impls, in an enum and its
 * patterns, in a trait with its own members, in a generic impl with a where clause, and produced by a macro.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SelfResolveTest {
    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void everySelfPathResolves(HeadlessProjects projects) throws Exception {
        String text = RsFlowTesting.read("resolve", "SelfPaths.rs");
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text)) {
            ReadAction.run(() -> {
                Collection<RsPath> paths = PsiTreeUtil.findChildrenOfType(source.getFile(), RsPath.class);
                assertThat(unresolvedSelfPaths(paths, text)).isEmpty();

                assertThat(target(paths, "Self::new")).isInstanceOf(RsFunction.class);
                assertThat(target(paths, "Self::ORIGIN")).isNotNull();
                assertThat(target(paths, "Self::Empty")).isInstanceOf(RsEnumVariant.class);
                assertThat(target(paths, "Self::push")).isInstanceOf(RsFunction.class);
                assertThat(target(paths, "Self::Item")).isInstanceOf(RsTypeAlias.class);
                assertThat(target(paths, "self")).isInstanceOf(RsSelfParameter.class);
                assertThat(targets(paths, "Self")).hasAtLeastOneElementOfType(RsImplItem.class)
                    .hasAtLeastOneElementOfType(RsTraitItem.class);
            });
        }
    }

    @Test
    @AllowLogError({
        "consulo.component.impl.internal.messagebus.MessageBusConnectionImpl",
        "consulo.application.internal.BackgroundTaskUtil"
    })
    public void selfProducedByAMacroIsTheImplOfTheCall(HeadlessProjects projects) throws Exception {
        String text = RsFlowTesting.read("resolve", "SelfPaths.rs");
        try (RsFlowTesting.CargoSource source = RsFlowTesting.openCargoSource(projects, text)) {
            ReadAction.run(() -> {
                RsMacroCall call = PsiTreeUtil.findChildrenOfType(source.getFile(), RsMacroCall.class).stream()
                    .filter(c -> c.getPath().getText().equals("make_new"))
                    .findFirst().orElseThrow();
                MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(call);
                assertThat(expansion).as("make_new! must expand").isNotNull();

                List<PsiElement> selfTargets = new ArrayList<>();
                for (PsiElement element : expansion.getElements()) {
                    for (RsPath path : PsiTreeUtil.findChildrenOfType(element, RsPath.class)) {
                        if (path.getText().equals("Self")) selfTargets.add(resolve(path));
                    }
                }
                assertThat(selfTargets).isNotEmpty().allSatisfy(target -> {
                    assertThat(target).isInstanceOf(RsImplItem.class);
                    assertThat(target.getText()).startsWith("impl Counter");
                });
            });
        }
    }

    private static List<String> unresolvedSelfPaths(Collection<RsPath> paths, String text) {
        List<String> unresolved = new ArrayList<>();
        for (RsPath path : paths) {
            String pathText = path.getText();
            if (!pathText.equals("Self") && !pathText.equals("self") && !pathText.startsWith("Self::")) continue;
            if (resolve(path) == null) {
                int line = text.substring(0, path.getTextOffset()).split("\n", -1).length;
                unresolved.add(line + ": " + pathText);
            }
        }
        return unresolved;
    }

    private static PsiElement target(Collection<RsPath> paths, String pathText) {
        List<PsiElement> targets = targets(paths, pathText);
        assertThat(targets).as(pathText).isNotEmpty();
        return targets.get(0);
    }

    private static List<PsiElement> targets(Collection<RsPath> paths, String pathText) {
        List<PsiElement> result = new ArrayList<>();
        for (RsPath path : paths) {
            if (path.getText().equals(pathText)) result.add(resolve(path));
        }
        return result;
    }

    private static PsiElement resolve(RsPath path) {
        PsiReference reference = path.getReference();
        return reference == null ? null : reference.resolve();
    }
}
