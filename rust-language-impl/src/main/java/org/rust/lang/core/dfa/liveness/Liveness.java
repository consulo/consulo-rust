/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.liveness;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.lang.core.dfa.DataFlow;
import org.rust.lang.core.dfa.DataFlow.*;
import org.rust.lang.core.dfa.ExprUseWalker;
import org.rust.lang.core.dfa.ExprUseWalker.*;
import org.rust.lang.core.dfa.MemoryCategorization.*;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.RsInferenceResult;

import java.util.*;
import org.rust.lang.core.psi.ext.impl.*;

// ---- Enums and Data Classes ----

public class Liveness {

    public enum DeclarationKind {
        Parameter, Variable
    }

    public static class DeadDeclaration {
        @Nonnull
        public final RsPatBinding binding;
        @Nonnull
        public final DeclarationKind kind;

        public DeadDeclaration(@Nonnull RsPatBinding binding, @Nonnull DeclarationKind kind) {
            this.binding = binding;
            this.kind = kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DeadDeclaration)) return false;
            DeadDeclaration that = (DeadDeclaration) o;
            return binding.equals(that.binding) && kind == that.kind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(binding, kind);
        }
    }

    public static class LivenessResult {
        @Nonnull
        public final List<DeadDeclaration> deadDeclarations;
        @Nonnull
        public final Map<RsPatBinding, List<RsElement>> lastUsages;

        public LivenessResult(
            @Nonnull List<DeadDeclaration> deadDeclarations,
            @Nonnull Map<RsPatBinding, List<RsElement>> lastUsages
        ) {
            this.deadDeclarations = deadDeclarations;
            this.lastUsages = lastUsages;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LivenessResult)) return false;
            LivenessResult that = (LivenessResult) o;
            return deadDeclarations.equals(that.deadDeclarations) && lastUsages.equals(that.lastUsages);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deadDeclarations, lastUsages);
        }
    }

    // ---- LivenessContext ----
    public static class LivenessContext {
        @Nonnull
        public final RsInferenceResult inference;
        @Nonnull
        public final RsBlock body;
        @Nonnull
        public final ControlFlowGraph cfg;
        @Nonnull
        public final ImplLookup implLookup;
        @Nonnull
        private final List<DeadDeclaration> deadDeclarations;
        @Nonnull
        private final Map<RsPatBinding, List<RsElement>> lastUsages;

        private LivenessContext(
            @Nonnull RsInferenceResult inference,
            @Nonnull RsBlock body,
            @Nonnull ControlFlowGraph cfg,
            @Nonnull ImplLookup implLookup
        ) {
            this.inference = inference;
            this.body = body;
            this.cfg = cfg;
            this.implLookup = implLookup;
            this.deadDeclarations = new ArrayList<>();
            this.lastUsages = new HashMap<>();
        }

        @Nullable
        public static LivenessContext buildFor(@Nonnull RsInferenceContextOwner owner) {
            Object bodyObj = RsInferenceContextOwnerUtil.getBody(owner);
            if (!(bodyObj instanceof RsBlock)) return null;
            RsBlock body = (RsBlock) bodyObj;
            ControlFlowGraph cfg = ExtensionsUtil.getControlFlowGraph(owner);
            if (cfg == null) return null;
            return new LivenessContext(
                ExtensionsUtil.getSelfInferenceResult(owner),
                body,
                cfg,
                ImplLookup.relativeTo(body)
            );
        }

        public void registerDeadDeclaration(@Nonnull RsPatBinding binding, @Nonnull DeclarationKind kind) {
            deadDeclarations.add(new DeadDeclaration(binding, kind));
        }

        public void registerLastUsage(@Nonnull RsPatBinding binding, @Nonnull RsElement usageElement) {
            lastUsages.computeIfAbsent(binding, k -> new ArrayList<>()).add(usageElement);
        }

        @Nonnull
        public LivenessResult check() {
            GatherLivenessContext gatherLivenessContext = new GatherLivenessContext(this);
            LivenessData livenessData = gatherLivenessContext.gather();
            FlowedLivenessData flowedLiveness = FlowedLivenessData.buildFor(this, livenessData, cfg);
            flowedLiveness.collectDeadDeclarations();
            flowedLiveness.collectLastUsages();
            return new LivenessResult(deadDeclarations, lastUsages);
        }
    }

    // ---- LiveDataFlowOperator ----
    public static class LiveDataFlowOperator implements DataFlow.DataFlowOperator {
        public static final LiveDataFlowOperator INSTANCE = new LiveDataFlowOperator();

        @Override
        public int join(int succ, int pred) {
            return succ | pred;
        }

        @Override
        public boolean getInitialValue() {
            return false;
        }
    }

    // ---- FlowedLivenessData ----
    public static class FlowedLivenessData {
        @Nonnull
        private final LivenessContext ctx;
        @Nonnull
        private final LivenessData livenessData;
        @Nonnull
        private final DataFlowContext<LiveDataFlowOperator> dfcxLivePaths;

        private FlowedLivenessData(
            @Nonnull LivenessContext ctx,
            @Nonnull LivenessData livenessData,
            @Nonnull DataFlowContext<LiveDataFlowOperator> dfcxLivePaths
        ) {
            this.ctx = ctx;
            this.livenessData = livenessData;
            this.dfcxLivePaths = dfcxLivePaths;
        }

        public void collectDeadDeclarations() {
            for (UsagePath path : livenessData.paths) {
                if (path instanceof UsagePath.Base) {
                    UsagePath.Base basePath = (UsagePath.Base) path;
                    if (isDeadOnEntry(basePath, basePath.declaration)) {
                        ctx.registerDeadDeclaration(basePath.declaration, basePath.getDeclarationKind());
                    }
                }
            }
        }

        public void collectLastUsages() {
            for (Usage usage : livenessData.usages) {
                UsagePath usagePath = usage.path;
                RsElement usageElement = usage.element;
                if (isDeadOnEntry(usagePath, usageElement)) {
                    ctx.registerLastUsage(usagePath.getDeclaration(), usageElement);
                }
            }
        }

        private boolean isDeadOnEntry(@Nonnull UsagePath usagePath, @Nonnull RsElement element) {
            boolean[] isDead = {true};
            dfcxLivePaths.eachBitOnEntry(element, index -> {
                UsagePath path = livenessData.paths.get(index);
                if (usagePath.equals(path)) {
                    isDead[0] = false;
                } else {
                    boolean isEachExtensionDead = livenessData.eachBasePath(path, it -> !it.equals(usagePath));
                    if (!isEachExtensionDead) isDead[0] = false;
                }
                return isDead[0];
            });
            return isDead[0];
        }

        @Nonnull
        public static FlowedLivenessData buildFor(@Nonnull LivenessContext ctx, @Nonnull LivenessData livenessData, @Nonnull ControlFlowGraph cfg) {
            DataFlowContext<LiveDataFlowOperator> dfcxLivePaths = new DataFlowContext<>(
                cfg, LiveDataFlowOperator.INSTANCE, livenessData.paths.size(), FlowDirection.Backward
            );
            livenessData.addGenKills(dfcxLivePaths);
            dfcxLivePaths.propagate();
            return new FlowedLivenessData(ctx, livenessData, dfcxLivePaths);
        }
    }

    // ---- GatherLivenessContext ----
    public static class GatherLivenessContext implements ExprUseWalker.Delegate {
        @Nonnull
        private final LivenessContext ctx;
        @Nonnull
        private final LivenessData livenessData;

        public GatherLivenessContext(@Nonnull LivenessContext ctx) {
            this(ctx, new LivenessData());
        }

        public GatherLivenessContext(@Nonnull LivenessContext ctx, @Nonnull LivenessData livenessData) {
            this.ctx = ctx;
            this.livenessData = livenessData;
        }

        @Override
        public void consume(@Nonnull RsElement element, @Nonnull Cmt cmt, @Nonnull ConsumeMode mode) {
            livenessData.addUsage(element, cmt);
        }

        @Override
        public void matchedPat(@Nonnull RsPat pat, @Nonnull Cmt cmt, @Nonnull MatchMode mode) {
        }

        @Override
        public void consumePat(@Nonnull RsPat pat, @Nonnull Cmt cmt, @Nonnull ConsumeMode mode) {
            for (RsPatBinding binding : RsElementUtil.descendantsOfType(pat, RsPatBinding.class)) {
                livenessData.addDeclaration(binding);
            }
            livenessData.addUsage(pat, cmt);
        }

        @Override
        public void declarationWithoutInit(@Nonnull RsPatBinding binding) {
            livenessData.addDeclaration(binding);
        }

        @Override
        public void mutate(@Nonnull RsElement assignmentElement, @Nonnull Cmt assigneeCmt, @Nonnull MutateMode mode) {
            if (mode == MutateMode.WriteAndRead) {
                livenessData.addUsage(assignmentElement, assigneeCmt);
            }
        }

        @Override
        public void useElement(@Nonnull RsElement element, @Nonnull Cmt cmt) {
            livenessData.addUsage(element, cmt);
        }

        @Nonnull
        public LivenessData gather() {
            ExprUseWalker gatherVisitor = new ExprUseWalker(this, new MemoryCategorizationContext(ctx.implLookup, ctx.inference));
            gatherVisitor.consumeBody(ctx.body);
            return livenessData;
        }
    }

    // ---- UsagePath ----
    public static abstract class UsagePath {
        @Nonnull
        public abstract RsPatBinding getDeclaration();

        public static class Base extends UsagePath {
            @Nonnull
            public final RsPatBinding declaration;

            public Base(@Nonnull RsPatBinding declaration) {
                this.declaration = declaration;
            }

            @Nonnull
            @Override
            public RsPatBinding getDeclaration() {
                return declaration;
            }

            @Nonnull
            public DeclarationKind getDeclarationKind() {
                return RsElementUtil.ancestorOrSelf(declaration, RsValueParameter.class) != null
                    ? DeclarationKind.Parameter : DeclarationKind.Variable;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Base)) return false;
                return declaration.equals(((Base) o).declaration);
            }

            @Override
            public int hashCode() {
                return declaration.hashCode();
            }

            @Override
            public String toString() {
                return declaration.getText();
            }
        }

        public static class Extend extends UsagePath {
            @Nonnull
            public final UsagePath parent;

            public Extend(@Nonnull UsagePath parent) {
                this.parent = parent;
            }

            @Nonnull
            @Override
            public RsPatBinding getDeclaration() {
                return parent.getDeclaration();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Extend)) return false;
                return parent.equals(((Extend) o).parent);
            }

            @Override
            public int hashCode() {
                return parent.hashCode();
            }

            @Override
            public String toString() {
                return "Extend(" + parent + ")";
            }
        }

        @Nonnull
        private Base getBase() {
            if (this instanceof Base) return (Base) this;
            return ((Extend) this).parent.getBase();
        }

        @Nonnull
        public DeclarationKind getDeclarationKind() {
            return getBase().getDeclarationKind();
        }

        @Nullable
        public static UsagePath computeFor(@Nonnull Cmt cmt) {
            Categorization category = cmt.category;
            if (category instanceof Categorization.Rvalue) {
                if (cmt.element instanceof RsExpr) {
                    RsElement decl = ExtensionsUtil.getDeclaration((RsExpr) cmt.element);
                    if (decl instanceof RsPatBinding) {
                        return new Base((RsPatBinding) decl);
                    }
                }
                return null;
            }
            if (category instanceof Categorization.Local) {
                if (((Categorization.Local) category).declaration instanceof RsPatBinding) {
                    return new Base((RsPatBinding) ((Categorization.Local) category).declaration);
                }
                return null;
            }
            if (category instanceof Categorization.Deref) {
                Cmt baseCmt = ((Categorization.Deref) category).unwrapDerefs();
                return computeFor(baseCmt);
            }
            if (category instanceof Categorization.Interior) {
                Cmt baseCmt = ((Categorization.Interior) category).getCmt();
                UsagePath parent = computeFor(baseCmt);
                if (parent == null) return null;
                return new Extend(parent);
            }
            return null;
        }
    }

    // ---- Usage ----
    public static class Usage {
        @Nonnull
        public final UsagePath path;
        @Nonnull
        public final RsElement element;

        public Usage(@Nonnull UsagePath path, @Nonnull RsElement element) {
            this.path = path;
            this.element = element;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Usage)) return false;
            Usage u = (Usage) o;
            return path.equals(u.path) && element.equals(u.element);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, element);
        }

        @Override
        public String toString() {
            return "Usage(" + path + ")";
        }
    }

    // ---- Declaration ----
    public static class Declaration {
        @Nonnull
        public final UsagePath.Base path;
        @Nonnull
        public final RsElement element;

        public Declaration(@Nonnull UsagePath.Base path) {
            this(path, path.declaration);
        }

        public Declaration(@Nonnull UsagePath.Base path, @Nonnull RsElement element) {
            this.path = path;
            this.element = element;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Declaration)) return false;
            Declaration that = (Declaration) o;
            return path.equals(that.path) && element.equals(that.element);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, element);
        }

        @Override
        public String toString() {
            return "Declaration(" + path + ")";
        }
    }

    // ---- LivenessData ----
    public static class LivenessData {
        @Nonnull
        public final Set<Usage> usages;
        @Nonnull
        public final Set<Declaration> declarations;
        @Nonnull
        public final List<UsagePath> paths;
        @Nonnull
        private final Map<UsagePath, Integer> pathToIndex;

        public LivenessData() {
            this.usages = new LinkedHashSet<>();
            this.declarations = new LinkedHashSet<>();
            this.paths = new ArrayList<>();
            this.pathToIndex = new HashMap<>();
        }

        private void addUsagePath(@Nonnull UsagePath usagePath) {
            if (!pathToIndex.containsKey(usagePath)) {
                int index = paths.size();
                paths.add(usagePath);
                pathToIndex.put(usagePath, index);
            }
        }

        public boolean eachBasePath(@Nonnull UsagePath usagePath, @Nonnull java.util.function.Predicate<UsagePath> predicate) {
            UsagePath path = usagePath;
            while (true) {
                if (!predicate.test(path)) return false;
                if (path instanceof UsagePath.Base) return true;
                path = ((UsagePath.Extend) path).parent;
            }
        }

        public void addGenKills(@Nonnull DataFlowContext<LiveDataFlowOperator> dfcxLiveness) {
            for (Usage usage : usages) {
                Integer bit = pathToIndex.get(usage.path);
                if (bit == null) throw new IllegalStateException("No such usage path in pathToIndex");
                dfcxLiveness.addGen(usage.element, bit);
            }
            for (Declaration declaration : declarations) {
                Integer bit = pathToIndex.get(declaration.path);
                if (bit == null) throw new IllegalStateException("No such declaration path in pathToIndex");
                dfcxLiveness.addKill(KillFrom.ScopeEnd, declaration.element, bit);
            }
        }

        public void addUsage(@Nonnull RsElement element, @Nonnull Cmt cmt) {
            UsagePath usagePath = UsagePath.computeFor(cmt);
            if (usagePath == null) return;
            if (!pathToIndex.containsKey(usagePath)) return;
            usages.add(new Usage(usagePath, element));
        }

        public void addDeclaration(@Nonnull RsPatBinding element) {
            UsagePath.Base usagePath = new UsagePath.Base(element);
            addUsagePath(usagePath);
            declarations.add(new Declaration(usagePath));
        }
    }
}
