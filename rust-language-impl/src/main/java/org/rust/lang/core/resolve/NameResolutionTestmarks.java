/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.rust.openapiext.Testmark;

public final class NameResolutionTestmarks {
    private NameResolutionTestmarks() {}

    public static final Testmark DollarCrateMagicIdentifier = new Testmark();
    public static final Testmark SelfInGroup = new Testmark();
    public static final Testmark CrateRootModule = new Testmark();
    public static final Testmark ModRsFile = new Testmark();
    public static final Testmark SelfRelatedTypeSpecialCase = new Testmark();
    public static final Testmark SkipAssocTypeFromImpl = new Testmark();
    public static final Testmark UpdateDefMapsForAllCratesWhenFindingModData = new Testmark();
    public static final Testmark TypeAliasToImpl = new Testmark();
}
