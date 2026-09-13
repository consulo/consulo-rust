/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.imports;

import jakarta.annotation.Nonnull;
import org.rust.settings.RsCodeInsightSettings;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.rust.lang.core.imports.ImportCandidate;
import org.rust.lang.core.imports.ImportCandidatesCollector;
import org.rust.lang.core.imports.ImportContext;
import org.rust.lang.core.imports.ImportInfo;
import org.rust.lang.core.imports.ImportUtils;
import org.rust.lang.core.imports.RsImportHelper;
import org.rust.lang.core.imports.TypeReferencesInfo;

/**
 * {@code org.rust.ide.utils.imports} package because {@code import} is a reserved Java keyword.
 */
public final class ImportBridge {
    private ImportBridge() {
    }

    public static void importElement(@Nonnull RsElement context, @Nonnull RsQualifiedNamedElement element) {
        importElements(context, Collections.singleton(element));
    }

    public static void importElements(@Nonnull RsElement context, @Nonnull Set<RsQualifiedNamedElement> elements) {
        if (!RsCodeInsightSettings.getInstance().importOutOfScopeItems) return;
        ImportContext importContext = ImportContext.from(context, ImportContext.Type.OTHER);
        if (importContext == null) return;
        for (RsQualifiedNamedElement element : elements) {
            ImportCandidate candidate = ImportCandidatesCollector.findImportCandidate(importContext, element);
            if (candidate != null) {
                importCandidate(candidate, context);
            }
        }
    }

    public static void importTypeReferencesFromElement(@Nonnull RsElement context, @Nonnull RsElement element) {
        TypeReferencesInfo info = RsImportHelper.getTypeReferencesInfoFromElement(context, element);
        importElements(context, info.getToImport());
    }

    public static void importTypeReferencesFromTy(@Nonnull RsElement context, @Nonnull Ty ty) {
        importTypeReferencesFromTys(context, Collections.singletonList(ty));
    }

    public static void importTypeReferencesFromTys(@Nonnull RsElement context, @Nonnull Collection<Ty> tys) {
        TypeReferencesInfo info = RsImportHelper.getTypeReferencesInfoFromTys(context, tys.toArray(new Ty[0]));
        importElements(context, info.getToImport());
    }

    // --- ImportInfoBridge methods ---

    @Nonnull
    public static Object createLocalImportInfo(@Nonnull String usePath) {
        return new ImportInfo.LocalImportInfo(usePath);
    }

    public static void doImport(@Nonnull Object importInfo, @Nonnull RsElement context) {
        if (!(importInfo instanceof ImportInfo)) return;
        ImportInfo info = (ImportInfo) importInfo;
        if (RsElementUtil.isIntentionPreviewElement(context)) return;

        info.insertExternCrateIfNeeded(context);

        RsPsiFactory psiFactory = new RsPsiFactory(context.getProject());
        RsMod insertionScope = RsElementUtil.getContainingMod(context);
        if (insertionScope == null) return;

        RsUseItem useItem = psiFactory.createUseItem(info.getUsePath());
        ImportUtils.insertUseItem(insertionScope, psiFactory, useItem);
    }

    /** {@code fun ImportCandidate.import(context) = info.import(context)}. */
    public static void importCandidate(@Nonnull Object candidate, @Nonnull RsElement context) {
        if (!(candidate instanceof ImportCandidate)) return;
        doImport(((ImportCandidate) candidate).getInfo(), context);
    }

    // --- stdlibAttributes bridge ---

    @Nonnull
    public static RsFile.Attributes getStdlibAttributes(@Nonnull RsElement element) {
        return ImportUtils.getStdlibAttributes(element);
    }
}
