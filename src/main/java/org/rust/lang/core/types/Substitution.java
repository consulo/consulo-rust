/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.regions.ReEarlyBound;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.stdext.CollectionsUtil;

import java.util.*;
import java.util.function.Function;

public class Substitution implements TypeFoldable<Substitution> {
    @NotNull
    public static final Substitution EMPTY = new Substitution();

    @NotNull
    private final Map<TyTypeParameter, Ty> myTypeSubst;
    @NotNull
    private final Map<ReEarlyBound, Region> myRegionSubst;
    @NotNull
    private final Map<CtConstParameter, Const> myConstSubst;

    public Substitution() {
        this(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @NotNull
    public static Substitution getEMPTY() {
        return EMPTY;
    }

    public Substitution(@NotNull Map<TyTypeParameter, Ty> typeSubst) {
        this(typeSubst, Collections.emptyMap(), Collections.emptyMap());
    }

    public Substitution(@NotNull Map<TyTypeParameter, Ty> typeSubst, @NotNull Map<ReEarlyBound, Region> regionSubst, @NotNull Map<CtConstParameter, Const> constSubst) {
        myTypeSubst = typeSubst;
        myRegionSubst = regionSubst;
        myConstSubst = constSubst;
    }

    @NotNull
    public Map<TyTypeParameter, Ty> getTypeSubst() {
        return myTypeSubst;
    }

    @NotNull
    public Map<ReEarlyBound, Region> getRegionSubst() {
        return myRegionSubst;
    }

    @NotNull
    public Map<CtConstParameter, Const> getConstSubst() {
        return myConstSubst;
    }

    @NotNull
    public Collection<Ty> getTypes() {
        return myTypeSubst.values();
    }

    @NotNull
    public Collection<Region> getRegions() {
        return myRegionSubst.values();
    }

    @NotNull
    public Collection<Const> getConsts() {
        return myConstSubst.values();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public Collection<Kind> getKinds() {
        List<Kind> result = new ArrayList<>();
        result.addAll((Collection<? extends Kind>) getTypes());
        result.addAll((Collection<? extends Kind>) getRegions());
        result.addAll((Collection<? extends Kind>) getConsts());
        return result;
    }

    @NotNull
    public Substitution plus(@NotNull Substitution other) {
        return new Substitution(
            mergeMaps(myTypeSubst, other.myTypeSubst),
            mergeMaps(myRegionSubst, other.myRegionSubst),
            mergeMaps(myConstSubst, other.myConstSubst)
        );
    }

    @Nullable
    public Ty get(@NotNull TyTypeParameter key) {
        return myTypeSubst.get(key);
    }

    @Nullable
    public Ty get(@NotNull RsTypeParameter psi) {
        return myTypeSubst.get(TyTypeParameter.named(psi));
    }

    @Nullable
    public Region get(@NotNull ReEarlyBound key) {
        return myRegionSubst.get(key);
    }

    @Nullable
    public Region get(@NotNull RsLifetimeParameter psi) {
        return myRegionSubst.get(new ReEarlyBound(psi));
    }

    @Nullable
    public Const get(@NotNull CtConstParameter key) {
        return myConstSubst.get(key);
    }

    @Nullable
    public Const get(@NotNull RsConstParameter psi) {
        return myConstSubst.get(new CtConstParameter(psi));
    }

    @Nullable
    public TyTypeParameter typeParameterByName(@NotNull String name) {
        for (TyTypeParameter key : myTypeSubst.keySet()) {
            if (key.toString().equals(name)) return key;
        }
        return null;
    }

    @NotNull
    public Substitution substituteInValues(@NotNull Substitution map) {
        Map<TyTypeParameter, Ty> newTypeSubst = new HashMap<>();
        for (Map.Entry<TyTypeParameter, Ty> entry : myTypeSubst.entrySet()) {
            newTypeSubst.put(entry.getKey(), FoldUtil.substitute(entry.getValue(), map));
        }
        Map<ReEarlyBound, Region> newRegionSubst = new HashMap<>();
        for (Map.Entry<ReEarlyBound, Region> entry : myRegionSubst.entrySet()) {
            newRegionSubst.put(entry.getKey(), FoldUtil.substitute(entry.getValue(), map));
        }
        Map<CtConstParameter, Const> newConstSubst = new HashMap<>();
        for (Map.Entry<CtConstParameter, Const> entry : myConstSubst.entrySet()) {
            newConstSubst.put(entry.getKey(), FoldUtil.substitute(entry.getValue(), map));
        }
        return new Substitution(newTypeSubst, newRegionSubst, newConstSubst);
    }

    @NotNull
    public Substitution foldValues(@NotNull TypeFolder folder) {
        Map<TyTypeParameter, Ty> newTypeSubst = new HashMap<>();
        for (Map.Entry<TyTypeParameter, Ty> entry : myTypeSubst.entrySet()) {
            newTypeSubst.put(entry.getKey(), entry.getValue().foldWith(folder));
        }
        Map<ReEarlyBound, Region> newRegionSubst = new HashMap<>();
        for (Map.Entry<ReEarlyBound, Region> entry : myRegionSubst.entrySet()) {
            newRegionSubst.put(entry.getKey(), entry.getValue().foldWith(folder));
        }
        Map<CtConstParameter, Const> newConstSubst = new HashMap<>();
        for (Map.Entry<CtConstParameter, Const> entry : myConstSubst.entrySet()) {
            newConstSubst.put(entry.getKey(), entry.getValue().foldWith(folder));
        }
        return new Substitution(newTypeSubst, newRegionSubst, newConstSubst);
    }

    @Override
    @NotNull
    public Substitution superFoldWith(@NotNull TypeFolder folder) {
        return foldValues(folder);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        for (Ty ty : myTypeSubst.values()) {
            if (ty.visitWith(visitor)) return true;
        }
        for (Region r : myRegionSubst.values()) {
            if (r.visitWith(visitor)) return true;
        }
        for (Const c : myConstSubst.values()) {
            if (c.visitWith(visitor)) return true;
        }
        return false;
    }

    @NotNull
    public List<Pair<Ty, Ty>> zipTypeValues(@NotNull Substitution other) {
        return CollectionsUtil.zipValues(myTypeSubst, other.myTypeSubst);
    }

    @NotNull
    public List<Pair<Const, Const>> zipConstValues(@NotNull Substitution other) {
        return CollectionsUtil.zipValues(myConstSubst, other.myConstSubst);
    }

    @NotNull
    public Substitution mapTypeKeys(@NotNull Function<Map.Entry<TyTypeParameter, Ty>, TyTypeParameter> transform) {
        Map<TyTypeParameter, Ty> newMap = new HashMap<>();
        for (Map.Entry<TyTypeParameter, Ty> entry : myTypeSubst.entrySet()) {
            newMap.put(transform.apply(entry), entry.getValue());
        }
        return new Substitution(newMap, myRegionSubst, myConstSubst);
    }

    @NotNull
    public Substitution mapTypeValues(@NotNull Function<Map.Entry<TyTypeParameter, Ty>, Ty> transform) {
        Map<TyTypeParameter, Ty> newMap = new HashMap<>();
        for (Map.Entry<TyTypeParameter, Ty> entry : myTypeSubst.entrySet()) {
            newMap.put(entry.getKey(), transform.apply(entry));
        }
        return new Substitution(newMap, myRegionSubst, myConstSubst);
    }

    @NotNull
    public Substitution mapConstKeys(@NotNull Function<Map.Entry<CtConstParameter, Const>, CtConstParameter> transform) {
        Map<CtConstParameter, Const> newMap = new HashMap<>();
        for (Map.Entry<CtConstParameter, Const> entry : myConstSubst.entrySet()) {
            newMap.put(transform.apply(entry), entry.getValue());
        }
        return new Substitution(myTypeSubst, myRegionSubst, newMap);
    }

    @NotNull
    public Substitution mapConstValues(@NotNull Function<Map.Entry<CtConstParameter, Const>, Const> transform) {
        Map<CtConstParameter, Const> newMap = new HashMap<>();
        for (Map.Entry<CtConstParameter, Const> entry : myConstSubst.entrySet()) {
            newMap.put(entry.getKey(), transform.apply(entry));
        }
        return new Substitution(myTypeSubst, myRegionSubst, newMap);
    }

    public boolean visitValues(@NotNull TypeVisitor visitor) {
        for (Ty ty : myTypeSubst.values()) {
            if (ty.visitWith(visitor)) return true;
        }
        for (Region r : myRegionSubst.values()) {
            if (r.visitWith(visitor)) return true;
        }
        for (Const c : myConstSubst.values()) {
            if (c.visitWith(visitor)) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Substitution that = (Substitution) other;
        return myTypeSubst.equals(that.myTypeSubst);
    }

    @Override
    public int hashCode() {
        return myTypeSubst.hashCode();
    }

    @NotNull
    private static <K, V> Map<K, V> mergeMaps(@NotNull Map<K, V> map1, @NotNull Map<K, V> map2) {
        if (map1.isEmpty()) return map2;
        if (map2.isEmpty()) return map1;
        Map<K, V> result = new HashMap<>(map1.size() + map2.size());
        result.putAll(map1);
        result.putAll(map2);
        return result;
    }
}
