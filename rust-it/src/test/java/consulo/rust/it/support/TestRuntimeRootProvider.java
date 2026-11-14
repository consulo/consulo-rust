/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.support;

import consulo.annotation.component.ServiceImpl;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.internal.RuntimeRootProvider;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

/**
 * Modules without compiler output: the platform's implementation lives in the compiler, which the headless tests do
 * not load, and a module that depends on another module asks for it when its roots are enumerated.
 */
@ServiceImpl
public class TestRuntimeRootProvider implements RuntimeRootProvider {
    @Nullable
    @Override
    public String getCompilerOutputUrl(ContentFolderTypeProvider contentFolderType) {
        return null;
    }

    @Nullable
    @Override
    public VirtualFile getCompilerOutput(ContentFolderTypeProvider contentFolderType) {
        return null;
    }
}
