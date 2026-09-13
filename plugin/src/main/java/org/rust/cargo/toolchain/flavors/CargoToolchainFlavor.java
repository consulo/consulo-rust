/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;


import consulo.annotation.component.ExtensionImpl;
import consulo.util.io.FileUtil;
import consulo.process.local.EnvironmentUtil;
import org.rust.stdext.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import consulo.application.util.UserHomeFileUtil;

@ExtensionImpl(id = "rust.cargoToolchainFlavor", order = "first")
public class CargoToolchainFlavor extends RsToolchainFlavor {
    @Override
    protected Stream<Path> getHomePathCandidates() {
        Path cargoHome = null;
        String cargoHomeEnv = EnvironmentUtil.getValue("CARGO_HOME");
        if (cargoHomeEnv != null) {
            cargoHome = PathUtil.toPathOrNull(cargoHomeEnv);
        }
        Path userHome = PathUtil.toPath(consulo.application.util.UserHomeFileUtil.expandUserHome("~/.cargo/"));
        return Stream.of(cargoHome, userHome)
            .filter(Objects::nonNull)
            .map(p -> p.resolve("bin"))
            .filter(Files::isDirectory);
    }
}
