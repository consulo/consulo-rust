/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import consulo.annotation.component.ExtensionImpl;
import consulo.platform.Platform;
import consulo.util.io.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@ExtensionImpl(id = "rust.winToolchainFlavor")
public class RsWinToolchainFlavor extends RsToolchainFlavor {
    @Override
    protected Stream<Path> getHomePathCandidates(Platform platform) {
        String programFilesEnv = platform.os().getEnvironmentVariable("ProgramFiles");
        if (programFilesEnv == null) return Stream.empty();
        Path programFiles = platform.fs().getPath(programFilesEnv);
        if (!Files.isDirectory(programFiles)) return Stream.empty();
        try (Stream<Path> children = Files.list(programFiles)) {
            List<Path> bins = children
                .filter(Files::isDirectory)
                .filter(p -> FileUtil.getNameWithoutExtension(p.getFileName().toString()).toLowerCase().startsWith("rust"))
                .map(p -> p.resolve("bin"))
                .filter(Files::isDirectory)
                .toList();
            return bins.stream();
        }
        catch (IOException e) {
            return Stream.empty();
        }
    }

    @Override
    protected boolean isApplicable(Platform platform) {
        return platform.os().isWindows();
    }
}
