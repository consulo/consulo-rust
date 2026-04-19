/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CfgOptions;
import org.rust.openapiext.CachedVirtualFile;

import java.util.Collections;
import java.util.List;

final class TargetImpl implements CargoWorkspace.Target {

    private final PackageImpl myPkg;
    private final String myCrateRootUrl;
    private final String myName;
    private final CargoWorkspace.TargetKind myKind;
    private final CargoWorkspace.Edition myEdition;
    private final boolean myDoctest;
    private final List<String> myRequiredFeatures;

    private final CachedVirtualFile myCrateRootCache;
    private final CfgOptions myCfgOptions;

    TargetImpl(
        PackageImpl pkg,
        String crateRootUrl,
        String name,
        CargoWorkspace.TargetKind kind,
        CargoWorkspace.Edition edition,
        boolean doctest,
        List<String> requiredFeatures
    ) {
        myPkg = pkg;
        myCrateRootUrl = crateRootUrl;
        myName = name;
        myKind = kind;
        myEdition = edition;
        myDoctest = doctest;
        myRequiredFeatures = requiredFeatures;
        myCrateRootCache = new CachedVirtualFile(crateRootUrl);

        CfgOptions pkgCfgOptions = pkg.getCfgOptions() != null ? pkg.getCfgOptions() : CfgOptions.EMPTY;
        CfgOptions procMacroCfg = kind.isProcMacro()
            ? new CfgOptions(Collections.emptyMap(), Collections.singleton("proc_macro"))
            : CfgOptions.EMPTY;
        myCfgOptions = pkg.getWorkspaceImpl().getCfgOptions().plus(pkgCfgOptions).plus(procMacroCfg);
    }

    @Override
    public String getName() { return myName; }

    @Override
    public CargoWorkspace.TargetKind getKind() { return myKind; }

    @Override
    @Nullable
    public VirtualFile getCrateRoot() { return myCrateRootCache.getValue(); }

    @Override
    public CargoWorkspace.Package getPkg() { return myPkg; }

    PackageImpl getPkgImpl() { return myPkg; }

    @Override
    public CargoWorkspace.Edition getEdition() { return myEdition; }

    @Override
    public boolean getDoctest() { return myDoctest; }

    @Override
    public List<String> getRequiredFeatures() { return myRequiredFeatures; }

    @Override
    public CfgOptions getCfgOptions() { return myCfgOptions; }

    String getCrateRootUrl() { return myCrateRootUrl; }

    @Override
    public String toString() {
        return "Target(name='" + myName + "', kind=" + myKind + ", crateRootUrl='" + myCrateRootUrl + "')";
    }
}
