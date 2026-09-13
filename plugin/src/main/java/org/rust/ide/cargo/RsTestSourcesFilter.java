/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.cargo;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.content.TestSourcesFilter;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.injected.RsDoctestLanguageInjector;

@ExtensionImpl
public class RsTestSourcesFilter extends TestSourcesFilter {

    @Override
    public boolean isTestSource(@Nonnull VirtualFile file, @Nonnull Project project) {
        return RsDoctestLanguageInjector.isDoctestInjection(file, project);
    }
}
