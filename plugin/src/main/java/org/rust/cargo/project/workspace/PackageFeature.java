/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import org.rust.lang.utils.PresentableNodeData;

import java.util.Objects;

/**
 * A <a href="https://doc.rust-lang.org/cargo/reference/features.html">cargo feature</a> with {@code name} in some package {@code pkg}
 */
public final class PackageFeature implements PresentableNodeData {

    private final CargoWorkspace.Package myPkg;
    private final String myName;

    public PackageFeature(CargoWorkspace.Package pkg, String name) {
        myPkg = pkg;
        myName = name;
    }

    public CargoWorkspace.Package getPkg() {
        return myPkg;
    }

    public String getName() {
        return myName;
    }

    @Override
    public String getText() {
        return myPkg.getName() + "/" + myName;
    }

    @Override
    public String toString() {
        return getText();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackageFeature)) return false;
        PackageFeature that = (PackageFeature) o;
        return Objects.equals(myPkg, that.myPkg) && Objects.equals(myName, that.myName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myPkg, myName);
    }
}
