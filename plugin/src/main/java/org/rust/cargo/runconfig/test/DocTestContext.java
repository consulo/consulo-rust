/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class DocTestContext {
    @Nonnull
    private final RsQualifiedNamedElement owner;
    private final int lineNumber;
    private final boolean isIgnored;

    public DocTestContext(@Nonnull RsQualifiedNamedElement owner, int lineNumber, boolean isIgnored) {
        this.owner = owner;
        this.lineNumber = lineNumber;
        this.isIgnored = isIgnored;
    }

    @Nonnull
    public RsQualifiedNamedElement getOwner() {
        return owner;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isIgnored() {
        return isIgnored;
    }
}
