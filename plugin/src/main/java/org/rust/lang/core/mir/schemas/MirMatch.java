/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
        @Nonnull
        private final ThirPat pattern;
        @Nullable
        private final Object guard;
        @Nonnull
        private final ThirExpr body;
        @Nonnull
        private final Scope scope;
        @Nonnull
        private final MirSpan span;

        public MirArm(
            @Nonnull ThirPat pattern,
            @Nullable Object guard,
            @Nonnull ThirExpr body,
            @Nonnull Scope scope,
            @Nonnull MirSpan span
        ) {
            this.pattern = pattern;
            this.guard = guard;
            this.body = body;
            this.scope = scope;
            this.span = span;
        }

        @Nonnull
        public ThirPat getPattern() {
            return pattern;
        }

        @Nullable
        public Object getGuard() {
            return guard;
        }

        @Nonnull
        public ThirExpr getBody() {
            return body;
        }

        @Nonnull
        public Scope getScope() {
            return scope;
        }

        @Nonnull
        public MirSpan getSpan() {
            return span;
        }
    }

    public static final class MirCandidate {
        @Nonnull
        private final MirSpan span;
        private final boolean hasGuard;
        @Nonnull
        private List<MirMatchPair> matchPairs;
        @Nonnull
        private List<MirBinding> bindings;
        @Nonnull
        private final List<MirCandidate> subcandidates;
        @Nullable
        private MirBasicBlockImpl otherwiseBlock;
        @Nullable
        private MirBasicBlockImpl preBindingBlock;
        @Nullable
        private MirBasicBlockImpl nextCandidatePreBindingBlock;

        public MirCandidate(
            @Nonnull MirSpan span,
            boolean hasGuard,
            @Nonnull List<MirMatchPair> matchPairs,
            @Nonnull List<MirBinding> bindings,
            @Nonnull List<MirCandidate> subcandidates,
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

        public MirCandidate(@Nonnull PlaceBuilder place, @Nonnull ThirPat pattern, boolean hasGuard) {
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

        @Nonnull
        public MirSpan getSpan() {
            return span;
        }

        public boolean isHasGuard() {
            return hasGuard;
        }

        @Nonnull
        public List<MirMatchPair> getMatchPairs() {
            return matchPairs;
        }

        public void setMatchPairs(@Nonnull List<MirMatchPair> matchPairs) {
            this.matchPairs = matchPairs;
        }

        @Nonnull
        public List<MirBinding> getBindings() {
            return bindings;
        }

        public void setBindings(@Nonnull List<MirBinding> bindings) {
            this.bindings = bindings;
        }

        @Nonnull
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

        public void visitLeaves(@Nonnull Consumer<MirCandidate> callback) {
            if (subcandidates.isEmpty()) {
                callback.accept(this);
            } else {
                throw new UnsupportedOperationException("TODO");
            }
        }
    }

    public static final class MirMatchPair {
        @Nonnull
        private final PlaceBuilder place;
        @Nonnull
        private final ThirPat pattern;

        private MirMatchPair(@Nonnull PlaceBuilder place, @Nonnull ThirPat pattern) {
            this.place = place;
            this.pattern = pattern;
        }

        @Nonnull
        public static MirMatchPair create(@Nonnull PlaceBuilder place, @Nonnull ThirPat pattern) {
            // TODO place.resolve_upvar
            // TODO may_need_cast
            return new MirMatchPair(place, pattern);
        }

        @Nonnull
        public PlaceBuilder getPlace() {
            return place;
        }

        @Nonnull
        public ThirPat getPattern() {
            return pattern;
        }
    }

    public static final class MirBinding {
        @Nonnull
        private final MirSpan span;
        @Nonnull
        private final MirPlace source;
        @Nonnull
        private final LocalVar variable;
        @Nonnull
        private final ThirBindingMode bindingMode;

        public MirBinding(
            @Nonnull MirSpan span,
            @Nonnull MirPlace source,
            @Nonnull LocalVar variable,
            @Nonnull ThirBindingMode bindingMode
        ) {
            this.span = span;
            this.source = source;
            this.variable = variable;
            this.bindingMode = bindingMode;
        }

        @Nonnull
        public MirSpan getSpan() {
            return span;
        }

        @Nonnull
        public MirPlace getSource() {
            return source;
        }

        @Nonnull
        public LocalVar getVariable() {
            return variable;
        }

        @Nonnull
        public ThirBindingMode getBindingMode() {
            return bindingMode;
        }
    }
}
