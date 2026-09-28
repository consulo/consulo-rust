/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.psi.PsiElement;
import consulo.language.editor.action.TypeDeclarationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.ide.navigation.goto_.RsTypeDeclarationProvider;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Navigation extensions the plugin has to be on the extension point for.
 * <p>
 * A class that implements a consulo {@code @ExtensionAPI} but carries no {@code @ExtensionImpl}
 * compiles, passes every test that does not boot the application, and is simply never used - the
 * feature does nothing and nothing says why. That is how the whole run-from-context path was lost, so
 * registration is asserted here rather than trusted.
 *
 * @see CargoRunConfigurationProducerRegisteredTest
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class RsNavigationExtensionsRegisteredTest {

    @Test
    public void goToTypeDeclarationHasItsProvider(Application application) {
        List<TypeDeclarationProvider> providers =
            application.getExtensionPoint(TypeDeclarationProvider.class).getExtensionList();

        assertThat(providers)
            .as("Go to Type Declaration does nothing for Rust unless the provider is registered")
            .hasAtLeastOneElementOfType(RsTypeDeclarationProvider.class);
    }

    /**
     * Being registered is not enough. {@code getSymbolTypeDeclarations} is not abstract - the base
     * class answers null - so a provider carrying the upstream one-argument signature compiles, loads
     * and then silently answers nothing, which is what this port shipped. Structural rather than
     * behavioural: it pins the override, not the navigation.
     */
    @Test
    public void theProviderOverridesTheMethodTheEditorCalls() throws Exception {
        Method called = TypeDeclarationProvider.class
            .getMethod("getSymbolTypeDeclarations", PsiElement.class, Editor.class, int.class);

        Method overriding = RsTypeDeclarationProvider.class
            .getMethod(called.getName(), called.getParameterTypes());

        assertThat(overriding.getDeclaringClass())
            .as("the provider must override the method the editor calls, not one of its own")
            .isEqualTo(RsTypeDeclarationProvider.class);
    }
}
