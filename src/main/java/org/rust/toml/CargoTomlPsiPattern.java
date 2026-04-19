/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.VirtualFilePattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.CargoConstants;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;
import org.toml.lang.psi.ext.TomlKeyKt;

import java.util.List;
import java.util.Set;

public class CargoTomlPsiPattern {
    public static final CargoTomlPsiPattern INSTANCE = new CargoTomlPsiPattern();

    private static final String TOML_KEY_CONTEXT_NAME = "key";
    private static final String TOML_KEY_VALUE_CONTEXT_NAME = "keyValue";
    private static final Set<String> PACKAGE_URL_ATTRIBUTES = Set.of("homepage", "repository", "documentation");

    @NotNull
    private static <I extends PsiElement> PsiElementPattern.Capture<I> cargoTomlPsiElement(@NotNull Class<I> clazz) {
        return PlatformPatterns.psiElement(clazz).inVirtualFile(
            StandardPatterns.or(
                new VirtualFilePattern().withName(CargoConstants.MANIFEST_FILE),
                new VirtualFilePattern().withName(CargoConstants.XARGO_MANIFEST_FILE)
            )
        );
    }

    /** Any element inside any TomlKey in Cargo.toml */
    private final PsiElementPattern.Capture<PsiElement> myInKey =
        cargoTomlPsiElement(PsiElement.class)
            .withParent(TomlKeySegment.class);

    @NotNull
    public PsiElementPattern.Capture<PsiElement> getInKey() {
        return myInKey;
    }

    @NotNull
    public PsiElementPattern.Capture<PsiElement> inValueWithKey(@NotNull String key) {
        return cargoTomlPsiElement(PsiElement.class).inside(tomlKeyValue(key));
    }

    private final PsiElementPattern.Capture<TomlTableHeader> myOnDependencyTableHeader =
        cargoTomlPsiElement(TomlTableHeader.class)
            .with(new PatternCondition<TomlTableHeader>("dependenciesCondition") {
                @Override
                public boolean accepts(@NotNull TomlTableHeader header, ProcessingContext context) {
                    return Util.isDependencyListHeader(header);
                }
            });

    private final PsiElementPattern.Capture<TomlTable> myOnDependencyTable =
        cargoTomlPsiElement(TomlTable.class)
            .withChild(myOnDependencyTableHeader);

    private final PsiElementPattern.Capture<TomlKeySegment> myOnDependencyKey =
        cargoTomlPsiElement(TomlKeySegment.class)
            .withSuperParent(3, myOnDependencyTable);

    @NotNull
    public PsiElementPattern.Capture<TomlKeySegment> getOnDependencyKey() {
        return myOnDependencyKey;
    }

    private final PsiElementPattern.Capture<TomlTable> myOnSpecificDependencyTable =
        cargoTomlPsiElement(TomlTable.class)
            .withChild(
                PlatformPatterns.psiElement(TomlTableHeader.class)
                    .with(new PatternCondition<TomlTableHeader>("specificDependencyCondition") {
                        @Override
                        public boolean accepts(@NotNull TomlTableHeader header, ProcessingContext context) {
                            return Util.isSpecificDependencyTableHeader(header);
                        }
                    })
            );

    private final PsiElementPattern.Capture<TomlKeySegment> myOnSpecificDependencyHeaderKey =
        cargoTomlPsiElement(TomlKeySegment.class)
            .withSuperParent(2,
                PlatformPatterns.psiElement(TomlTableHeader.class)
                    .with(new PatternCondition<TomlTableHeader>("specificDependencyCondition") {
                        @Override
                        public boolean accepts(@NotNull TomlTableHeader header, ProcessingContext context) {
                            TomlKey key = header.getKey();
                            List<TomlKeySegment> names = key != null ? key.getSegments() : List.of();
                            if (names.size() < 2) return false;
                            return Util.isDependencyKey(names.get(names.size() - 2));
                        }
                    })
            );

    @NotNull
    public PsiElementPattern.Capture<TomlKeySegment> getOnSpecificDependencyHeaderKey() {
        return myOnSpecificDependencyHeaderKey;
    }

    private final PsiElementPattern.Capture<PsiElement> myInSpecificDependencyHeaderKey =
        cargoTomlPsiElement(PsiElement.class)
            .withParent(myOnSpecificDependencyHeaderKey);

    @NotNull
    public PsiElementPattern.Capture<PsiElement> getInSpecificDependencyHeaderKey() {
        return myInSpecificDependencyHeaderKey;
    }

    private final PsiElementPattern.Capture<TomlKeyValue> myOnDependencyKeyValue =
        cargoTomlPsiElement(TomlKeyValue.class)
            .withParent(myOnDependencyTable);

    private final PsiElementPattern.Capture<PsiElement> myInDependencyKeyValue =
        cargoTomlPsiElement(PsiElement.class)
            .inside(PlatformPatterns.psiElement(TomlKeyValue.class))
            .with(new PatternCondition<PsiElement>("dependencyKeyValueCondition") {
                @Override
                public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
                    PsiElement current = element;
                    while (current != null) {
                        if (current instanceof TomlKeyValue) {
                            return myOnDependencyKeyValue.accepts(current);
                        }
                        current = current.getParent();
                    }
                    return false;
                }
            });

    @NotNull
    public PsiElementPattern.Capture<PsiElement> getInDependencyKeyValue() {
        return myInDependencyKeyValue;
    }

    private final PsiElementPattern.Capture<TomlKeyValue> myOnSpecificDependencyKeyValue =
        cargoTomlPsiElement(TomlKeyValue.class)
            .withParent(
                StandardPatterns.or(
                    myOnSpecificDependencyTable,
                    PlatformPatterns.psiElement(TomlInlineTable.class).withSuperParent(2, myOnDependencyTable)
                )
            );

    private final PsiElementPattern.Capture<PsiElement> myInSpecificDependencyKeyValue =
        cargoTomlPsiElement(PsiElement.class)
            .inside(PlatformPatterns.psiElement(TomlKeyValue.class))
            .with(new PatternCondition<PsiElement>("specificDependencyKeyValueCondition") {
                @Override
                public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
                    PsiElement current = element;
                    while (current != null) {
                        if (current instanceof TomlKeyValue) {
                            return myOnSpecificDependencyKeyValue.accepts(current);
                        }
                        current = current.getParent();
                    }
                    return false;
                }
            });

    @NotNull
    public PsiElementPattern.Capture<PsiElement> getInSpecificDependencyKeyValue() {
        return myInSpecificDependencyKeyValue;
    }

    private final PsiElementPattern.Capture<TomlLiteral> myWorkspacePath =
        cargoTomlStringLiteral().withParent(
            PlatformPatterns.psiElement(TomlArray.class).withParent(
                PlatformPatterns.psiElement(TomlKeyValue.class).withParent(
                    tomlTable("workspace")
                )
            )
        );

    @NotNull
    public PsiElementPattern.Capture<TomlLiteral> getWorkspacePath() {
        return myWorkspacePath;
    }

    private final PsiElementPattern.Capture<TomlLiteral> myPackageWorkspacePath =
        cargoTomlStringLiteral().withParent(
            tomlKeyValue("workspace").withParent(tomlTable("package"))
        );

    @NotNull
    public PsiElementPattern.Capture<TomlLiteral> getPackageWorkspacePath() {
        return myPackageWorkspacePath;
    }

    private final PsiElementPattern.Capture<TomlLiteral> myPath =
        cargoTomlStringLiteral().withParent(tomlKeyValue("path"));

    @NotNull
    public PsiElementPattern.Capture<TomlLiteral> getPath() {
        return myPath;
    }

    private final PsiElementPattern.Capture<TomlLiteral> myBuildPath =
        cargoTomlStringLiteral().withParent(
            tomlKeyValue("build").withParent(tomlTable("package"))
        );

    @NotNull
    public PsiElementPattern.Capture<TomlLiteral> getBuildPath() {
        return myBuildPath;
    }

    private final PsiElementPattern.Capture<TomlArray> myOnFeatureDependencyArray =
        PlatformPatterns.psiElement(TomlArray.class)
            .withSuperParent(1, PlatformPatterns.psiElement(TomlKeyValue.class))
            .withSuperParent(2, tomlTable("features"));

    private final PsiElementPattern.Capture<PsiElement> myInFeatureDependencyArray =
        cargoTomlPsiElement(PsiElement.class)
            .inside(myOnFeatureDependencyArray);

    @NotNull
    public PsiElementPattern.Capture<PsiElement> getInFeatureDependencyArray() {
        return myInFeatureDependencyArray;
    }

    private final PsiElementPattern.Capture<TomlLiteral> myOnFeatureDependencyLiteral =
        cargoTomlStringLiteral().withParent(myOnFeatureDependencyArray);

    @NotNull
    public PsiElementPattern.Capture<TomlLiteral> getOnFeatureDependencyLiteral() {
        return myOnFeatureDependencyLiteral;
    }

    private final PsiElementPattern.Capture<TomlKeyValue> myOnDependency =
        PlatformPatterns.psiElement(TomlKeyValue.class)
            .withParent(
                StandardPatterns.or(
                    PlatformPatterns.psiElement(TomlInlineTable.class).withSuperParent(2, myOnDependencyTable),
                    myOnSpecificDependencyTable
                )
            );

    @NotNull
    public PsiElementPattern.Capture<TomlKeyValue> getOnDependency() {
        return myOnDependency;
    }

    @NotNull
    public PsiElementPattern.Capture<TomlKeyValue> dependencyProperty(@NotNull String name) {
        return PlatformPatterns.psiElement(TomlKeyValue.class)
            .with(new PatternCondition<TomlKeyValue>("name") {
                @Override
                public boolean accepts(@NotNull TomlKeyValue e, ProcessingContext context) {
                    String keyName = TomlKeyKt.getName(e.getKey());
                    return name.equals(keyName);
                }
            })
            .withParent(
                StandardPatterns.or(
                    PlatformPatterns.psiElement(TomlInlineTable.class).withSuperParent(2, myOnDependencyTable),
                    myOnSpecificDependencyTable
                )
            );
    }

    private final PsiElementPattern.Capture<TomlArray> myOnDependencyPackageFeatureArray =
        PlatformPatterns.psiElement(TomlArray.class)
            .withParent(dependencyProperty("features"));

    private final PsiElementPattern.Capture<PsiElement> myInDependencyPackageFeatureArray =
        cargoTomlPsiElement(PsiElement.class)
            .inside(myOnDependencyPackageFeatureArray);

    @NotNull
    public PsiElementPattern.Capture<PsiElement> getInDependencyPackageFeatureArray() {
        return myInDependencyPackageFeatureArray;
    }

    private final PsiElementPattern.Capture<PsiElement> myInDependencyInlineTableVersion =
        cargoTomlPsiElement(PsiElement.class)
            .inside(cargoTomlStringLiteral().withParent(dependencyProperty("version")));

    @NotNull
    public PsiElementPattern.Capture<PsiElement> getInDependencyInlineTableVersion() {
        return myInDependencyInlineTableVersion;
    }

    private final PsiElementPattern.Capture<PsiElement> myInDependencyTableKey =
        cargoTomlPsiElement(PsiElement.class)
            .inside(myOnDependency)
            .withParent(cargoTomlPsiElement(TomlKeySegment.class));

    @NotNull
    public PsiElementPattern.Capture<PsiElement> getInDependencyTableKey() {
        return myInDependencyTableKey;
    }

    private final PsiElementPattern.Capture<TomlLiteral> myOnDependencyPackageFeature =
        cargoTomlStringLiteral().withParent(myOnDependencyPackageFeatureArray);

    @NotNull
    public PsiElementPattern.Capture<TomlLiteral> getOnDependencyPackageFeature() {
        return myOnDependencyPackageFeature;
    }

    private final PsiElementPattern.Capture<TomlLiteral> myDependencyGitUrl =
        cargoTomlStringLiteral().withParent(dependencyProperty("git"));

    @NotNull
    public PsiElementPattern.Capture<TomlLiteral> getDependencyGitUrl() {
        return myDependencyGitUrl;
    }

    private final PsiElementPattern.Capture<TomlLiteral> myPackageUrl =
        cargoTomlStringLiteral().withParent(
            PlatformPatterns.psiElement(TomlKeyValue.class)
                .withParent(tomlTable("package"))
                .with(new PatternCondition<TomlKeyValue>("name") {
                    @Override
                    public boolean accepts(@NotNull TomlKeyValue e, ProcessingContext context) {
                        String keyName = TomlKeyKt.getName(e.getKey());
                        return PACKAGE_URL_ATTRIBUTES.contains(keyName);
                    }
                })
        );

    @NotNull
    public PsiElementPattern.Capture<TomlLiteral> getPackageUrl() {
        return myPackageUrl;
    }

    @NotNull
    private static PsiElementPattern.Capture<TomlLiteral> cargoTomlStringLiteral() {
        return cargoTomlPsiElement(TomlLiteral.class)
            .with(new PatternCondition<TomlLiteral>("stringLiteral") {
                @Override
                public boolean accepts(@NotNull TomlLiteral e, ProcessingContext context) {
                    return TomlLiteralKt.getKind(e) instanceof TomlLiteralKind.String;
                }
            });
    }

    @NotNull
    private static PsiElementPattern.Capture<TomlKeyValue> tomlKeyValue(@NotNull String key) {
        return PlatformPatterns.psiElement(TomlKeyValue.class).withChild(
            PlatformPatterns.psiElement(TomlKey.class).withText(key)
        );
    }

    @NotNull
    private static PsiElementPattern.Capture<TomlTable> tomlTable(@NotNull String key) {
        return PlatformPatterns.psiElement(TomlTable.class).with(new PatternCondition<TomlTable>("WithName (" + key + ")") {
            @Override
            public boolean accepts(@NotNull TomlTable e, ProcessingContext context) {
                TomlKey tableKey = e.getHeader().getKey();
                List<TomlKeySegment> names = tableKey != null ? tableKey.getSegments() : List.of();
                return names.size() == 1 && names.get(0).textMatches(key);
            }
        });
    }
}
