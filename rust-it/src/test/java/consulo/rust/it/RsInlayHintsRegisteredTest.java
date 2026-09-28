/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.Application;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.editor.inlay.InlayActionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.ide.hints.type.RsChainMethodTypeHintsProvider;
import org.rust.ide.hints.type.RsInlayActionHandler;
import org.rust.ide.hints.type.RsInlayTypeHintsProvider;
import org.rust.ide.hints.value.RsInlayValueHintsProvider;
import org.rust.lang.RsLanguage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HeadlessApplicationExtension.class)
public class RsInlayHintsRegisteredTest {

    @Test
    public void allThreeProvidersAreOnTheExtensionPoint(Application application) {
        List<DeclarativeInlayHintsProvider> providers =
            application.getExtensionPoint(DeclarativeInlayHintsProvider.class).getExtensionList();

        assertThat(providers)
            .as("type hints")
            .hasAtLeastOneElementOfType(RsInlayTypeHintsProvider.class);
        assertThat(providers)
            .as("method chain hints")
            .hasAtLeastOneElementOfType(RsChainMethodTypeHintsProvider.class);
        assertThat(providers)
            .as("value hints")
            .hasAtLeastOneElementOfType(RsInlayValueHintsProvider.class);
    }

    @Test
    public void everyOptionIdIsUniqueAcrossProviders(Application application) {
        List<String> optionIds = application.getExtensionPoint(DeclarativeInlayHintsProvider.class)
            .getExtensionList().stream()
            .filter(p -> p.getLanguage() == RsLanguage.INSTANCE)
            .flatMap(p -> p.getOptions().stream())
            .map(o -> o.id())
            .toList();

        assertThat(optionIds).doesNotHaveDuplicates();
    }

    @Test
    public void clickingATypeNameHasAHandler(Application application) {
        assertThat(InlayActionHandler.getActionHandler(RsInlayActionHandler.HANDLER_ID))
            .as("type names in a hint carry a navigation payload; without the handler the click is dead")
            .isInstanceOf(RsInlayActionHandler.class);
    }
}
