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
import org.toml.lang.psi.TomlArrayTable;
import org.toml.lang.psi.TomlFile;
import org.toml.lang.psi.TomlHeaderOwner;
import org.toml.lang.psi.TomlKey;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlKeyValue;
import org.toml.lang.psi.TomlKeyValueOwner;
import org.toml.lang.psi.TomlTable;
import org.toml.lang.psi.TomlValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A TOML table seen as a JSON object.
 * <p>
 * TOML spreads one object over syntax that does not nest in the PSI: {@code a.b = 1} sets {@code b}
 * inside {@code a}, and {@code [a.b]} opens a table further down the file. Members are therefore
 * collected into a tree of objects keyed by segment, so the schema sees the same shape Cargo does.
 */
final class TomlJsonObjectAdapter extends TomlJsonValueAdapter<PsiElement> implements JsonObjectValueAdapter {
    private final Map<String, TomlJsonPropertyAdapter> myProperties = new LinkedHashMap<>();
    @Nullable
    private TomlKeyValueOwner myUnreadEntries;

    /**
     * An object holding the entries of a table, an inline table or an array table element.
     */
    TomlJsonObjectAdapter(@Nonnull TomlKeyValueOwner owner) {
        super(owner);
        myUnreadEntries = owner;
    }

    private TomlJsonObjectAdapter(@Nonnull PsiElement delegate) {
        super(delegate);
    }

    /**
     * The whole file as one object: top-level entries, every {@code [table]} placed under its header
     * path and every {@code [[array.table]]} appended to the array at its header path.
     */
    @Nonnull
    static TomlJsonObjectAdapter forFile(@Nonnull TomlFile file) {
        TomlJsonObjectAdapter root = new TomlJsonObjectAdapter(file);
        for (PsiElement child : file.getChildren()) {
            if (child instanceof TomlKeyValue keyValue) {
                root.addKeyValue(keyValue);
            }
            else if (child instanceof TomlArrayTable arrayTable) {
                List<TomlKeySegment> header = headerSegments(arrayTable);
                if (!header.isEmpty()) {
                    TomlJsonObjectAdapter parent = root.descend(header.subList(0, header.size() - 1));
                    parent.arrayTable(header.get(header.size() - 1)).add(new TomlJsonObjectAdapter(arrayTable));
                }
            }
            else if (child instanceof TomlTable table) {
                List<TomlKeySegment> header = headerSegments(table);
                if (!header.isEmpty()) {
                    TomlJsonObjectAdapter target = root.descend(header);
                    for (TomlKeyValue entry : table.getEntries()) {
                        target.addKeyValue(entry);
                    }
                }
            }
        }
        return root;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Nonnull
    @Override
    public JsonObjectValueAdapter getAsObject() {
        return this;
    }

    @Override
    public List<JsonPropertyAdapter> getPropertyList() {
        readEntries();
        return new ArrayList<>(myProperties.values());
    }

    private void readEntries() {
        TomlKeyValueOwner owner = myUnreadEntries;
        if (owner != null) {
            myUnreadEntries = null;
            for (TomlKeyValue entry : owner.getEntries()) {
                addKeyValue(entry);
            }
        }
    }

    private void addKeyValue(@Nonnull TomlKeyValue keyValue) {
        List<TomlKeySegment> segments = keyValue.getKey().getSegments();
        if (segments.isEmpty()) {
            return;
        }
        TomlJsonObjectAdapter target = descend(segments.subList(0, segments.size() - 1));
        TomlJsonPropertyAdapter property = target.property(segments.get(segments.size() - 1), keyValue);
        TomlValue value = keyValue.getValue();
        if (value != null) {
            property.addValue(createAdapterByType(value));
        }
    }

    @Nonnull
    private TomlJsonObjectAdapter descend(@Nonnull List<TomlKeySegment> path) {
        TomlJsonObjectAdapter current = this;
        for (TomlKeySegment segment : path) {
            current = current.child(segment);
        }
        return current;
    }

    @Nonnull
    private TomlJsonObjectAdapter child(@Nonnull TomlKeySegment segment) {
        readEntries();
        TomlJsonPropertyAdapter property = property(segment, segment);
        for (JsonValueAdapter value : property.getValues()) {
            if (value instanceof TomlJsonObjectAdapter object) {
                return object;
            }
            // A dotted header reaches into the last element of an array of tables.
            if (value instanceof TomlJsonArrayTableAdapter arrayTable && arrayTable.last() != null) {
                return arrayTable.last();
            }
        }
        TomlJsonObjectAdapter object = new TomlJsonObjectAdapter((PsiElement) segment);
        property.addValue(object);
        return object;
    }

    @Nonnull
    private TomlJsonArrayTableAdapter arrayTable(@Nonnull TomlKeySegment segment) {
        readEntries();
        TomlJsonPropertyAdapter property = property(segment, segment);
        for (JsonValueAdapter value : property.getValues()) {
            if (value instanceof TomlJsonArrayTableAdapter arrayTable) {
                return arrayTable;
            }
        }
        TomlJsonArrayTableAdapter arrayTable = new TomlJsonArrayTableAdapter(segment);
        property.addValue(arrayTable);
        return arrayTable;
    }

    @Nonnull
    private TomlJsonPropertyAdapter property(@Nonnull TomlKeySegment name, @Nonnull PsiElement delegate) {
        String key = name.getName() == null ? "" : name.getName();
        return myProperties.computeIfAbsent(key, k -> new TomlJsonPropertyAdapter(k, name, delegate, this));
    }

    @Nonnull
    private static List<TomlKeySegment> headerSegments(@Nonnull TomlHeaderOwner headerOwner) {
        TomlKey key = headerOwner.getHeader().getKey();
        return key == null ? List.of() : key.getSegments();
    }
}
