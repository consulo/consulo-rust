/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.jetbrains.jsonSchema.extension.adapter.JsonArrayValueAdapter;
import com.jetbrains.jsonSchema.extension.adapter.JsonObjectValueAdapter;
import com.jetbrains.jsonSchema.extension.adapter.JsonValueAdapter;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.toml.lang.psi.TomlArray;
import org.toml.lang.psi.TomlElement;
import org.toml.lang.psi.TomlKeyValueOwner;

/**
 * Presents a TOML value to the JSON schema engine: a table becomes an object, an array an array,
 * and everything else a scalar whose kind is read from the literal.
 */
abstract class TomlJsonValueAdapter<T extends PsiElement> implements JsonValueAdapter {
    protected final T element;

    protected TomlJsonValueAdapter(@Nonnull T element) {
        this.element = element;
    }

    @Nonnull
    static TomlJsonValueAdapter<?> createAdapterByType(@Nonnull TomlElement value) {
        if (value instanceof TomlKeyValueOwner owner) {
            return new TomlJsonObjectAdapter(owner);
        }
        if (value instanceof TomlArray array) {
            return new TomlJsonArrayAdapter(array);
        }
        return new TomlJsonGenericValueAdapter(value);
    }

    @Override
    public final PsiElement getDelegate() {
        return element;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public boolean isStringLiteral() {
        return false;
    }

    @Override
    public boolean isNumberLiteral() {
        return false;
    }

    @Override
    public boolean isBooleanLiteral() {
        return false;
    }

    @Nullable
    @Override
    public JsonObjectValueAdapter getAsObject() {
        return null;
    }

    @Nullable
    @Override
    public JsonArrayValueAdapter getAsArray() {
        return null;
    }
}
