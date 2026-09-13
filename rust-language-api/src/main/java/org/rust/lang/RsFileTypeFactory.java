/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import jakarta.annotation.Nonnull;

/**
 * Registers {@link RsFileType} for the {@code .rs} extension.
 */
@ExtensionImpl
public class RsFileTypeFactory extends FileTypeFactory {

    @Override
    public void createFileTypes(@Nonnull FileTypeConsumer consumer) {
        consumer.consume(RsFileType.INSTANCE, RsFileType.INSTANCE.getDefaultExtension());
    }
}
