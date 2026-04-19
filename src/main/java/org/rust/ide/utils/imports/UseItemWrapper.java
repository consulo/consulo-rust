/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import org.rust.lang.core.psi.ext.RsElementUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPathImplMixin;

import java.util.Comparator;
import org.rust.lang.core.psi.ext.RsPathUtil;

public class UseItemWrapper implements Comparable<UseItemWrapper> {
    @NotNull
    private final RsUseItem useItem;
    @Nullable
    private final RsPath basePath;
    @Nullable
    private final String useSpeckText;
    private final int packageGroupLevel;

    public UseItemWrapper(@NotNull RsUseItem useItem) {
        this.useItem = useItem;
        RsUseSpeck useSpeck = useItem.getUseSpeck();
        RsPath path = useSpeck != null ? useSpeck.getPath() : null;
        this.basePath = path != null ? RsPathUtil.basePath(path) : null;
        this.useSpeckText = useSpeck != null ? getPathTextLower(useSpeck) : null;

        // `use` order:
        // 1. Standard library (stdlib)
        // 2. Related third party (extern crate)
        // 3. Local
        //    - otherwise
        //    - crate::
        //    - super::
        //    - self::
        if (basePath != null && basePath.getSelf() != null) {
            this.packageGroupLevel = 6;
        } else if (basePath != null && basePath.getSuper() != null) {
            this.packageGroupLevel = 5;
        } else if (basePath != null && basePath.getCrate() != null) {
            this.packageGroupLevel = 4;
        } else {
            PackageOrigin origin = null;
            if (basePath != null && basePath.getReference() != null) {
                var resolved = basePath.getReference().resolve();
                if (resolved != null) {
                    var crate = RsElementUtil.getContainingCrate(resolved);
                    if (crate != null) {
                        origin = crate.getOrigin();
                    }
                }
            }
            if (origin == null) {
                this.packageGroupLevel = 3;
            } else {
                switch (origin) {
                    case WORKSPACE:
                        this.packageGroupLevel = 3;
                        break;
                    case DEPENDENCY:
                        this.packageGroupLevel = 2;
                        break;
                    case STDLIB:
                    case STDLIB_DEPENDENCY:
                        this.packageGroupLevel = 1;
                        break;
                    default:
                        this.packageGroupLevel = 3;
                        break;
                }
            }
        }
    }

    @NotNull
    public RsUseItem getUseItem() {
        return useItem;
    }

    public int getPackageGroupLevel() {
        return packageGroupLevel;
    }

    @Override
    public int compareTo(@NotNull UseItemWrapper other) {
        int cmp = Integer.compare(this.packageGroupLevel, other.packageGroupLevel);
        if (cmp != 0) return cmp;
        if (this.useSpeckText == null && other.useSpeckText == null) return 0;
        if (this.useSpeckText == null) return -1;
        if (other.useSpeckText == null) return 1;
        return this.useSpeckText.compareTo(other.useSpeckText);
    }

    @Nullable
    private static String getPathTextLower(@NotNull RsUseSpeck useSpeck) {
        RsPath path = useSpeck.getPath();
        if (path == null) return null;
        String text = path.getText();
        return text != null ? text.toLowerCase() : null;
    }

    @NotNull
    public static final Comparator<RsUseSpeck> COMPARATOR_FOR_SPECKS_IN_USE_GROUP =
        Comparator.<RsUseSpeck, Boolean>comparing(speck -> {
            RsPath path = speck.getPath();
            return path == null || path.getSelf() == null;
        }).thenComparing(speck -> {
            String text = getPathTextLower(speck);
            return text != null ? text : "";
        });
}
