/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CfgOptions;
import org.rust.openapiext.CachedVirtualFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

final class PackageImpl extends UserDataHolderBase implements CargoWorkspace.Package {

    private final WorkspaceImpl myWorkspace;
    private final String myId;
    // Note: In tests, we use in-memory file system,
    // so we can't use Path here.
    private final String myContentRootUrl;
    private final String myName;
    private final String myVersion;
    private final List<TargetImpl> myTargets;
    @Nullable
    private final String mySource;
    private PackageOrigin myOrigin;
    private final CargoWorkspace.Edition myEdition;
    @Nullable
    private final CfgOptions myCfgOptions;
    /** See {@link org.rust.cargo.toolchain.impl.CargoMetadata.Package#features} */
    private final Map<String, List<String>> myRawFeatures;
    private final Set<String> myCargoEnabledFeatures;
    private final Map<String, String> myEnv;
    @Nullable
    private final String myOutDirUrl;
    @Nullable
    private final CargoWorkspaceData.ProcMacroArtifact myProcMacroArtifact;

    private final CachedVirtualFile myContentRootCache;
    private final CachedVirtualFile myOutDirCache;
    private final ArrayList<DependencyImpl> myDependencies;
    private final Set<PackageFeature> myFeatures;

    PackageImpl(
        WorkspaceImpl workspace,
        String id,
        String contentRootUrl,
        String name,
        String version,
        Collection<CargoWorkspaceData.Target> targetsData,
        @Nullable String source,
        PackageOrigin origin,
        CargoWorkspace.Edition edition,
        @Nullable CfgOptions cfgOptions,
        Map<String, List<String>> rawFeatures,
        Set<String> cargoEnabledFeatures,
        Map<String, String> env,
        @Nullable String outDirUrl,
        @Nullable CargoWorkspaceData.ProcMacroArtifact procMacroArtifact
    ) {
        myWorkspace = workspace;
        myId = id;
        myContentRootUrl = contentRootUrl;
        myName = name;
        myVersion = version;
        mySource = source;
        myOrigin = origin;
        myEdition = edition;
        myCfgOptions = cfgOptions;
        myRawFeatures = rawFeatures;
        myCargoEnabledFeatures = cargoEnabledFeatures;
        myEnv = env;
        myOutDirUrl = outDirUrl;
        myProcMacroArtifact = procMacroArtifact;

        myContentRootCache = new CachedVirtualFile(contentRootUrl);
        myOutDirCache = new CachedVirtualFile(outDirUrl);
        myDependencies = new ArrayList<>();

        List<TargetImpl> targets = new ArrayList<>();
        for (CargoWorkspaceData.Target t : targetsData) {
            targets.add(new TargetImpl(
                this,
                t.getCrateRootUrl(),
                t.getName(),
                t.getKind(),
                t.getEdition(),
                t.getDoctest(),
                t.getRequiredFeatures()
            ));
        }
        myTargets = targets;

        Set<PackageFeature> features = new HashSet<>();
        for (String featureName : rawFeatures.keySet()) {
            features.add(new PackageFeature(this, featureName));
        }
        myFeatures = features;
    }

    @Override
    @Nullable
    public VirtualFile getContentRoot() {
        return myContentRootCache.getValue();
    }

    @Override
    public Path getRootDirectory() {
        return Paths.get(VirtualFileManager.extractPath(myContentRootUrl));
    }

    @Override
    public String getId() { return myId; }

    @Override
    public String getName() { return myName; }

    @Override
    public String getVersion() { return myVersion; }

    @Override
    @Nullable
    public String getSource() { return mySource; }

    @Override
    public PackageOrigin getOrigin() { return myOrigin; }

    void setOrigin(PackageOrigin origin) { myOrigin = origin; }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<CargoWorkspace.Target> getTargets() {
        return (Collection<CargoWorkspace.Target>) (Collection<?>) myTargets;
    }

    List<TargetImpl> getTargetsInternal() { return myTargets; }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<CargoWorkspace.Dependency> getDependencies() {
        return (Collection<CargoWorkspace.Dependency>) (Collection<?>) myDependencies;
    }

    ArrayList<DependencyImpl> getDependenciesInternal() { return myDependencies; }

    @Override
    @Nullable
    public CfgOptions getCfgOptions() { return myCfgOptions; }

    @Override
    public Set<PackageFeature> getFeatures() { return myFeatures; }

    @Override
    public CargoWorkspace getWorkspace() { return myWorkspace; }

    WorkspaceImpl getWorkspaceImpl() { return myWorkspace; }

    @Override
    public CargoWorkspace.Edition getEdition() { return myEdition; }

    @Override
    public Map<String, String> getEnv() { return myEnv; }

    @Override
    @Nullable
    public VirtualFile getOutDir() { return myOutDirCache.getValue(); }

    @Override
    public Map<String, FeatureState> getFeatureState() {
        Map<String, FeatureState> state = myWorkspace.getFeaturesState().get(getRootDirectory());
        return state != null ? state : Collections.emptyMap();
    }

    @Override
    @Nullable
    public CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact() { return myProcMacroArtifact; }

    String getContentRootUrl() { return myContentRootUrl; }

    Map<String, List<String>> getRawFeatures() { return myRawFeatures; }

    Set<String> getCargoEnabledFeatures() { return myCargoEnabledFeatures; }

    @Nullable
    String getOutDirUrl() { return myOutDirUrl; }

    CargoWorkspaceData.Package asPackageData(@Nullable CargoWorkspace.Edition edition) {
        List<CargoWorkspaceData.Target> targetData = new ArrayList<>();
        for (TargetImpl t : myTargets) {
            targetData.add(new CargoWorkspaceData.Target(
                t.getCrateRootUrl(),
                t.getName(),
                t.getKind(),
                edition != null ? edition : t.getEdition(),
                t.getDoctest(),
                t.getRequiredFeatures()
            ));
        }
        return new CargoWorkspaceData.Package(
            myId,
            myContentRootUrl,
            myName,
            myVersion,
            targetData,
            mySource,
            myOrigin,
            edition != null ? edition : myEdition,
            myRawFeatures,
            myCargoEnabledFeatures,
            myCfgOptions,
            myEnv,
            myOutDirUrl,
            myProcMacroArtifact
        );
    }

    @Override
    public String toString() {
        return "Package(name='" + myName + "', contentRootUrl='" + myContentRootUrl + "', outDirUrl='" + myOutDirUrl + "')";
    }
}
