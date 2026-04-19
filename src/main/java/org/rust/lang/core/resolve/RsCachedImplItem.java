/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.rust.stdext.Lazy;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.RsInferenceContext;
import org.rust.lang.core.types.infer.TypeInferenceUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.*;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;

/**
 * Used for optimization purposes, to reduce access to cache and PSI tree in some very hot places,
 * {@link ImplLookup#assembleCandidates} and processAssociatedItems in particular.
 */
public class RsCachedImplItem {
    @NotNull
    private final RsImplItem impl;
    @Nullable
    private final RsTraitRef traitRef;
    @NotNull
    private final List<Crate> containingCrates;
    private final boolean isValid;
    private final boolean isNegativeImpl;

    // Lazy fields
    private volatile BoundElement<RsTraitItem> implementedTrait;
    private volatile boolean implementedTraitInitialized;

    private volatile Ty type;
    private volatile List<TyTypeParameter> generics;
    private volatile List<CtConstParameter> constGenerics;
    private volatile boolean typeAndGenericsInitialized;

    private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

    @NotNull
    private final TraitImplSource.ExplicitImpl explicitImpl;

    public RsCachedImplItem(@NotNull RsImplItem impl) {
        this.impl = impl;
        this.traitRef = impl.getTraitRef();
        this.isNegativeImpl = RsImplItemUtil.isNegativeImpl(impl);

        // init block equivalent
        boolean[] validAndCrates = getValidAndCrates(impl);
        // We return isValid = validFromProject && !isReservationImpl
        this.isValid = validAndCrates[0];
        this.containingCrates = getCratesFromImpl(impl);
        this.explicitImpl = new TraitImplSource.ExplicitImpl(this);
    }

    private static boolean[] getValidAndCrates(RsImplItem impl) {
        // Check if the impl is a valid project member (has a containing crate)
        RsFile file = impl.getContainingFile() instanceof RsFile ? (RsFile) impl.getContainingFile() : null;
        boolean isValid = file != null && file.getCrate() != null;
        boolean isReservation = RsImplItemUtil.isReservationImpl(impl);
        return new boolean[] { isValid && !isReservation };
    }

    private static List<Crate> getCratesFromImpl(RsImplItem impl) {
        RsFile file = impl.getContainingFile() instanceof RsFile ? (RsFile) impl.getContainingFile() : null;
        if (file != null) {
            return file.getCrates();
        }
        return Collections.emptyList();
    }

    @NotNull
    public RsImplItem getImpl() {
        return impl;
    }

    @NotNull
    public List<Crate> getContainingCrates() {
        return containingCrates;
    }

    public boolean isValid() {
        return isValid;
    }

    public boolean isNegativeImpl() {
        return isNegativeImpl;
    }

    public boolean isInherent() {
        return traitRef == null;
    }

    @Nullable
    public BoundElement<RsTraitItem> getImplementedTrait() {
        if (!implementedTraitInitialized) {
            synchronized (this) {
                if (!implementedTraitInitialized) {
                    implementedTrait = traitRef != null ? RsTraitRefUtil.resolveToBoundTrait(traitRef) : null;
                    implementedTraitInitialized = true;
                }
            }
        }
        return implementedTrait;
    }

    @Nullable
    public Ty getType() {
        ensureTypeAndGenerics();
        return type;
    }

    @Nullable
    public List<TyTypeParameter> getGenerics() {
        ensureTypeAndGenerics();
        return generics;
    }

    @Nullable
    public List<CtConstParameter> getConstGenerics() {
        ensureTypeAndGenerics();
        return constGenerics;
    }

    private void ensureTypeAndGenerics() {
        if (!typeAndGenericsInitialized) {
            synchronized (this) {
                if (!typeAndGenericsInitialized) {
                    RsTypeReference typeRef = impl.getTypeReference();
                    if (typeRef != null) {
                        type = ExtensionsUtil.getRawType(typeRef);
                        generics = TypeInferenceUtil.getGenerics(impl);
                        constGenerics = TypeInferenceUtil.getConstGenerics(impl);
                    }
                    typeAndGenericsInitialized = true;
                }
            }
        }
    }

    @NotNull
    public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
        if (implAndTraitExpandedMembers == null) {
            synchronized (this) {
                if (implAndTraitExpandedMembers == null) {
                    implAndTraitExpandedMembers = computeImplAndTraitExpandedMembers();
                }
            }
        }
        return implAndTraitExpandedMembers;
    }

    @NotNull
    private Map<String, List<RsAbstractable>> computeImplAndTraitExpandedMembers() {
        Map<String, List<RsAbstractable>> membersMap = new LinkedHashMap<>();
        RsMembers members = impl.getMembers();
        if (members != null) {
            for (RsAbstractable member : RsMembersUtil.getExpandedMembers(members)) {
                String name = member.getName();
                if (name == null) continue;
                membersMap.computeIfAbsent(name, k -> new SmartList<>()).add(member);
            }
        }
        BoundElement<RsTraitItem> trait = getImplementedTrait();
        if (trait != null) {
            RsMembers traitMembers = ((RsTraitItem) trait.getElement()).getMembers();
            if (traitMembers != null) {
                Set<String> implMemberNames = new HashSet<>(membersMap.keySet());
                for (RsAbstractable traitMember : RsMembersUtil.getExpandedMembers(traitMembers)) {
                    String name = traitMember.getName();
                    if (name == null) continue;
                    if (implMemberNames.contains(name)) continue;
                    membersMap.computeIfAbsent(name, k -> new SmartList<>()).add(traitMember);
                }
            }
        }
        return membersMap;
    }

    @NotNull
    public TraitImplSource.ExplicitImpl getExplicitImpl() {
        return explicitImpl;
    }

    @NotNull
    public static RsCachedImplItem forImpl(@NotNull RsImplItem impl) {
        // Access the cached value through the mixin
        return ((RsImplItemImplMixin) impl).getCachedImplItem().getValue();
    }

    @NotNull
    public static <T> CachedValueProvider.Result<T> toCachedResult(
        @NotNull RsElement psi,
        @Nullable Crate containingCrate,
        @NotNull T cachedImpl
    ) {
        PsiFile containingFile = psi.getContainingFile();
        ModificationTracker modTracker;
        if (containingCrate != null && containingCrate.getOrigin() == PackageOrigin.WORKSPACE) {
            modTracker = RsPsiManagerUtil.getRustStructureModificationTracker(containingFile.getProject());
        } else {
            modTracker = RsPsiManagerUtil.getRustPsiManager(containingFile.getProject())
                .getRustStructureModificationTrackerInDependencies();
        }

        if (!containingFile.isPhysical() || containingFile.getVirtualFile() instanceof VirtualFileWindow) {
            return CachedValueProvider.Result.create(cachedImpl, modTracker,
                (ModificationTracker) containingFile::getModificationStamp);
        } else {
            return CachedValueProvider.Result.create(cachedImpl, modTracker);
        }
    }
}
