/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.workspace.*;
import org.rust.cargo.project.workspace.CargoWorkspace.Edition;
import org.rust.cargo.project.workspace.CargoWorkspace.LibKind;
import org.rust.openapiext.RsPathManager;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.HashCode;
import org.rust.stdext.CollectionsUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CargoMetadata {

    private static final Logger LOG = Logger.getInstance(CargoMetadata.class);

    private CargoMetadata() {
    }

    // ---- Project ----

    public static final class Project {
        private final List<Package> packages;
        private final Resolve resolve;
        private final int version;
        private final List<String> workspace_members;
        private final String workspace_root;

        public Project(List<Package> packages, Resolve resolve, int version, List<String> workspace_members, String workspace_root) {
            this.packages = packages;
            this.resolve = resolve;
            this.version = version;
            this.workspace_members = workspace_members;
            this.workspace_root = workspace_root;
        }

        public List<Package> getPackages() { return packages; }
        public Resolve getResolve() { return resolve; }
        public int getVersion() { return version; }
        public List<String> getWorkspace_members() { return workspace_members; }
        public String getWorkspace_root() { return workspace_root; }

        public Project convertPaths(Function<String, String> converter) {
            return new Project(
                packages.stream().map(p -> p.convertPaths(converter)).collect(Collectors.toList()),
                resolve,
                version,
                workspace_members,
                converter.apply(workspace_root)
            );
        }

        public Project replacePaths(Function<String, String> replacer) {
            return new Project(
                packages.stream().map(p -> p.replacePaths(replacer)).collect(Collectors.toList()),
                resolve,
                version,
                workspace_members,
                replacer.apply(workspace_root)
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Project)) return false;
            Project that = (Project) o;
            return version == that.version &&
                Objects.equals(packages, that.packages) &&
                Objects.equals(resolve, that.resolve) &&
                Objects.equals(workspace_members, that.workspace_members) &&
                Objects.equals(workspace_root, that.workspace_root);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packages, resolve, version, workspace_members, workspace_root);
        }
    }

    // ---- Package ----

    public static final class Package {
        private final String name;
        private final String version;
        private final List<String> authors;
        @Nullable private final String description;
        @Nullable private final String repository;
        @Nullable private final String license;
        @Nullable private final String license_file;
        @Nullable private final String source;
        private final String id;
        private final String manifest_path;
        private final List<Target> targets;
        @Nullable private final String edition;
        private final Map<String, List<String>> features;
        private final List<RawDependency> dependencies;

        public Package(String name, String version, List<String> authors,
                       @Nullable String description, @Nullable String repository,
                       @Nullable String license, @Nullable String license_file,
                       @Nullable String source, String id, String manifest_path,
                       List<Target> targets, @Nullable String edition,
                       Map<String, List<String>> features, List<RawDependency> dependencies) {
            this.name = name;
            this.version = version;
            this.authors = authors;
            this.description = description;
            this.repository = repository;
            this.license = license;
            this.license_file = license_file;
            this.source = source;
            this.id = id;
            this.manifest_path = manifest_path;
            this.targets = targets;
            this.edition = edition;
            this.features = features;
            this.dependencies = dependencies;
        }

        public String getName() { return name; }
        public String getVersion() { return version; }
        public List<String> getAuthors() { return authors; }
        @Nullable public String getDescription() { return description; }
        @Nullable public String getRepository() { return repository; }
        @Nullable public String getLicense() { return license; }
        @Nullable public String getLicense_file() { return license_file; }
        @Nullable public String getSource() { return source; }
        public String getId() { return id; }
        public String getManifest_path() { return manifest_path; }
        public List<Target> getTargets() { return targets; }
        @Nullable public String getEdition() { return edition; }
        public Map<String, List<String>> getFeatures() { return features; }
        public List<RawDependency> getDependencies() { return dependencies; }

        public Package copy(String newId, String newManifestPath, List<Target> newTargets, List<RawDependency> newDependencies) {
            return new Package(name, version, authors, description, repository, license, license_file,
                source, newId, newManifestPath, newTargets, edition, features, newDependencies);
        }

        public Package convertPaths(Function<String, String> converter) {
            return new Package(
                name, version, authors, description, repository, license, license_file,
                source, id, converter.apply(manifest_path),
                targets.stream().map(t -> t.convertPaths(converter)).collect(Collectors.toList()),
                edition, features, dependencies
            );
        }

        public Package replacePaths(Function<String, String> replacer) {
            return new Package(
                name, version, authors, description, repository, license, license_file,
                source, id, replacer.apply(manifest_path),
                targets.stream().map(t -> t.replacePaths(replacer)).collect(Collectors.toList()),
                edition, features, dependencies
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Package)) return false;
            Package that = (Package) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    // ---- RawDependency ----

    public static final class RawDependency {
        private final String name;
        @Nullable private final String rename;
        @Nullable private final String kind;
        @Nullable private final String target;
        private final boolean optional;
        private final boolean uses_default_features;
        private final List<String> features;

        public RawDependency(String name, @Nullable String rename, @Nullable String kind,
                             @Nullable String target, boolean optional,
                             boolean uses_default_features, List<String> features) {
            this.name = name;
            this.rename = rename;
            this.kind = kind;
            this.target = target;
            this.optional = optional;
            this.uses_default_features = uses_default_features;
            this.features = features;
        }

        public String getName() { return name; }
        @Nullable public String getRename() { return rename; }
        @Nullable public String getKind() { return kind; }
        @Nullable public String getTarget() { return target; }
        public boolean isOptional() { return optional; }
        public boolean isUses_default_features() { return uses_default_features; }
        public List<String> getFeatures() { return features; }

        public RawDependency copy(@Nullable String newKind) {
            return new RawDependency(name, rename, newKind, target, optional, uses_default_features, features);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RawDependency)) return false;
            RawDependency that = (RawDependency) o;
            return optional == that.optional &&
                uses_default_features == that.uses_default_features &&
                Objects.equals(name, that.name) &&
                Objects.equals(rename, that.rename) &&
                Objects.equals(kind, that.kind) &&
                Objects.equals(target, that.target) &&
                Objects.equals(features, that.features);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, rename, kind, target, optional, uses_default_features, features);
        }
    }

    // ---- Target ----

    public static final class Target {
        private final List<String> kind;
        private final String name;
        private final String src_path;
        private final List<String> crate_types;
        @Nullable private final String edition;
        @Nullable private final Boolean doctest;

        @JsonProperty("required-features")
        @Nullable private final List<String> required_features;

        public Target(List<String> kind, String name, String src_path,
                      List<String> crate_types, @Nullable String edition,
                      @Nullable Boolean doctest, @Nullable List<String> required_features) {
            this.kind = kind;
            this.name = name;
            this.src_path = src_path;
            this.crate_types = crate_types;
            this.edition = edition;
            this.doctest = doctest;
            this.required_features = required_features;
        }

        public List<String> getKind() { return kind; }
        public String getName() { return name; }
        public String getSrc_path() { return src_path; }
        public List<String> getCrate_types() { return crate_types; }
        @Nullable public String getEdition() { return edition; }
        @Nullable public Boolean getDoctest() { return doctest; }
        @Nullable public List<String> getRequired_features() { return required_features; }

        public TargetKind getCleanKind() {
            if (kind.size() == 1) {
                switch (kind.get(0)) {
                    case "bin": return TargetKind.BIN;
                    case "example": return TargetKind.EXAMPLE;
                    case "test": return TargetKind.TEST;
                    case "bench": return TargetKind.BENCH;
                    case "proc-macro": return TargetKind.LIB;
                    case "custom-build": return TargetKind.CUSTOM_BUILD;
                }
            }
            if (kind.stream().anyMatch(k -> k.endsWith("lib"))) {
                return TargetKind.LIB;
            }
            return TargetKind.UNKNOWN;
        }

        public List<CrateType> getCleanCrateTypes() {
            return crate_types.stream().map(ct -> {
                switch (ct) {
                    case "bin": return CrateType.BIN;
                    case "lib": return CrateType.LIB;
                    case "dylib": return CrateType.DYLIB;
                    case "staticlib": return CrateType.STATICLIB;
                    case "cdylib": return CrateType.CDYLIB;
                    case "rlib": return CrateType.RLIB;
                    case "proc-macro": return CrateType.PROC_MACRO;
                    default: return CrateType.UNKNOWN;
                }
            }).collect(Collectors.toList());
        }

        public Target copy(String newSrcPath) {
            return new Target(kind, name, newSrcPath, crate_types, edition, doctest, required_features);
        }

        public Target convertPaths(Function<String, String> converter) {
            return new Target(kind, name, converter.apply(src_path), crate_types, edition, doctest, required_features);
        }

        public Target replacePaths(Function<String, String> replacer) {
            return new Target(kind, name, replacer.apply(src_path), crate_types, edition, doctest, required_features);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Target)) return false;
            Target that = (Target) o;
            return Objects.equals(kind, that.kind) &&
                Objects.equals(name, that.name) &&
                Objects.equals(src_path, that.src_path) &&
                Objects.equals(crate_types, that.crate_types);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, name, src_path, crate_types);
        }
    }

    // ---- TargetKind ----

    public enum TargetKind {
        LIB, BIN, TEST, EXAMPLE, BENCH, CUSTOM_BUILD, UNKNOWN
    }

    // ---- CrateType ----

    public enum CrateType {
        BIN, LIB, DYLIB, STATICLIB, CDYLIB, RLIB, PROC_MACRO, UNKNOWN
    }

    // ---- Resolve ----

    public static final class Resolve {
        private final List<ResolveNode> nodes;

        public Resolve(List<ResolveNode> nodes) {
            this.nodes = nodes;
        }

        public List<ResolveNode> getNodes() { return nodes; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Resolve)) return false;
            return Objects.equals(nodes, ((Resolve) o).nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodes);
        }
    }

    // ---- ResolveNode ----

    public static final class ResolveNode {
        private final String id;
        private final List<String> dependencies;
        @Nullable private final List<Dep> deps;
        @Nullable private final List<String> features;

        public ResolveNode(String id, List<String> dependencies, @Nullable List<Dep> deps, @Nullable List<String> features) {
            this.id = id;
            this.dependencies = dependencies;
            this.deps = deps;
            this.features = features;
        }

        public String getId() { return id; }
        public List<String> getDependencies() { return dependencies; }
        @Nullable public List<Dep> getDeps() { return deps; }
        @Nullable public List<String> getFeatures() { return features; }

        public ResolveNode copy(String newId, List<String> newDependencies, List<Dep> newDeps) {
            return new ResolveNode(newId, newDependencies, newDeps, features);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ResolveNode)) return false;
            ResolveNode that = (ResolveNode) o;
            return Objects.equals(id, that.id) &&
                Objects.equals(dependencies, that.dependencies) &&
                Objects.equals(deps, that.deps) &&
                Objects.equals(features, that.features);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, dependencies, deps, features);
        }
    }

    // ---- Dep ----

    public static final class Dep {
        private final String pkg;
        @Nullable private final String name;
        @Nullable private final List<DepKindInfo> dep_kinds;

        public Dep(String pkg, @Nullable String name, @Nullable List<DepKindInfo> dep_kinds) {
            this.pkg = pkg;
            this.name = name;
            this.dep_kinds = dep_kinds;
        }

        public String getPkg() { return pkg; }
        @Nullable public String getName() { return name; }
        @Nullable public List<DepKindInfo> getDep_kinds() { return dep_kinds; }

        public Dep copy(String newPkg, List<DepKindInfo> newDepKinds) {
            return new Dep(newPkg, name, newDepKinds);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dep)) return false;
            Dep that = (Dep) o;
            return Objects.equals(pkg, that.pkg) &&
                Objects.equals(name, that.name) &&
                Objects.equals(dep_kinds, that.dep_kinds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pkg, name, dep_kinds);
        }
    }

    // ---- DepKindInfo ----

    public static final class DepKindInfo {
        @Nullable private final String kind;
        @Nullable private final String target;

        public DepKindInfo(@Nullable String kind, @Nullable String target) {
            this.kind = kind;
            this.target = target;
        }

        @Nullable public String getKind() { return kind; }
        @Nullable public String getTarget() { return target; }

        public CargoWorkspace.DepKindInfo clean() {
            CargoWorkspace.DepKind depKind;
            if ("dev".equals(kind)) {
                depKind = CargoWorkspace.DepKind.Development;
            } else if ("build".equals(kind)) {
                depKind = CargoWorkspace.DepKind.Build;
            } else {
                depKind = CargoWorkspace.DepKind.Normal;
            }
            return new CargoWorkspace.DepKindInfo(depKind, target);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DepKindInfo)) return false;
            DepKindInfo that = (DepKindInfo) o;
            return Objects.equals(kind, that.kind) && Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, target);
        }
    }

    // ---- clean method ----

    public static CargoWorkspaceData clean(Project project) {
        return clean(project, null);
    }

    public static CargoWorkspaceData clean(
        Project project,
        @Nullable BuildMessages buildMessages
    ) {
        LocalFileSystem fs = LocalFileSystem.getInstance();
        VirtualFile workspaceRoot = fs.refreshAndFindFileByPath(project.getWorkspace_root());
        if (workspaceRoot == null) {
            throw new IllegalArgumentException(
                "`cargo metadata` reported a workspace path which does not exist at `" + project.getWorkspace_root() + "`"
            );
        }

        List<String> members = project.getWorkspace_members();
        Map<String, ResolveNode> packageIdToNode = project.getResolve().getNodes().stream()
            .collect(Collectors.toMap(ResolveNode::getId, n -> n));

        List<CargoWorkspaceData.Package> packages = project.getPackages().stream().map(pkg -> {
            ResolveNode resolveNode = packageIdToNode.get(pkg.getId());
            if (resolveNode == null) {
                LOG.error("Could not find package with `id` '" + pkg.getId() + "' in `resolve` section of the `cargo metadata` output.");
            }
            Set<String> enabledFeatures = resolveNode != null && resolveNode.getFeatures() != null
                ? new HashSet<>(resolveNode.getFeatures())
                : Collections.emptySet();
            List<RustcMessage.CompilerMessage> pkgBuildMessages = buildMessages != null
                ? buildMessages.get(pkg.getId())
                : Collections.emptyList();
            return cleanPackage(fs, pkg, members.contains(pkg.getId()), enabledFeatures, pkgBuildMessages);
        }).collect(Collectors.toList());

        Map<String, Set<CargoWorkspaceData.Dependency>> dependencyMap = new HashMap<>();
        for (ResolveNode node : project.getResolve().getNodes()) {
            Set<CargoWorkspaceData.Dependency> dependencySet;
            if (node.getDeps() != null) {
                dependencySet = node.getDeps().stream().map(dep -> {
                    List<CargoWorkspace.DepKindInfo> depKindsLowered;
                    if (dep.getDep_kinds() != null) {
                        depKindsLowered = dep.getDep_kinds().stream().map(DepKindInfo::clean).collect(Collectors.toList());
                    } else {
                        depKindsLowered = List.of(new CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Unclassified, null));
                    }
                    return new CargoWorkspaceData.Dependency(dep.getPkg(), dep.getName(), depKindsLowered);
                }).collect(Collectors.toSet());
            } else {
                dependencySet = node.getDependencies().stream()
                    .map(id -> new CargoWorkspaceData.Dependency(id, null, List.of(new CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Unclassified, null))))
                    .collect(Collectors.toSet());
            }
            dependencyMap.put(node.getId(), dependencySet);
        }

        Map<String, List<RawDependency>> rawDeps = project.getPackages().stream()
            .collect(Collectors.toMap(Package::getId, Package::getDependencies));

        return new CargoWorkspaceData(packages, dependencyMap, rawDeps, workspaceRoot.getUrl());
    }

    private static CargoWorkspaceData.Package cleanPackage(
        LocalFileSystem fs,
        Package pkg,
        boolean isWorkspaceMember,
        Set<String> enabledFeatures,
        List<RustcMessage.CompilerMessage> buildMessagesList
    ) {
        String rootPath = PathUtil.getParentPath(pkg.getManifest_path());
        VirtualFile root = fs.refreshAndFindFileByPath(rootPath);
        if (root != null && !isWorkspaceMember) {
            root = root.getCanonicalFile();
        }
        if (root == null) {
            throw new CargoMetadataException(
                "`cargo metadata` reported a package which does not exist at `" + pkg.getManifest_path() + "`"
            );
        }

        Map<String, List<String>> features = new HashMap<>(pkg.getFeatures());

        // Backcompat Cargo 1.59.0: optional dependencies are features implicitly.
        Set<String> allFeatureDependencies = features.values().stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        for (RawDependency dependency : pkg.getDependencies()) {
            String featureName = dependency.getRename() != null ? dependency.getRename() : dependency.getName();
            if (dependency.isOptional() && !features.containsKey(featureName)) {
                String depFeatureName = "dep:" + featureName;
                if (!allFeatureDependencies.contains(depFeatureName)) {
                    features.put(featureName, List.of(depFeatureName));
                }
            }
        }

        RustcMessage.BuildScriptMessage buildScriptMessage = null;
        for (RustcMessage.CompilerMessage msg : buildMessagesList) {
            if (msg instanceof RustcMessage.BuildScriptMessage) {
                buildScriptMessage = (RustcMessage.BuildScriptMessage) msg;
                break;
            }
        }

        CargoWorkspaceData.ProcMacroArtifact procMacroArtifact = getProcMacroArtifact(buildMessagesList);

        CfgOptions cfgOptions = buildScriptMessage != null && buildScriptMessage.getCfgs() != null
            ? CfgOptions.parse(buildScriptMessage.getCfgs())
            : null;

        Map<String, String> envFromBuildscript = new HashMap<>();
        if (buildScriptMessage != null && buildScriptMessage.getEnv() != null) {
            for (List<String> entry : buildScriptMessage.getEnv()) {
                if (entry.size() == 2) {
                    envFromBuildscript.put(entry.get(0), entry.get(1));
                }
            }
        }

        SemVer semver = SemVer.parseFromText(pkg.getVersion());

        Map<String, String> env = new LinkedHashMap<>(envFromBuildscript);
        env.put("CARGO_MANIFEST_DIR", rootPath);
        env.put("CARGO", "cargo");
        env.put("CARGO_PKG_VERSION", pkg.getVersion());
        env.put("CARGO_PKG_VERSION_MAJOR", semver != null ? String.valueOf(semver.getMajor()) : "");
        env.put("CARGO_PKG_VERSION_MINOR", semver != null ? String.valueOf(semver.getMinor()) : "");
        env.put("CARGO_PKG_VERSION_PATCH", semver != null ? String.valueOf(semver.getPatch()) : "");
        env.put("CARGO_PKG_VERSION_PRE", semver != null && semver.getPreRelease() != null ? semver.getPreRelease() : "");
        env.put("CARGO_PKG_AUTHORS", String.join(";", pkg.getAuthors()));
        env.put("CARGO_PKG_NAME", pkg.getName());
        env.put("CARGO_PKG_DESCRIPTION", pkg.getDescription() != null ? pkg.getDescription() : "");
        env.put("CARGO_PKG_REPOSITORY", pkg.getRepository() != null ? pkg.getRepository() : "");
        env.put("CARGO_PKG_LICENSE", pkg.getLicense() != null ? pkg.getLicense() : "");
        env.put("CARGO_PKG_LICENSE_FILE", pkg.getLicense_file() != null ? pkg.getLicense_file() : "");
        env.put("CARGO_CRATE_NAME", pkg.getName().replace('-', '_'));

        String outDirUrl = null;
        if (buildScriptMessage != null && buildScriptMessage.getOutDir() != null) {
            VirtualFile outDir = root.getFileSystem().refreshAndFindFileByPath(buildScriptMessage.getOutDir());
            if (outDir != null) {
                if (!isWorkspaceMember) {
                    outDir = outDir.getCanonicalFile();
                }
                if (outDir != null) {
                    outDirUrl = outDir.getUrl();
                }
            }
        }

        final VirtualFile finalRoot = root;
        List<CargoWorkspaceData.Target> targets = pkg.getTargets().stream()
            .map(t -> cleanTarget(finalRoot, t, isWorkspaceMember))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return new CargoWorkspaceData.Package(
            pkg.getId(),
            root.getUrl(),
            pkg.getName(),
            pkg.getVersion(),
            targets,
            pkg.getSource(),
            isWorkspaceMember ? PackageOrigin.WORKSPACE : PackageOrigin.DEPENDENCY,
            cleanEdition(pkg.getEdition()),
            features,
            enabledFeatures,
            cfgOptions,
            env,
            outDirUrl,
            procMacroArtifact
        );
    }

    @Nullable
    private static CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact(List<RustcMessage.CompilerMessage> buildMessages) {
        List<String> DYNAMIC_LIBRARY_EXTENSIONS = List.of(".dll", ".so", ".dylib");

        List<RustcMessage.CompilerArtifactMessage> procMacroArtifacts = buildMessages.stream()
            .filter(m -> m instanceof RustcMessage.CompilerArtifactMessage)
            .map(m -> (RustcMessage.CompilerArtifactMessage) m)
            .filter(m -> m.getTarget().getKind().contains("proc-macro") && m.getTarget().getCrate_types().contains("proc-macro"))
            .collect(Collectors.toList());

        String procMacroArtifactPath = procMacroArtifacts.stream()
            .flatMap(m -> m.getFilenames().stream())
            .filter(file -> DYNAMIC_LIBRARY_EXTENSIONS.stream().anyMatch(file::endsWith))
            .findFirst()
            .orElse(null);

        if (procMacroArtifactPath == null) return null;

        Path originPath = Path.of(procMacroArtifactPath);
        HashCode hash;
        try {
            hash = HashCode.ofFile(originPath);
        } catch (IOException e) {
            LOG.warn(e);
            return null;
        }

        Path path = copyProcMacroArtifactToTempDir(originPath, hash);
        return new CargoWorkspaceData.ProcMacroArtifact(path, hash);
    }

    private static synchronized Path copyProcMacroArtifactToTempDir(Path originPath, HashCode hash) {
        try {
            Path temp = RsPathManager.INSTANCE.tempPluginDirInSystem().resolve("proc_macros");
            Files.createDirectories(temp);
            String filename = originPath.getFileName().toString();
            String extension = PathUtil.getFileExtension(filename);
            Path targetPath = temp.resolve(filename + "." + hash + "." + extension);
            if (!Files.exists(targetPath) || Files.size(originPath) != Files.size(targetPath)) {
                Files.copy(originPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return targetPath;
        } catch (IOException e) {
            LOG.warn(e);
            return originPath;
        }
    }

    @Nullable
    private static CargoWorkspaceData.Target cleanTarget(VirtualFile root, Target target, boolean isWorkspaceMember) {
        VirtualFile mainFile = OpenApiUtil.findFileByMaybeRelativePath(root, target.getSrc_path());
        if (mainFile != null && !isWorkspaceMember) {
            mainFile = mainFile.getCanonicalFile();
        }
        if (mainFile == null) return null;

        return new CargoWorkspaceData.Target(
            mainFile.getUrl(),
            target.getName(),
            makeTargetKind(target.getCleanKind(), target.getCleanCrateTypes()),
            cleanEdition(target.getEdition()),
            target.getDoctest() != null ? target.getDoctest() : true,
            target.getRequired_features() != null ? target.getRequired_features() : Collections.emptyList()
        );
    }

    private static CargoWorkspace.TargetKind makeTargetKind(TargetKind target, List<CrateType> crateTypes) {
        switch (target) {
            case LIB: return new CargoWorkspace.TargetKind.Lib(toLibKinds(crateTypes));
            case BIN: return CargoWorkspace.TargetKind.Bin.INSTANCE;
            case TEST: return CargoWorkspace.TargetKind.Test.INSTANCE;
            case EXAMPLE:
                if (crateTypes.contains(CrateType.BIN)) {
                    return CargoWorkspace.TargetKind.ExampleBin.INSTANCE;
                } else {
                    return new CargoWorkspace.TargetKind.ExampleLib(toLibKinds(crateTypes));
                }
            case BENCH: return CargoWorkspace.TargetKind.Bench.INSTANCE;
            case CUSTOM_BUILD: return CargoWorkspace.TargetKind.CustomBuild.INSTANCE;
            default: return CargoWorkspace.TargetKind.Unknown.INSTANCE;
        }
    }

    private static EnumSet<LibKind> toLibKinds(List<CrateType> crateTypes) {
        List<LibKind> kinds = crateTypes.stream().map(ct -> {
            switch (ct) {
                case LIB: return LibKind.LIB;
                case DYLIB: return LibKind.DYLIB;
                case STATICLIB: return LibKind.STATICLIB;
                case CDYLIB: return LibKind.CDYLIB;
                case RLIB: return LibKind.RLIB;
                case PROC_MACRO: return LibKind.PROC_MACRO;
                default: return LibKind.UNKNOWN;
            }
        }).collect(Collectors.toList());
        return EnumSet.copyOf(kinds);
    }

    private static Edition cleanEdition(@Nullable String edition) {
        if (Edition.EDITION_2015.getPresentation().equals(edition)) return Edition.EDITION_2015;
        if (Edition.EDITION_2018.getPresentation().equals(edition)) return Edition.EDITION_2018;
        if (Edition.EDITION_2021.getPresentation().equals(edition)) return Edition.EDITION_2021;
        return Edition.EDITION_2015;
    }
}
