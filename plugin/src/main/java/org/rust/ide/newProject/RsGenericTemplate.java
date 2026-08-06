/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;


import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;

public abstract class RsGenericTemplate extends RsProjectTemplate {

    @SuppressWarnings("UnstableApiUsage")
    protected RsGenericTemplate( @Nonnull String name, boolean isBinary) {
        super(name, isBinary, RsIcons.RUST);
    }

    public static final RsGenericTemplate CargoBinaryTemplate = new RsGenericTemplate(
        RsBundle.message("list.item.binary.application"), true
    ) {};

    public static final RsGenericTemplate CargoLibraryTemplate = new RsGenericTemplate(
        RsBundle.message("list.item.library"), false
    ) {};
}
