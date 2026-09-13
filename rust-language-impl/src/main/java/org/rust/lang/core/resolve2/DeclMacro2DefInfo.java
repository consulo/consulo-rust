/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMacroBody;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.stdext.HashCode;

public class DeclMacro2DefInfo extends MacroDefInfo {
    private final int crate;
    @Nonnull
    private final ModPath path;
    @Nonnull
    private final String bodyText;
    @Nonnull
    private final HashCode bodyHash;
    private final boolean hasRustcBuiltinMacro;
    @Nonnull
    private final Project project;

    @Nullable
    private volatile RsMacroBody body;
    private volatile boolean bodyInitialized;

    public DeclMacro2DefInfo(
        int crate,
        @Nonnull ModPath path,
        @Nonnull String bodyText,
        @Nonnull HashCode bodyHash,
        boolean hasRustcBuiltinMacro,
        @Nonnull Project project
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
    @Nonnull
    public ModPath getPath() {
        return path;
    }

    @Nonnull
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
