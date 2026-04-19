/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementExtUtil;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.psi.RsPsiManagerUtil;
import org.rust.lang.core.resolve.indexes.RsLangItemIndex;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CargoProject-related object that allows to lookup rust items in project dependencies,
 * including the standard library.
 */
public class KnownItems {
    @NotNull
    private final KnownItemsLookup lookup;

    private static final Key<CachedValue<KnownItems>> KNOWN_ITEMS_KEY = Key.create("KNOWN_ITEMS_KEY");
    private static final KnownItems DUMMY = new KnownItems(DummyKnownItemsLookup.INSTANCE);

    public KnownItems(@NotNull KnownItemsLookup lookup) {
        this.lookup = lookup;
    }

    @Nullable
    public RsNamedElement findLangItemRaw(@NotNull String langAttribute, @NotNull String crateName) {
        return lookup.findLangItem(langAttribute, crateName);
    }

    @Nullable
    public RsNamedElement findItemRaw(@NotNull String path, boolean isStd) {
        return lookup.findItem(path, isStd);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends RsNamedElement> T findLangItem(@NotNull String langAttribute, @NotNull String crateName, @NotNull Class<T> clazz) {
        RsNamedElement raw = findLangItemRaw(langAttribute, crateName);
        return clazz.isInstance(raw) ? clazz.cast(raw) : null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends RsNamedElement> T findItem(@NotNull String path, boolean isStd, @NotNull Class<T> clazz) {
        RsNamedElement raw = findItemRaw(path, isStd);
        return clazz.isInstance(raw) ? clazz.cast(raw) : null;
    }

    // --- Struct/Enum items ---
    @Nullable public RsStructOrEnumItemElement getVec() { return findItem("alloc::vec::Vec", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getString() { return findItem("alloc::string::String", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getArguments() { return findItem("core::fmt::Arguments", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getOption() { return findItem("core::option::Option", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getResult() { return findItem("core::result::Result", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getRc() { return findItem("alloc::rc::Rc", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getArc() {
        RsStructOrEnumItemElement r = findItem("alloc::sync::Arc", true, RsStructOrEnumItemElement.class);
        return r != null ? r : findItem("alloc::arc::Arc", true, RsStructOrEnumItemElement.class);
    }
    @Nullable public RsStructOrEnumItemElement getCell() { return findItem("core::cell::Cell", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getRefCell() { return findItem("core::cell::RefCell", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getUnsafeCell() { return findItem("core::cell::UnsafeCell", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getMutex() { return findItem("std::sync::mutex::Mutex", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getPathItem() { return findItem("std::path::Path", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getPathBuf() { return findItem("std::path::PathBuf", true, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getCStr() { return findItem("std::ffi::CStr", true, RsStructOrEnumItemElement.class); }

    // --- Trait items ---
    @Nullable public RsTraitItem getIterator() { return findItem("core::iter::Iterator", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getIntoIterator() { return findItem("core::iter::IntoIterator", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getAsRef() { return findItem("core::convert::AsRef", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getAsMut() { return findItem("core::convert::AsMut", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getFrom() { return findItem("core::convert::From", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getTryFrom() { return findItem("core::convert::TryFrom", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getFromStr() { return findItem("core::str::FromStr", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getBorrow() { return findItem("core::borrow::Borrow", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getBorrowMut() { return findItem("core::borrow::BorrowMut", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getHash() { return findItem("core::hash::Hash", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getDefault() { return findItem("core::default::Default", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getDisplay() { return findItem("core::fmt::Display", true, RsTraitItem.class); }
    @Nullable public RsStructItem getFormatter() { return findItem("core::fmt::Formatter", true, RsStructItem.class); }
    @Nullable public RsTraitItem getToOwned() { return findItem("alloc::borrow::ToOwned", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getToString() { return findItem("alloc::string::ToString", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getTry() {
        RsTraitItem t = findItem("core::ops::try_trait::Try", true, RsTraitItem.class);
        return t != null ? t : findItem("core::ops::try::Try", true, RsTraitItem.class);
    }
    @Nullable public RsTraitItem getGenerator() { return findItem("core::ops::generator::Generator", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getFuture() { return findItem("core::future::future::Future", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getIntoFuture() { return findItem("core::future::into_future::IntoFuture", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getOctal() { return findItem("core::fmt::Octal", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getLowerHex() { return findItem("core::fmt::LowerHex", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getUpperHex() { return findItem("core::fmt::UpperHex", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getPointer() { return findItem("core::fmt::Pointer", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getBinary() { return findItem("core::fmt::Binary", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getLowerExp() { return findItem("core::fmt::LowerExp", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getUpperExp() { return findItem("core::fmt::UpperExp", true, RsTraitItem.class); }

    // --- Lang items ---
    @Nullable public RsTraitItem getDeref() { return findLangItem("deref", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getDrop() { return findLangItem("drop", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getSized() { return findLangItem("sized", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getUnsize() { return findLangItem("unsize", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getCoerceUnsized() { return findLangItem("coerce_unsized", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getDestruct() { return findLangItem("destruct", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getFn() { return findLangItem("fn", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getFnMut() { return findLangItem("fn_mut", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getFnOnce() { return findLangItem("fn_once", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getIndex() { return findLangItem("index", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getIndexMut() { return findLangItem("index_mut", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getClone() { return findLangItem("clone", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getCopy() { return findLangItem("copy", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getPartialEq() { return findLangItem("eq", AutoInjectedCrates.CORE, RsTraitItem.class); }
    @Nullable public RsTraitItem getEq() { return findItem("core::cmp::Eq", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getPartialOrd() {
        RsTraitItem t = findLangItem("partial_ord", AutoInjectedCrates.CORE, RsTraitItem.class);
        return t != null ? t : findItem("core::cmp::PartialOrd", true, RsTraitItem.class);
    }
    @Nullable public RsTraitItem getOrd() { return findItem("core::cmp::Ord", true, RsTraitItem.class); }
    @Nullable public RsTraitItem getDebug() {
        RsTraitItem t = findLangItem("debug_trait", AutoInjectedCrates.CORE, RsTraitItem.class);
        return t != null ? t : findItem("core::fmt::Debug", true, RsTraitItem.class);
    }
    @Nullable public RsStructOrEnumItemElement getBox() { return findLangItem("owned_box", "alloc", RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getPin() { return findLangItem("pin", "core", RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getManuallyDrop() { return findLangItem("manually_drop", AutoInjectedCrates.CORE, RsStructOrEnumItemElement.class); }

    @Nullable public RsFunction getDrop_fn() { return findItem("core::mem::drop", true, RsFunction.class); }

    @Nullable public RsStructOrEnumItemElement getRange() { return findLangItem("Range", AutoInjectedCrates.CORE, RsStructOrEnumItemElement.class); }
    @Nullable public RsStructOrEnumItemElement getRangeInclusive() { return findLangItem("RangeInclusive", AutoInjectedCrates.CORE, RsStructOrEnumItemElement.class); }
    @Nullable public RsFunction getRangeInclusiveNew() { return findLangItem("range_inclusive_new", AutoInjectedCrates.CORE, RsFunction.class); }

    // --- Known derivable traits map ---

    private static final Map<String, KnownDerivableTrait> KNOWN_DERIVABLE_TRAITS;
    static {
        KNOWN_DERIVABLE_TRAITS = new HashMap<>();
        for (KnownDerivableTrait trait : KnownDerivableTrait.values()) {
            KNOWN_DERIVABLE_TRAITS.put(trait.name(), trait);
        }
    }

    @NotNull
    public static Map<String, KnownDerivableTrait> getKNOWN_DERIVABLE_TRAITS() {
        return KNOWN_DERIVABLE_TRAITS;
    }

    // --- Static factory methods ---

    @NotNull
    public static KnownItems getKnownItems(@NotNull CargoProject cargoProject) {
        return CachedValuesManager.getManager(cargoProject.getProject()).getCachedValue(cargoProject, KNOWN_ITEMS_KEY, () -> {
            CargoWorkspace workspace = cargoProject.getWorkspace();
            KnownItems items;
            if (workspace == null) {
                items = DUMMY;
            } else {
                items = new KnownItems(new RealKnownItemsLookup(cargoProject.getProject(), workspace));
            }
            return CachedValueProvider.Result.create(items, RsPsiManagerUtil.getRustStructureModificationTracker(cargoProject.getProject()));
        }, false);
    }

    @NotNull
    public static KnownItems getKnownItems(@NotNull RsElement element) {
        CargoProject cargoProject = RsElementExtUtil.getCargoProject(element);
        return cargoProject != null ? getKnownItems(cargoProject) : DUMMY;
    }

    @NotNull
    public static KnownItems knownItems(@NotNull RsElement element) {
        return getKnownItems(element);
    }

    // --- KnownItemsLookup interface ---

    public interface KnownItemsLookup {
        @Nullable
        RsNamedElement findLangItem(@NotNull String langAttribute, @NotNull String crateName);

        @Nullable
        RsNamedElement findItem(@NotNull String path, boolean isStd);
    }

    private static class DummyKnownItemsLookup implements KnownItemsLookup {
        static final DummyKnownItemsLookup INSTANCE = new DummyKnownItemsLookup();

        @Nullable
        @Override
        public RsNamedElement findLangItem(@NotNull String langAttribute, @NotNull String crateName) {
            return null;
        }

        @Nullable
        @Override
        public RsNamedElement findItem(@NotNull String path, boolean isStd) {
            return null;
        }
    }

    private static class RealKnownItemsLookup implements KnownItemsLookup {
        @NotNull
        private final Project project;
        @NotNull
        private final CargoWorkspace workspace;
        private final Map<String, Optional<RsNamedElement>> langItems = new ConcurrentHashMap<>();
        private final Map<String, Optional<RsNamedElement>> resolvedItems = new ConcurrentHashMap<>();

        RealKnownItemsLookup(@NotNull Project project, @NotNull CargoWorkspace workspace) {
            this.project = project;
            this.workspace = workspace;
        }

        @Nullable
        @Override
        public RsNamedElement findLangItem(@NotNull String langAttribute, @NotNull String crateName) {
            return langItems.computeIfAbsent(langAttribute, key ->
                Optional.ofNullable(RsLangItemIndex.findLangItem(project, key, crateName))
            ).orElse(null);
        }

        @Nullable
        @Override
        public RsNamedElement findItem(@NotNull String path, boolean isStd) {
            return resolvedItems.computeIfAbsent(path, key -> {
                var result = NameResolutionUtil.resolveStringPath(key, workspace, project, ThreeState.fromBoolean(isStd));
                return Optional.ofNullable(result != null ? result.getFirst() : null);
            }).orElse(null);
        }
    }
}
