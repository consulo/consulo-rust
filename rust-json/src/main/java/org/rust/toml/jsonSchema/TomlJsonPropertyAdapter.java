/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.jsonSchema;

import com.jetbrains.jsonSchema.extension.adapter.JsonObjectValueAdapter;
import com.jetbrains.jsonSchema.extension.adapter.JsonPropertyAdapter;
import com.jetbrains.jsonSchema.extension.adapter.JsonValueAdapter;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.toml.lang.psi.TomlElement;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlKeyValue;
import org.toml.lang.psi.TomlKeyValueOwner;
import org.toml.lang.psi.TomlValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * One member of a TOML object. Its PSI is either a {@code key = value} pair or, for a member
 * introduced by a dotted key or a table header, the key segment that names it.
 */
final class TomlJsonPropertyAdapter implements JsonPropertyAdapter {
    private final String myName;
    private final TomlElement myNameElement;
    private final PsiElement myDelegate;
    @Nullable
    private final JsonObjectValueAdapter myParent;
    private final List<JsonValueAdapter> myValues = new ArrayList<>(1);

    TomlJsonPropertyAdapter(@Nonnull String name,
                            @Nonnull TomlElement nameElement,
                            @Nonnull PsiElement delegate,
                            @Nullable JsonObjectValueAdapter parent) {
        myName = name;
        myNameElement = nameElement;
        myDelegate = delegate;
        myParent = parent;
    }

    /**
     * The property of a single {@code key = value} pair, named after the last segment of its key.
     */
    @Nonnull
    static TomlJsonPropertyAdapter of(@Nonnull TomlKeyValue keyValue) {
        List<TomlKeySegment> segments = keyValue.getKey().getSegments();
        String name = segments.isEmpty() ? "" : segments.get(segments.size() - 1).getName();
        PsiElement parent = keyValue.getParent();
        TomlJsonPropertyAdapter property = new TomlJsonPropertyAdapter(
            name == null ? "" : name,
            keyValue.getKey(),
            keyValue,
            parent instanceof TomlKeyValueOwner owner ? new TomlJsonObjectAdapter(owner) : null);
        TomlValue value = keyValue.getValue();
        if (value != null) {
            property.addValue(TomlJsonValueAdapter.createAdapterByType(value));
        }
        return property;
    }

    void addValue(@Nonnull JsonValueAdapter value) {
        myValues.add(value);
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public JsonValueAdapter getNameValueAdapter() {
        return new TomlJsonGenericValueAdapter(myNameElement);
    }

    @Override
    public Collection<JsonValueAdapter> getValues() {
        return myValues;
    }

    @Override
    public PsiElement getDelegate() {
        return myDelegate;
    }

    @Nullable
    @Override
    public JsonObjectValueAdapter getParentObject() {
        return myParent;
    }
}
