/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.jetbrains.jsonSchema.extension.adapter.JsonArrayValueAdapter;
import com.jetbrains.jsonSchema.extension.adapter.JsonValueAdapter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.toml.lang.psi.TomlKeySegment;

import java.util.ArrayList;
import java.util.List;

/**
 * The array formed by every {@code [[array.table]]} with the same header, in file order.
 */
final class TomlJsonArrayTableAdapter extends TomlJsonValueAdapter<TomlKeySegment> implements JsonArrayValueAdapter {
    private final List<TomlJsonObjectAdapter> myElements = new ArrayList<>();

    TomlJsonArrayTableAdapter(@Nonnull TomlKeySegment headerSegment) {
        super(headerSegment);
    }

    void add(@Nonnull TomlJsonObjectAdapter element) {
        myElements.add(element);
    }

    @Nullable
    TomlJsonObjectAdapter last() {
        return myElements.isEmpty() ? null : myElements.get(myElements.size() - 1);
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Nonnull
    @Override
    public JsonArrayValueAdapter getAsArray() {
        return this;
    }

    @Override
    public List<JsonValueAdapter> getElements() {
        return new ArrayList<>(myElements);
    }
}
