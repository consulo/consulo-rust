/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.crate.Crate;

/**
 * Path of a single named element within the specified crate.
 */
public final class QualifiedItemPath {
    private final String myCrateRelativePath;
    private final Integer myCrateId;

    public QualifiedItemPath(String crateRelativePath, Integer crateId) {
        myCrateRelativePath = crateRelativePath;
        myCrateId = crateId;
    }

    public String getCrateRelativePath() {
        return myCrateRelativePath;
    }

    public Integer getCrateId() {
        return myCrateId;
    }

    public boolean matches(RsQualifiedNamedElement target) {
        if (target == null) return false;
        String targetPath = target.getCrateRelativePath();
        Crate crate = RsElementUtil.getContainingCrate(target);
        Integer targetCrateId = crate != null ? crate.getId() : null;
        return myCrateRelativePath.equals(targetPath) && java.util.Objects.equals(myCrateId, targetCrateId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualifiedItemPath that = (QualifiedItemPath) o;
        return myCrateRelativePath.equals(that.myCrateRelativePath) && java.util.Objects.equals(myCrateId, that.myCrateId);
    }

    @Override
    public int hashCode() {
        int result = myCrateRelativePath.hashCode();
        result = 31 * result + (myCrateId != null ? myCrateId.hashCode() : 0);
        return result;
    }
}
