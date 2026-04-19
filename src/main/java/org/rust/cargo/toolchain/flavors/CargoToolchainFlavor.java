/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.EnvironmentUtil;
import org.rust.stdext.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class CargoToolchainFlavor extends RsToolchainFlavor {
    @Override
    protected Stream<Path> getHomePathCandidates() {
        Path cargoHome = null;
        String cargoHomeEnv = EnvironmentUtil.getValue("CARGO_HOME");
        if (cargoHomeEnv != null) {
            cargoHome = PathUtil.toPathOrNull(cargoHomeEnv);
        }
        Path userHome = PathUtil.toPath(FileUtil.expandUserHome("~/.cargo/"));
        return Stream.of(cargoHome, userHome)
            .filter(Objects::nonNull)
            .map(p -> p.resolve("bin"))
            .filter(Files::isDirectory);
    }
}
