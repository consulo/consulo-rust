/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class RsWinToolchainFlavor extends RsToolchainFlavor {

    @Override
    protected Stream<Path> getHomePathCandidates() {
        String programFilesEnv = System.getenv("ProgramFiles");
        if (programFilesEnv == null) return Stream.empty();
        Path programFiles = Paths.get(programFilesEnv);
        if (!Files.exists(programFiles) || !Files.isDirectory(programFiles)) return Stream.empty();
        try {
            return Files.list(programFiles)
                .filter(Files::isDirectory)
                .filter(p -> {
                    String name = FileUtil.getNameWithoutExtension(p.getFileName().toString());
                    return name.toLowerCase().startsWith("rust");
                })
                .map(p -> p.resolve("bin"))
                .filter(Files::isDirectory);
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    @Override
    protected boolean isApplicable() {
        return SystemInfo.isWindows;
    }
}
