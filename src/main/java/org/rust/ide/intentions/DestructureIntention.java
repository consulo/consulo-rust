/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.refactoring.ExtraxtExpressionUtils;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.ide.utils.template.EditorExt;
import org.rust.ide.utils.template.RsTemplateBuilder;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyTuple;
import org.rust.lang.core.types.ty.Ty;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.SmartPointerUtil;
import org.rust.lang.core.psi.ext.RsFieldsOwnerUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.psi.ext.RsMod;

public class DestructureIntention extends RsElementBaseIntentionAction<DestructureIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.use.destructuring.declaration");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static abstract class Context {
        public final RsPatIdent patIdent;

        protected Context(RsPatIdent patIdent) {
            this.patIdent = patIdent;
        }

        public static class Struct extends Context {
            public final RsStructItem struct;
            public final TyAdt type;

            public Struct(RsPatIdent patIdent, RsStructItem struct, TyAdt type) {
                super(patIdent);
                this.struct = struct;
                this.type = type;
            }
        }

        public static class Tuple extends Context {
            public final TyTuple tuple;

            public Tuple(RsPatIdent patIdent, TyTuple tuple) {
                super(patIdent);
                this.tuple = tuple;
            }
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsPatIdent patIdent = RsPsiJavaUtil.ancestorStrict(element, RsPatIdent.class);
        if (patIdent == null) return null;
        Ty ty = RsTypesUtil.getType(patIdent.getPatBinding());
        if (ty instanceof TyAdt) {
            TyAdt tyAdt = (TyAdt) ty;
            if (!(tyAdt.getItem() instanceof RsStructItem)) return null;
            RsStructItem struct = (RsStructItem) tyAdt.getItem();
            RsMod mod = RsPsiJavaUtil.contextStrict(element, RsMod.class);
            if (mod == null) return null;
            if (!RsFieldsOwnerUtil.canBeInstantiatedIn(struct, mod)) return null;
            return new Context.Struct(patIdent, struct, tyAdt);
        } else if (ty instanceof TyTuple) {
            return new Context.Tuple(patIdent, (TyTuple) ty);
        }
        return null;
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsPatBinding patBinding = ctx.patIdent.getPatBinding();
        RsPsiFactory factory = new RsPsiFactory(patBinding.getProject());
        SearchScope searchScope = getSearchScope(patBinding);
        List<RsPath> usages = ReferencesSearch.search(patBinding, searchScope)
            .findAll().stream()
            .map(ref -> ref.getElement())
            .filter(el -> el instanceof RsPath)
            .map(el -> (RsPath) el)
            .collect(Collectors.toList());

        HashSet<String> bindings = findBindings(usages, patBinding);

        List<RsPatBinding> toRename = new ArrayList<>();

        if (ctx instanceof Context.Struct) {
            Context.Struct structCtx = (Context.Struct) ctx;
            String typeName = TypeRendering.render(structCtx.type, false);
            RsStructItem struct = structCtx.struct;

            if (RsStructItemUtil.isTupleStruct(struct)) {
                RsPatTupleStruct newStruct = factory.createPatTupleStruct(struct, typeName);
                RsPatTupleStruct replaced = (RsPatTupleStruct) ctx.patIdent.replace(newStruct);
                int fieldCount = RsFieldsOwnerUtil.getPositionalFields(struct).size();
                List<String> fieldNames = allocateTupleFieldNames(bindings, replaced, toRename, fieldCount);
                ImportBridge.importTypeReferencesFromTy(replaced, structCtx.type);
                replaceTupleUsages(fieldCount, fieldNames, struct.getName(), factory, usages);
            } else {
                RsPatStruct newStruct = factory.createPatStruct(struct, typeName);
                RsPatStruct replaced = (RsPatStruct) ctx.patIdent.replace(newStruct);
                List<String> fieldNamesList = new ArrayList<>();
                if (struct.getBlockFields() != null) {
                    for (RsNamedFieldDecl f : struct.getBlockFields().getNamedFieldDeclList()) {
                        if (f.getName() != null) fieldNamesList.add(f.getName());
                    }
                }
                Map<String, String> fieldNames = allocateStructFieldNames(bindings, replaced, toRename, fieldNamesList);
                ImportBridge.importTypeReferencesFromTy(replaced, structCtx.type);
                replaceStructUsages(struct, fieldNames, factory, usages);
            }
        } else if (ctx instanceof Context.Tuple) {
            Context.Tuple tupleCtx = (Context.Tuple) ctx;
            int fieldCount = tupleCtx.tuple.getTypes().size();
            RsPatTup patTuple = factory.createPatTuple(fieldCount);
            RsPatTup replaced = (RsPatTup) ctx.patIdent.replace(patTuple);
            List<String> fieldNames = allocateTupleFieldNames(bindings, replaced, toRename, fieldCount);
            replaceTupleUsages(fieldCount, fieldNames, null, factory, usages);
        }

        if (!toRename.isEmpty()) {
            renameNewBindings(ctx.patIdent.getContainingFile(), editor, toRename);
        }
    }

    private HashSet<String> findBindings(List<RsPath> usages, RsPatBinding patBinding) {
        HashSet<String> bindings = new HashSet<>();
        for (RsPath usage : usages) {
            bindings.addAll(RsPsiJavaUtil.getAllVisibleBindings(usage));
        }
        bindings.remove(patBinding.getIdentifier().getText());
        return bindings;
    }

    private static void renameNewBindings(PsiElement context, Editor editor, List<RsPatBinding> toBeRenamed) {
        List<SmartPsiElementPointer<RsPatBinding>> toBeRenamedPtrs = toBeRenamed.stream()
            .map(SmartPointerUtil::createSmartPointer)
            .collect(Collectors.toList());
        Project project = context.getProject();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        RsTemplateBuilder tpl = EditorExt.newTemplateBuilder(editor, context.getContainingFile());
        for (SmartPsiElementPointer<RsPatBinding> bindingPtr : toBeRenamedPtrs) {
            RsPatBinding binding = bindingPtr.getElement();
            if (binding == null) continue;
            RsTemplateBuilder.TemplateVariable variable = tpl.introduceVariable(binding);
            ReferencesSearch.search(binding, getSearchScope(binding)).forEach(ref -> {
                variable.replaceElementWithVariable(ref.getElement());
            });
        }
        tpl.runInline();
    }

    private static SearchScope getSearchScope(PsiElement element) {
        RsBlock block = RsPsiJavaUtil.ancestorStrict(element, RsBlock.class);
        if (block != null) return new LocalSearchScope(block);
        RsFunction function = RsPsiJavaUtil.ancestorStrict(element, RsFunction.class);
        if (function != null) return new LocalSearchScope(function);
        return new LocalSearchScope(element);
    }

    private static Map<String, String> allocateStructFieldNames(
        HashSet<String> bindings,
        RsPatStruct destructuredElement,
        List<RsPatBinding> toRename,
        List<String> fieldNames
    ) {
        RsPsiFactory factory = new RsPsiFactory(destructuredElement.getProject());
        Map<String, String> allocatedNames = new LinkedHashMap<>();
        for (String name : fieldNames) {
            allocatedNames.put(name, allocateName(bindings, name));
        }

        for (RsPatField patField : destructuredElement.getPatFieldList()) {
            RsPatBinding binding = patField.getPatBinding();
            if (binding == null) continue;
            String name = binding.getName();
            if (name == null) continue;
            String newName = allocatedNames.getOrDefault(name, name);
            if (!name.equals(newName)) {
                RsPatFieldFull patFieldFull = (RsPatFieldFull) binding.replace(factory.createPatFieldFull(name, newName));
                RsPatBinding newBinding = ExtraxtExpressionUtils.findBinding(patFieldFull);
                if (newBinding != null) {
                    toRename.add(newBinding);
                }
            }
        }
        return allocatedNames;
    }

    private static List<String> allocateTupleFieldNames(
        HashSet<String> bindings,
        RsElement destructuredElement,
        List<RsPatBinding> toRename,
        int fieldCount
    ) {
        RsPsiFactory factory = new RsPsiFactory(destructuredElement.getProject());
        List<String> fieldNames = new ArrayList<>();
        for (int i = 0; i <= fieldCount; i++) {
            fieldNames.add(allocateName(bindings, "_" + i));
        }
        List<RsPatIdent> fields = RsPsiJavaUtil.childrenOfType(destructuredElement, RsPatIdent.class);
        for (int i = 0; i < Math.min(fields.size(), fieldNames.size()); i++) {
            PsiElement inserted = fields.get(i).getPatBinding().getIdentifier().replace(factory.createIdentifier(fieldNames.get(i)));
            RsPatBinding binding = PsiTreeUtil.getParentOfType(inserted, RsPatBinding.class);
            if (binding != null) {
                toRename.add(binding);
            }
        }
        return fieldNames;
    }

    private static void replaceStructUsages(RsStructItem struct, Map<String, String> fieldNames, RsPsiFactory factory, List<RsPath> usages) {
        for (RsPath element : usages) {
            PsiElement parentParent = element.getParent() != null ? element.getParent().getParent() : null;
            if (parentParent instanceof RsDotExpr) {
                RsDotExpr dot = (RsDotExpr) parentParent;
                if (dot.getMethodCall() != null) continue;

                PsiElement field = dot.getFieldLookup() != null ? dot.getFieldLookup().getIdentifier() : null;
                if (field != null) {
                    String name = field.getText();
                    dot.replace(factory.createExpression(fieldNames.getOrDefault(name, name)));
                } else if (element.getParent() instanceof RsPathExpr) {
                    String name = struct.getName();
                    if (name == null) continue;
                    List<RsNamedFieldDecl> declList = struct.getBlockFields() != null ? struct.getBlockFields().getNamedFieldDeclList() : Collections.emptyList();
                    List<String> fields = new ArrayList<>();
                    for (RsNamedFieldDecl f : declList) {
                        String fieldName = fieldNames.getOrDefault(f.getName(), f.getName());
                        if (fieldName != null && fieldName.equals(f.getName())) {
                            fields.add(fieldName);
                        } else {
                            fields.add(f.getName() + ": " + fieldName);
                        }
                    }
                    String fieldsStr = "{ " + String.join(", ", fields) + " }";
                    PsiElement structLiteral = factory.createStructLiteralWithFields(name, fieldsStr);
                    element.getParent().replace(structLiteral);
                }
            }
        }
    }

    private static void replaceTupleUsages(int fieldCount, List<String> fieldNames, String structName, RsPsiFactory factory, List<RsPath> usages) {
        for (RsPath element : usages) {
            PsiElement parentParent = element.getParent() != null ? element.getParent().getParent() : null;
            if (parentParent instanceof RsDotExpr) {
                RsDotExpr dot = (RsDotExpr) parentParent;
                if (dot.getMethodCall() != null) continue;

                PsiElement integerLiteral = dot.getFieldLookup() != null ? dot.getFieldLookup().getIntegerLiteral() : null;
                if (integerLiteral != null) {
                    try {
                        int indexNumber = Integer.parseInt(integerLiteral.getText());
                        String replacement = indexNumber < fieldNames.size() ? fieldNames.get(indexNumber) : "_" + integerLiteral.getText();
                        dot.replace(factory.createExpression(replacement));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (element.getParent() instanceof RsPathExpr) {
                    String prefix = (structName != null ? structName : "") + "(";
                    List<String> parts = new ArrayList<>();
                    for (int i = 0; i < fieldCount; i++) {
                        parts.add(i < fieldNames.size() ? fieldNames.get(i) : "_" + i);
                    }
                    PsiElement tupleExpr = factory.createExpression(prefix + String.join(", ", parts) + ")");
                    element.getParent().replace(tupleExpr);
                }
            }
        }
    }

    private static String allocateName(HashSet<String> bindings, String baseName) {
        String name = baseName;
        int index = 0;
        while (bindings.contains(name)) {
            name = baseName + index;
            index++;
        }
        bindings.add(name);
        return name;
    }
}
