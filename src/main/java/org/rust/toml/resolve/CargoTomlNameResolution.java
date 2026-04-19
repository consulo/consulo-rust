/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.rust.toml.Util;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;

import java.util.*;

public final class CargoTomlNameResolution {
    private CargoTomlNameResolution() {}

    @NotNull
    public static ResolveResult[] resolveFeature(@NotNull TomlFile file, @NotNull String featureName, boolean depOnly) {
        List<ResolveResult> results = new ArrayList<>();
        for (TomlKeySegment segment : allFeatures(file, depOnly)) {
            if (featureName.equals(segment.getText())) {
                results.add(new PsiElementResolveResult(segment));
            }
        }
        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @NotNull
    public static List<TomlKeySegment> allFeatures(@NotNull TomlFile file, boolean depOnly) {
        Set<String> explicitFeatures = new HashSet<>();
        List<TomlKeySegment> result = new ArrayList<>();
        List<TomlTable> tables = Util.getTableList(file);
        for (TomlTable table : tables) {
            TomlTableHeader header = table.getHeader();
            if (Util.isFeatureListHeader(header) && !depOnly) {
                // [features]
                for (TomlKeyValue entry : table.getEntries()) {
                    List<TomlKeySegment> segments = entry.getKey().getSegments();
                    if (segments.size() == 1) {
                        TomlKeySegment key = segments.get(0);
                        String name = key.getName();
                        if (name != null) {
                            explicitFeatures.add(name);
                        }
                        result.add(key);
                    }
                }
            } else if (Util.isDependencyListHeader(header)) {
                // [dependencies]
                // bar = { version = "*", optional = true }
                for (TomlKeyValue entry : table.getEntries()) {
                    TomlValue value = entry.getValue();
                    if (value instanceof TomlInlineTable) {
                        TomlInlineTable inlineTable = (TomlInlineTable) value;
                        TomlValue optionalValue = Util.getValueWithKey(inlineTable, "optional");
                        if (optionalValue != null && asBoolean(optionalValue) == Boolean.TRUE) {
                            List<TomlKeySegment> segments = entry.getKey().getSegments();
                            if (segments.size() == 1) {
                                TomlKeySegment key = segments.get(0);
                                if (key.getName() == null || !explicitFeatures.contains(key.getName())) {
                                    result.add(key);
                                }
                            }
                        }
                    }
                }
            } else if (Util.isSpecificDependencyTableHeader(header)) {
                // [dependencies.bar]
                // version = "*"
                // optional = true
                TomlValue optionalValue = Util.getValueWithKey(table, "optional");
                if (optionalValue != null && asBoolean(optionalValue) == Boolean.TRUE) {
                    TomlKey headerKey = header.getKey();
                    if (headerKey != null) {
                        List<TomlKeySegment> segments = headerKey.getSegments();
                        if (!segments.isEmpty()) {
                            TomlKeySegment lastKey = segments.get(segments.size() - 1);
                            if (lastKey.getName() == null || !explicitFeatures.contains(lastKey.getName())) {
                                result.add(lastKey);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static Boolean asBoolean(@NotNull TomlValue value) {
        if (!(value instanceof TomlLiteral)) return null;
        Object kind = TomlLiteralKt.getKind((TomlLiteral) value);
        if (!(kind instanceof TomlLiteralKind.Boolean)) return null;
        return "true".equals(((TomlLiteralKind.Boolean) kind).getNode().getText());
    }
}
