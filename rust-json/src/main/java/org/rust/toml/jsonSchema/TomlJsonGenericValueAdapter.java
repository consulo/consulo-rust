/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import jakarta.annotation.Nonnull;
import org.toml.lang.psi.TomlElement;
import org.toml.lang.psi.TomlKey;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlLiteral;
import org.toml.lang.psi.ext.TomlLiteralExt;
import org.toml.lang.psi.ext.TomlLiteralKind;

/**
 * A scalar TOML value. A key or key segment counts as a string, so a property name can be checked
 * like a value.
 */
final class TomlJsonGenericValueAdapter extends TomlJsonValueAdapter<TomlElement> {
    TomlJsonGenericValueAdapter(@Nonnull TomlElement element) {
        super(element);
    }

    @Override
    public boolean isStringLiteral() {
        return element instanceof TomlKey || element instanceof TomlKeySegment || kind() instanceof TomlLiteralKind.StringKind;
    }

    @Override
    public boolean isNumberLiteral() {
        return kind() instanceof TomlLiteralKind.NumberKind;
    }

    @Override
    public boolean isBooleanLiteral() {
        return kind() instanceof TomlLiteralKind.BooleanKind;
    }

    private TomlLiteralKind kind() {
        return element instanceof TomlLiteral literal ? TomlLiteralExt.getKind(literal) : null;
    }
}
