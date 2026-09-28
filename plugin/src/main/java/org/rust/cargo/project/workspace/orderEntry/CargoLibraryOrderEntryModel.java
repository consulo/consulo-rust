/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace.orderEntry;

import consulo.content.RootProvider;
import consulo.content.RootProviderBase;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.orderEntry.CustomOrderEntryModel;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.rust.bundle.RustBundleType;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoLibraries;
import org.rust.cargo.project.workspace.CargoLibrary;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.StandardLibrary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Puts the sources of one {@link CargoLibrary} on a module, the way a library order entry puts a
 * library on it.
 * <p>
 * The entry carries its own roots rather than looking them up in the Cargo workspace, so that a
 * project that has not been synced yet still resolves: the module file is the record of what the last
 * sync found. A workspace that later resolves to different roots shows up as a change of the entry
 * through {@link #isEquivalentTo} and makes the platform reindex.
 */
public class CargoLibraryOrderEntryModel implements CustomOrderEntryModel {

    private static final Logger LOG = Logger.getInstance(CargoLibraryOrderEntryModel.class);

    private final CargoLibrary.Kind myKind;
    private final String myId;
    private final String myName;
    @Nullable
    private final String myVersion;

    /** Manifest of the Cargo project this library belongs to, the stable identity of that project. */
    @Nullable
    private final String myManifestPath;

    /**
     * The roots as written to the module file. {@code null} for an entry written before the roots were
     * stored, which falls back to a lookup in the workspace.
     */
    @Nullable
    private final List<String> mySourceRootUrls;
    @Nullable
    private final List<String> myExcludedRootUrls;

    @Nullable
    private volatile Set<VirtualFile> mySourceRoots;
    @Nullable
    private volatile Set<VirtualFile> myExcludedRoots;

    @Nullable
    private ModuleRootLayer myModuleRootLayer;

    private final MyRootProvider myRootProvider = new MyRootProvider();

    /**
     * Creates an entry as read back from a module file.
     */
    public CargoLibraryOrderEntryModel(
        @Nonnull CargoLibrary.Kind kind,
        @Nonnull String id,
        @Nonnull String name,
        @Nullable String version,
        @Nullable String manifestPath,
        @Nullable List<String> sourceRootUrls,
        @Nullable List<String> excludedRootUrls
    ) {
        myKind = kind;
        myId = id;
        myName = name;
        myVersion = version;
        myManifestPath = manifestPath;
        mySourceRootUrls = sourceRootUrls == null ? null : List.copyOf(sourceRootUrls);
        myExcludedRootUrls = excludedRootUrls == null ? null : List.copyOf(excludedRootUrls);
    }

    /**
     * Creates an entry for a library of the current workspace.
     */
    public CargoLibraryOrderEntryModel(@Nonnull CargoLibrary library, @Nullable String manifestPath) {
        this(
            library.getKind(),
            library.getId(),
            library.getName(),
            library.getVersion(),
            manifestPath,
            urlsOf(library.getSourceRoots()),
            urlsOf(effectiveExcludedRoots(library))
        );
    }

    @Nonnull
    public CargoLibrary.Kind getKind() {
        return myKind;
    }

    @Nonnull
    public String getId() {
        return myId;
    }

    @Nonnull
    public String getName() {
        return myName;
    }

    @Nullable
    public String getVersion() {
        return myVersion;
    }

    @Nullable
    public String getManifestPath() {
        return myManifestPath;
    }

    @Nullable
    public List<String> getSourceRootUrls() {
        return mySourceRootUrls;
    }

    @Nullable
    public List<String> getExcludedRootUrls() {
        return myExcludedRootUrls;
    }

    @Override
    public void bind(@Nonnull ModuleRootLayer moduleRootLayer) {
        myModuleRootLayer = moduleRootLayer;
    }

    @Nonnull
    @Override
    public String getPresentableName() {
        return myVersion != null ? myName + " " + myVersion : myName;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * The entry follows {@code Cargo.toml} rather than the classpath editor, so it cannot be removed
     * by hand.
     */
    @Override
    public boolean isSynthetic() {
        return true;
    }

    @Nonnull
    @Override
    public RootProvider getRootProvider() {
        return myRootProvider;
    }

    @Nonnull
    @Override
    public VirtualFile[] getExcludedRoots() {
        return VirtualFileUtil.toVirtualFileArray(excludedRoots());
    }

    /**
     * Per package, not per Cargo project: the same crate reached from several modules is indexed once.
     * The manifest path is deliberately left out - see {@link #getManifestPath()}.
     */
    @Nullable
    @Override
    public Object getEqualObject() {
        return myKind.name() + " " + myId;
    }

    @Override
    public boolean isEquivalentTo(@Nonnull CustomOrderEntryModel model) {
        if (!(model instanceof CargoLibraryOrderEntryModel other)) return false;
        return myKind == other.myKind
            && myId.equals(other.myId)
            && myName.equals(other.myName)
            && Objects.equals(myVersion, other.myVersion)
            && Objects.equals(myManifestPath, other.myManifestPath)
            && sourceRoots().equals(other.sourceRoots())
            && excludedRoots().equals(other.excludedRoots());
    }

    @Nonnull
    @Override
    public CargoLibraryOrderEntryModel clone() {
        return new CargoLibraryOrderEntryModel(
            myKind, myId, myName, myVersion, myManifestPath, mySourceRootUrls, myExcludedRootUrls
        );
    }

    @Nonnull
    private Set<VirtualFile> sourceRoots() {
        Set<VirtualFile> roots = mySourceRoots;
        if (roots == null) {
            if (myKind == CargoLibrary.Kind.STDLIB) {
                roots = stdlibRoots();
            }
            else {
                roots = mySourceRootUrls != null ? resolve(mySourceRootUrls) : resolveFromWorkspace(true);
            }
            mySourceRoots = roots;
        }
        return roots;
    }

    /**
     * Locates a standard library crate under the bundle the module is bound to, from its name alone.
     * <p>
     * Nothing about the toolchain is written into the module file, so the entry survives a move of the
     * toolchain and follows the module to another bundle. An absent or incomplete bundle simply yields no
     * roots: the crate stops resolving until a toolchain is selected, which is the honest answer.
     */
    @Nonnull
    private Set<VirtualFile> stdlibRoots() {
        ModuleRootLayer layer = myModuleRootLayer;
        if (layer == null) return diagnose("no layer bound");
        RustModuleExtension extension = layer.getExtension(RustModuleExtension.class);
        if (extension == null) return diagnose("no Rust module extension");
        VirtualFile srcDir = RustBundleType.stdlibSrcDir(extension.getSdk());
        if (srcDir == null) return diagnose("no stdlib sources on bundle " + extension.getSdk());

        VirtualFile crateDir = StandardLibrary.findFirstFileByRelativePaths(srcDir, List.of(myName, "lib" + myName));
        if (crateDir == null) return diagnose("no crate directory under " + srcDir.getPath());
        crateDir = crateDir.getCanonicalFile();
        if (crateDir == null) return diagnose("crate directory has no canonical form");

        Set<VirtualFile> roots = new LinkedHashSet<>();
        roots.add(crateDir);
        roots.addAll(CargoWorkspace.stdlibAdditionalRoots(myName, crateDir));
        LOG.debug("stdlib crate " + myName + " -> " + roots.size() + " root(s)");
        return Collections.unmodifiableSet(roots);
    }

    @Nonnull
    private Set<VirtualFile> diagnose(@Nonnull String reason) {
        LOG.debug("stdlib crate " + myName + " has no roots: " + reason);
        return Set.of();
    }

    @Nonnull
    private Set<VirtualFile> excludedRoots() {
        Set<VirtualFile> roots = myExcludedRoots;
        if (roots == null) {
            if (myKind == CargoLibrary.Kind.STDLIB) {
                roots = Set.of();
            }
            else {
                roots = myExcludedRootUrls != null ? resolve(myExcludedRootUrls) : resolveFromWorkspace(false);
            }
            myExcludedRoots = roots;
        }
        return roots;
    }

    @Nonnull
    private static Set<VirtualFile> resolve(@Nonnull List<String> urls) {
        if (urls.isEmpty()) return Set.of();
        VirtualFileManager fileManager = VirtualFileManager.getInstance();
        Set<VirtualFile> result = new LinkedHashSet<>();
        for (String url : urls) {
            VirtualFile file = fileManager.findFileByUrl(url);
            if (file != null) {
                result.add(file);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * The path for an entry written before the roots were stored: the coordinates still identify the
     * library inside the resolved workspace, when there is one.
     */
    @Nonnull
    private Set<VirtualFile> resolveFromWorkspace(boolean source) {
        ModuleRootLayer layer = myModuleRootLayer;
        if (layer == null) return Set.of();
        Project project = layer.getProject();
        if (project.isDisposed()) return Set.of();
        CargoLibrary library = CargoLibraries.find(project, myKind, myId);
        if (library == null) return Set.of();
        return source ? library.getSourceRoots() : effectiveExcludedRoots(library);
    }

    /**
     * The exclusions of {@code library} that the platform can act on. An exclusion outside every source
     * root of the entry is kept by the root index but does nothing, so it is not worth writing.
     */
    @Nonnull
    private static Set<VirtualFile> effectiveExcludedRoots(@Nonnull CargoLibrary library) {
        Set<VirtualFile> sourceRoots = library.getSourceRoots();
        Set<VirtualFile> result = new LinkedHashSet<>();
        for (VirtualFile excludedRoot : library.getExcludedRoots()) {
            for (VirtualFile sourceRoot : sourceRoots) {
                if (VirtualFileUtil.isAncestor(sourceRoot, excludedRoot, true)) {
                    result.add(excludedRoot);
                    break;
                }
            }
        }
        return result;
    }

    @Nonnull
    private static List<String> urlsOf(@Nonnull Set<VirtualFile> files) {
        List<String> result = new ArrayList<>(files.size());
        for (VirtualFile file : files) {
            result.add(file.getUrl());
        }
        return result;
    }

    private class MyRootProvider extends RootProviderBase {
        @Nonnull
        @Override
        public String[] getUrls(@Nonnull String rootType) {
            Set<VirtualFile> roots = roots(rootType);
            String[] urls = new String[roots.size()];
            int i = 0;
            for (VirtualFile root : roots) {
                urls[i++] = root.getUrl();
            }
            return urls;
        }

        @Nonnull
        @Override
        public VirtualFile[] getFiles(@Nonnull String rootType) {
            return VirtualFileUtil.toVirtualFileArray(roots(rootType));
        }

        /**
         * The same directories answer both root types. The source root is what the indexer walks, while
         * the search scope a module builds out of its order entries is assembled from the binaries roots
         * of everything that is not a module, so a crate published under sources alone would resolve to
         * nothing.
         */
        @Nonnull
        private Set<VirtualFile> roots(@Nonnull String rootType) {
            return SourcesOrderRootType.ID.equals(rootType) || BinariesOrderRootType.ID.equals(rootType)
                ? sourceRoots() : Set.of();
        }
    }
}
