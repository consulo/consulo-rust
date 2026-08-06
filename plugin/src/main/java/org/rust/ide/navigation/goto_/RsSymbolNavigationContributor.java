/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.stubs.index.RsNamedElementIndex;

public class RsSymbolNavigationContributor
    extends RsNavigationContributorBase<RsNamedElement> {

    public RsSymbolNavigationContributor() {
        super(RsNamedElementIndex.KEY, RsNamedElement.class);
    }
}
