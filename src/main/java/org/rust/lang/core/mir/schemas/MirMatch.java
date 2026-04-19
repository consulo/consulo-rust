/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.building.PlaceBuilder;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;
import org.rust.lang.core.thir.LocalVar;
import org.rust.lang.core.thir.ThirBindingMode;
import org.rust.lang.core.thir.ThirExpr;
import org.rust.lang.core.thir.ThirPat;
import org.rust.lang.core.types.regions.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class MirMatch {
    private MirMatch() {
    }

    public static final class MirArm {
        @NotNull
        private final ThirPat pattern;
        @Nullable
        private final Object guard;
        @NotNull
        private final ThirExpr body;
        @NotNull
        private final Scope scope;
        @NotNull
        private final MirSpan span;

        public MirArm(
            @NotNull ThirPat pattern,
            @Nullable Object guard,
            @NotNull ThirExpr body,
            @NotNull Scope scope,
            @NotNull MirSpan span
        ) {
            this.pattern = pattern;
            this.guard = guard;
            this.body = body;
            this.scope = scope;
            this.span = span;
        }

        @NotNull
        public ThirPat getPattern() {
            return pattern;
        }

        @Nullable
        public Object getGuard() {
            return guard;
        }

        @NotNull
        public ThirExpr getBody() {
            return body;
        }

        @NotNull
        public Scope getScope() {
            return scope;
        }

        @NotNull
        public MirSpan getSpan() {
            return span;
        }
    }

    public static final class MirCandidate {
        @NotNull
        private final MirSpan span;
        private final boolean hasGuard;
        @NotNull
        private List<MirMatchPair> matchPairs;
        @NotNull
        private List<MirBinding> bindings;
        @NotNull
        private final List<MirCandidate> subcandidates;
        @Nullable
        private MirBasicBlockImpl otherwiseBlock;
        @Nullable
        private MirBasicBlockImpl preBindingBlock;
        @Nullable
        private MirBasicBlockImpl nextCandidatePreBindingBlock;

        public MirCandidate(
            @NotNull MirSpan span,
            boolean hasGuard,
            @NotNull List<MirMatchPair> matchPairs,
            @NotNull List<MirBinding> bindings,
            @NotNull List<MirCandidate> subcandidates,
            @Nullable MirBasicBlockImpl otherwiseBlock,
            @Nullable MirBasicBlockImpl preBindingBlock,
            @Nullable MirBasicBlockImpl nextCandidatePreBindingBlock
        ) {
            this.span = span;
            this.hasGuard = hasGuard;
            this.matchPairs = matchPairs;
            this.bindings = bindings;
            this.subcandidates = subcandidates;
            this.otherwiseBlock = otherwiseBlock;
            this.preBindingBlock = preBindingBlock;
            this.nextCandidatePreBindingBlock = nextCandidatePreBindingBlock;
        }

        public MirCandidate(@NotNull PlaceBuilder place, @NotNull ThirPat pattern, boolean hasGuard) {
            this(
                pattern.getSource(),
                hasGuard,
                new ArrayList<>(Collections.singletonList(MirMatchPair.create(place, pattern))),
                new ArrayList<>(),
                Collections.emptyList(),
                null,
                null,
                null
            );
        }

        @NotNull
        public MirSpan getSpan() {
            return span;
        }

        public boolean isHasGuard() {
            return hasGuard;
        }

        @NotNull
        public List<MirMatchPair> getMatchPairs() {
            return matchPairs;
        }

        public void setMatchPairs(@NotNull List<MirMatchPair> matchPairs) {
            this.matchPairs = matchPairs;
        }

        @NotNull
        public List<MirBinding> getBindings() {
            return bindings;
        }

        public void setBindings(@NotNull List<MirBinding> bindings) {
            this.bindings = bindings;
        }

        @NotNull
        public List<MirCandidate> getSubcandidates() {
            return subcandidates;
        }

        @Nullable
        public MirBasicBlockImpl getOtherwiseBlock() {
            return otherwiseBlock;
        }

        public void setOtherwiseBlock(@Nullable MirBasicBlockImpl otherwiseBlock) {
            this.otherwiseBlock = otherwiseBlock;
        }

        @Nullable
        public MirBasicBlockImpl getPreBindingBlock() {
            return preBindingBlock;
        }

        public void setPreBindingBlock(@Nullable MirBasicBlockImpl preBindingBlock) {
            this.preBindingBlock = preBindingBlock;
        }

        @Nullable
        public MirBasicBlockImpl getNextCandidatePreBindingBlock() {
            return nextCandidatePreBindingBlock;
        }

        public void setNextCandidatePreBindingBlock(@Nullable MirBasicBlockImpl nextCandidatePreBindingBlock) {
            this.nextCandidatePreBindingBlock = nextCandidatePreBindingBlock;
        }

        public void visitLeaves(@NotNull Consumer<MirCandidate> callback) {
            if (subcandidates.isEmpty()) {
                callback.accept(this);
            } else {
                throw new UnsupportedOperationException("TODO");
            }
        }
    }

    public static final class MirMatchPair {
        @NotNull
        private final PlaceBuilder place;
        @NotNull
        private final ThirPat pattern;

        private MirMatchPair(@NotNull PlaceBuilder place, @NotNull ThirPat pattern) {
            this.place = place;
            this.pattern = pattern;
        }

        @NotNull
        public static MirMatchPair create(@NotNull PlaceBuilder place, @NotNull ThirPat pattern) {
            // TODO place.resolve_upvar
            // TODO may_need_cast
            return new MirMatchPair(place, pattern);
        }

        @NotNull
        public PlaceBuilder getPlace() {
            return place;
        }

        @NotNull
        public ThirPat getPattern() {
            return pattern;
        }
    }

    public static final class MirBinding {
        @NotNull
        private final MirSpan span;
        @NotNull
        private final MirPlace source;
        @NotNull
        private final LocalVar variable;
        @NotNull
        private final ThirBindingMode bindingMode;

        public MirBinding(
            @NotNull MirSpan span,
            @NotNull MirPlace source,
            @NotNull LocalVar variable,
            @NotNull ThirBindingMode bindingMode
        ) {
            this.span = span;
            this.source = source;
            this.variable = variable;
            this.bindingMode = bindingMode;
        }

        @NotNull
        public MirSpan getSpan() {
            return span;
        }

        @NotNull
        public MirPlace getSource() {
            return source;
        }

        @NotNull
        public LocalVar getVariable() {
            return variable;
        }

        @NotNull
        public ThirBindingMode getBindingMode() {
            return bindingMode;
        }
    }
}
