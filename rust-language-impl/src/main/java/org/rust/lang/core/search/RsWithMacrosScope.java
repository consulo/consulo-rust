/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.search;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.scope.DelegatingGlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import org.rust.lang.core.macros.MacroExpansionManager;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;

public class RsWithMacrosScope extends DelegatingGlobalSearchScope {

    private final MacroExpansionManager myService;

    public RsWithMacrosScope(Project project, GlobalSearchScope scope) {
        super(scope);
        myService = MacroExpansionManagerUtil.getMacroExpansionManagerIfCreated(project);
    }

    @Override
    public boolean contains(VirtualFile file) {
        return super.contains(file) || (myService != null && myService.isExpansionFileOfCurrentProject(file));
    }
}
