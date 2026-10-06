/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.debugger;

import consulo.annotation.component.ExtensionImpl;
import consulo.nativeDev.debugger.NativeLineBreakpointTypeResolver;
import consulo.virtualFileSystem.fileType.FileType;
import org.rust.lang.RsFileType;

@ExtensionImpl
public class RsNativeLineBreakpointTypeResolver extends NativeLineBreakpointTypeResolver {
    @Override
    public FileType getFileType() {
        return RsFileType.INSTANCE;
    }
}
