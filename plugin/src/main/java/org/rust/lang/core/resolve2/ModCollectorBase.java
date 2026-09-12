/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.language.psi.stub.StubElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.MacroCallBody;
import org.rust.lang.core.psi.RsProcMacroKind;
import org.rust.lang.core.psi.ext.QueryAttributes;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsExternCrateItemUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsMacro2Util;
import org.rust.lang.core.psi.ext.RsMacroUtil;
import org.rust.lang.core.psi.ext.RsModDeclItemUtil;
import org.rust.lang.core.psi.ext.RsModItemUtil;
import org.rust.lang.core.psi.ext.RsUseItemUtil;
import org.rust.lang.core.psi.ext.RsVisStubKind;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve2.util.PathUtil;
import org.rust.lang.core.stubs.RsAliasStub;
import org.rust.lang.core.stubs.RsAttrProcMacroOwnerStub;
import org.rust.lang.core.stubs.RsAttributeOwnerStub;
import org.rust.lang.core.stubs.RsEnumItemStub;
import org.rust.lang.core.stubs.RsExternCrateItemStub;
import org.rust.lang.core.stubs.RsForeignModStub;
import org.rust.lang.core.stubs.RsFunctionStub;
import org.rust.lang.core.stubs.RsImplItemStub;
import org.rust.lang.core.stubs.RsMacro2Stub;
import org.rust.lang.core.stubs.RsMacroCallStub;
import org.rust.lang.core.stubs.RsMacroStub;
import org.rust.lang.core.stubs.RsModDeclItemStub;
import org.rust.lang.core.stubs.RsModItemStub;
import org.rust.lang.core.stubs.RsNamedStub;
import org.rust.lang.core.stubs.RsPathStub;
import org.rust.lang.core.stubs.RsTraitItemStub;
import org.rust.lang.core.stubs.RsUseItemStub;
import org.rust.lang.core.stubs.RsVisStub;
import org.rust.stdext.HashCode;

import java.util.List;
import java.util.Set;

/**
 * Walks the stub tree of a module and reports every item that takes part in name resolution to a
 * {@link ModVisitor}.
 * <p>
 * Used when collecting explicit items (filling {@link ModData} and calculating the file hash), when
 * collecting items produced by a macro expansion, and when checking whether a file changed.
 */
public final class ModCollectorBase {

    @Nonnull
    private final ModVisitor visitor;
    @Nonnull
    private final Crate crate;
    private final boolean isDeeplyEnabledByCfg;

    private ModCollectorBase(@Nonnull ModVisitor visitor, @Nonnull Crate crate, boolean isDeeplyEnabledByCfg) {
        this.visitor = visitor;
        this.crate = crate;
        this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
    }

    public static void collectMod(
        @Nonnull StubElement<? extends RsElement> itemsOwner,
        boolean isDeeplyEnabledByCfg,
        @Nonnull ModVisitor visitor,
        @Nonnull Crate crate
    ) {
        ModCollectorBase collector = new ModCollectorBase(visitor, crate, isDeeplyEnabledByCfg);
        collector.collectElements(itemsOwner);
        visitor.afterCollectMod();
    }

    /** {@code itemsOwner} is the stub of an {@code RsMod} or an {@code RsForeignModItem}. */
    private void collectElements(@Nonnull StubElement<?> itemsOwner) {
        List<StubElement> items = itemsOwner.getChildrenStubs();

        // `#[macro_use] extern crate` imports macros for every item that follows, so extern crates are
        // hoisted ahead of the rest rather than being left to resolution order.
        for (StubElement item : items) {
            if (item instanceof RsExternCrateItemStub) {
                collectExternCrate((RsExternCrateItemStub) item);
            }
        }

        int macroIndexInParent = 0;
        for (StubElement item : items) {
            if (!(item instanceof RsExternCrateItemStub)) {
                collectElement(item, macroIndexInParent);
            }
            if (hasMacroIndex(item)) {
                macroIndexInParent++;
            }
        }
    }

    private void collectElement(@Nonnull StubElement<?> element, int macroIndexInParent) {
        // Attribute and derive proc macro calls are not collected: ProcMacroCallLight does not carry the
        // attribute paths and offsets their expansion needs, so the item is collected as written.
        if (element instanceof RsImplItemStub) {
            // impls are not named, so they play no part in name resolution
            return;
        }
        if (element instanceof RsForeignModStub) {
            collectElements(element);
            return;
        }
        if (element instanceof RsUseItemStub) {
            collectUseItem((RsUseItemStub) element);
            return;
        }
        if (element instanceof RsExternCrateItemStub) {
            throw new IllegalStateException("extern crates are processed eagerly");
        }
        if (element instanceof RsMacroCallStub) {
            collectMacroCall((RsMacroCallStub) element, macroIndexInParent);
            return;
        }
        if (element instanceof RsMacroStub) {
            collectMacroDef((RsMacroStub) element, macroIndexInParent);
            return;
        }
        if (element instanceof RsMacro2Stub) {
            collectMacro2Def((RsMacro2Stub) element);
            return;
        }
        // Must come after the macro stubs, which are named as well.
        if (element instanceof RsNamedStub) {
            collectItem((RsNamedStub) element, macroIndexInParent);
        }
        // Anything else - attributes, visibility, the ABI of a foreign mod - carries no name.
    }

    private void collectUseItem(@Nonnull RsUseItemStub useItem) {
        VisibilityLight visibility = visibilityOf(useItem);
        boolean hasPreludeImport = RsUseItemUtil.HAS_PRELUDE_IMPORT_PROP.getByStub(useItem, crate);
        boolean enabled = isDeeplyEnabledByCfg && isEnabledByCfgSelf(useItem);
        PathUtil.forEachLeafSpeck(useItem, (usePath, alias, isStarImport, offsetInExpansion) -> {
            // `use self;` imports nothing
            if (alias == null && usePath.length == 1 && "self".equals(usePath[0])) return;

            visitor.collectImport(new ImportLight(
                usePath,
                alias,
                visibility,
                isStarImport,
                false,
                hasPreludeImport,
                enabled,
                offsetInExpansion
            ));
        });
    }

    private void collectExternCrate(@Nonnull RsExternCrateItemStub externCrate) {
        String name = externCrate.getName();
        if (name == null) return;

        RsAliasStub aliasStub = externCrate.getAlias();
        String alias = aliasStub != null ? aliasStub.getName() : null;
        // `extern crate self;` without an alias brings nothing new into scope.
        if ("self".equals(name) && alias == null) return;

        visitor.collectImport(new ImportLight(
            new String[]{name},
            alias,
            visibilityOf(externCrate),
            false,
            true,
            false,
            isDeeplyEnabledByCfg && isEnabledByCfgSelf(externCrate),
            RsExternCrateItemUtil.EXTERN_CRATE_HAS_MACRO_USE_PROP.getByStub(externCrate, crate),
            -1
        ));
    }

    private void collectItem(@Nonnull RsNamedStub item, int macroIndexInParent) {
        if (item instanceof RsEnumItemStub || item instanceof RsModItemStub || item instanceof RsModDeclItemStub) {
            collectModOrEnum(item, macroIndexInParent);
        }
        else {
            collectSimpleItem(item);
        }
    }

    private void collectModOrEnum(@Nonnull RsNamedStub item, int macroIndexInParent) {
        if (!(item instanceof RsAttributeOwnerStub)) return;
        String name = item.getName();
        if (name == null) return;

        RsAttributeOwnerStub attributeOwner = (RsAttributeOwnerStub) item;
        boolean hasMacroUse;
        String pathAttribute;
        if (item instanceof RsModItemStub) {
            hasMacroUse = RsModItemUtil.MOD_ITEM_HAS_MACRO_USE_PROP.getByStub((RsModItemStub) item, crate);
            pathAttribute = pathAttributeOf(attributeOwner);
        }
        else if (item instanceof RsModDeclItemStub) {
            hasMacroUse = RsModDeclItemUtil.MOD_DECL_HAS_MACRO_USE_PROP.getByStub((RsModDeclItemStub) item, crate);
            pathAttribute = pathAttributeOf(attributeOwner);
        }
        else {
            hasMacroUse = false;
            pathAttribute = null;
        }

        visitor.collectModOrEnumItem(new ModOrEnumItemLight(
            name,
            visibilityOf((StubElement<?>) item),
            isDeeplyEnabledByCfg && isEnabledByCfgSelf(attributeOwner),
            Namespace.getNamespaces((StubElement<?>) item, crate),
            macroIndexInParent,
            pathAttribute,
            hasMacroUse
        ), item);
    }

    private void collectSimpleItem(@Nonnull RsNamedStub item) {
        SimpleItemLight itemLight = lowerSimpleItem(item);
        if (itemLight != null) {
            visitor.collectSimpleItem(itemLight);
        }
    }

    @Nullable
    private SimpleItemLight lowerSimpleItem(@Nonnull RsNamedStub item) {
        if (!(item instanceof RsAttributeOwnerStub)) return null;
        RsAttributeOwnerStub attributeOwner = (RsAttributeOwnerStub) item;

        String procMacroName = null;
        RsProcMacroKind procMacroKind = null;
        if (item instanceof RsFunctionStub
            && RsFunctionUtil.IS_PROC_MACRO_DEF_PROP.getByStub((RsFunctionStub) item, crate)) {
            QueryAttributes<?> attributes = attributesOf(attributeOwner);
            procMacroKind = RsProcMacroKind.fromDefAttributes(attributes);
            procMacroName = attributes.getFirstArgOfSingularAttribute("proc_macro_derive");
        }

        String name = procMacroName != null ? procMacroName : item.getName();
        if (name == null) return null;

        return new SimpleItemLight(
            name,
            visibilityOf((StubElement<?>) item),
            isDeeplyEnabledByCfg && isEnabledByCfgSelf(attributeOwner),
            Namespace.getNamespaces((StubElement<?>) item, crate),
            item instanceof RsTraitItemStub,
            procMacroKind
        );
    }

    private void collectMacroCall(@Nonnull RsMacroCallStub call, int macroIndexInParent) {
        if (!(isDeeplyEnabledByCfg && isEnabledByCfgSelf(call))) return;
        String body = call.getMacroBody();
        if (body == null) return;
        RsPathStub path = call.getPath();
        if (path == null) return;
        String[] pathSegments = PathUtil.getPathWithAdjustedDollarCrate(path);
        if (pathSegments == null) return;

        int bodyStart = call.getBodyStartOffset();
        visitor.collectMacroCall(new MacroCallLight(
            pathSegments,
            new MacroCallBody.FunctionLike(body),
            call.getBodyHash(),
            true,
            macroIndexInParent,
            path.getStartOffset(),
            bodyStart,
            bodyStart < 0 ? -1 : bodyStart + body.length()
        ), call);
    }

    private void collectMacroDef(@Nonnull RsMacroStub def, int macroIndexInParent) {
        if (!(isDeeplyEnabledByCfg && isEnabledByCfgSelf(def))) return;
        String name = def.getName();
        if (name == null) return;
        String body = def.getMacroBody();
        if (body == null) return;
        HashCode bodyHash = def.getBodyHash();
        if (bodyHash == null) return;

        visitor.collectMacroDef(new MacroDefLight(
            name,
            body,
            bodyHash.toString(),
            RsMacroUtil.HAS_MACRO_EXPORT_PROP.getByStub(def, crate),
            RsMacroUtil.HAS_MACRO_EXPORT_LOCAL_INNER_MACROS_PROP.getByStub(def, crate),
            RsMacroUtil.HAS_RUSTC_BUILTIN_MACRO_PROP.getByStub(def, crate),
            true,
            macroIndexInParent
        ));
    }

    private void collectMacro2Def(@Nonnull RsMacro2Stub def) {
        if (!(isDeeplyEnabledByCfg && isEnabledByCfgSelf(def))) return;
        String name = def.getName();
        if (name == null) return;
        String body = def.getMacroBody();
        if (body == null) return;
        HashCode bodyHash = def.getBodyHash();
        if (bodyHash == null) return;

        visitor.collectMacro2Def(new Macro2DefLight(
            name,
            body,
            bodyHash.toString(),
            RsMacro2Util.MACRO2_HAS_RUSTC_BUILTIN_MACRO_PROP.getByStub(def, crate),
            visibilityOf(def),
            true
        ));
    }

    /** Only these two shift the index their siblings are numbered by. */
    private static boolean hasMacroIndex(@Nonnull StubElement<?> element) {
        return element instanceof RsModDeclItemStub || element instanceof RsAttrProcMacroOwnerStub;
    }

    private boolean isEnabledByCfgSelf(@Nonnull RsAttributeOwnerStub stub) {
        if (!stub.getMayHaveCfg()) return true;
        return RsDocAndAttributeOwnerUtil.evaluateCfg(stub, crate) != ThreeValuedLogic.False;
    }

    @Nonnull
    private QueryAttributes<?> attributesOf(@Nonnull RsAttributeOwnerStub stub) {
        return new QueryAttributes<>(stub.getRawMetaItems());
    }

    @Nullable
    private String pathAttributeOf(@Nonnull RsAttributeOwnerStub stub) {
        if (!stub.getHasAttrs()) return null;
        return attributesOf(stub).lookupStringValueForKey("path");
    }

    @Nonnull
    private static VisibilityLight visibilityOf(@Nonnull StubElement<?> item) {
        RsVisStub vis = (RsVisStub) item.findChildStubByType(RsVisStub.Type);
        if (vis == null) return VisibilityLight.PRIV;
        RsVisStubKind kind = vis.getKind();
        if (kind == RsVisStubKind.PUB) return VisibilityLight.PUB;
        if (kind == RsVisStubKind.CRATE) return VisibilityLight.PUB_CRATE;

        String[] path = PathUtil.getRestrictedPath(vis);
        if (path == null || path.length == 0) return VisibilityLight.PUB_CRATE;
        if (path.length == 1) {
            if ("crate".equals(path[0])) return VisibilityLight.PUB_CRATE;
            if ("self".equals(path[0])) return VisibilityLight.PRIV;
        }
        return new VisibilityLight(VisibilityLight.Kind.RESTRICTED, path);
    }
}
