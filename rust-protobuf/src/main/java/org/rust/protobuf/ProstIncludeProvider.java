/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.protobuf;

import com.intellij.protobuf.lang.psi.PbFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.generated.RsGeneratedCode;
import org.rust.lang.core.macros.generated.RsGeneratedIncludeProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * The {@code <package>.rs} files prost-build and tonic-build write to {@code OUT_DIR} - what
 * {@code tonic::include_proto!("package")} includes - generated in memory from the {@code .proto} files of the
 * Cargo package, for a package whose build script depends on prost-build, tonic-build or tonic-prost-build.
 */
@ExtensionImpl
public final class ProstIncludeProvider implements RsGeneratedIncludeProvider {
    private static final Set<String> PROST_BUILDERS = Set.of("prost-build", "tonic-build", "tonic-prost-build");
    private static final Set<String> TONIC_BUILDERS = Set.of("tonic-build", "tonic-prost-build");

    @Nullable
    @Override
    public RsGeneratedCode generate(@Nonnull Crate crate, @Nonnull String fileName) {
        if (!fileName.endsWith(".rs") || fileName.contains("/") || fileName.contains("\\")) return null;
        CargoWorkspace.Target target = crate.getCargoTarget();
        if (target == null) return null;
        CargoWorkspace.Package pkg = target.getPkg();
        Set<String> builders = buildDependencies(pkg);
        if (builders.isEmpty()) return null;
        VirtualFile root = pkg.getContentRoot();
        if (root == null) return null;

        // prost writes one file per protobuf package, `_.rs` for the files without a package
        String packageName = fileName.substring(0, fileName.length() - ".rs".length());
        if (packageName.equals("_")) packageName = "";
        List<PbFile> files = protoFiles(crate.getProject(), root);
        List<PbFile> packageFiles = new ArrayList<>();
        for (PbFile file : files) {
            if (file.getPackageQualifiedName().toString().equals(packageName)) packageFiles.add(file);
        }
        if (packageFiles.isEmpty()) return null;

        RsGeneratedCode.Builder out = RsGeneratedCode.builder();
        for (PbFile file : files) {
            out.source(file);
        }
        ProtoSymbols symbols = new ProtoSymbols(files);
        ProstGenerator prost = new ProstGenerator(symbols, out, packageName);
        for (PbFile file : packageFiles) {
            prost.generateFile(file);
        }
        if (builders.stream().anyMatch(TONIC_BUILDERS::contains)) {
            String buildScript = buildScriptText(pkg);
            boolean server = !buildScript.matches("(?s).*\\.build_server\\(\\s*false\\s*\\).*");
            boolean client = !buildScript.matches("(?s).*\\.build_client\\(\\s*false\\s*\\).*");
            TonicGenerator tonic = new TonicGenerator(symbols, prost, out, server, client, tonicBodyType(pkg));
            for (PbFile file : packageFiles) {
                tonic.generateFile(file);
            }
        }
        return out.build();
    }

    /** The code generators among the build dependencies of the package. */
    @Nonnull
    private static Set<String> buildDependencies(@Nonnull CargoWorkspace.Package pkg) {
        Set<String> builders = new HashSet<>();
        for (CargoWorkspace.Dependency dependency : pkg.getDependencies()) {
            String name = dependency.getPkg().getName();
            if (!PROST_BUILDERS.contains(name)) continue;
            for (CargoWorkspace.DepKindInfo kind : dependency.getDepKinds()) {
                if (kind.getKind() == CargoWorkspace.DepKind.Build) builders.add(name);
            }
        }
        return builders;
    }

    /** {@code tonic::body::Body} since tonic 0.14, {@code tonic::body::BoxBody} in the versions before. */
    @Nonnull
    private static String tonicBodyType(@Nonnull CargoWorkspace.Package pkg) {
        for (CargoWorkspace.Dependency dependency : pkg.getDependencies()) {
            if (!dependency.getPkg().getName().equals("tonic")) continue;
            String[] version = dependency.getPkg().getVersion().split("\\.");
            try {
                int major = Integer.parseInt(version[0]);
                int minor = version.length > 1 ? Integer.parseInt(version[1]) : 0;
                return major == 0 && minor < 14 ? "tonic::body::BoxBody" : "tonic::body::Body";
            }
            catch (NumberFormatException e) {
                break;
            }
        }
        return "tonic::body::Body";
    }

    @Nonnull
    private static String buildScriptText(@Nonnull CargoWorkspace.Package pkg) {
        CargoWorkspace.Target buildScript = pkg.getCustomBuildTarget();
        VirtualFile file = buildScript != null ? buildScript.getCrateRoot() : null;
        if (file == null) return "";
        try {
            return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            return "";
        }
    }

    /** The {@code .proto} files of the package, found without indexes; the build output is skipped. */
    @Nonnull
    private static List<PbFile> protoFiles(@Nonnull Project project, @Nonnull VirtualFile root) {
        List<PbFile> files = new ArrayList<>();
        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFileUtil.visitChildrenRecursively(root, new VirtualFileVisitor<Void>() {
            @Override
            public boolean visitFile(@Nonnull VirtualFile file) {
                if (file.isDirectory()) {
                    String name = file.getName();
                    return file.equals(root) || !name.equals("target") && !name.startsWith(".");
                }
                if ("proto".equals(file.getExtension())) {
                    PsiFile psiFile = psiManager.findFile(file);
                    if (psiFile instanceof PbFile pbFile) files.add(pbFile);
                }
                return true;
            }
        });
        return files;
    }
}
