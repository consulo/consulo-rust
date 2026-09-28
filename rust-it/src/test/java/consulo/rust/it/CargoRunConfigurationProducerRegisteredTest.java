/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.Application;
import consulo.execution.action.RunConfigurationProducer;
import consulo.it.HeadlessApplicationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rust.cargo.runconfig.command.CompositeCargoRunConfigurationProducer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nothing runs a Rust file unless a producer is on the extension point.
 * <p>
 * {@code ConfigurationContext} builds a configuration from context by enumerating the
 * {@link RunConfigurationProducer} extension point. The gutter icon on {@code fn main} comes from a
 * line marker contributor instead, which is registered separately - so an unregistered producer looks
 * like a working icon that does nothing when clicked, while a run configuration picked by hand still
 * runs fine. That is the shape of the regression this guards.
 * <p>
 * Only the composite is expected: it reaches the executable, test and bench producers itself so that
 * it can compare their candidates.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class CargoRunConfigurationProducerRegisteredTest {

    @Test
    public void theCargoProducerIsOnTheExtensionPoint(Application application) {
        List<RunConfigurationProducer> producers =
            application.getExtensionPoint(RunConfigurationProducer.class).getExtensionList();

        assertThat(producers)
            .as("a Rust run configuration can only be created from context by a registered producer")
            .hasAtLeastOneElementOfType(CompositeCargoRunConfigurationProducer.class);
    }
}
