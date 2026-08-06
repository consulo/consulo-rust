/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import java.util.Collections;
import java.util.List;
import java.util.Set;

final class DependencyImpl implements CargoWorkspace.Dependency {

    private final PackageImpl myPkg;
    private final String myName;
    private final List<CargoWorkspace.DepKindInfo> myDepKinds;
    private final boolean myIsOptional;
    private final boolean myAreDefaultFeaturesEnabled;
    private final Set<String> myRequiredFeatures;
    private final String myCargoFeatureDependencyPackageName;

    DependencyImpl(
        PackageImpl pkg,
        String name,
        List<CargoWorkspace.DepKindInfo> depKinds,
        boolean isOptional,
        boolean areDefaultFeaturesEnabled,
        Set<String> requiredFeatures,
        String cargoFeatureDependencyPackageName
    ) {
        myPkg = pkg;
        myName = name;
        myDepKinds = depKinds;
        myIsOptional = isOptional;
        myAreDefaultFeaturesEnabled = areDefaultFeaturesEnabled;
        myRequiredFeatures = requiredFeatures;
        myCargoFeatureDependencyPackageName = cargoFeatureDependencyPackageName;
    }

    DependencyImpl(PackageImpl pkg, List<CargoWorkspace.DepKindInfo> depKinds) {
        CargoWorkspace.Target libTarget = pkg.getLibTarget();
        String defaultName = libTarget != null ? libTarget.getNormName() : pkg.getNormName();
        myPkg = pkg;
        myName = defaultName;
        myDepKinds = depKinds;
        myIsOptional = false;
        myAreDefaultFeaturesEnabled = true;
        myRequiredFeatures = Collections.emptySet();
        myCargoFeatureDependencyPackageName = defaultName.equals(libTarget != null ? libTarget.getNormName() : null)
            ? pkg.getName() : defaultName;
    }

    @Override
    public CargoWorkspace.Package getPkg() { return myPkg; }

    PackageImpl getPkgImpl() { return myPkg; }

    @Override
    public String getName() { return myName; }

    @Override
    public String getCargoFeatureDependencyPackageName() { return myCargoFeatureDependencyPackageName; }

    @Override
    public List<CargoWorkspace.DepKindInfo> getDepKinds() { return myDepKinds; }

    @Override
    public Set<String> getRequiredFeatures() { return myRequiredFeatures; }

    boolean isOptional() { return myIsOptional; }

    boolean isAreDefaultFeaturesEnabled() { return myAreDefaultFeaturesEnabled; }

    DependencyImpl withPackage(PackageImpl newPkg) {
        return new DependencyImpl(
            newPkg,
            myName,
            myDepKinds,
            myIsOptional,
            myAreDefaultFeaturesEnabled,
            myRequiredFeatures,
            myCargoFeatureDependencyPackageName
        );
    }

    @Override
    public String toString() {
        return myName;
    }
}
