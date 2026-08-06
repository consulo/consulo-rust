/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    @Nonnull
    public static final Substitution EMPTY = new Substitution();

    @Nonnull
    private final Map<TyTypeParameter, Ty> myTypeSubst;
    @Nonnull
    private final Map<ReEarlyBound, Region> myRegionSubst;
    @Nonnull
    private final Map<CtConstParameter, Const> myConstSubst;

    public Substitution() {
        this(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    @Nonnull
    public static Substitution getEMPTY() {
        return EMPTY;
    }

    public Substitution(@Nonnull Map<TyTypeParameter, Ty> typeSubst) {
        this(typeSubst, Collections.emptyMap(), Collections.emptyMap());
    }

    public Substitution(@Nonnull Map<TyTypeParameter, Ty> typeSubst, @Nonnull Map<ReEarlyBound, Region> regionSubst, @Nonnull Map<CtConstParameter, Const> constSubst) {
        myTypeSubst = typeSubst;
        myRegionSubst = regionSubst;
        myConstSubst = constSubst;
    }

    @Nonnull
    public Map<TyTypeParameter, Ty> getTypeSubst() {
        return myTypeSubst;
    }

    @Nonnull
    public Map<ReEarlyBound, Region> getRegionSubst() {
        return myRegionSubst;
    }

    @Nonnull
    public Map<CtConstParameter, Const> getConstSubst() {
        return myConstSubst;
    }

    @Nonnull
    public Collection<Ty> getTypes() {
        return myTypeSubst.values();
    }

    @Nonnull
    public Collection<Region> getRegions() {
        return myRegionSubst.values();
    }

    @Nonnull
    public Collection<Const> getConsts() {
        return myConstSubst.values();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public Collection<Kind> getKinds() {
        List<Kind> result = new ArrayList<>();
        result.addAll((Collection<? extends Kind>) getTypes());
        result.addAll((Collection<? extends Kind>) getRegions());
        result.addAll((Collection<? extends Kind>) getConsts());
        return result;
    }

    @Nonnull
    public Substitution plus(@Nonnull Substitution other) {
        return new Substitution(
            mergeMaps(myTypeSubst, other.myTypeSubst),
            mergeMaps(myRegionSubst, other.myRegionSubst),
            mergeMaps(myConstSubst, other.myConstSubst)
        );
    }

    @Nullable
    public Ty get(@Nonnull TyTypeParameter key) {
        return myTypeSubst.get(key);
    }

    @Nullable
    public Ty get(@Nonnull RsTypeParameter psi) {
        return myTypeSubst.get(TyTypeParameter.named(psi));
    }

    @Nullable
    public Region get(@Nonnull ReEarlyBound key) {
        return myRegionSubst.get(key);
    }

    @Nullable
    public Region get(@Nonnull RsLifetimeParameter psi) {
        return myRegionSubst.get(new ReEarlyBound(psi));
    }

    @Nullable
    public Const get(@Nonnull CtConstParameter key) {
        return myConstSubst.get(key);
    }

    @Nullable
    public Const get(@Nonnull RsConstParameter psi) {
        return myConstSubst.get(new CtConstParameter(psi));
    }

    @Nullable
    public TyTypeParameter typeParameterByName(@Nonnull String name) {
        for (TyTypeParameter key : myTypeSubst.keySet()) {
            if (key.toString().equals(name)) return key;
        }
        return null;
    }

    @Nonnull
    public Substitution substituteInValues(@Nonnull Substitution map) {
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

    @Nonnull
    public Substitution foldValues(@Nonnull TypeFolder folder) {
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
    @Nonnull
    public Substitution superFoldWith(@Nonnull TypeFolder folder) {
        return foldValues(folder);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
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

    @Nonnull
    public List<Pair<Ty, Ty>> zipTypeValues(@Nonnull Substitution other) {
        return CollectionsUtil.zipValues(myTypeSubst, other.myTypeSubst);
    }

    @Nonnull
    public List<Pair<Const, Const>> zipConstValues(@Nonnull Substitution other) {
        return CollectionsUtil.zipValues(myConstSubst, other.myConstSubst);
    }

    @Nonnull
    public Substitution mapTypeKeys(@Nonnull Function<Map.Entry<TyTypeParameter, Ty>, TyTypeParameter> transform) {
        Map<TyTypeParameter, Ty> newMap = new HashMap<>();
        for (Map.Entry<TyTypeParameter, Ty> entry : myTypeSubst.entrySet()) {
            newMap.put(transform.apply(entry), entry.getValue());
        }
        return new Substitution(newMap, myRegionSubst, myConstSubst);
    }

    @Nonnull
    public Substitution mapTypeValues(@Nonnull Function<Map.Entry<TyTypeParameter, Ty>, Ty> transform) {
        Map<TyTypeParameter, Ty> newMap = new HashMap<>();
        for (Map.Entry<TyTypeParameter, Ty> entry : myTypeSubst.entrySet()) {
            newMap.put(entry.getKey(), transform.apply(entry));
        }
        return new Substitution(newMap, myRegionSubst, myConstSubst);
    }

    @Nonnull
    public Substitution mapConstKeys(@Nonnull Function<Map.Entry<CtConstParameter, Const>, CtConstParameter> transform) {
        Map<CtConstParameter, Const> newMap = new HashMap<>();
        for (Map.Entry<CtConstParameter, Const> entry : myConstSubst.entrySet()) {
            newMap.put(transform.apply(entry), entry.getValue());
        }
        return new Substitution(myTypeSubst, myRegionSubst, newMap);
    }

    @Nonnull
    public Substitution mapConstValues(@Nonnull Function<Map.Entry<CtConstParameter, Const>, Const> transform) {
        Map<CtConstParameter, Const> newMap = new HashMap<>();
        for (Map.Entry<CtConstParameter, Const> entry : myConstSubst.entrySet()) {
            newMap.put(entry.getKey(), transform.apply(entry));
        }
        return new Substitution(myTypeSubst, myRegionSubst, newMap);
    }

    public boolean visitValues(@Nonnull TypeVisitor visitor) {
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

    @Nonnull
    private static <K, V> Map<K, V> mergeMaps(@Nonnull Map<K, V> map1, @Nonnull Map<K, V> map2) {
        if (map1.isEmpty()) return map2;
        if (map2.isEmpty()) return map1;
        Map<K, V> result = new HashMap<>(map1.size() + map2.size());
        result.putAll(map1);
        result.putAll(map2);
        return result;
    }
}
