/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.resolve.TraitImplSource;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Collections;
import java.util.List;

public class MethodPick {
    @Nonnull
    private final RsFunction myElement;
    @Nonnull
    private final Ty myFormalSelfTy;
    @Nonnull
    private final Ty myMethodSelfTy;
    private final int myDerefCount;
    @Nonnull
    private final TraitImplSource mySource;
    @Nonnull
    private final List<Autoderef.AutoderefStep> myDerefSteps;
    @Nullable
    private final AutorefOrPtrAdjustment myAutorefOrPtrAdjustment;
    private final boolean myIsValid;
    @Nonnull
    private final List<Obligation> myObligations;

    public MethodPick(@Nonnull RsFunction element,
                      @Nonnull Ty formalSelfTy,
                      @Nonnull Ty methodSelfTy,
                      int derefCount,
                      @Nonnull TraitImplSource source,
                      @Nonnull List<Autoderef.AutoderefStep> derefSteps,
                      @Nullable AutorefOrPtrAdjustment autorefOrPtrAdjustment,
                      boolean isValid,
                      @Nonnull List<Obligation> obligations) {
        myElement = element;
        myFormalSelfTy = formalSelfTy;
        myMethodSelfTy = methodSelfTy;
        myDerefCount = derefCount;
        mySource = source;
        myDerefSteps = derefSteps;
        myAutorefOrPtrAdjustment = autorefOrPtrAdjustment;
        myIsValid = isValid;
        myObligations = obligations;
    }

    @Nonnull
    public RsFunction getElement() {
        return myElement;
    }

    @Nonnull
    public Ty getFormalSelfTy() {
        return myFormalSelfTy;
    }

    @Nonnull
    public Ty getMethodSelfTy() {
        return myMethodSelfTy;
    }

    public int getDerefCount() {
        return myDerefCount;
    }

    @Nonnull
    public TraitImplSource getSource() {
        return mySource;
    }

    @Nonnull
    public List<Autoderef.AutoderefStep> getDerefSteps() {
        return myDerefSteps;
    }

    @Nullable
    public AutorefOrPtrAdjustment getAutorefOrPtrAdjustment() {
        return myAutorefOrPtrAdjustment;
    }

    public boolean isValid() {
        return myIsValid;
    }

    @Nonnull
    public List<Obligation> getObligations() {
        return myObligations;
    }

    @Nonnull
    public MethodResolveVariant toMethodResolveVariant() {
        return new MethodResolveVariant(myElement.getName(), myElement, myFormalSelfTy, myDerefCount, mySource);
    }

    @Nonnull
    public static MethodPick from(@Nonnull MethodResolveVariant m,
                                  @Nonnull Ty methodSelfTy,
                                  @Nonnull List<Autoderef.AutoderefStep> derefSteps,
                                  @Nullable AutorefOrPtrAdjustment autorefOrPtrAdjustment,
                                  @Nonnull List<Obligation> obligations) {
        return new MethodPick(m.getElement(), m.getSelfTy(), methodSelfTy, m.getDerefCount(),
            m.getSource(), derefSteps, autorefOrPtrAdjustment, true, obligations);
    }

    @Nonnull
    public static MethodPick from(@Nonnull MethodResolveVariant m) {
        return new MethodPick(m.getElement(), m.getSelfTy(), TyUnknown.INSTANCE, m.getDerefCount(),
            m.getSource(), Collections.emptyList(), null, false, Collections.emptyList());
    }

    // --- AutorefOrPtrAdjustment ---

    public static abstract class AutorefOrPtrAdjustment {
        private AutorefOrPtrAdjustment() {
        }

        public static class Autoref extends AutorefOrPtrAdjustment {
            @Nonnull
            private final Mutability myMutability;
            private final boolean myUnsize;

            public Autoref(@Nonnull Mutability mutability, boolean unsize) {
                myMutability = mutability;
                myUnsize = unsize;
            }

            @Nonnull
            public Mutability getMutability() {
                return myMutability;
            }

            public boolean isUnsize() {
                return myUnsize;
            }
        }

        public static class ToConstPtr extends AutorefOrPtrAdjustment {
            public static final ToConstPtr INSTANCE = new ToConstPtr();

            private ToConstPtr() {
            }
        }
    }
}
