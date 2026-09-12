/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.navigation.GotoSymbolContributor;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.stubs.index.RsNamedElementIndex;

@ExtensionImpl
public class RsSymbolNavigationContributor
    extends RsNavigationContributorBase<RsNamedElement>
    implements GotoSymbolContributor {

    public RsSymbolNavigationContributor() {
        super(RsNamedElementIndex.KEY, RsNamedElement.class);
    }
}
