/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMacroBody;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.stdext.HashCode;

public class DeclMacro2DefInfo extends MacroDefInfo {
    private final int crate;
    @NotNull
    private final ModPath path;
    @NotNull
    private final String bodyText;
    @NotNull
    private final HashCode bodyHash;
    private final boolean hasRustcBuiltinMacro;
    @NotNull
    private final Project project;

    @Nullable
    private volatile RsMacroBody body;
    private volatile boolean bodyInitialized;

    public DeclMacro2DefInfo(
        int crate,
        @NotNull ModPath path,
        @NotNull String bodyText,
        @NotNull HashCode bodyHash,
        boolean hasRustcBuiltinMacro,
        @NotNull Project project
    ) {
        this.crate = crate;
        this.path = path;
        this.bodyText = bodyText;
        this.bodyHash = bodyHash;
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
    public HashCode getBodyHash() {
        return bodyHash;
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
