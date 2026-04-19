/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.liveness;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

// ---- Enums and Data Classes ----

public class Liveness {

    public enum DeclarationKind {
        Parameter, Variable
    }

    public static class DeadDeclaration {
        @NotNull
        public final RsPatBinding binding;
        @NotNull
        public final DeclarationKind kind;

        public DeadDeclaration(@NotNull RsPatBinding binding, @NotNull DeclarationKind kind) {
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
        @NotNull
        public final List<DeadDeclaration> deadDeclarations;
        @NotNull
        public final Map<RsPatBinding, List<RsElement>> lastUsages;

        public LivenessResult(
            @NotNull List<DeadDeclaration> deadDeclarations,
            @NotNull Map<RsPatBinding, List<RsElement>> lastUsages
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
        @NotNull
        public final RsInferenceResult inference;
        @NotNull
        public final RsBlock body;
        @NotNull
        public final ControlFlowGraph cfg;
        @NotNull
        public final ImplLookup implLookup;
        @NotNull
        private final List<DeadDeclaration> deadDeclarations;
        @NotNull
        private final Map<RsPatBinding, List<RsElement>> lastUsages;

        private LivenessContext(
            @NotNull RsInferenceResult inference,
            @NotNull RsBlock body,
            @NotNull ControlFlowGraph cfg,
            @NotNull ImplLookup implLookup
        ) {
            this.inference = inference;
            this.body = body;
            this.cfg = cfg;
            this.implLookup = implLookup;
            this.deadDeclarations = new ArrayList<>();
            this.lastUsages = new HashMap<>();
        }

        @Nullable
        public static LivenessContext buildFor(@NotNull RsInferenceContextOwner owner) {
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

        public void registerDeadDeclaration(@NotNull RsPatBinding binding, @NotNull DeclarationKind kind) {
            deadDeclarations.add(new DeadDeclaration(binding, kind));
        }

        public void registerLastUsage(@NotNull RsPatBinding binding, @NotNull RsElement usageElement) {
            lastUsages.computeIfAbsent(binding, k -> new ArrayList<>()).add(usageElement);
        }

        @NotNull
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
        @NotNull
        private final LivenessContext ctx;
        @NotNull
        private final LivenessData livenessData;
        @NotNull
        private final DataFlowContext<LiveDataFlowOperator> dfcxLivePaths;

        private FlowedLivenessData(
            @NotNull LivenessContext ctx,
            @NotNull LivenessData livenessData,
            @NotNull DataFlowContext<LiveDataFlowOperator> dfcxLivePaths
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

        private boolean isDeadOnEntry(@NotNull UsagePath usagePath, @NotNull RsElement element) {
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

        @NotNull
        public static FlowedLivenessData buildFor(@NotNull LivenessContext ctx, @NotNull LivenessData livenessData, @NotNull ControlFlowGraph cfg) {
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
        @NotNull
        private final LivenessContext ctx;
        @NotNull
        private final LivenessData livenessData;

        public GatherLivenessContext(@NotNull LivenessContext ctx) {
            this(ctx, new LivenessData());
        }

        public GatherLivenessContext(@NotNull LivenessContext ctx, @NotNull LivenessData livenessData) {
            this.ctx = ctx;
            this.livenessData = livenessData;
        }

        @Override
        public void consume(@NotNull RsElement element, @NotNull Cmt cmt, @NotNull ConsumeMode mode) {
            livenessData.addUsage(element, cmt);
        }

        @Override
        public void matchedPat(@NotNull RsPat pat, @NotNull Cmt cmt, @NotNull MatchMode mode) {
        }

        @Override
        public void consumePat(@NotNull RsPat pat, @NotNull Cmt cmt, @NotNull ConsumeMode mode) {
            for (RsPatBinding binding : RsElementUtil.descendantsOfType(pat, RsPatBinding.class)) {
                livenessData.addDeclaration(binding);
            }
            livenessData.addUsage(pat, cmt);
        }

        @Override
        public void declarationWithoutInit(@NotNull RsPatBinding binding) {
            livenessData.addDeclaration(binding);
        }

        @Override
        public void mutate(@NotNull RsElement assignmentElement, @NotNull Cmt assigneeCmt, @NotNull MutateMode mode) {
            if (mode == MutateMode.WriteAndRead) {
                livenessData.addUsage(assignmentElement, assigneeCmt);
            }
        }

        @Override
        public void useElement(@NotNull RsElement element, @NotNull Cmt cmt) {
            livenessData.addUsage(element, cmt);
        }

        @NotNull
        public LivenessData gather() {
            ExprUseWalker gatherVisitor = new ExprUseWalker(this, new MemoryCategorizationContext(ctx.implLookup, ctx.inference));
            gatherVisitor.consumeBody(ctx.body);
            return livenessData;
        }
    }

    // ---- UsagePath ----
    public static abstract class UsagePath {
        @NotNull
        public abstract RsPatBinding getDeclaration();

        public static class Base extends UsagePath {
            @NotNull
            public final RsPatBinding declaration;

            public Base(@NotNull RsPatBinding declaration) {
                this.declaration = declaration;
            }

            @NotNull
            @Override
            public RsPatBinding getDeclaration() {
                return declaration;
            }

            @NotNull
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
            @NotNull
            public final UsagePath parent;

            public Extend(@NotNull UsagePath parent) {
                this.parent = parent;
            }

            @NotNull
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

        @NotNull
        private Base getBase() {
            if (this instanceof Base) return (Base) this;
            return ((Extend) this).parent.getBase();
        }

        @NotNull
        public DeclarationKind getDeclarationKind() {
            return getBase().getDeclarationKind();
        }

        @Nullable
        public static UsagePath computeFor(@NotNull Cmt cmt) {
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
        @NotNull
        public final UsagePath path;
        @NotNull
        public final RsElement element;

        public Usage(@NotNull UsagePath path, @NotNull RsElement element) {
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
        @NotNull
        public final UsagePath.Base path;
        @NotNull
        public final RsElement element;

        public Declaration(@NotNull UsagePath.Base path) {
            this(path, path.declaration);
        }

        public Declaration(@NotNull UsagePath.Base path, @NotNull RsElement element) {
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
        @NotNull
        public final Set<Usage> usages;
        @NotNull
        public final Set<Declaration> declarations;
        @NotNull
        public final List<UsagePath> paths;
        @NotNull
        private final Map<UsagePath, Integer> pathToIndex;

        public LivenessData() {
            this.usages = new LinkedHashSet<>();
            this.declarations = new LinkedHashSet<>();
            this.paths = new ArrayList<>();
            this.pathToIndex = new HashMap<>();
        }

        private void addUsagePath(@NotNull UsagePath usagePath) {
            if (!pathToIndex.containsKey(usagePath)) {
                int index = paths.size();
                paths.add(usagePath);
                pathToIndex.put(usagePath, index);
            }
        }

        public boolean eachBasePath(@NotNull UsagePath usagePath, @NotNull java.util.function.Predicate<UsagePath> predicate) {
            UsagePath path = usagePath;
            while (true) {
                if (!predicate.test(path)) return false;
                if (path instanceof UsagePath.Base) return true;
                path = ((UsagePath.Extend) path).parent;
            }
        }

        public void addGenKills(@NotNull DataFlowContext<LiveDataFlowOperator> dfcxLiveness) {
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

        public void addUsage(@NotNull RsElement element, @NotNull Cmt cmt) {
            UsagePath usagePath = UsagePath.computeFor(cmt);
            if (usagePath == null) return;
            if (!pathToIndex.containsKey(usagePath)) return;
            usages.add(new Usage(usagePath, element));
        }

        public void addDeclaration(@NotNull RsPatBinding element) {
            UsagePath.Base usagePath = new UsagePath.Base(element);
            addUsagePath(usagePath);
            declarations.add(new Declaration(usagePath));
        }
    }
}
