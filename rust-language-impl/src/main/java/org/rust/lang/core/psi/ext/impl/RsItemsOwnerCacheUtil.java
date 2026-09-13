/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import consulo.util.dataholder.Key;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

import java.util.*;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.*;

/**
 * Cache utility for expanded items in RsItemsOwner.
 * <p>
 */
public final class RsItemsOwnerCacheUtil {
    private static final Key<CachedValue<RsCachedItems>> EXPANDED_ITEMS_KEY =
        Key.create("EXPANDED_ITEMS_KEY");

    private RsItemsOwnerCacheUtil() {
    }

    @Nonnull
    public static RsCachedItems getExpandedItemsCached(@Nonnull RsItemsOwner owner) {
        return CachedValuesManager.getManager(owner.getProject()).getCachedValue(owner, EXPANDED_ITEMS_KEY, () -> {
            List<RsUseItem> imports = new ArrayList<>();
            List<RsMacro> macros = new ArrayList<>();
            Map<String, List<RsItemElement>> named = new LinkedHashMap<>();

            processExpandedItemsInternal(owner, element -> {
                if (element instanceof RsUseItem) {
                    imports.add((RsUseItem) element);
                } else if (element instanceof RsMacro) {
                    macros.add((RsMacro) element);
                } else if (element instanceof RsItemElement) {
                    RsItemElement item = (RsItemElement) element;
                    if (item instanceof RsForeignModItem && isEnabledByCfgSelf(item)) {
                        for (RsItemElement child : PsiElementUtil.stubChildrenOfType(item, RsItemElement.class)) {
                            String childName = child.getName();
                            if (childName != null) {
                                named.computeIfAbsent(childName, k -> new ArrayList<>()).add(child);
                            }
                        }
                    } else {
                        String name;
                        if (item instanceof RsExternCrateItem) {
                            name = RsExternCrateItemUtil.getNameWithAlias((RsExternCrateItem) item);
                        } else if (item instanceof RsFunction && RsFunctionUtil.isProcMacroDef((RsFunction) item)) {
                            name = RsFunctionUtil.getProcMacroName((RsFunction) item);
                        } else {
                            name = item.getName();
                        }
                        if (name != null) {
                            named.computeIfAbsent(name, k -> new ArrayList<>()).add(item);
                        }
                    }
                }
                return false;
            });

            // In the case of ambiguity, remove cfg-disabled items
            for (Map.Entry<String, List<RsItemElement>> entry : named.entrySet()) {
                List<RsItemElement> value = entry.getValue();
                if (value.size() > 1) {
                    List<RsItemElement> cfgEnabled = new ArrayList<>();
                    for (RsItemElement item : value) {
                        if (isEnabledByCfgSelf(item)) cfgEnabled.add(item);
                    }
                    if (!cfgEnabled.isEmpty()) {
                        entry.setValue(cfgEnabled);
                    }
                }
            }

            Object modTracker = RsPsiUtilUtil.getRustStructureOrAnyPsiModificationTracker(owner);
            return CachedValueProvider.Result.create(
                new RsCachedItems(imports, macros, named),
                modTracker
            );
        }, false);
    }

    private static boolean processExpandedItemsInternal(@Nonnull RsItemsOwner owner,
                                                         @Nonnull java.util.function.Predicate<RsElement> processor) {
        for (RsElement element : (Iterable<RsElement>) () -> RsItemsOwnerUtil.getItemsAndMacros(owner).iterator()) {
            if (processItem(element, false, processor)) return true;
        }
        return false;
    }

    private static boolean processItem(@Nonnull RsElement element,
                                        boolean withMacroCalls,
                                        @Nonnull java.util.function.Predicate<RsElement> processor) {
        if (element instanceof RsItemElement || element instanceof RsMacro) {
            return processor.test(element);
        }
        if (element instanceof RsMacroCall) {
            if (withMacroCalls && processor.test(element)) return true;
            // Macro expansion handling would be needed here for full fidelity
        }
        return false;
    }

    private static boolean isEnabledByCfgSelf(@Nonnull RsElement element) {
        if (!(element instanceof RsDocAndAttributeOwner)) return true;
        return RsDocAndAttributeOwnerUtil.evaluateCfg(
            (RsDocAndAttributeOwner) element, null
        ) != org.rust.lang.utils.evaluation.ThreeValuedLogic.False;
    }
}
