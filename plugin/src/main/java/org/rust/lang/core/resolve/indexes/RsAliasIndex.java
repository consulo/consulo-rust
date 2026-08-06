/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.psi.stub.StubTree;
import consulo.language.psi.stub.StubTreeBuilder;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.stub.DefaultFileTypeSpecificInputFilter;
import consulo.index.io.ID;
import consulo.index.io.DataIndexer;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.KeyDescriptor;
import com.intellij.util.io.externalizer.StringCollectionExternalizer;
import jakarta.annotation.Nonnull;
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

    @Nonnull
    @Override
    public ID<TyFingerprint, List<String>> getName() {
        return KEY;
    }

    @Nonnull
    @Override
    public DataIndexer<TyFingerprint, List<String>, FileContent> getIndexer() {
        return inputData -> {
            StubTree stubTree = getStubTree(inputData);
            if (stubTree == null) return Collections.emptyMap();
            HashMap<TyFingerprint, List<String>> map = new HashMap<>();
            for (consulo.language.psi.stub.StubElement<?> stub : stubTree.getPlainList()) {
                if (stub instanceof RsTypeAliasStub) {
                    RsTypeAliasStub typeAliasStub = (RsTypeAliasStub) stub;
                    consulo.language.psi.PsiElement psi = typeAliasStub.getPsi();
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

                    consulo.language.psi.stub.StubElement<?> parentStub = aliasStub.getParentStub();
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

    @Nonnull
    @Override
    public KeyDescriptor<TyFingerprint> getKeyDescriptor() {
        return TyFingerprint.KEY_DESCRIPTOR;
    }

    @Nonnull
    @Override
    public DataExternalizer<List<String>> getValueExternalizer() {
        return StringCollectionExternalizer.STRING_LIST_EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(RsFileType.INSTANCE);
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Nonnull
    @Override
    public Collection<FileType> getFileTypesWithSizeLimitNotApplicable() {
        return Collections.singletonList(RsFileType.INSTANCE);
    }

    @Nonnull
    public static List<String> findPotentialAliases(@Nonnull Project project, @Nonnull TyFingerprint tyf) {
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
        consulo.language.psi.stub.Stub rootStub = MacroExpansionStubsProvider.findStubForMacroExpansionFile(inputData);
        if (rootStub == null) {
            rootStub = StubTreeBuilder.buildStubTree(inputData);
        }
        if (rootStub instanceof RsFileStub) {
            return new StubTree((RsFileStub) rootStub);
        }
        return null;
    }
}
