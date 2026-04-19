/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.toml.Util;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;
import org.toml.lang.psi.ext.TomlKeyKt;

import java.util.*;

public abstract class CargoDependencyCrateVisitor extends TomlVisitor {
    public abstract void visitDependency(@NotNull DependencyCrate dependency);

    @Override
    public void visitKeyValue(@NotNull TomlKeyValue element) {
        if (!(element.getParent() instanceof TomlTable)) return;
        TomlTable table = (TomlTable) element.getParent();
        DependencyTable depTable = DependencyTable.fromTomlTable(table);
        if (depTable == null) return;
        if (!(depTable instanceof DependencyTable.General)) return;

        List<TomlKeySegment> segments = element.getKey().getSegments();
        if (segments.isEmpty()) return;
        TomlKeySegment segment = segments.get(0);
        String name = segment.getName();
        if (name == null) return;
        TomlValue value = element.getValue();
        if (value == null) return;

        if (value instanceof TomlLiteral && TomlLiteralKt.getKind((TomlLiteral) value) instanceof TomlLiteralKind.String) {
            Map<String, TomlValue> props = new HashMap<>();
            props.put("version", value);
            visitDependency(new DependencyCrate(name, segment, props));
        } else if (value instanceof TomlInlineTable) {
            TomlInlineTable inlineTable = (TomlInlineTable) value;
            TomlKeyValue pkg = getPackageKeyValue(inlineTable);
            TomlElement originalNameElement;
            String originalName;
            if (pkg != null && pkg.getValue() != null) {
                originalNameElement = pkg.getValue();
                originalName = Util.getStringValue(pkg.getValue());
            } else {
                originalNameElement = segment;
                originalName = name;
            }
            if (originalName != null && originalNameElement != null) {
                visitDependency(new DependencyCrate(originalName, originalNameElement, collectProperties(inlineTable)));
            }
        }
    }

    @Override
    public void visitTable(@NotNull TomlTable element) {
        DependencyTable depTable = DependencyTable.fromTomlTable(element);
        if (depTable == null) return;
        if (!(depTable instanceof DependencyTable.Specific)) return;
        DependencyTable.Specific specific = (DependencyTable.Specific) depTable;

        visitDependency(new DependencyCrate(specific.myCrateName, specific.myCrateNameElement, collectProperties(element)));
    }

    @NotNull
    private static Map<String, TomlValue> collectProperties(@NotNull TomlKeyValueOwner owner) {
        Map<String, TomlValue> result = new HashMap<>();
        for (TomlKeyValue entry : owner.getEntries()) {
            String keyName = TomlKeyKt.getName(entry.getKey());
            if (keyName == null) continue;
            TomlValue val = entry.getValue();
            if (val == null) continue;
            result.put(keyName, val);
        }
        return result;
    }

    @Nullable
    private static TomlKeyValue getPackageKeyValue(@NotNull TomlKeyValueOwner owner) {
        for (TomlKeyValue entry : owner.getEntries()) {
            List<TomlKeySegment> segments = entry.getKey().getSegments();
            if (!segments.isEmpty() && "package".equals(segments.get(0).getName())) {
                return entry;
            }
        }
        return null;
    }

    // Inner classes for dependency table classification
    static abstract class DependencyTable {
        static class General extends DependencyTable {}

        static class Specific extends DependencyTable {
            final String myCrateName;
            final TomlElement myCrateNameElement;

            Specific(@NotNull String crateName, @NotNull TomlElement crateNameElement) {
                myCrateName = crateName;
                myCrateNameElement = crateNameElement;
            }
        }

        @Nullable
        static DependencyTable fromTomlTable(@NotNull TomlTable table) {
            TomlKey key = table.getHeader().getKey();
            if (key == null) return null;
            List<TomlKeySegment> segments = key.getSegments();
            int dependencyNameIndex = -1;
            for (int i = 0; i < segments.size(); i++) {
                if (Util.isDependencyKey(segments.get(i))) {
                    dependencyNameIndex = i;
                    break;
                }
            }

            if (dependencyNameIndex == segments.size() - 1) {
                return new General();
            } else if (dependencyNameIndex != -1) {
                TomlKeyValue pkg = getPackageKeyValue(table);
                TomlElement nameElement;
                String name;
                if (pkg != null && pkg.getValue() != null) {
                    nameElement = pkg.getValue();
                    name = Util.getStringValue(pkg.getValue());
                } else {
                    int idx = dependencyNameIndex + 1;
                    TomlKeySegment segment = idx < segments.size() ? segments.get(idx) : null;
                    nameElement = segment;
                    name = segment != null ? segment.getName() : null;
                }
                if (nameElement != null && name != null) {
                    return new Specific(name, nameElement);
                }
                return null;
            }
            return null;
        }

        @Nullable
        private static TomlKeyValue getPackageKeyValue(@NotNull TomlKeyValueOwner owner) {
            for (TomlKeyValue entry : owner.getEntries()) {
                List<TomlKeySegment> segments = entry.getKey().getSegments();
                if (!segments.isEmpty() && "package".equals(segments.get(0).getName())) {
                    return entry;
                }
            }
            return null;
        }
    }
}
