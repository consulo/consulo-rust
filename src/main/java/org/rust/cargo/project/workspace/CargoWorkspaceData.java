/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.toolchain.impl.CargoMetadata;
import org.rust.stdext.HashCode;

import java.nio.file.Path;
import java.util.*;

/**
 * A POD-style representation of {@link CargoWorkspace} used as an intermediate representation
 * between {@code cargo metadata} JSON and {@link CargoWorkspace} object graph.
 * <p>
 * Dependency graph is represented via adjacency list, where {@code Index} is the order of a particular
 * package in {@code packages} list.
 */
public final class CargoWorkspaceData {

    private final List<Package> myPackages;
    /** Resolved dependencies with package IDs in values (instead of just names and versions) */
    private final Map<String, Set<Dependency>> myDependencies;
    /** Dependencies as they listed in the package Cargo.toml, without package resolution or any additional data */
    private final Map<String, List<CargoMetadata.RawDependency>> myRawDependencies;
    @Nullable
    private final String myWorkspaceRootUrl;

    public CargoWorkspaceData(
        List<Package> packages,
        Map<String, Set<Dependency>> dependencies,
        Map<String, List<CargoMetadata.RawDependency>> rawDependencies,
        @Nullable String workspaceRootUrl
    ) {
        myPackages = packages;
        myDependencies = dependencies;
        myRawDependencies = rawDependencies;
        myWorkspaceRootUrl = workspaceRootUrl;
    }

    public CargoWorkspaceData(
        List<Package> packages,
        Map<String, Set<Dependency>> dependencies,
        Map<String, List<CargoMetadata.RawDependency>> rawDependencies
    ) {
        this(packages, dependencies, rawDependencies, null);
    }

    public List<Package> getPackages() {
        return myPackages;
    }

    public Map<String, Set<Dependency>> getDependencies() {
        return myDependencies;
    }

    public Map<String, List<CargoMetadata.RawDependency>> getRawDependencies() {
        return myRawDependencies;
    }

    @Nullable
    public String getWorkspaceRootUrl() {
        return myWorkspaceRootUrl;
    }

    public CargoWorkspaceData copy(
        List<Package> packages,
        Map<String, Set<Dependency>> dependencies,
        Map<String, List<CargoMetadata.RawDependency>> rawDependencies,
        @Nullable String workspaceRootUrl
    ) {
        return new CargoWorkspaceData(packages, dependencies, rawDependencies, workspaceRootUrl);
    }

    public CargoWorkspaceData copyWithPackages(List<Package> packages) {
        return new CargoWorkspaceData(packages, myDependencies, myRawDependencies, myWorkspaceRootUrl);
    }

    public CargoWorkspaceData copyWithDependencies(Map<String, Set<Dependency>> dependencies) {
        return new CargoWorkspaceData(myPackages, dependencies, myRawDependencies, myWorkspaceRootUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CargoWorkspaceData)) return false;
        CargoWorkspaceData that = (CargoWorkspaceData) o;
        return Objects.equals(myPackages, that.myPackages) &&
            Objects.equals(myDependencies, that.myDependencies) &&
            Objects.equals(myRawDependencies, that.myRawDependencies) &&
            Objects.equals(myWorkspaceRootUrl, that.myWorkspaceRootUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myPackages, myDependencies, myRawDependencies, myWorkspaceRootUrl);
    }

    @Override
    public String toString() {
        return "CargoWorkspaceData(" +
            "packages=" + myPackages +
            ", dependencies=" + myDependencies +
            ", rawDependencies=" + myRawDependencies +
            ", workspaceRootUrl=" + myWorkspaceRootUrl +
            ")";
    }

    public static final class Package {
        private final String myId;
        private final String myContentRootUrl;
        private final String myName;
        private final String myVersion;
        private final Collection<Target> myTargets;
        @Nullable
        private final String mySource;
        private final PackageOrigin myOrigin;
        private final CargoWorkspace.Edition myEdition;
        /** All features available in this package (including optional dependencies) */
        private final Map<String, List<String>> myFeatures;
        /** Enabled features (from Cargo point of view) */
        private final Set<String> myEnabledFeatures;
        @Nullable
        private final CfgOptions myCfgOptions;
        private final Map<String, String> myEnv;
        @Nullable
        private final String myOutDirUrl;
        @Nullable
        private final ProcMacroArtifact myProcMacroArtifact;

        public Package(
            String id,
            String contentRootUrl,
            String name,
            String version,
            Collection<Target> targets,
            @Nullable String source,
            PackageOrigin origin,
            CargoWorkspace.Edition edition,
            Map<String, List<String>> features,
            Set<String> enabledFeatures,
            @Nullable CfgOptions cfgOptions,
            Map<String, String> env,
            @Nullable String outDirUrl,
            @Nullable ProcMacroArtifact procMacroArtifact
        ) {
            myId = id;
            myContentRootUrl = contentRootUrl;
            myName = name;
            myVersion = version;
            myTargets = targets;
            mySource = source;
            myOrigin = origin;
            myEdition = edition;
            myFeatures = features;
            myEnabledFeatures = enabledFeatures;
            myCfgOptions = cfgOptions;
            myEnv = env;
            myOutDirUrl = outDirUrl;
            myProcMacroArtifact = procMacroArtifact;
        }

        public Package(
            String id,
            String contentRootUrl,
            String name,
            String version,
            Collection<Target> targets,
            @Nullable String source,
            PackageOrigin origin,
            CargoWorkspace.Edition edition,
            Map<String, List<String>> features,
            Set<String> enabledFeatures,
            @Nullable CfgOptions cfgOptions,
            Map<String, String> env,
            @Nullable String outDirUrl
        ) {
            this(id, contentRootUrl, name, version, targets, source, origin, edition,
                features, enabledFeatures, cfgOptions, env, outDirUrl, null);
        }

        public String getId() { return myId; }
        public String getContentRootUrl() { return myContentRootUrl; }
        public String getName() { return myName; }
        public String getVersion() { return myVersion; }
        public Collection<Target> getTargets() { return myTargets; }
        @Nullable public String getSource() { return mySource; }
        public PackageOrigin getOrigin() { return myOrigin; }
        public CargoWorkspace.Edition getEdition() { return myEdition; }
        public Map<String, List<String>> getFeatures() { return myFeatures; }
        public Set<String> getEnabledFeatures() { return myEnabledFeatures; }
        @Nullable public CfgOptions getCfgOptions() { return myCfgOptions; }
        public Map<String, String> getEnv() { return myEnv; }
        @Nullable public String getOutDirUrl() { return myOutDirUrl; }
        @Nullable public ProcMacroArtifact getProcMacroArtifact() { return myProcMacroArtifact; }

        public Package copy(
            String id,
            String contentRootUrl,
            String name,
            String version,
            Collection<Target> targets,
            @Nullable String source,
            PackageOrigin origin,
            CargoWorkspace.Edition edition,
            Map<String, List<String>> features,
            Set<String> enabledFeatures,
            @Nullable CfgOptions cfgOptions,
            Map<String, String> env,
            @Nullable String outDirUrl,
            @Nullable ProcMacroArtifact procMacroArtifact
        ) {
            return new Package(id, contentRootUrl, name, version, targets, source, origin, edition,
                features, enabledFeatures, cfgOptions, env, outDirUrl, procMacroArtifact);
        }

        public Package copyWithId(String id) {
            return copy(id, myContentRootUrl, myName, myVersion, myTargets, mySource, myOrigin, myEdition,
                myFeatures, myEnabledFeatures, myCfgOptions, myEnv, myOutDirUrl, myProcMacroArtifact);
        }

        public Package copyWithOrigin(PackageOrigin origin) {
            return copy(myId, myContentRootUrl, myName, myVersion, myTargets, mySource, origin, myEdition,
                myFeatures, myEnabledFeatures, myCfgOptions, myEnv, myOutDirUrl, myProcMacroArtifact);
        }

        public Package copyWithTargetsAndEdition(Collection<Target> targets, CargoWorkspace.Edition edition) {
            return copy(myId, myContentRootUrl, myName, myVersion, targets, mySource, myOrigin, edition,
                myFeatures, myEnabledFeatures, myCfgOptions, myEnv, myOutDirUrl, myProcMacroArtifact);
        }

        public Package copyWithFeaturesAndEnabledFeatures(Map<String, List<String>> features, Set<String> enabledFeatures) {
            return copy(myId, myContentRootUrl, myName, myVersion, myTargets, mySource, myOrigin, myEdition,
                features, enabledFeatures, myCfgOptions, myEnv, myOutDirUrl, myProcMacroArtifact);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Package)) return false;
            Package that = (Package) o;
            return Objects.equals(myId, that.myId) &&
                Objects.equals(myContentRootUrl, that.myContentRootUrl) &&
                Objects.equals(myName, that.myName) &&
                Objects.equals(myVersion, that.myVersion) &&
                Objects.equals(myTargets, that.myTargets) &&
                Objects.equals(mySource, that.mySource) &&
                myOrigin == that.myOrigin &&
                myEdition == that.myEdition &&
                Objects.equals(myFeatures, that.myFeatures) &&
                Objects.equals(myEnabledFeatures, that.myEnabledFeatures) &&
                Objects.equals(myCfgOptions, that.myCfgOptions) &&
                Objects.equals(myEnv, that.myEnv) &&
                Objects.equals(myOutDirUrl, that.myOutDirUrl) &&
                Objects.equals(myProcMacroArtifact, that.myProcMacroArtifact);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myId, myContentRootUrl, myName, myVersion, myTargets, mySource, myOrigin,
                myEdition, myFeatures, myEnabledFeatures, myCfgOptions, myEnv, myOutDirUrl, myProcMacroArtifact);
        }

        @Override
        public String toString() {
            return "Package(id=" + myId + ", name=" + myName + ", version=" + myVersion + ")";
        }
    }

    public static final class Target {
        private final String myCrateRootUrl;
        private final String myName;
        private final CargoWorkspace.TargetKind myKind;
        private final CargoWorkspace.Edition myEdition;
        private final boolean myDoctest;
        private final List<String> myRequiredFeatures;

        public Target(
            String crateRootUrl,
            String name,
            CargoWorkspace.TargetKind kind,
            CargoWorkspace.Edition edition,
            boolean doctest,
            List<String> requiredFeatures
        ) {
            myCrateRootUrl = crateRootUrl;
            myName = name;
            myKind = kind;
            myEdition = edition;
            myDoctest = doctest;
            myRequiredFeatures = requiredFeatures;
        }

        public String getCrateRootUrl() { return myCrateRootUrl; }
        public String getName() { return myName; }
        public CargoWorkspace.TargetKind getKind() { return myKind; }
        public CargoWorkspace.Edition getEdition() { return myEdition; }
        public boolean getDoctest() { return myDoctest; }
        public List<String> getRequiredFeatures() { return myRequiredFeatures; }

        public Target copyWithEdition(CargoWorkspace.Edition edition) {
            return new Target(myCrateRootUrl, myName, myKind, edition, myDoctest, myRequiredFeatures);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Target)) return false;
            Target that = (Target) o;
            return myDoctest == that.myDoctest &&
                Objects.equals(myCrateRootUrl, that.myCrateRootUrl) &&
                Objects.equals(myName, that.myName) &&
                Objects.equals(myKind, that.myKind) &&
                myEdition == that.myEdition &&
                Objects.equals(myRequiredFeatures, that.myRequiredFeatures);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myCrateRootUrl, myName, myKind, myEdition, myDoctest, myRequiredFeatures);
        }

        @Override
        public String toString() {
            return "Target(name=" + myName + ", kind=" + myKind + ", crateRootUrl=" + myCrateRootUrl + ")";
        }
    }

    public static final class Dependency {
        private final String myId;
        @Nullable
        private final String myName;
        private final List<CargoWorkspace.DepKindInfo> myDepKinds;

        public Dependency(String id, @Nullable String name, List<CargoWorkspace.DepKindInfo> depKinds) {
            myId = id;
            myName = name;
            myDepKinds = depKinds;
        }

        public Dependency(String id, List<CargoWorkspace.DepKindInfo> depKinds) {
            this(id, null, depKinds);
        }

        public Dependency(String id) {
            this(id, null, Collections.singletonList(new CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Unclassified, null)));
        }

        public String getId() { return myId; }
        @Nullable public String getName() { return myName; }
        public List<CargoWorkspace.DepKindInfo> getDepKinds() { return myDepKinds; }

        public Dependency copyWithId(String id) {
            return new Dependency(id, myName, myDepKinds);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Dependency)) return false;
            Dependency that = (Dependency) o;
            return Objects.equals(myId, that.myId) &&
                Objects.equals(myName, that.myName) &&
                Objects.equals(myDepKinds, that.myDepKinds);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myId, myName, myDepKinds);
        }

        @Override
        public String toString() {
            return "Dependency(id=" + myId + ", name=" + myName + ")";
        }
    }

    public static final class ProcMacroArtifact {
        private final Path myPath;
        private final HashCode myHash;

        public ProcMacroArtifact(Path path, HashCode hash) {
            myPath = path;
            myHash = hash;
        }

        public Path getPath() { return myPath; }
        public HashCode getHash() { return myHash; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProcMacroArtifact)) return false;
            ProcMacroArtifact that = (ProcMacroArtifact) o;
            return Objects.equals(myPath, that.myPath) && Objects.equals(myHash, that.myHash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myPath, myHash);
        }

        @Override
        public String toString() {
            return "ProcMacroArtifact(path=" + myPath + ", hash=" + myHash + ")";
        }
    }
}
