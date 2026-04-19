/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.rust.stdext.Lazy;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMacroBody;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.stdext.HashCode;

public class DeclMacroDefInfo extends MacroDefInfo {
    private final int crate;
    @NotNull
    private final ModPath path;
    @NotNull
    private final MacroIndex macroIndex;
    @NotNull
    private final String bodyText;
    @NotNull
    private final HashCode bodyHash;
    private final boolean hasMacroExport;
    private final boolean hasLocalInnerMacros;
    private final boolean hasRustcBuiltinMacro;
    @NotNull
    private final Project project;

    /** Lazy because usually it should not be used (thanks to macro expansion cache) */
    @Nullable
    private volatile RsMacroBody body;
    private volatile boolean bodyInitialized;

    public DeclMacroDefInfo(
        int crate,
        @NotNull ModPath path,
        @NotNull MacroIndex macroIndex,
        @NotNull String bodyText,
        @NotNull HashCode bodyHash,
        boolean hasMacroExport,
        boolean hasLocalInnerMacros,
        boolean hasRustcBuiltinMacro,
        @NotNull Project project
    ) {
        this.crate = crate;
        this.path = path;
        this.macroIndex = macroIndex;
        this.bodyText = bodyText;
        this.bodyHash = bodyHash;
        this.hasMacroExport = hasMacroExport;
        this.hasLocalInnerMacros = hasLocalInnerMacros;
        this.hasRustcBuiltinMacro = hasRustcBuiltinMacro;
        this.project = project;
    }

    @Override
    public int getCrate() {
        return crate;
    }

    @Override
    @NotNull
    public ModPath getPath() {
        return path;
    }

    @NotNull
    public MacroIndex getMacroIndex() {
        return macroIndex;
    }

    @NotNull
    public HashCode getBodyHash() {
        return bodyHash;
    }

    public boolean isHasMacroExport() {
        return hasMacroExport;
    }

    public boolean isHasLocalInnerMacros() {
        return hasLocalInnerMacros;
    }

    public boolean isHasRustcBuiltinMacro() {
        return hasRustcBuiltinMacro;
    }

    @Nullable
    public RsMacroBody getBody() {
        if (!bodyInitialized) {
            synchronized (this) {
                if (!bodyInitialized) {
                    RsPsiFactory psiFactory = new RsPsiFactory(project, false);
                    body = psiFactory.createMacroBody(bodyText);
                    bodyInitialized = true;
                }
            }
        }
        return body;
    }
}
