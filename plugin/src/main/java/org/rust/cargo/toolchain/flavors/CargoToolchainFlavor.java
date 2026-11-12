/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import consulo.annotation.component.ExtensionImpl;
import consulo.platform.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

@ExtensionImpl(id = "rust.cargoToolchainFlavor", order = "first")
public class CargoToolchainFlavor extends RsToolchainFlavor {
    @Override
    protected Stream<Path> getHomePathCandidates(Platform platform) {
        String cargoHomeEnv = platform.os().getEnvironmentVariable("CARGO_HOME");
        Path cargoHome = cargoHomeEnv == null || cargoHomeEnv.isBlank() ? null : platform.fs().getPath(cargoHomeEnv);
        Path userHome = platform.user().homePath().resolve(".cargo");
        return Stream.of(cargoHome, userHome)
            .filter(Objects::nonNull)
            .map(p -> p.resolve("bin"))
            .filter(Files::isDirectory);
    }
}
