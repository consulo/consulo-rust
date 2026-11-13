/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileTemplate.FileTemplateContributor;
import consulo.fileTemplate.FileTemplateRegistrator;

/**
 * Registers {@code fileTemplates/internal/Rust File.rs.ft}, the template behind "New > Rust File".
 */
@ExtensionImpl
public class RsFileTemplateContributor implements FileTemplateContributor {
    public static final String RUST_FILE_TEMPLATE = "Rust File";

    @Override
    public void register(FileTemplateRegistrator registrator) {
        registrator.registerInternalTemplate(RUST_FILE_TEMPLATE);
    }
}
