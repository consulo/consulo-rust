/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.util.io.DigestUtil;
import com.intellij.util.io.IOUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.stubs.RsEnumItemStub;
import org.rust.lang.core.stubs.RsEnumVariantStub;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsMacroCallStub;
import org.rust.lang.core.stubs.RsModItemStub;
import org.rust.lang.core.stubs.RsNamedStub;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.VirtualFileExtUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.stubs.RsAttributeOwnerStub;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;
import org.rust.stdext.HashCode;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.*;
import org.rust.lang.core.psi.ext.RsEnumItemUtil;

/**
 * Provides functions for tracking file modifications for the resolve system.
 */
public final class FileModificationTracker {

    private FileModificationTracker() {}

    /**
     * Writeable interface for objects that can be written to a DataOutput.
     */
    public interface Writeable {
        void writeTo(@NotNull DataOutput data);
    }

    /**
     * Gets the modification stamp used for resolve.
     * Extension function on RsFile.
     */
    public static long getModificationStampForResolve(@NotNull RsFile file) {
        return file.getViewProvider().getModificationStamp();
    }

    /**
     * Checks if a file has changed compared to the DefMap.
     */
    public static boolean isFileChanged(@NotNull RsFile file, @NotNull CrateDefMap defMap, @NotNull Crate crate) {
        int fileId = VirtualFileExtUtil.getFileId(file.getVirtualFile());
        FileInfo fileInfo = defMap.getFileInfos().get(fileId);
        if (fileInfo == null) {
            if (OpenApiUtil.isUnitTestMode()) {
                throw new IllegalStateException("Can't find fileInfo for " + file.getVirtualFile() + " in " + defMap);
            }
            return false;
        }

        boolean isCrateRoot = Objects.equals(file.getVirtualFile(), crate.getRootModFile());
        if (isCrateRoot && file.getRecursionLimit(crate) != defMap.getRecursionLimitRaw()) return true;

        boolean isEnabledByCfgInner = RsElementUtil.isEnabledByCfgSelf(file, crate);
        boolean isDeeplyEnabledByCfg = fileInfo.getModData().isDeeplyEnabledByCfgOuter() && isEnabledByCfgInner;
        HashCalculator hashCalculator = new HashCalculator(isEnabledByCfgInner);
        ModLightCollector visitor = new ModLightCollector(
            crate,
            hashCalculator,
            "",
            true,
            isCrateRoot ? file.getStdlibAttributes(crate) : null
        );
        RsFileStub fileStub = ModCollector.getOrBuildFileStub(file);
        if (fileStub == null) return false;
        ModCollectorBase.collectMod(fileStub, isDeeplyEnabledByCfg, visitor, crate);
        return !hashCalculator.getFileHash().equals(fileInfo.getHash());
    }

    /**
     * Calculates hash for a single module.
     */
    @NotNull
    private static HashCode calculateModHash(@NotNull ModDataLight modData, @Nullable RsFile.Attributes stdlibAttributes) {
        MessageDigest digest = DigestUtil.sha1();
        DataOutputStream data = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest));

        modData.sort();
        try {
            writeItemLights(data, modData.items);
            writeEnumLights(data, modData.enums);
            writeImportLights(data, modData.imports);
            writeCount(data, modData.macroCalls.size());
            writeCount(data, modData.procMacroCalls.size());
            writeCount(data, modData.macroDefs.size());
            writeMacro2DefLights(data, modData.macro2Defs);
            data.writeByte(stdlibAttributes != null ? stdlibAttributes.ordinal() : RsFile.Attributes.values().length);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }

        return HashCode.fromByteArray(digest.digest());
    }

    private static void writeElements(@NotNull DataOutput data, @NotNull List<? extends Writeable> elements) throws java.io.IOException {
        org.rust.stdext.IoUtil.writeVarInt(data, elements.size());
        for (Writeable element : elements) {
            element.writeTo(data);
        }
    }

    private static void writeItemLights(@NotNull DataOutput data, @NotNull List<ItemLight> items) throws java.io.IOException {
        org.rust.stdext.IoUtil.writeVarInt(data, items.size());
        for (ItemLight item : items) {
            item.writeTo(data);
        }
    }

    private static void writeEnumLights(@NotNull DataOutput data, @NotNull List<EnumLight> enums) throws java.io.IOException {
        org.rust.stdext.IoUtil.writeVarInt(data, enums.size());
        for (EnumLight e : enums) {
            e.writeTo(data);
        }
    }

    private static void writeImportLights(@NotNull DataOutput data, @NotNull List<ImportLight> imports) throws java.io.IOException {
        org.rust.stdext.IoUtil.writeVarInt(data, imports.size());
        for (ImportLight imp : imports) {
            IOUtil.writeUTF(data, Arrays.toString(imp.getUsePath()));
        }
    }

    private static void writeMacro2DefLights(@NotNull DataOutput data, @NotNull List<Macro2DefLight> defs) throws java.io.IOException {
        org.rust.stdext.IoUtil.writeVarInt(data, defs.size());
        for (Macro2DefLight def : defs) {
            IOUtil.writeUTF(data, def.getName());
        }
    }

    private static void writeCount(@NotNull DataOutput data, int count) throws java.io.IOException {
        org.rust.stdext.IoUtil.writeVarInt(data, count);
    }

    /**
     * Lightweight module data for hash calculation.
     */
    static class ModDataLight {
        @NotNull final List<ItemLight> items = new ArrayList<>();
        @NotNull final List<EnumLight> enums = new ArrayList<>();
        @NotNull final List<ImportLight> imports = new ArrayList<>();
        @NotNull final List<MacroCallLight> macroCalls = new ArrayList<>();
        @NotNull final List<ProcMacroCallLight> procMacroCalls = new ArrayList<>();
        @NotNull final List<MacroDefLight> macroDefs = new ArrayList<>();
        @NotNull final List<Macro2DefLight> macro2Defs = new ArrayList<>();

        void sort() {
            items.sort(Comparator.comparing(ItemLight::getName));
            enums.sort(Comparator.comparing(e -> e.item.getName()));
            imports.sort(Comparator.comparing(i -> Arrays.toString(i.getUsePath())));
            macro2Defs.sort(Comparator.comparing(Macro2DefLight::getName));
        }
    }

    /**
     * Calculates hash of a single file (including all inline modules).
     */
    public static class HashCalculator {
        private final boolean isEnabledByCfgInner;
        private final List<ModHash> modulesHash = new ArrayList<>();

        public HashCalculator(boolean isEnabledByCfgInner) {
            this.isEnabledByCfgInner = isEnabledByCfgInner;
        }

        @NotNull
        public ModVisitor getVisitor(
            @NotNull Crate crate,
            @NotNull String fileRelativePath,
            @Nullable RsFile.Attributes stdlibAttributes
        ) {
            return new ModLightCollector(crate, this, fileRelativePath, false, stdlibAttributes);
        }

        public void onCollectMod(@NotNull String fileRelativePath, @NotNull HashCode hash) {
            modulesHash.add(new ModHash(fileRelativePath, hash));
        }

        @NotNull
        public HashCode getFileHash() {
            modulesHash.sort(Comparator.comparing(mh -> mh.fileRelativePath));
            MessageDigest digest = DigestUtil.sha1();
            for (ModHash modHash : modulesHash) {
                digest.update(modHash.fileRelativePath.getBytes());
                digest.update(modHash.hash.toByteArray());
            }
            digest.update((byte) (isEnabledByCfgInner ? 1 : 0));
            return HashCode.fromByteArray(digest.digest());
        }

        private static class ModHash {
            @NotNull final String fileRelativePath;
            @NotNull final HashCode hash;

            ModHash(@NotNull String fileRelativePath, @NotNull HashCode hash) {
                this.fileRelativePath = fileRelativePath;
                this.hash = hash;
            }
        }
    }

    /**
     * Lightweight collector for module items used in hash calculation.
     */
    private static class ModLightCollector implements ModVisitor {
        @NotNull private final Crate crate;
        @NotNull private final HashCalculator hashCalculator;
        @NotNull private final String fileRelativePath;
        private final boolean collectChildModules;
        @Nullable private final RsFile.Attributes stdlibAttributes;
        @NotNull final ModDataLight modData = new ModDataLight();

        ModLightCollector(
            @NotNull Crate crate,
            @NotNull HashCalculator hashCalculator,
            @NotNull String fileRelativePath,
            boolean collectChildModules,
            @Nullable RsFile.Attributes stdlibAttributes
        ) {
            this.crate = crate;
            this.hashCalculator = hashCalculator;
            this.fileRelativePath = fileRelativePath;
            this.collectChildModules = collectChildModules;
            this.stdlibAttributes = stdlibAttributes;
        }

        @Override
        public void collectSimpleItem(@NotNull SimpleItemLight item) {
            modData.items.add(item);
        }

        @Override
        public void collectModOrEnumItem(@NotNull ModOrEnumItemLight item, @NotNull RsNamedStub stub) {
            if (stub instanceof RsEnumItemStub) {
                collectEnum(item, (RsEnumItemStub) stub);
                return;
            }
            modData.items.add(item);
            if (collectChildModules && stub instanceof RsModItemStub) {
                collectMod((RsModItemStub) stub, item.getName(), item.isDeeplyEnabledByCfg());
            }
        }

        private void collectEnum(@NotNull ModOrEnumItemLight enumItem, @NotNull RsEnumItemStub enumStub) {
            List<EnumVariantLight> variants = new ArrayList<>();
            for (RsEnumVariantStub variantStub : RsEnumItemUtil.getVariants(enumStub)) {
                String name = variantStub.getName();
                if (name == null) continue;
                boolean isVariantDeeplyEnabled = enumItem.isDeeplyEnabledByCfg()
                    && RsDocAndAttributeOwnerUtil.evaluateCfg(variantStub, crate) != ThreeValuedLogic.False;
                boolean hasBlockFields = variantStub.getBlockFields() != null;
                variants.add(new EnumVariantLight(name, isVariantDeeplyEnabled, hasBlockFields));
            }
            variants.sort(Comparator.comparing(v -> v.name));
            modData.enums.add(new EnumLight(enumItem, variants));
        }

        @Override
        public void collectImport(@NotNull ImportLight importItem) {
            modData.imports.add(importItem);
        }

        @Override
        public void collectMacroCall(@NotNull MacroCallLight call, @NotNull RsMacroCallStub stub) {
            modData.macroCalls.add(call);
        }

        @Override
        public void collectProcMacroCall(@NotNull ProcMacroCallLight call) {
            modData.procMacroCalls.add(call);
        }

        @Override
        public void collectMacroDef(@NotNull MacroDefLight def) {
            modData.macroDefs.add(def);
        }

        @Override
        public void collectMacro2Def(@NotNull Macro2DefLight def) {
            modData.macro2Defs.add(def);
        }

        @Override
        public void afterCollectMod() {
            HashCode fileHash = calculateModHash(modData, stdlibAttributes);
            hashCalculator.onCollectMod(fileRelativePath, fileHash);
        }

        private void collectMod(@NotNull RsModItemStub mod, @NotNull String modName, boolean isDeeplyEnabledByCfg) {
            String childRelativePath = fileRelativePath + "::" + modName;
            ModLightCollector visitor = new ModLightCollector(crate, hashCalculator, childRelativePath, true, null);
            ModCollectorBase.collectMod(mod, isDeeplyEnabledByCfg, visitor, crate);
        }
    }

    /**
     * Lightweight representation of an enum variant for hash calculation.
     */
    private static class EnumVariantLight implements Writeable {
        @NotNull final String name;
        final boolean isDeeplyEnabledByCfg;
        final boolean hasBlockFields;

        private static final int IS_DEEPLY_ENABLED_BY_CFG_MASK = 1;
        private static final int HAS_BLOCK_FIELDS = 2;

        EnumVariantLight(@NotNull String name, boolean isDeeplyEnabledByCfg, boolean hasBlockFields) {
            this.name = name;
            this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
            this.hasBlockFields = hasBlockFields;
        }

        @Override
        public void writeTo(@NotNull DataOutput data) {
            try {
                IOUtil.writeUTF(data, name);
                int flags = 0;
                if (isDeeplyEnabledByCfg) flags |= IS_DEEPLY_ENABLED_BY_CFG_MASK;
                if (hasBlockFields) flags |= HAS_BLOCK_FIELDS;
                data.writeByte(flags);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Lightweight representation of an enum for hash calculation.
     */
    private static class EnumLight implements Writeable {
        @NotNull final ModOrEnumItemLight item;
        @NotNull final List<EnumVariantLight> variants;

        EnumLight(@NotNull ModOrEnumItemLight item, @NotNull List<EnumVariantLight> variants) {
            this.item = item;
            this.variants = variants;
        }

        @Override
        public void writeTo(@NotNull DataOutput data) {
            try {
                item.writeTo(data);
                writeElements(data, variants);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
