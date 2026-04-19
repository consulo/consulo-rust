/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.toml.lang.psi.*;

import java.util.*;
import java.util.stream.Collectors;

public class TomlSchema {
    private final List<TomlTableSchema> myTables;

    private TomlSchema(@NotNull List<TomlTableSchema> tables) {
        myTables = tables;
    }

    @NotNull
    public Collection<String> topLevelKeys(boolean isArray) {
        return myTables.stream()
            .filter(t -> t.myIsArray == isArray)
            .map(t -> t.myName)
            .collect(Collectors.toList());
    }

    @NotNull
    public Collection<String> keysForTable(@NotNull String tableName) {
        for (TomlTableSchema table : myTables) {
            if (tableName.equals(table.myName)) {
                return table.myKeys;
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    public static TomlSchema parse(@NotNull Project project, @NotNull String example) {
        PsiFile toml = PsiFileFactory.getInstance(project)
            .createFileFromText("Cargo.toml", TomlFileType.INSTANCE, example);

        List<TomlTableSchema> tables = new ArrayList<>();
        for (PsiElement child : toml.getChildren()) {
            if (child instanceof TomlKeyValueOwner) {
                TomlTableSchema schema = getSchema((TomlKeyValueOwner) child);
                if (schema != null) {
                    tables.add(schema);
                }
            }
        }

        return new TomlSchema(tables);
    }

    @Nullable
    private static TomlTableSchema getSchema(@NotNull TomlKeyValueOwner owner) {
        String name;
        boolean isArray;
        if (owner instanceof TomlTable) {
            TomlTable table = (TomlTable) owner;
            TomlKey key = table.getHeader().getKey();
            if (key == null) return null;
            List<TomlKeySegment> segments = key.getSegments();
            if (segments.isEmpty()) return null;
            name = segments.get(0).getName();
            isArray = false;
        } else if (owner instanceof TomlArrayTable) {
            TomlArrayTable arrayTable = (TomlArrayTable) owner;
            TomlKey key = arrayTable.getHeader().getKey();
            if (key == null) return null;
            List<TomlKeySegment> segments = key.getSegments();
            if (segments.isEmpty()) return null;
            name = segments.get(0).getName();
            isArray = true;
        } else {
            return null;
        }
        if (name == null) return null;

        List<String> keys = new ArrayList<>();
        for (TomlKeyValue entry : owner.getEntries()) {
            String keyText = entry.getKey().getText();
            if (!"foo".equals(keyText)) {
                keys.add(keyText);
            }
        }
        return new TomlTableSchema(name, isArray, keys);
    }

    private static class TomlTableSchema {
        final String myName;
        final boolean myIsArray;
        final Collection<String> myKeys;

        TomlTableSchema(@NotNull String name, boolean isArray, @NotNull Collection<String> keys) {
            myName = name;
            myIsArray = isArray;
            myKeys = keys;
        }
    }
}
