/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.resolve.TraitImplSource;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Collections;
import java.util.List;

public class MethodPick {
    @NotNull
    private final RsFunction myElement;
    @NotNull
    private final Ty myFormalSelfTy;
    @NotNull
    private final Ty myMethodSelfTy;
    private final int myDerefCount;
    @NotNull
    private final TraitImplSource mySource;
    @NotNull
    private final List<Autoderef.AutoderefStep> myDerefSteps;
    @Nullable
    private final AutorefOrPtrAdjustment myAutorefOrPtrAdjustment;
    private final boolean myIsValid;
    @NotNull
    private final List<Obligation> myObligations;

    public MethodPick(@NotNull RsFunction element,
                      @NotNull Ty formalSelfTy,
                      @NotNull Ty methodSelfTy,
                      int derefCount,
                      @NotNull TraitImplSource source,
                      @NotNull List<Autoderef.AutoderefStep> derefSteps,
                      @Nullable AutorefOrPtrAdjustment autorefOrPtrAdjustment,
                      boolean isValid,
                      @NotNull List<Obligation> obligations) {
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

    @NotNull
    public RsFunction getElement() {
        return myElement;
    }

    @NotNull
    public Ty getFormalSelfTy() {
        return myFormalSelfTy;
    }

    @NotNull
    public Ty getMethodSelfTy() {
        return myMethodSelfTy;
    }

    public int getDerefCount() {
        return myDerefCount;
    }

    @NotNull
    public TraitImplSource getSource() {
        return mySource;
    }

    @NotNull
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

    @NotNull
    public List<Obligation> getObligations() {
        return myObligations;
    }

    @NotNull
    public MethodResolveVariant toMethodResolveVariant() {
        return new MethodResolveVariant(myElement.getName(), myElement, myFormalSelfTy, myDerefCount, mySource);
    }

    @NotNull
    public static MethodPick from(@NotNull MethodResolveVariant m,
                                  @NotNull Ty methodSelfTy,
                                  @NotNull List<Autoderef.AutoderefStep> derefSteps,
                                  @Nullable AutorefOrPtrAdjustment autorefOrPtrAdjustment,
                                  @NotNull List<Obligation> obligations) {
        return new MethodPick(m.getElement(), m.getSelfTy(), methodSelfTy, m.getDerefCount(),
            m.getSource(), derefSteps, autorefOrPtrAdjustment, true, obligations);
    }

    @NotNull
    public static MethodPick from(@NotNull MethodResolveVariant m) {
        return new MethodPick(m.getElement(), m.getSelfTy(), TyUnknown.INSTANCE, m.getDerefCount(),
            m.getSource(), Collections.emptyList(), null, false, Collections.emptyList());
    }

    // --- AutorefOrPtrAdjustment ---

    public static abstract class AutorefOrPtrAdjustment {
        private AutorefOrPtrAdjustment() {
        }

        public static class Autoref extends AutorefOrPtrAdjustment {
            @NotNull
            private final Mutability myMutability;
            private final boolean myUnsize;

            public Autoref(@NotNull Mutability mutability, boolean unsize) {
                myMutability = mutability;
                myUnsize = unsize;
            }

            @NotNull
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
