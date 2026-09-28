/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace.state;

import com.dslplatform.json.CompiledJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain bean mirror of {@link org.rust.cargo.api.workspace.CargoWorkspaceData}, shaped so that
 * {@code consulo.util.xml.serializer.XmlSerializer} can write it into the project model and read it
 * back without any hand-written element mapping.
 * <p>
 * Everything is a public field of a type the serializer understands - string, boolean, enum name,
 * list or map - so the awkward parts of the workspace model are flattened here rather than in a
 * separate mapping layer: a target kind becomes its name plus its library kinds, {@code CfgOptions}
 * becomes two collections, and a proc macro artifact becomes a path and a hash.
 */
@CompiledJson
public class CargoWorkspaceState {

    public List<PackageState> packages = new ArrayList<>();

    /** Package id -> the packages it depends on. */
    public Map<String, List<DependencyState>> dependencies = new LinkedHashMap<>();

    /** Package id -> the dependencies as its {@code Cargo.toml} lists them. */
    public Map<String, List<RawDependencyState>> rawDependencies = new LinkedHashMap<>();

    public String workspaceRootUrl;

    /** Workspace-level cfg options, which live beside the data rather than inside it. */
    public Map<String, List<String>> cfgKeyValueOptions = new LinkedHashMap<>();
    public List<String> cfgNameOptions = new ArrayList<>();
    public boolean hasCfgOptions;

    @CompiledJson
    public static class PackageState {
        public String id;
        public String contentRootUrl;
        public String name;
        public String version;
        public String source;
        /** Name of a {@code PackageOrigin} constant. */
        public String origin;
        /** Name of a {@code CargoWorkspace.Edition} constant. */
        public String edition;
        public String outDirUrl;
        public String procMacroPath;
        public String procMacroHash;

        public List<TargetState> targets = new ArrayList<>();
        public Map<String, List<String>> features = new LinkedHashMap<>();
        public List<String> enabledFeatures = new ArrayList<>();
        public Map<String, List<String>> cfgKeyValueOptions = new LinkedHashMap<>();
        public List<String> cfgNameOptions = new ArrayList<>();
        public Map<String, String> env = new LinkedHashMap<>();

        /** Whether the package had cfg options at all, which is not the same as having none. */
        public boolean hasCfgOptions;
    }

    @CompiledJson
    public static class TargetState {
        public String crateRootUrl;
        public String name;
        /** {@code lib}, {@code bin}, {@code test}, {@code example}, {@code bench}, {@code custom-build} or {@code unknown}. */
        public String kind;
        /** Names of {@code CargoWorkspace.LibKind} constants, for the kinds that carry them. */
        public List<String> libKinds = new ArrayList<>();
        public String edition;
        public boolean doctest;
        public List<String> requiredFeatures = new ArrayList<>();
    }

    @CompiledJson
    public static class DependencyState {
        public String id;
        public String name;
        public List<DepKindState> depKinds = new ArrayList<>();
    }

    @CompiledJson
    public static class DepKindState {
        /** Name of a {@code CargoWorkspace.DepKind} constant. */
        public String kind;
        public String target;
    }

    @CompiledJson
    public static class RawDependencyState {
        public String name;
        public String rename;
        public String kind;
        public String target;
        public boolean optional;
        public boolean usesDefaultFeatures;
        public List<String> features = new ArrayList<>();
    }
}
