/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.WriteAction;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkTable;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.rust.bundle.RustBundleType;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A mock Rust bundle: a toolchain is just a directory layout, so a test can build one without
 * installing anything.
 * <p>
 * Two properties matter. The bundle must carry no roots of its own - the standard library reaches a
 * module as one order entry per crate, so that a file under it can be attributed to the crate that
 * owns it, which a single sources root could never do. And a stdlib crate must be locatable under the
 * bundle from its name alone, which is what keeps machine-specific toolchain paths out of module files.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class RustBundleStdlibTest {

    @Test
    public void bundleCarriesNoRootsAndLocatesStdlibCratesByName(HeadlessProjects projects) throws Exception {
        Path home = Files.createTempDirectory("consulo-rust-it-bundle");
        // what RustBundleType.isValidSdkHome looks for
        Files.createDirectories(home.resolve("bin"));
        Files.writeString(home.resolve("bin/rustc"), "");
        Files.writeString(home.resolve("bin/cargo"), "");

        Path library = home.resolve("lib/rustlib/src/rust/library");
        for (String crate : new String[]{"core", "std", "alloc"}) {
            Files.createDirectories(library.resolve(crate + "/src"));
            Files.writeString(library.resolve(crate + "/src/lib.rs"), "// " + crate + "\n");
        }
        // std's sibling, which additionalRoots pulls in
        Files.createDirectories(library.resolve("backtrace/src"));
        Files.writeString(library.resolve("backtrace/src/lib.rs"), "// backtrace\n");

        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(home);

        SdkTable sdkTable = SdkTable.getInstance();
        Sdk sdk = WriteAction.compute(() -> {
            Sdk created = sdkTable.createSdk("mock-rust", RustBundleType.getInstance());
            SdkModificator modificator = created.getSdkModificator();
            modificator.setHomePath(home.toString());
            modificator.commitChanges();
            sdkTable.addSdk(created);
            return created;
        });

        try {
            WriteAction.run(() -> RustBundleType.getInstance().setupSdkPaths(sdk));

            assertThat(sdk.getRootProvider().getUrls(SourcesOrderRootType.ID))
                .as("the bundle must carry no sources root - the stdlib comes as one entry per crate")
                .isEmpty();
            assertThat(sdk.getRootProvider().getUrls(BinariesOrderRootType.ID))
                .as("and no binaries root either, so /usr/bin is never watched")
                .isEmpty();

            VirtualFile srcDir = RustBundleType.stdlibSrcDir(sdk);
            assertThat(srcDir)
                .as("the standard library sources must be found under the bundle")
                .isNotNull();
            assertThat(srcDir.findChild("core"))
                .as("a stdlib crate must be locatable by name, with no path stored anywhere")
                .isNotNull();
            assertThat(srcDir.findChild("backtrace"))
                .as("std's additional root must be reachable the same way")
                .isNotNull();

            assertThat(RustBundleType.stdlibSrcDir(null))
                .as("no bundle means no stdlib roots, which must degrade quietly")
                .isNull();
        }
        finally {
            WriteAction.run(() -> sdkTable.removeSdk(sdk));
        }
    }
}
