/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Used for optimization purposes, to reduce access to a cache and PSI tree in one very hot
 * place - processItemDeclarations.
 * <p>
 */
public class RsCachedItems {
    @Nonnull
    private final List<RsUseItem> myImports;
    @Nonnull
    private final List<RsMacro> myLegacyMacros;
    @Nonnull
    private final Map<String, List<RsItemElement>> myNamed;

    private volatile List<RsItemElement> myCfgEnabledNamedItems;

    public RsCachedItems(@Nonnull List<RsUseItem> imports,
                         @Nonnull List<RsMacro> legacyMacros,
                         @Nonnull Map<String, List<RsItemElement>> named) {
        myImports = imports;
        myLegacyMacros = legacyMacros;
        myNamed = named;
    }

    @Nonnull
    public List<RsUseItem> getImports() {
        return myImports;
    }

    /** May contain cfg-disabled items. RsMacro2 are stored in named. */
    @Nonnull
    public List<RsMacro> getLegacyMacros() {
        return myLegacyMacros;
    }

    /** May contain cfg-disabled items. */
    @Nonnull
    public Map<String, List<RsItemElement>> getNamed() {
        return myNamed;
    }

    @Nonnull
    public List<RsItemElement> getCfgEnabledNamedItems() {
        List<RsItemElement> result = myCfgEnabledNamedItems;
        if (result == null) {
            synchronized (this) {
                result = myCfgEnabledNamedItems;
                if (result == null) {
                    result = new ArrayList<>();
                    for (List<RsItemElement> items : myNamed.values()) {
                        for (RsItemElement item : items) {
                            if (isEnabledByCfgSelf(item)) {
                                result.add(item);
                            }
                        }
                    }
                    result = Collections.unmodifiableList(result);
                    myCfgEnabledNamedItems = result;
                }
            }
        }
        return result;
    }

    @Nullable
    public List<RsItemElement> getNamedElementsIfCfgEnabled(@Nonnull String name) {
        List<RsItemElement> items = myNamed.get(name);
        if (items == null) return null;
        List<RsItemElement> enabled = new ArrayList<>();
        for (RsItemElement item : items) {
            if (isEnabledByCfgSelf(item)) {
                enabled.add(item);
            }
        }
        return enabled.isEmpty() ? null : enabled;
    }

    private static boolean isEnabledByCfgSelf(@Nonnull RsItemElement item) {
        if (!(item instanceof RsDocAndAttributeOwner)) return true;
        return RsDocAndAttributeOwnerUtil.evaluateCfg((RsDocAndAttributeOwner) item, null) != ThreeValuedLogic.False;
    }
}
