/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.editorActions.TabOutScopesTracker;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.doc.psi.RsDocPathLinkParent;
import org.rust.ide.refactoring.RsNamesValidatorUtil;

public class RsDefaultInsertHandler implements InsertHandler<LookupElement> {

    @Override
    public final void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        PsiElement psiElement = item.getPsiElement();
        if (!(psiElement instanceof RsElement)) return;
        RsElement element = (RsElement) psiElement;
        String scopeName = item.getLookupString();
        handleInsert(element, scopeName, context, item);
    }

    protected void handleInsert(
        RsElement element,
        String scopeName,
        InsertionContext context,
        LookupElement item
    ) {
        Document document = context.getDocument();

        boolean shouldEscapeName = element instanceof RsNameIdentifierOwner
            && !RsNamesValidator.isIdentifier(scopeName)
            && RsNamesValidatorUtil.getCanBeEscaped(scopeName)
            && !scopeName.startsWith("iter().");
        if (shouldEscapeName) {
            document.insertString(context.getStartOffset(), RsNamesValidatorUtil.RS_RAW_PREFIX);
            context.commitDocument();
        }

        if (element instanceof RsGenericDeclaration) {
            addGenericTypeCompletion((RsGenericDeclaration) element, document, context);
        }

        if (LookupElements.getElementOfType(context, RsDocPathLinkParent.class) != null) return;

        RsUseItem curUseItem = LookupElements.getElementOfType(context, RsUseItem.class);

        if (element instanceof RsMod) {
            RsPath rsPath = LookupElements.getElementOfType(context, RsPath.class);
            if (shouldAppendDoubleColonToMod(rsPath, scopeName)) {
                addSuffix(context, "::");
            }
        } else if (element instanceof RsConstant || element instanceof RsTraitItem || element instanceof RsStructItem) {
            appendSemicolon(context, curUseItem);
        } else if (element instanceof RsFunction) {
            RsFunction fn = (RsFunction) element;
            if (curUseItem != null) {
                appendSemicolon(context, curUseItem);
            } else if (RsFunctionUtil.isProcMacroDef(fn)) {
                if (RsFunctionUtil.isBangProcMacroDef(fn)) {
                    appendProcMacroBraces(context, document);
                }
            } else {
                boolean isMethodCall = LookupElements.getElementOfType(context, RsMethodOrField.class) != null;
                if (!LookupElements.alreadyHasCallParens(context)) {
                    document.insertString(context.getSelectionEndOffset(), "()");
                    doNotAddOpenParenCompletionChar(context);
                }
                int caretShift = RsFunctionUtil.getValueParameters(fn).isEmpty() && (isMethodCall || !RsFunctionUtil.getHasSelfParameters(fn)) ? 2 : 1;
                EditorModificationUtil.moveCaretRelatively(context.getEditor(), caretShift);
                if (!LookupElements.alreadyHasCallParens(context) && caretShift == 1) {
                    TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(context.getEditor());
                }
                if (!RsFunctionUtil.getValueParameters(fn).isEmpty()) {
                    AutoPopupController controller = AutoPopupController.getInstance(element.getProject());
                    if (controller != null) {
                        controller.autoPopupParameterInfo(context.getEditor(), element);
                    }
                }
            }
        } else if (element instanceof RsEnumVariant) {
            RsEnumVariant variant = (RsEnumVariant) element;
            if (curUseItem == null) {
                String text;
                int shift;
                if (variant.getTupleFields() != null) {
                    doNotAddOpenParenCompletionChar(context);
                    text = "()";
                    shift = 1;
                } else if (variant.getBlockFields() != null) {
                    text = " {}";
                    shift = 2;
                } else {
                    text = "";
                    shift = 0;
                }
                if (!LookupElements.nextCharIs(context, '{') && !LookupElements.alreadyHasCallParens(context)) {
                    document.insertString(context.getSelectionEndOffset(), text);
                }
                EditorModificationUtil.moveCaretRelatively(context.getEditor(), shift);
                if (shift != 0) {
                    TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(context.getEditor());
                }
            }
        } else if (element instanceof RsMacroDefinitionBase) {
            if (curUseItem == null) {
                appendMacroBraces(context, document, (RsMacroDefinitionBase) element);
            } else {
                appendSemicolon(context, curUseItem);
            }
        }
    }

    private static void appendProcMacroBraces(InsertionContext context, Document document) {
        int caretShift = 2;
        boolean addBraces = !LookupElements.nextCharIs(context, '!');
        if (addBraces) {
            document.insertString(context.getSelectionEndOffset(), "!()");
        }
        EditorModificationUtil.moveCaretRelatively(context.getEditor(), caretShift);
        if (addBraces) {
            TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(context.getEditor());
        }
    }

    private static void appendMacroBraces(InsertionContext context, Document document, RsMacroDefinitionBase element) {
        int caretShift = 2;
        boolean addBraces = !LookupElements.nextCharIs(context, '!');
        if (addBraces) {
            MacroBraces braces = RsMacroDefinitionBaseUtil.getPreferredBraces(element);
            StringBuilder text = new StringBuilder();
            text.append("!");
            if (braces == MacroBraces.BRACES) {
                text.append(" ");
                caretShift = 3;
            }
            text.append(braces.getOpenText());
            text.append(braces.getCloseText());
            document.insertString(context.getSelectionEndOffset(), text.toString());
        }
        EditorModificationUtil.moveCaretRelatively(context.getEditor(), caretShift);
        if (addBraces) {
            TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(context.getEditor());
        }
    }

    private static void appendSemicolon(InsertionContext context, RsUseItem curUseItem) {
        if (curUseItem != null) {
            PsiElement lastChild = curUseItem.getLastChild();
            boolean hasSemicolon = lastChild != null && lastChild.getNode().getElementType() == RsElementTypes.SEMICOLON;
            boolean isInUseGroup = LookupElements.getElementOfType(context, RsUseGroup.class) != null;
            if (!(hasSemicolon || isInUseGroup)) {
                addSuffix(context, ";");
            }
        }
    }

    private static void addGenericTypeCompletion(RsGenericDeclaration element, Document document, InsertionContext context) {
        boolean allTypeParamsHaveDefaults = true;
        for (RsTypeParameter tp : RsGenericDeclarationUtil.getTypeParameters(element)) {
            if (tp.getTypeReference() == null) {
                allTypeParamsHaveDefaults = false;
                break;
            }
        }
        boolean allConstParamsHaveDefaults = true;
        for (RsConstParameter cp : RsGenericDeclarationUtil.getConstParameters(element)) {
            if (cp.getExpr() == null) {
                allConstParamsHaveDefaults = false;
                break;
            }
        }
        if (allTypeParamsHaveDefaults && allConstParamsHaveDefaults) return;

        RsPath path = LookupElements.getElementOfType(context, RsPath.class);
        if (path == null || !(path.getParent() instanceof RsTypeReference)) return;

        boolean insertedBraces = false;
        if (element instanceof RsElement && CompletionUtilsUtil.isFnLikeTrait((RsElement) element)) {
            if (!LookupElements.alreadyHasCallParens(context)) {
                document.insertString(context.getSelectionEndOffset(), "()");
                doNotAddOpenParenCompletionChar(context);
                insertedBraces = true;
            }
        } else {
            if (!LookupElements.nextCharIs(context, '<')) {
                document.insertString(context.getSelectionEndOffset(), "<>");
                insertedBraces = true;
            }
        }

        EditorModificationUtil.moveCaretRelatively(context.getEditor(), 1);
        if (insertedBraces) {
            TabOutScopesTracker.getInstance().registerEmptyScopeAtCaret(context.getEditor());
        }
    }

    private static void doNotAddOpenParenCompletionChar(InsertionContext context) {
        if (context.getCompletionChar() == '(') {
            context.setAddCompletionChar(false);
            LookupElements.Testmarks.DoNotAddOpenParenCompletionChar.hit();
        }
    }

    private static boolean shouldAppendDoubleColonToMod(PsiElement element, String scopeName) {
        if (!"crate".equals(scopeName) && !"self".equals(scopeName) && !"super".equals(scopeName)) {
            return false;
        }
        if (element != null && element.getParent() != null && element.getParent().getParent() instanceof RsUseGroup) {
            return false;
        }
        if (element != null && element.getParent() instanceof RsVisRestriction) {
            return false;
        }
        return true;
    }

    public static void addSuffix(InsertionContext ctx, String suffix) {
        ctx.getDocument().insertString(ctx.getSelectionEndOffset(), suffix);
        EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), suffix.length());
    }
}
