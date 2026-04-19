/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.stubs.StubTree;
import com.intellij.psi.stubs.StubTreeBuilder;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.externalizer.StringCollectionExternalizer;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.search.RsWithMacrosProjectScope;
import org.rust.lang.RsFileType;
import org.rust.lang.core.macros.MacroExpansionStubsProvider;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.PathKind;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.stubs.RsAliasStub;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsTypeAliasStub;
import org.rust.lang.core.stubs.RsUseSpeckStub;
import org.rust.lang.core.types.TyFingerprint;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;

public class RsAliasIndex extends FileBasedIndexExtension<TyFingerprint, List<String>> {

    private static final ID<TyFingerprint, List<String>> KEY =
        ID.create("org.rust.lang.core.resolve.indexes.RsAliasIndex");

    @NotNull
    @Override
    public ID<TyFingerprint, List<String>> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<TyFingerprint, List<String>, FileContent> getIndexer() {
        return inputData -> {
            StubTree stubTree = getStubTree(inputData);
            if (stubTree == null) return Collections.emptyMap();
            HashMap<TyFingerprint, List<String>> map = new HashMap<>();
            for (com.intellij.psi.stubs.StubElement<?> stub : stubTree.getPlainList()) {
                if (stub instanceof RsTypeAliasStub) {
                    RsTypeAliasStub typeAliasStub = (RsTypeAliasStub) stub;
                    com.intellij.psi.PsiElement psi = typeAliasStub.getPsi();
                    if (psi instanceof org.rust.lang.core.psi.RsTypeAlias) {
                        org.rust.lang.core.psi.RsTypeAlias typeAlias = (org.rust.lang.core.psi.RsTypeAlias) psi;
                        if (!(org.rust.lang.core.psi.ext.RsAbstractableImplUtil.getOwnerBySyntaxOnly(typeAlias) instanceof RsAbstractableOwner.Impl)) {
                            String aliasedName = typeAliasStub.getName();
                            if (aliasedName == null) continue;
                            org.rust.lang.core.psi.RsTypeReference typeRef = typeAlias.getTypeReference();
                            if (typeRef == null) continue;
                            for (TyFingerprint tyf : TyFingerprint.create(typeRef, Collections.emptyList())) {
                                map.computeIfAbsent(tyf, k -> new ArrayList<>()).add(aliasedName);
                            }
                        }
                    }
                } else if (stub instanceof RsAliasStub) {
                    RsAliasStub aliasStub = (RsAliasStub) stub;
                    String aliasedName = aliasStub.getName();
                    if (aliasedName == null) continue;

                    com.intellij.psi.stubs.StubElement<?> parentStub = aliasStub.getParentStub();
                    if (!(parentStub instanceof RsUseSpeckStub)) continue;
                    RsUseSpeckStub useSpeckStub = (RsUseSpeckStub) parentStub;
                    if (useSpeckStub.isStarImport()) continue;
                    org.rust.lang.core.stubs.RsPathStub pathStub = useSpeckStub.getPath();
                    if (pathStub == null) continue;
                    if (pathStub.getKind() != PathKind.IDENTIFIER) continue;
                    String parentUseSpeckName = pathStub.getReferenceName();
                    if (parentUseSpeckName == null) continue;

                    map.computeIfAbsent(new TyFingerprint(parentUseSpeckName), k -> new ArrayList<>()).add(aliasedName);
                }
            }
            return map;
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<TyFingerprint> getKeyDescriptor() {
        return TyFingerprint.KEY_DESCRIPTOR;
    }

    @NotNull
    @Override
    public DataExternalizer<List<String>> getValueExternalizer() {
        return StringCollectionExternalizer.STRING_LIST_EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(RsFileType.INSTANCE);
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @NotNull
    @Override
    public Collection<FileType> getFileTypesWithSizeLimitNotApplicable() {
        return Collections.singletonList(RsFileType.INSTANCE);
    }

    @NotNull
    public static List<String> findPotentialAliases(@NotNull Project project, @NotNull TyFingerprint tyf) {
        HashSet<String> result = new HashSet<>();
        FileBasedIndex.getInstance().processValues(
            KEY,
            tyf,
            null,
            (file, value) -> {
                Object psi = OpenApiUtil.toPsiFile(file, project);
                if (psi instanceof RsFile) {
                    RsFile rsFile = (RsFile) psi;
                    if (!rsFile.getCrates().isEmpty()) {
                        result.addAll(value);
                    }
                }
                return true;
            },
            new RsWithMacrosProjectScope(project)
        );
        return new ArrayList<>(result);
    }

    private static StubTree getStubTree(FileContent inputData) {
        com.intellij.psi.stubs.Stub rootStub = MacroExpansionStubsProvider.findStubForMacroExpansionFile(inputData);
        if (rootStub == null) {
            rootStub = StubTreeBuilder.buildStubTree(inputData);
        }
        if (rootStub instanceof RsFileStub) {
            return new StubTree((RsFileStub) rootStub);
        }
        return null;
    }
}
