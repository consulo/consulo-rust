/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;

public class RsCustomTemplate extends RsProjectTemplate {

    @NotNull
    private final String url;

    @SuppressWarnings("UnstableApiUsage")
    public RsCustomTemplate(@NlsContexts.ListItem @NotNull String name, @NotNull String url) {
        super(name, false, RsIcons.CARGO_GENERATE);
        this.url = url;
    }

    @NotNull
    public String getUrl() {
        return url;
    }

    @NotNull
    public String getShortLink() {
        int idx = url.indexOf("//");
        if (idx >= 0) {
            return url.substring(idx + 2);
        }
        return url;
    }

    public static final RsCustomTemplate ProcMacroTemplate = new RsCustomTemplate(
        RsBundle.message("list.item.procedural.macro"),
        "https://github.com/intellij-rust/rust-procmacro-quickstart-template"
    );

    public static final RsCustomTemplate WasmPackTemplate = new RsCustomTemplate(
        RsBundle.message("list.item.webassembly.lib"),
        "https://github.com/intellij-rust/wasm-pack-template"
    );
}
