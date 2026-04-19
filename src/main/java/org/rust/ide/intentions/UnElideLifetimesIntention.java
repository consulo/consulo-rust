/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.regions.ReEarlyBound;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.doc.psi.ext.RsDocPsiElementUtil;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class UnElideLifetimesIntention extends RsElementBaseIntentionAction<UnElideLifetimesIntention.LifetimeContext> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.un.elide.lifetimes");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Nullable
    @Override
    public LifetimeContext findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (RsDocPsiElementUtil.isInDocComment(element)) return null;
        RsFunction fn = PsiTreeUtil.getParentOfType(element, RsFunction.class, false, RsBlock.class);
        if (fn == null) return null;

        LifetimeContext ctx = getLifetimeContext(fn);
        if (ctx == null) return null;
        List<RsLifetime> outputLifetimes = ctx.getOutput() != null ? ctx.getOutput().getLifetimes() : null;
        if (outputLifetimes != null) {
            boolean hasNonNull = false;
            for (RsLifetime lt : outputLifetimes) {
                if (lt != null) hasNonNull = true;
            }
            if (hasNonNull || outputLifetimes.size() > 1) return null;
        }

        List<RsLifetime> inputLifetimes = new ArrayList<>();
        for (PotentialLifetimeRef input : ctx.getInputs()) {
            inputLifetimes.addAll(input.getLifetimes());
        }
        if (ctx.getSelf() != null) {
            inputLifetimes.addAll(ctx.getSelf().getLifetimes());
        }
        if (inputLifetimes.isEmpty()) return null;
        for (RsLifetime lt : inputLifetimes) {
            if (lt != null) return null;
        }

        return ctx;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull LifetimeContext ctx) {
        RsFunction fn = ctx.getFn();
        List<PotentialLifetimeRef> inputs = ctx.getInputs();
        PotentialLifetimeRef output = ctx.getOutput();
        PotentialLifetimeRef self = ctx.getSelf();

        List<String> addedLifetimes = new ArrayList<>();
        Iterator<String> generator = nameGenerator();

        List<PotentialLifetimeRef> allInputs = new ArrayList<>();
        if (self != null) allInputs.add(self);
        allInputs.addAll(inputs);

        for (PotentialLifetimeRef ref : allInputs) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < ref.getLifetimes().size(); i++) {
                names.add(generator.next());
            }
            addLifetimeParameter(ref, names);
            addedLifetimes.addAll(names);
        }

        List<String> existingParams = new ArrayList<>();
        for (RsGenericParameter param : (fn.getTypeParameterList() != null ? RsTypeParameterListUtil.getGenericParameters(fn.getTypeParameterList(), false) : java.util.Collections.<RsGenericParameter>emptyList())) {
            existingParams.add(param.getText());
        }
        List<String> allParams = new ArrayList<>(addedLifetimes);
        allParams.addAll(existingParams);
        RsTypeParameterList genericParams = new RsPsiFactory(project).createTypeParameterList(allParams);
        RsTypeParameterList existing = fn.getTypeParameterList();
        if (existing != null) {
            existing.replace(genericParams);
        } else {
            fn.addAfter(genericParams, fn.getIdentifier());
        }

        // return type
        if (output == null) return;

        if (self != null || inputs.size() == 1) {
            addLifetimeParameter(output, Collections.singletonList(addedLifetimes.get(0)));
        } else {
            String unknownLifetime = "'_";
            PsiElement resultElement = addLifetimeParameter(output, Collections.singletonList(unknownLifetime));
            resultElement.accept(new RsRecursiveVisitor() {
                @Override
                public void visitLifetime(@NotNull RsLifetime o) {
                    if (o.getQuoteIdentifier().getText().equals(unknownLifetime)) {
                        int start = PsiElementExt.getStartOffset(o) + 1;
                        org.rust.openapiext.Editor.moveCaretToOffset(editor, o, start);
                        editor.getSelectionModel().setSelection(start, PsiElementExt.getEndOffset(o));
                    }
                }
            });
        }
    }

    @NotNull
    private Iterator<String> nameGenerator() {
        return new Iterator<String>() {
            private int myIndex = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                int abcSize = 'z' - 'a' + 1;
                char letter = (char) ('a' + myIndex % abcSize);
                int idx = myIndex / abcSize;
                myIndex++;
                if (idx == 0) {
                    return "'" + letter;
                } else {
                    return "'" + letter + idx;
                }
            }
        };
    }

    public static class LifetimeContext {
        private final RsFunction myFn;
        private final List<PotentialLifetimeRef> myInputs;
        @Nullable
        private final PotentialLifetimeRef myOutput;
        @Nullable
        private final PotentialLifetimeRef mySelf;

        public LifetimeContext(@NotNull RsFunction fn, @NotNull List<PotentialLifetimeRef> inputs,
                               @Nullable PotentialLifetimeRef output, @Nullable PotentialLifetimeRef self) {
            myFn = fn;
            myInputs = inputs;
            myOutput = output;
            mySelf = self;
        }

        @NotNull
        public RsFunction getFn() {
            return myFn;
        }

        @NotNull
        public List<PotentialLifetimeRef> getInputs() {
            return myInputs;
        }

        @Nullable
        public PotentialLifetimeRef getOutput() {
            return myOutput;
        }

        @Nullable
        public PotentialLifetimeRef getSelf() {
            return mySelf;
        }
    }

    public static abstract class PotentialLifetimeRef {
        private final RsElement myElement;

        protected PotentialLifetimeRef(@NotNull RsElement element) {
            myElement = element;
        }

        @NotNull
        public RsElement getElement() {
            return myElement;
        }

        @NotNull
        public abstract List<RsLifetime> getLifetimes();

        public static class Self extends PotentialLifetimeRef {
            private final RsSelfParameter mySelf;

            public Self(@NotNull RsSelfParameter self) {
                super(self);
                mySelf = self;
            }

            @NotNull
            public RsSelfParameter getSelfParam() {
                return mySelf;
            }

            @NotNull
            @Override
            public List<RsLifetime> getLifetimes() {
                return Collections.singletonList(mySelf.getLifetime());
            }
        }

        public static class RefLike extends PotentialLifetimeRef {
            private final RsRefLikeType myRef;

            public RefLike(@NotNull RsRefLikeType ref) {
                super(ref);
                myRef = ref;
            }

            @NotNull
            public RsRefLikeType getRef() {
                return myRef;
            }

            @NotNull
            @Override
            public List<RsLifetime> getLifetimes() {
                return Collections.singletonList(myRef.getLifetime());
            }
        }

        public static class PathType extends PotentialLifetimeRef {
            private final RsPathType myPathType;
            private final Ty myType;

            public PathType(@NotNull RsPathType pathType, @NotNull Ty type) {
                super(pathType);
                myPathType = pathType;
                myType = type;
            }

            @NotNull
            public RsPathType getPathType() {
                return myPathType;
            }

            @NotNull
            public Ty getType() {
                return myType;
            }

            @NotNull
            public List<Region> getTypeLifetimes() {
                Ty rawType = RsTypesUtil.getRawType(myPathType);
                if (rawType instanceof TyAdt) {
                    List<Region> result = new ArrayList<>();
                    for (Region r : ((TyAdt) rawType).getTypeParameterValues().getRegions()) {
                        if (r instanceof ReEarlyBound) {
                            result.add(r);
                        }
                    }
                    return result;
                }
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public List<RsLifetime> getLifetimes() {
                List<Region> typeLifetimes = getTypeLifetimes();
                RsPath path = myPathType.getPath();
                RsTypeArgumentList typeArgList = path != null ? path.getTypeArgumentList() : null;
                List<RsLifetime> actualLifetimes = typeArgList != null ? typeArgList.getLifetimeList() : null;
                List<RsLifetime> result = new ArrayList<>();
                for (int i = 0; i < typeLifetimes.size(); i++) {
                    RsLifetime lt = (actualLifetimes != null && i < actualLifetimes.size()) ? actualLifetimes.get(i) : null;
                    result.add(lt);
                }
                return result;
            }
        }
    }

    private static boolean isPotentialLifetimeAdt(@NotNull RsTypeReference ref) {
        Ty type = RsTypesUtil.getRawType(ref);
        if (type instanceof TyAdt) {
            for (Region r : ((TyAdt) type).getTypeParameterValues().getRegions()) {
                if (!(r instanceof ReEarlyBound)) return false;
            }
            return true;
        }
        return false;
    }

    @Nullable
    private static PotentialLifetimeRef parsePotentialLifetimeType(@NotNull RsTypeReference ref) {
        if (ref instanceof RsRefLikeType) {
            return new PotentialLifetimeRef.RefLike((RsRefLikeType) ref);
        }
        if (ref instanceof RsPathType && isPotentialLifetimeAdt(ref)) {
            return new PotentialLifetimeRef.PathType((RsPathType) ref, RsTypesUtil.getRawType(ref));
        }
        return null;
    }

    @Nullable
    private static LifetimeContext getLifetimeContext(@NotNull RsFunction fn) {
        List<RsValueParameter> valueParameters = RsFunctionUtil.getRawValueParameters(fn);

        // Current implementation of the intention works incorrectly in the case of function parameters with
        // #[cfg] attributes. Disable the intention in this case for now.
        for (RsValueParameter param : valueParameters) {
            if (RsElementUtil.queryAttributes(param).hasCfgAttr()) return null;
        }

        List<PotentialLifetimeRef> inputArgs = new ArrayList<>();
        for (RsValueParameter param : valueParameters) {
            RsTypeReference typeRef = param.getTypeReference();
            if (typeRef != null) {
                PotentialLifetimeRef parsed = parsePotentialLifetimeType(typeRef);
                if (parsed != null) {
                    inputArgs.add(parsed);
                }
            }
        }

        PotentialLifetimeRef retType = null;
        RsRetType retTypeElement = fn.getRetType();
        if (retTypeElement != null) {
            RsTypeReference typeRef = retTypeElement.getTypeReference();
            if (typeRef != null) {
                retType = parsePotentialLifetimeType(typeRef);
            }
        }

        PotentialLifetimeRef selfRef = null;
        RsSelfParameter selfParam = fn.getSelfParameter();
        if (selfParam != null) {
            selfRef = new PotentialLifetimeRef.Self(selfParam);
        }

        return new LifetimeContext(fn, inputArgs, retType, selfRef);
    }

    @NotNull
    private static PsiElement addLifetimeParameter(@NotNull PotentialLifetimeRef ref, @NotNull List<String> names) {
        RsPsiFactory factory = new RsPsiFactory(ref.getElement().getProject());
        if (ref instanceof PotentialLifetimeRef.Self) {
            PsiElement elem = ref.getElement();
            return elem.replace(factory.createMethodParam(elem.getText().replaceFirst("&", "&" + names.get(0) + " ")));
        } else if (ref instanceof PotentialLifetimeRef.RefLike) {
            PsiElement elem = ref.getElement();
            RsTypeReference typeRef = factory.createType(elem.getText().replaceFirst("&", "&" + names.get(0) + " "));
            return elem.replace(typeRef);
        } else if (ref instanceof PotentialLifetimeRef.PathType) {
            PotentialLifetimeRef.PathType pathTypeRef = (PotentialLifetimeRef.PathType) ref;
            RsPathType elem = pathTypeRef.getPathType();
            List<String> restArgs = new ArrayList<>();
            for (RsElement arg : RsMethodOrPathUtil.getGenericArguments(elem.getPath(), false, true, true, true)) {
                restArgs.add(arg.getText());
            }
            List<String> allArgs = new ArrayList<>(names);
            allArgs.addAll(restArgs);
            RsTypeArgumentList types = factory.createTypeArgumentList(allArgs);
            RsTypeReference replacement = factory.createType(elem.getPath().getReferenceName() + types.getText());
            return elem.replace(replacement);
        }
        throw new IllegalArgumentException("Unknown PotentialLifetimeRef type");
    }
}
