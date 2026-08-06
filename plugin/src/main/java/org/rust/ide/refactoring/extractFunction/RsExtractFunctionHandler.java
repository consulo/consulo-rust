/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiParserFacade;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.usage.UsageInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.refactoring.RsRenameProcessor;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.openapiext.CommandWriteActionUtilsUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsExtractFunctionHandler implements RefactoringActionHandler {

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, @Nullable DataContext dataContext) {
        // Not called from the editor
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nullable PsiFile file, @Nullable DataContext dataContext) {
        if (!(file instanceof RsFile)) return;
        if (editor == null) return;
        Integer start = editor.getSelectionModel().getSelectionStart();
        Integer end = editor.getSelectionModel().getSelectionEnd();
        RsExtractFunctionConfig config = RsExtractFunctionConfig.create(file, start, end);
        if (config == null) return;

        ExtractFunctionUi.extractFunctionDialog(project, config, () -> extractFunction(project, file, config));
    }

    private void extractFunction(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull RsExtractFunctionConfig config) {
        CommandWriteActionUtilsUtil.runWriteCommandAction(
            project,
            RefactoringBundle.message("extract.method.title"),
            new PsiFile[]{file},
            () -> {
                RsPsiFactory psiFactory = new RsPsiFactory(project);
                RsFunction extractedFunction = addExtractedFunction(project, config, psiFactory);
                if (extractedFunction == null) return null;
                replaceOldStatementsWithCallExpr(config, psiFactory);
                List<Parameter> params = config.getValueParameters().stream()
                    .filter(Parameter::isSelected)
                    .collect(Collectors.toList());
                List<String> names = params.stream().map(Parameter::getName).collect(Collectors.toList());
                renameFunctionParameters(extractedFunction, names);
                RsImportHelper.importTypeReferencesFromTys(extractedFunction, config.getParametersAndReturnTypes());
                return null;
            }
        );
    }

    @Nullable
    private RsFunction addExtractedFunction(
        @Nonnull Project project,
        @Nonnull RsExtractFunctionConfig config,
        @Nonnull RsPsiFactory psiFactory
    ) {
        RsFunction function = psiFactory.createFunction(config.getFunctionText());
        PsiParserFacade psiParserFacade = PsiParserFacade.getInstance(project);
        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(config.getFunction());

        if (owner instanceof RsAbstractableOwner.Impl && !((RsAbstractableOwner.Impl) owner).isInherent()) {
            // Add to inherent impl - simplified
            PsiElement newline = psiParserFacade.createWhiteSpaceFromText("\n\n");
            RsBlock block1 = RsFunctionUtil.getBlock(config.getFunction());
            PsiElement end = block1 != null ? block1.getRbrace() : null;
            if (end == null) return null;
            PsiElement addedNewline = config.getFunction().addAfter(newline, end);
            return (RsFunction) config.getFunction().addAfter(function, addedNewline);
        } else {
            PsiElement newline = psiParserFacade.createWhiteSpaceFromText("\n\n");
            RsBlock block2 = RsFunctionUtil.getBlock(config.getFunction());
            PsiElement end = block2 != null ? block2.getRbrace() : null;
            if (end == null) return null;
            PsiElement addedNewline = config.getFunction().addAfter(newline, end);
            return (RsFunction) config.getFunction().addAfter(function, addedNewline);
        }
    }

    private void renameFunctionParameters(@Nonnull RsFunction function, @Nonnull List<String> newNames) {
        List<RsPatBinding> parameters = new ArrayList<>();
        for (RsValueParameter param : function.getRawValueParameters()) {
            if (param.getPat() instanceof RsPatIdent) {
                parameters.add(((RsPatIdent) param.getPat()).getPatBinding());
            }
        }
        for (int i = 0; i < Math.min(parameters.size(), newNames.size()); i++) {
            RsPatBinding parameter = parameters.get(i);
            String newName = newNames.get(i);
            if (!newName.equals(parameter.getName())) {
                Collection<consulo.language.psi.PsiReference> parameterUsages =
                    ReferencesSearch.search(parameter, new LocalSearchScope(function)).findAll();
                UsageInfo[] usageInfo = parameterUsages.stream().map(UsageInfo::new).toArray(UsageInfo[]::new);
                new RsRenameProcessor().renameElement(parameter, newName, usageInfo, null);
            }
        }
    }

    private void replaceOldStatementsWithCallExpr(@Nonnull RsExtractFunctionConfig config, @Nonnull RsPsiFactory factory) {
        String call = generateFunctionCallFull(config);
        List<PsiElement> elements = config.getElements();
        for (int i = 0; i < elements.size() - 1; i++) {
            elements.get(i).delete();
        }
        PsiElement last = elements.get(elements.size() - 1);
        if (last instanceof RsExpr) {
            String callWithoutSemicolon = call.endsWith(";") ? call.substring(0, call.length() - 1) : call;
            last.replace(factory.createExpression(callWithoutSemicolon));
        } else if (last instanceof RsStmt) {
            last.replace(factory.createStatement(call));
        }
    }

    @Nonnull
    private String generateFunctionCallFull(@Nonnull RsExtractFunctionConfig config) {
        String call = generateFunctionCall(config);
        String letPrefix = config.getOutputVariables().getExprText() != null
            ? "let " + config.getOutputVariables().getExprText() + " = "
            : "";
        boolean needSemicolon = !letPrefix.isEmpty()
            || (config.getOutputVariables().getType() instanceof TyUnit
                && (config.getControlFlow() == null || config.getReturnKind() == ReturnKind.TRY_OPERATOR));
        String controlFlow = config.getControlFlow() != null ? config.getControlFlow().getText() : null;
        String result;
        switch (config.getReturnKind()) {
            case VALUE:
                result = letPrefix + call;
                break;
            case BOOL:
                result = "if " + call + " { " + controlFlow + "; }";
                break;
            case OPTION_CONTROL_FLOW:
                result = "if let Some(value) = " + call + " {\n" + controlFlow + " value;\n}";
                break;
            case OPTION_VALUE:
                result = letPrefix + "match " + call + " {\nSome(value) => value,\nNone => " + controlFlow + ",\n}";
                break;
            case RESULT:
                result = letPrefix + "match " + call + " {\nOk(value) => value,\nErr(value) => " + controlFlow + " value,\n}";
                break;
            case TRY_OPERATOR:
                result = letPrefix + call + "?";
                break;
            default:
                result = call;
        }
        return result + (needSemicolon ? ";" : "");
    }

    @Nonnull
    private String generateFunctionCall(@Nonnull RsExtractFunctionConfig config) {
        String self;
        if (!config.getParameters().isEmpty() && config.getParameters().get(0).isSelf()) {
            self = "self.";
        } else if (RsAbstractableUtil.getOwner(config.getFunction()).isImplOrTrait()) {
            self = "Self::";
        } else {
            self = "";
        }
        String await = config.isAsync() ? ".await" : "";
        return self + config.getName() + "(" + config.getArgumentsText() + ")" + await;
    }
}
