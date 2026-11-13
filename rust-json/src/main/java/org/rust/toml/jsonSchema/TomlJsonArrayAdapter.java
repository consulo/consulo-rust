/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.jetbrains.jsonSchema.extension.adapter.JsonArrayValueAdapter;
import com.jetbrains.jsonSchema.extension.adapter.JsonValueAdapter;
import jakarta.annotation.Nonnull;
import org.toml.lang.psi.TomlArray;
import org.toml.lang.psi.TomlValue;

import java.util.ArrayList;
import java.util.List;

final class TomlJsonArrayAdapter extends TomlJsonValueAdapter<TomlArray> implements JsonArrayValueAdapter {
    private List<JsonValueAdapter> myElements;

    TomlJsonArrayAdapter(@Nonnull TomlArray array) {
        super(array);
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
        if (myElements == null) {
            List<JsonValueAdapter> elements = new ArrayList<>();
            for (TomlValue value : element.getElements()) {
                elements.add(createAdapterByType(value));
            }
            myElements = elements;
        }
        return myElements;
    }
}
