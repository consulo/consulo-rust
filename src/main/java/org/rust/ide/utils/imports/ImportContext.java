/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve2.CrateDefMap;
import org.rust.lang.core.resolve2.ModData;
import org.rust.lang.core.resolve2.RsModInfo;
import org.rust.lang.core.resolve2.ModInfoUtil;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public class ImportContext {
    /** Info of mod in which auto-import or completion is called */
    @NotNull
    private final RsModInfo rootInfo;
    /** Mod in which auto-import or completion is called */
    @NotNull
    private final RsMod rootMod;
    @NotNull
    private final Type type;
    @Nullable
    private final PathInfo pathInfo;

    private ImportContext(
        @NotNull RsModInfo rootInfo,
        @NotNull RsMod rootMod,
        @NotNull Type type,
        @Nullable PathInfo pathInfo
    ) {
        this.rootInfo = rootInfo;
        this.rootMod = rootMod;
        this.type = type;
        this.pathInfo = pathInfo;
    }

    @NotNull
    public Project getProject() {
        return rootInfo.getProject();
    }

    @NotNull
    public ModData getRootModData() {
        return rootInfo.getModData();
    }

    @NotNull
    public CrateDefMap getRootDefMap() {
        return rootInfo.getDefMap();
    }

    @NotNull
    public RsModInfo getRootInfo() {
        return rootInfo;
    }

    @NotNull
    public RsMod getRootMod() {
        return rootMod;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public PathInfo getPathInfo() {
        return pathInfo;
    }

    @Nullable
    public static ImportContext from(@NotNull RsPath path, @NotNull Type type) {
        return from(path, type, PathInfo.from(path, type == Type.COMPLETION));
    }

    @Nullable
    public static ImportContext from(@NotNull RsPath path) {
        return from(path, Type.AUTO_IMPORT);
    }

    @Nullable
    public static ImportContext from(@NotNull RsElement context, @NotNull Type type, @Nullable PathInfo pathInfo) {
        RsMod rootMod = context.getContainingMod();
        RsModInfo info = org.rust.lang.core.resolve2.FacadeResolveUtil.getModInfo(rootMod);
        if (info == null) return null;
        return new ImportContext(info, rootMod, type, pathInfo);
    }

    @Nullable
    public static ImportContext from(@NotNull RsElement context, @NotNull Type type) {
        return from(context, type, null);
    }

    @Nullable
    public static ImportContext from(@NotNull RsElement context) {
        return from(context, Type.AUTO_IMPORT, null);
    }

    public enum Type {
        AUTO_IMPORT,
        COMPLETION,
        OTHER
    }

    public static class PathInfo {
        @Nullable
        private final String rootPathText;
        @Nullable
        private final RustParserUtil.PathParsingMode rootPathParsingMode;
        @Nullable
        private final Set<Namespace> rootPathAllowedNamespaces;
        @Nullable
        private final List<String> nextSegments;
        @NotNull
        private final Predicate<RsQualifiedNamedElement> namespaceFilter;
        private final boolean parentIsMetaItem;

        private PathInfo(
            @Nullable String rootPathText,
            @Nullable RustParserUtil.PathParsingMode rootPathParsingMode,
            @Nullable Set<Namespace> rootPathAllowedNamespaces,
            @Nullable List<String> nextSegments,
            @NotNull Predicate<RsQualifiedNamedElement> namespaceFilter,
            boolean parentIsMetaItem
        ) {
            this.rootPathText = rootPathText;
            this.rootPathParsingMode = rootPathParsingMode;
            this.rootPathAllowedNamespaces = rootPathAllowedNamespaces;
            this.nextSegments = nextSegments;
            this.namespaceFilter = namespaceFilter;
            this.parentIsMetaItem = parentIsMetaItem;
        }

        @Nullable
        public String getRootPathText() {
            return rootPathText;
        }

        @Nullable
        public RustParserUtil.PathParsingMode getRootPathParsingMode() {
            return rootPathParsingMode;
        }

        @Nullable
        public Set<Namespace> getRootPathAllowedNamespaces() {
            return rootPathAllowedNamespaces;
        }

        @Nullable
        public List<String> getNextSegments() {
            return nextSegments;
        }

        @NotNull
        public Predicate<RsQualifiedNamedElement> getNamespaceFilter() {
            return namespaceFilter;
        }

        public boolean isParentIsMetaItem() {
            return parentIsMetaItem;
        }

        @NotNull
        public static PathInfo from(@NotNull RsPath path, boolean isCompletion) {
            RsPath rootPathCandidate = RsPathUtil.rootPath(path);
            RsPath rootPath = (rootPathCandidate == path) ? null : rootPathCandidate;
            return new PathInfo(
                rootPath != null ? rootPath.getText() : null,
                rootPath != null ? getPathParsingMode(rootPath) : null,
                rootPath != null ? RsPathUtil.allowedNamespaces(rootPath, isCompletion) : null,
                getNextSegments(path),
                createNamespaceFilter(path, isCompletion),
                path.getParent() instanceof RsMetaItem
            );
        }

        @Nullable
        private static List<String> getNextSegments(@NotNull RsPath path) {
            if (!(path.getParent() instanceof RsPath)) return null;
            java.util.ArrayList<String> segments = new java.util.ArrayList<>();
            RsPath current = (RsPath) path.getParent();
            while (current != null) {
                String name = current.getReferenceName();
                if (name == null) return null;
                segments.add(name);
                current = current.getParent() instanceof RsPath ? (RsPath) current.getParent() : null;
            }
            return segments;
        }
    }

    @NotNull
    public static RustParserUtil.PathParsingMode getPathParsingMode(@NotNull RsPath path) {
        if (path.getParent() instanceof RsPathExpr
            || path.getParent() instanceof RsStructLiteral
            || path.getParent() instanceof RsPatStruct
            || path.getParent() instanceof RsPatTupleStruct) {
            return RustParserUtil.PathParsingMode.VALUE;
        }
        return RustParserUtil.PathParsingMode.TYPE;
    }

    @NotNull
    private static Predicate<RsQualifiedNamedElement> createNamespaceFilter(@NotNull RsPath path, boolean isCompletion) {
        var context = path.getContext();
        if (context instanceof RsTypeReference) {
            return e -> e instanceof RsEnumItem
                || e instanceof RsStructItem
                || e instanceof RsTraitItem
                || e instanceof RsTypeAlias
                || e instanceof RsMacroDefinitionBase;
        }
        if (context instanceof RsPathExpr) {
            return e -> {
                if (e instanceof RsEnumItem) return isCompletion;
                return e instanceof RsFieldsOwner
                    || e instanceof RsConstant
                    || e instanceof RsFunction
                    || e instanceof RsTypeAlias
                    || e instanceof RsMacroDefinitionBase;
            };
        }
        if (context instanceof RsTraitRef) {
            return e -> e instanceof RsTraitItem;
        }
        if (context instanceof RsStructLiteral) {
            return e -> (e instanceof RsFieldsOwner && ((RsFieldsOwner) e).getBlockFields() != null)
                || e instanceof RsTypeAlias;
        }
        if (context instanceof RsPatBinding) {
            return e -> e instanceof RsEnumItem
                || e instanceof RsEnumVariant
                || e instanceof RsStructItem
                || e instanceof RsTypeAlias
                || e instanceof RsConstant
                || e instanceof RsFunction;
        }
        if (context instanceof RsPath) {
            return e -> Namespace.getNamespaces(e).contains(Namespace.Types);
        }
        if (context instanceof RsMacroCall) {
            return e -> Namespace.getNamespaces(e).contains(Namespace.Macros);
        }
        if (context instanceof RsMetaItem) {
            // simplified; full version has more conditions
            return e -> true;
        }
        return e -> true;
    }
}
