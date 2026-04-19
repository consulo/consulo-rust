/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.lang.core.completion.CompletionUtilsUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElementExtUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.openapiext.VirtualFileExtUtil;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class Util {

    private Util() {}

    private static volatile Boolean myComputeOnce;

    public static boolean tomlPluginIsAbiCompatible() {
        if (myComputeOnce == null) {
            synchronized (Util.class) {
                if (myComputeOnce == null) {
                    try {
                        Class.forName("org.toml.lang.psi.TomlKeySegment");
                        myComputeOnce = true;
                    } catch (LinkageError | ClassNotFoundException e) {
                        NotificationUtils.showBalloonWithoutProject(
                            RsBundle.message("notification.content.incompatible.toml.plugin.version.code.completion.for.cargo.toml.not.available"),
                            NotificationType.WARNING
                        );
                        myComputeOnce = false;
                    }
                }
            }
        }
        return myComputeOnce;
    }

    public static boolean isCargoToml(@NotNull PsiFile file) {
        return CargoConstants.MANIFEST_FILE.equals(file.getName());
    }

    public static boolean isDependencyKey(@NotNull TomlKeySegment segment) {
        String name = segment.getName();
        return "dependencies".equals(name) || "dev-dependencies".equals(name) || "build-dependencies".equals(name);
    }

    public static boolean isFeaturesKey(@NotNull TomlKeySegment segment) {
        return "features".equals(segment.getName());
    }

    public static boolean isFeatureDef(@NotNull TomlKeySegment segment) {
        PsiElement keyParent = segment.getParent();
        if (keyParent == null) return false;
        PsiElement kvCandidate = keyParent.getParent();
        if (!(kvCandidate instanceof TomlKeyValue)) return false;
        PsiElement tableCandidate = kvCandidate.getParent();
        if (!(tableCandidate instanceof TomlTable)) return false;
        TomlTable table = (TomlTable) tableCandidate;
        return isFeatureListHeader(table.getHeader()) && isCargoToml(table.getContainingFile());
    }

    public static boolean isDependencyListHeader(@NotNull TomlTableHeader header) {
        TomlKey key = header.getKey();
        if (key == null) return false;
        List<TomlKeySegment> segments = key.getSegments();
        if (segments.isEmpty()) return false;
        return isDependencyKey(segments.get(segments.size() - 1));
    }

    public static boolean isSpecificDependencyTableHeader(@NotNull TomlTableHeader header) {
        TomlKey key = header.getKey();
        List<TomlKeySegment> names = key != null ? key.getSegments() : Collections.emptyList();
        if (names.size() < 2) return false;
        return isDependencyKey(names.get(names.size() - 2));
    }

    public static boolean isFeatureListHeader(@NotNull TomlTableHeader header) {
        TomlKey key = header.getKey();
        if (key == null) return false;
        List<TomlKeySegment> segments = key.getSegments();
        if (segments.isEmpty()) return false;
        return isFeaturesKey(segments.get(segments.size() - 1));
    }

    @Nullable
    public static String getStringValue(@Nullable TomlValue value) {
        if (value == null) return null;
        if (!(value instanceof TomlLiteral)) return null;
        Object kind = TomlLiteralKt.getKind((TomlLiteral) value);
        if (kind instanceof TomlLiteralKind.String) {
            return ((TomlLiteralKind.String) kind).getValue();
        }
        return null;
    }

    @NotNull
    public static String getKeyStringValue(@NotNull TomlKey key) {
        List<TomlKeySegment> segments = key.getSegments();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) sb.append(".");
            sb.append(segments.get(i).getName());
        }
        return sb.toString();
    }

    @Nullable
    public static TomlValue getValueWithKey(@NotNull TomlKeyValueOwner owner, @NotNull String key) {
        for (TomlKeyValue entry : owner.getEntries()) {
            if (key.equals(entry.getKey().getText())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Nullable
    public static TomlKeyValue getClosestKeyValueAncestor(@NotNull PsiElement position) {
        PsiElement parent = position.getParent();
        if (parent == null) return null;
        TomlKeyValue keyValue = PsiElementExt.ancestorOrSelf(parent, TomlKeyValue.class);
        if (keyValue == null) {
            throw new IllegalStateException("PsiElementPattern must not allow values outside of TomlKeyValues");
        }
        TomlValue value = keyValue.getValue();
        if (value == null || (value instanceof TomlLiteral && RsPsiJavaUtil.isAncestorOf(value, position))) {
            return keyValue;
        }
        if (!(value instanceof TomlLiteral)) {
            return null;
        }
        if (RsPsiJavaUtil.isAncestorOf(value, position)) {
            return keyValue;
        }
        return null;
    }

    @Nullable
    public static TomlFile getPackageCargoTomlFile(@NotNull CargoWorkspace.Package pkg, @NotNull Project project) {
        if (pkg.getContentRoot() == null) return null;
        com.intellij.openapi.vfs.VirtualFile manifestFile = pkg.getContentRoot().findChild(CargoConstants.MANIFEST_FILE);
        if (manifestFile == null) return null;
        PsiFile psiFile = VirtualFileExtUtil.toPsiFile(manifestFile, project);
        if (psiFile instanceof TomlFile) {
            return (TomlFile) psiFile;
        }
        return null;
    }

    @Nullable
    public static CargoWorkspace.Package findCargoPackageForCargoToml(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile().getOriginalFile();
        CargoWorkspace.Package pkg = RsElementExtUtil.findCargoPackage(containingFile);
        if (pkg == null) return null;
        TomlFile tomlFile = getPackageCargoTomlFile(pkg, containingFile.getProject());
        if (tomlFile == containingFile) {
            return pkg;
        }
        return null;
    }

    @Nullable
    public static TomlFile findDependencyTomlFile(@NotNull TomlElement element, @NotNull String depName) {
        CargoWorkspace.Package pkg = findCargoPackageForCargoToml(element);
        if (pkg == null) return null;
        CargoWorkspace.Package depPkg = findDependencyByPackageName(pkg, depName);
        if (depPkg == null) return null;
        return getPackageCargoTomlFile(depPkg, element.getProject());
    }

    @Nullable
    private static CargoWorkspace.Package findDependencyByPackageName(@NotNull CargoWorkspace.Package pkg, @NotNull String pkgName) {
        for (CargoWorkspace.Dependency dep : pkg.getDependencies()) {
            if (pkgName.equals(dep.getCargoFeatureDependencyPackageName())) {
                return dep.getPkg();
            }
        }
        return null;
    }

    @Nullable
    public static TomlKeySegment getContainingDependencyKey(@NotNull TomlValue value) {
        PsiElement parent = value.getParent();
        if (parent == null) return null;
        PsiElement parentParent = parent.getParent();
        if (!(parentParent instanceof TomlElement)) return null;
        if (parentParent instanceof TomlTable) {
            TomlTable table = (TomlTable) parentParent;
            TomlKey key = table.getHeader().getKey();
            if (key == null) return null;
            List<TomlKeySegment> segments = key.getSegments();
            if (segments.isEmpty()) return null;
            return segments.get(segments.size() - 1);
        } else {
            PsiElement grandParent = parentParent.getParent();
            if (!(grandParent instanceof TomlKeyValue)) return null;
            TomlKeyValue kv = (TomlKeyValue) grandParent;
            List<TomlKeySegment> segments = kv.getKey().getSegments();
            if (segments.size() != 1) return null;
            return segments.get(0);
        }
    }

    @NotNull
    public static List<TomlTable> getTableList(@NotNull TomlFile file) {
        List<TomlTable> result = new ArrayList<>();
        for (PsiElement child : file.getChildren()) {
            if (child instanceof TomlTable) {
                result.add((TomlTable) child);
            }
        }
        return result;
    }

    @Nullable
    public static TomlElement findDependencyElement(@NotNull TomlFile file, @NotNull String dependencyName) {
        // Check for cases like:
        //     [dependencies.foo]
        //     version = "1.0.0"
        for (TomlTable table : getTableList(file)) {
            TomlKey key = table.getHeader().getKey();
            if (key != null && ("dependencies." + dependencyName).equals(getKeyStringValue(key))) {
                return table;
            }
        }

        // Check for cases like:
        //     [dependencies.xxx]
        //     name = "foo"
        //     version = "1.0.0"
        for (TomlTable table : getTableList(file)) {
            TomlKey key = table.getHeader().getKey();
            if (key == null) continue;
            List<TomlKeySegment> headerSegments = key.getSegments();
            if (headerSegments.size() <= 1) continue;
            if (!"dependencies".equals(headerSegments.get(0).getName())) continue;
            for (TomlKeyValue entry : table.getEntries()) {
                if ("name".equals(getKeyStringValue(entry.getKey())) && dependencyName.equals(getStringValue(entry.getValue()))) {
                    return table;
                }
            }
        }

        TomlTable dependenciesTable = null;
        for (TomlTable table : getTableList(file)) {
            TomlKey key = table.getHeader().getKey();
            if (key == null) continue;
            List<TomlKeySegment> segments = key.getSegments();
            if (segments.size() == 1 && "dependencies".equals(segments.get(0).getName())) {
                dependenciesTable = table;
                break;
            }
        }
        if (dependenciesTable == null) return null;

        // Check for cases like:
        //     [dependencies]
        //     foo = "1.0.0"
        // or
        //     [dependencies]
        //     foo = { version = "1.0.0" }
        for (TomlKeyValue entry : dependenciesTable.getEntries()) {
            if (dependencyName.equals(getKeyStringValue(entry.getKey()))) {
                TomlValue val = entry.getValue();
                if (val instanceof TomlInlineTable) {
                    boolean hasNameEntry = false;
                    for (TomlKeyValue inlineEntry : ((TomlInlineTable) val).getEntries()) {
                        if ("name".equals(getKeyStringValue(inlineEntry.getKey()))) {
                            hasNameEntry = true;
                            break;
                        }
                    }
                    if (!hasNameEntry) return val;
                } else {
                    return val;
                }
            }
        }

        // Check for cases like:
        //     [dependencies]
        //     xxx = { name = "foo", version = "1.0.0" }
        for (TomlKeyValue entry : dependenciesTable.getEntries()) {
            TomlValue val = entry.getValue();
            if (val instanceof TomlInlineTable) {
                for (TomlKeyValue inlineEntry : ((TomlInlineTable) val).getEntries()) {
                    if ("name".equals(getKeyStringValue(inlineEntry.getKey()))
                        && dependencyName.equals(getStringValue(inlineEntry.getValue()))) {
                        return val;
                    }
                }
            }
        }
        return null;
    }

    @NotNull
    public static List<String> findDependencyFeatures(@NotNull TomlFile file, @NotNull String dependencyName) {
        TomlElement dependencyElement = findDependencyElement(file, dependencyName);
        if (dependencyElement instanceof TomlKeyValueOwner) {
            TomlKeyValueOwner owner = (TomlKeyValueOwner) dependencyElement;
            for (TomlKeyValue entry : owner.getEntries()) {
                if ("features".equals(getKeyStringValue(entry.getKey()))) {
                    TomlValue val = entry.getValue();
                    if (val instanceof TomlArray) {
                        return ((TomlArray) val).getElements().stream()
                            .map(Util::getStringValue)
                            .filter(v -> v != null)
                            .collect(Collectors.toList());
                    }
                    return Collections.emptyList();
                }
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /**
     * Inserts `=` between key and value if missed and wraps inserted string with quotes if needed
     */
    public static class StringValueInsertionHandler implements InsertHandler<LookupElement> {
        private final TomlKeyValue myKeyValue;

        public StringValueInsertionHandler(@NotNull TomlKeyValue keyValue) {
            myKeyValue = keyValue;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            int startOffset = context.getStartOffset();
            TomlValue value = CompletionUtilsUtil.getElementOfType(context, TomlValue.class);
            boolean hasEq = false;
            for (PsiElement child : myKeyValue.getChildren()) {
                if (RsElementUtil.getElementType(child) == TomlElementTypes.EQ) {
                    hasEq = true;
                    break;
                }
            }
            boolean hasQuotes = value != null && (!(value instanceof TomlLiteral) || getLiteralType((TomlLiteral) value) != TomlElementTypes.NUMBER);

            if (!hasEq) {
                context.getDocument().insertString(startOffset - (hasQuotes ? 1 : 0), "= ");
                PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getDocument());
                startOffset += 2;
            }

            if (!hasQuotes) {
                context.getDocument().insertString(startOffset, "\"");
                context.getDocument().insertString(context.getSelectionEndOffset(), "\"");
            }
        }
    }

    /**
     * Wraps inserted string with quotes if needed
     */
    public static class StringLiteralInsertionHandler implements InsertHandler<LookupElement> {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            PsiElement leaf = CompletionUtilsUtil.getElementOfType(context, PsiElement.class);
            if (leaf == null) return;
            boolean hasQuotes = leaf.getParent() instanceof TomlLiteral && isTomlStringLiteral(RsElementUtil.getElementType(leaf));

            if (!hasQuotes) {
                context.getDocument().insertString(context.getStartOffset(), "\"");
                context.getDocument().insertString(context.getSelectionEndOffset(), "\"");
            }
        }
    }

    @NotNull
    private static IElementType getLiteralType(@NotNull TomlLiteral literal) {
        return RsElementUtil.getElementType(literal.getChildren()[0]);
    }

    private static boolean isTomlStringLiteral(@NotNull IElementType type) {
        return type == TomlElementTypes.BASIC_STRING
            || type == TomlElementTypes.LITERAL_STRING
            || type == TomlElementTypes.MULTILINE_BASIC_STRING
            || type == TomlElementTypes.MULTILINE_LITERAL_STRING;
    }
}
