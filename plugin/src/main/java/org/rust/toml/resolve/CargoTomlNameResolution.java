/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import jakarta.annotation.Nonnull;
import org.rust.toml.Util;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralExt;

import java.util.*;

public final class CargoTomlNameResolution {
    private CargoTomlNameResolution() {}

    @Nonnull
    public static ResolveResult[] resolveFeature(@Nonnull TomlFile file, @Nonnull String featureName, boolean depOnly) {
        List<ResolveResult> results = new ArrayList<>();
        for (TomlKeySegment segment : allFeatures(file, depOnly)) {
            if (featureName.equals(segment.getText())) {
                results.add(new PsiElementResolveResult(segment));
            }
        }
        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @Nonnull
    public static List<TomlKeySegment> allFeatures(@Nonnull TomlFile file, boolean depOnly) {
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
    private static Boolean asBoolean(@Nonnull TomlValue value) {
        if (!(value instanceof TomlLiteral)) return null;
        Object kind = TomlLiteralExt.getKind((TomlLiteral) value);
        if (!(kind instanceof TomlLiteralKind.BooleanKind)) return null;
        return "true".equals(((TomlLiteralKind.BooleanKind) kind).getNode().getText());
    }
}
