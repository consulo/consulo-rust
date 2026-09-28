/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.Application;
import consulo.it.HeadlessApplicationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The headless application boots with the Rust plugin on the classpath, and its {@code @ServiceImpl}s
 * resolve straight from the dependency classpath.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class CargoHeadlessSmokeTest {

    @Test
    public void applicationBoots(Application application) {
        assertThat(application).isNotNull();
        assertThat(Application.get()).isSameAs(application);
    }
}
