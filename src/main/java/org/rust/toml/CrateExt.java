/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.openapiext.WriteAccessCheckUtil;
import org.toml.lang.psi.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class CrateExt {

    private CrateExt() {}

    @Nullable
    public static TomlFile getTomlFile(@NotNull Crate crate) {
        if (crate.getCargoTarget() == null) return null;
        return Util.getPackageCargoTomlFile(crate.getCargoTarget().getPkg(), crate.getProject());
    }

    public static void addCargoDependency(@NotNull Crate crate, @NotNull String name, @NotNull String version) {
        addCargoDependency(crate, name, version, Collections.emptyList());
    }

    public static void addCargoDependency(@NotNull Crate crate, @NotNull String name, @NotNull String version, @NotNull List<String> features) {
        WriteAccessCheckUtil.checkWriteAccessAllowed();

        TomlFile cargoToml = getTomlFile(crate);
        if (cargoToml == null) return;
        Project project = crate.getProject();
        TomlPsiFactory factory = new TomlPsiFactory(project, false);

        String featuresArray = features.stream()
            .map(f -> "\"" + f + "\"")
            .collect(Collectors.joining(", ", "[", "]"));

        TomlElement existingDependency = Util.findDependencyElement(cargoToml, name);
        if (existingDependency instanceof TomlKeyValueOwner) {
            updateDependencyFeatures(factory, (TomlKeyValueOwner) existingDependency, features);
        } else if (existingDependency instanceof TomlLiteral) {
            String newVersion = Util.getStringValue((TomlValue) existingDependency);
            if (newVersion == null) newVersion = version;
            TomlInlineTable newEntry = factory.createInlineTable("version = \"" + newVersion + "\", features = " + featuresArray);
            existingDependency.replace(newEntry);
        } else {
            TomlTable existingDependencies = null;
            for (TomlTable table : Util.getTableList(cargoToml)) {
                TomlKey key = table.getHeader().getKey();
                if (key != null && "dependencies".equals(Util.getKeyStringValue(key))) {
                    existingDependencies = table;
                    break;
                }
            }
            TomlTable dependencies;
            if (existingDependencies != null) {
                dependencies = existingDependencies;
            } else {
                TomlTable newDependenciesTable = factory.createTable("dependencies");
                cargoToml.add(factory.createWhitespace("\n"));
                dependencies = (TomlTable) cargoToml.add(newDependenciesTable);
            }
            TomlKeyValue newDependencyKeyValue;
            if (features.isEmpty()) {
                newDependencyKeyValue = factory.createKeyValue(name, version);
            } else {
                newDependencyKeyValue = factory.createKeyValue(name, "{ version = \"" + version + "\", features = " + featuresArray + " }");
            }

            dependencies.add(factory.createWhitespace("\n"));
            dependencies.add(newDependencyKeyValue);
        }
    }

    private static void updateDependencyFeatures(@NotNull TomlPsiFactory factory,
                                                   @NotNull TomlKeyValueOwner table,
                                                   @NotNull List<String> features) {
        TomlKeyValue featuresEntry = null;
        for (TomlKeyValue entry : table.getEntries()) {
            if ("features".equals(Util.getKeyStringValue(entry.getKey()))) {
                featuresEntry = entry;
                break;
            }
        }
        if (featuresEntry == null) {
            String featuresArray = features.stream()
                .map(f -> "\"" + f + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
            TomlKeyValue newEntry = factory.createKeyValue("features", featuresArray);
            if (table instanceof TomlTable) {
                table.add(factory.createWhitespace("\n"));
                table.add(newEntry);
            } else if (table instanceof TomlInlineTable) {
                List<TomlKeyValue> allEntries = table.getEntries();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < allEntries.size(); i++) {
                    if (i > 0) sb.append(", ");
                    TomlKeyValue e = allEntries.get(i);
                    sb.append(e.getKey().getText()).append(" = ");
                    if (e.getValue() != null) sb.append(e.getValue().getText());
                }
                if (!allEntries.isEmpty()) sb.append(", ");
                sb.append("features = ").append(featuresArray);
                table.replace(factory.createInlineTable(sb.toString()));
            }
        } else {
            TomlValue val = featuresEntry.getValue();
            List<String> existingFeatures;
            if (val instanceof TomlArray) {
                existingFeatures = ((TomlArray) val).getElements().stream()
                    .map(Util::getStringValue)
                    .filter(v -> v != null)
                    .collect(Collectors.toList());
            } else {
                existingFeatures = Collections.emptyList();
            }
            List<String> newFeatures = new java.util.ArrayList<>(existingFeatures);
            for (String f : features) {
                if (!newFeatures.contains(f)) {
                    newFeatures.add(f);
                }
            }
            String newFeaturesArray = newFeatures.stream()
                .map(f -> "\"" + f + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
            featuresEntry.replace(factory.createKeyValue("features", newFeaturesArray));
        }
    }
}
