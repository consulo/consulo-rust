/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;

public class RsWithMacrosProjectScope extends RsWithMacrosScope {

    public RsWithMacrosProjectScope(Project project) {
        super(project, GlobalSearchScope.allScope(project));
    }
}
