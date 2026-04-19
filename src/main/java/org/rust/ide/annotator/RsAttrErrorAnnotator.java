/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.*;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.BuiltinAttributes.AttributeTemplate;
import org.rust.lang.core.psi.BuiltinAttributes.BuiltinAttributeInfo;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.TyInteger;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.lang.utils.RsErrorCodeUtil;
import org.rust.stdext.StdextUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.RsLitExprUtil;
import org.rust.lang.core.psi.RsLiteralKindUtil;
import org.rust.lang.core.psi.ext.RsElementExtUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;

public class RsAttrErrorAnnotator extends AnnotatorBase {
    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        RsAnnotationHolder rsHolder = new RsAnnotationHolder(holder);
        if (element instanceof RsMetaItem) {
            RsMetaItem metaItem = (RsMetaItem) element;
            ProcessingContext context = new ProcessingContext();
            if (!RsMetaItemUtil.isRootMetaItem(metaItem, context)) return;

            checkMetaBadDelim(metaItem, holder);
            checkAttrTemplateCompatible(metaItem, holder);
            checkLiteralSuffix(metaItem, holder);

            String name = metaItem.getName();
            if (name != null) {
                switch (name) {
                    case "deprecated":
                        checkDeprecatedAttr(metaItem, holder);
                        break;
                    case "feature":
                        checkFeatureAttribute(metaItem, rsHolder, context);
                        break;
                    case "cfg":
                    case "cfg_attr":
                        checkRootCfgPredicate(metaItem, rsHolder);
                        break;
                    case "repr":
                        checkReprAttr(metaItem, rsHolder, context);
                        break;
                    case "derive":
                        checkDeriveAttr(metaItem, rsHolder);
                        break;
                }
            }
        } else if (element instanceof RsDocAndAttributeOwner) {
            checkRsDocAndAttributeOwner((RsDocAndAttributeOwner) element, holder);
        }
    }

    private static void checkDeprecatedAttr(@NotNull RsMetaItem element, @NotNull AnnotationHolder holder) {
        if (RsMetaItemUtil.getTemplateType(element) != AttributeTemplateType.List) return;
        Set<String> usedArgs = new HashSet<>();
        RsMetaItemArgs metaItemArgs0 = element.getMetaItemArgs();
        List<RsMetaItem> argsList = metaItemArgs0 != null ? metaItemArgs0.getMetaItemList() : Collections.emptyList();
        for (RsMetaItem metaArg : argsList) {
            String argName = metaArg.getName();
            if (argName == null) continue;
            switch (argName) {
                case "since":
                case "note":
                case "suggestion": {
                    if (metaArg.getEq() == null) {
                        RsDiagnostic.addToHolder(new RsDiagnostic.IncorrectItemInDeprecatedAttr(metaArg), holder);
                    } else {
                        if (usedArgs.contains(argName)) {
                            RsDiagnostic.addToHolder(new RsDiagnostic.MultipleItemsInDeprecatedAttr(metaArg, argName), holder);
                        }
                        usedArgs.add(argName);
                    }
                    break;
                }
                default: {
                    RsDiagnostic.addToHolder(new RsDiagnostic.UnknownItemInDeprecatedAttr(metaArg, argName), holder);
                    break;
                }
            }
        }
    }

    private static void checkRsDocAndAttributeOwner(@NotNull RsDocAndAttributeOwner element, @NotNull AnnotationHolder holder) {
        Iterable<RsMetaItem> attrs = RsDocAndAttributeOwnerUtil.getQueryAttributes(element).getMetaItems();
        Map<String, List<RsMetaItem>> duplicates = new HashMap<>();
        for (RsMetaItem attr : attrs) {
            String name = attr.getName();
            if (name != null && BuiltinAttributes.RS_BUILTIN_ATTRIBUTES.containsKey(name)) {
                duplicates.computeIfAbsent(name, k -> new ArrayList<>()).add(attr);
            }
        }

        for (Map.Entry<String, List<RsMetaItem>> entry : duplicates.entrySet()) {
            String name = entry.getKey();
            List<RsMetaItem> entries = entry.getValue();
            if (name == null) continue;
            if (entries.size() == 1) continue;
            Object attrInfoObj = BuiltinAttributes.RS_BUILTIN_ATTRIBUTES.get(name);
            if (!(attrInfoObj instanceof BuiltinAttributeInfo)) continue;
            BuiltinAttributeInfo attrInfo = (BuiltinAttributeInfo) attrInfoObj;

            switch (attrInfo.duplicates()) {
                case DuplicatesOk:
                    continue;
                case WarnFollowing:
                    RsDiagnostic.addToHolder(new RsDiagnostic.UnusedAttribute(entries.get(0), getFix(entries.get(entries.size() - 1))), holder);
                    break;
                case WarnFollowingWordOnly: {
                    List<RsMetaItem> wordStyleArgs = new ArrayList<>();
                    for (RsMetaItem e : entries) {
                        if (RsMetaItemUtil.getTemplateType(e) == AttributeTemplateType.Word) {
                            wordStyleArgs.add(e);
                        }
                    }
                    if (wordStyleArgs.size() <= 1) continue;
                    RsDiagnostic.addToHolder(new RsDiagnostic.UnusedAttribute(wordStyleArgs.get(0), getFix(wordStyleArgs.get(wordStyleArgs.size() - 1))), holder);
                    break;
                }
                case ErrorFollowing:
                    RsDiagnostic.addToHolder(new RsDiagnostic.MultipleAttributes(entries.get(0), name, getFix(entries.get(entries.size() - 1))), holder);
                    break;
                case ErrorPreceding:
                    RsDiagnostic.addToHolder(new RsDiagnostic.MultipleAttributes(entries.get(entries.size() - 1), name, getFix(entries.get(0))), holder);
                    break;
                case FutureWarnFollowing:
                    RsDiagnostic.addToHolder(new RsDiagnostic.UnusedAttribute(entries.get(0), getFix(entries.get(entries.size() - 1)), true), holder);
                    break;
                case FutureWarnPreceding:
                    RsDiagnostic.addToHolder(new RsDiagnostic.UnusedAttribute(entries.get(entries.size() - 1), getFix(entries.get(0)), true), holder);
                    break;
            }
        }
    }

    @NotNull
    private static RemoveElementFix getFix(@NotNull RsMetaItem item) {
        PsiElement parent = item.getParent();
        if (parent instanceof RsAttr) {
            return new RemoveElementFix((RsAttr) parent);
        }
        return new RemoveElementFix(item);
    }

    private static void checkLiteralSuffix(@NotNull RsMetaItem metaItem, @NotNull AnnotationHolder holder) {
        String name = metaItem.getName();
        if (name == null) return;
        if (!BuiltinAttributes.RS_BUILTIN_ATTRIBUTES.containsKey(name)) return;
        RsMetaItemArgs metaItemArgs = metaItem.getMetaItemArgs();
        if (metaItemArgs == null) return;
        List<RsLitExpr> exprList = metaItemArgs.getLitExprList();
        for (RsLitExpr expr : exprList) {
            RsLiteralKind kind = RsLiteralKindUtil.getKind(expr);
            if (kind instanceof RsLiteralKind.RsLiteralWithSuffix) {
                String suffix = ((RsLiteralKind.RsLiteralWithSuffix) kind).getSuffix();
                if (suffix == null) continue;
                String editedText = expr.getText();
                if (editedText.endsWith(suffix)) {
                    editedText = editedText.substring(0, editedText.length() - suffix.length());
                }
                SubstituteTextFix fix = SubstituteTextFix.replace(
                    RsBundle.message("intention.name.remove.suffix"), metaItem.getContainingFile(), expr.getTextRange(), editedText
                );
                RsDiagnostic.addToHolder(new RsDiagnostic.AttributeSuffixedLiteral(expr, fix), holder);
            }
        }
    }

    private static void checkAttrTemplateCompatible(@NotNull RsMetaItem metaItem, @NotNull AnnotationHolder holder) {
        String name = metaItem.getName();
        if (name == null) return;
        Object attrInfoObj = BuiltinAttributes.RS_BUILTIN_ATTRIBUTES.get(name);
        if (!(attrInfoObj instanceof BuiltinAttributeInfo)) return;
        BuiltinAttributeInfo attrInfo = (BuiltinAttributeInfo) attrInfoObj;
        AttributeTemplate template = attrInfo.template();
        boolean isError = false;
        switch (RsMetaItemUtil.getTemplateType(metaItem)) {
            case List:
                if (template.list() == null) isError = true;
                break;
            case NameValueStr:
                if (template.nameValueStr() == null) isError = true;
                break;
            case Word:
                if (!template.word()) isError = true;
                break;
        }
        if (isError) {
            emitMalformedAttribute(metaItem, name, template, holder);
        }
    }

    private static void emitMalformedAttribute(@NotNull RsMetaItem metaItem, @NotNull String name, @NotNull AttributeTemplate template, @NotNull AnnotationHolder holder) {
        String inner = metaItem.getContext() instanceof RsInnerAttr ? "!" : "";
        boolean first = true;
        StringBuilder stringBuilder = new StringBuilder();
        if (template.word()) {
            first = false;
            stringBuilder.append("#").append(inner).append("[").append(name).append("]");
        }
        if (template.list() != null) {
            if (!first) stringBuilder.append(" or ");
            first = false;
            stringBuilder.append("#").append(inner).append("[").append(name).append("(").append(template.list()).append(")]");
        }
        if (template.nameValueStr() != null) {
            if (!first) stringBuilder.append(" or ");
            stringBuilder.append("#").append(inner).append("[").append(name).append(" = \"").append(template.nameValueStr()).append("\"]");
        }
        String msg = first ? RsBundle.message("tooltip.must.be.form") : RsBundle.message("tooltip.following.are.possible.correct.uses");
        RsDiagnostic.addToHolder(new RsDiagnostic.MalformedAttributeInput(metaItem, name, msg + " " + stringBuilder), holder);
    }

    @NotNull
    private static String setParen(@NotNull String text) {
        char[] chars = text.toCharArray();
        int leftIdx = -1;
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] == '[' || chars[i] == '{') { leftIdx = i; break; }
        }
        int rightIdx = -1;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ']' || chars[i] == '}') { rightIdx = i; break; }
        }
        if (leftIdx >= 0) chars[leftIdx] = '(';
        if (rightIdx >= 0) chars[rightIdx] = ')';
        return new String(chars);
    }

    private static void checkMetaBadDelim(@NotNull RsMetaItem element, @NotNull AnnotationHolder holder) {
        PsiElement nameElement = element.getPath();
        if (nameElement == null) {
            RsCompactTT compactTT = element.getCompactTT();
            if (compactTT != null) {
                for (PsiElement child : compactTT.getChildren()) {
                    if (RsElementUtil.getElementType(child) == RsElementTypes.IDENTIFIER) {
                        nameElement = child;
                        break;
                    }
                }
            }
        }
        if (nameElement == null) return;
        if (!BuiltinAttributes.RS_BUILTIN_ATTRIBUTES.containsKey(nameElement.getText())) return;

        RsCompactTT compactTT = element.getCompactTT();
        if (compactTT == null) return;
        PsiElement openDelim = null;
        for (PsiElement child : compactTT.getChildren()) {
            if (RsElementUtil.getElementType(child) == RsElementTypes.EQ
                || RsElementUtil.getElementType(child) == RsElementTypes.LBRACE
                || RsElementUtil.getElementType(child) == RsElementTypes.LBRACK) {
                openDelim = child;
                break;
            }
        }
        if (openDelim == null) return;
        if (RsElementUtil.getElementType(openDelim) == RsElementTypes.EQ) return;

        PsiElement closingDelim = null;
        PsiElement[] children = compactTT.getChildren();
        for (int i = children.length - 1; i >= 0; i--) {
            if (RsElementUtil.getElementType(children[i]) == RsElementTypes.RBRACE
                || RsElementUtil.getElementType(children[i]) == RsElementTypes.RBRACK) {
                closingDelim = children[i];
                break;
            }
        }
        if (closingDelim == null) return;

        boolean isBracePair = RsElementUtil.getElementType(openDelim) == RsElementTypes.LBRACE
            && RsElementUtil.getElementType(closingDelim) == RsElementTypes.RBRACE;
        boolean isBracketPair = RsElementUtil.getElementType(openDelim) == RsElementTypes.LBRACK
            && RsElementUtil.getElementType(closingDelim) == RsElementTypes.RBRACK;

        if (isBracePair || isBracketPair) {
            String fixedText = setParen(element.getText());
            SubstituteTextFix fix = SubstituteTextFix.replace(
                RsBundle.message("intention.name.replace.brackets"), element.getContainingFile(), element.getTextRange(), fixedText
            );
            RsDiagnostic.addToHolder(new RsDiagnostic.WrongMetaDelimiters(openDelim, closingDelim, fix), holder);
        }
    }

    private static void checkFeatureAttribute(@NotNull RsMetaItem item, @NotNull RsAnnotationHolder holder, @NotNull ProcessingContext context) {
        RsAttr attr = context.get(RsPsiPattern.META_ITEM_ATTR);
        if (!(attr instanceof RsInnerAttr)) return;
        RsPath path = item.getPath();
        if (path == null) return;
        RsMod containingMod = RsElementUtil.getContainingMod(item);
        if (containingMod == null || !containingMod.isCrateRoot()) return;
        RsMetaItemArgs metaItemArgs = item.getMetaItemArgs();
        List<RsMetaItem> metaItemList = metaItemArgs != null ? metaItemArgs.getMetaItemList() : Collections.emptyList();
        if (metaItemList.isEmpty()) return;

        for (RsMetaItem metaItem : metaItemList) {
            String featureName = metaItem.getName();
            if (featureName == null) continue;
            CompilerFeature feature = CompilerFeature.find(featureName);
            if (feature == null) continue;
            if (feature.availability(item) == FeatureAvailability.REMOVED) {
                boolean isAvailable = metaItemList.size() > 1 || (metaItemList.size() == 1 && item.getParent() == attr);
                RemoveMetaItemFix fix = isAvailable ? new RemoveMetaItemFix(metaItem) : null;
                RsDiagnostic.addToHolder(new RsDiagnostic.FeatureAttributeHasBeenRemoved(metaItem, featureName, fix), holder);
            }
        }

        RustcVersion version = RsElementExtUtil.getCargoProject(item) != null && RsElementExtUtil.getCargoProject(item).getRustcInfo() != null
            ? RsElementExtUtil.getCargoProject(item).getRustcInfo().getVersion() : null;
        if (version == null) return;
        if (RsDiagnostic.areUnstableFeaturesAvailable(item, version) != ThreeState.NO) return;

        String channelName = version.getChannel().getChannel();
        if (channelName == null) return;

        RemoveAttrFix fix = item.getParent() == attr ? new RemoveAttrFix((RsAttr) attr) : null;
        RsDiagnostic.addToHolder(new RsDiagnostic.FeatureAttributeInNonNightlyChannel(path, channelName, fix), holder);
    }

    private static void checkRootCfgPredicate(@NotNull RsMetaItem element, @NotNull RsAnnotationHolder holder) {
        RsMetaItemArgs args = element.getMetaItemArgs();
        if (args == null) return;
        List<RsMetaItem> metaItemList = args.getMetaItemList();
        if (metaItemList.isEmpty()) return;
        RsMetaItem item = metaItemList.get(0);
        checkCfgPredicate(holder, item);
    }

    private static void checkCfgPredicate(@NotNull RsAnnotationHolder holder, @NotNull RsMetaItem item) {
        String itemName = item.getName();
        if (itemName == null) return;
        RsMetaItemArgs args = item.getMetaItemArgs();
        if (args == null) return;
        switch (itemName) {
            case "all":
            case "any":
                for (RsMetaItem mi : args.getMetaItemList()) {
                    checkCfgPredicate(holder, mi);
                }
                break;
            case "not":
                if (args.getMetaItemList().size() == 1) {
                    RsMetaItem parameter = args.getMetaItemList().get(0);
                    checkCfgPredicate(holder, parameter);
                } else {
                    List<ConvertMalformedCfgNotPatternToCfgAllPatternFix> fixes = new ArrayList<>();
                    ConvertMalformedCfgNotPatternToCfgAllPatternFix fix = ConvertMalformedCfgNotPatternToCfgAllPatternFix.createIfCompatible(item);
                    if (fix != null) fixes.add(fix);
                    RsDiagnostic.addToHolder(new RsDiagnostic.CfgNotPatternIsMalformed(item, fixes), holder);
                }
                break;
            case "version":
                break;
            default: {
                RsPath path = item.getPath();
                if (path == null) return;
                List<String> candidates = Arrays.asList("all", "any", "not");
                List<NameSuggestionFix<RsPath>> fixes = NameSuggestionFix.createApplicable(path, itemName, candidates, 1, name -> {
                    RsPath newPath = new RsPsiFactory(path.getProject()).tryCreatePath(name);
                    if (newPath == null) throw new IllegalStateException("Cannot create path out of " + name);
                    return newPath;
                });
                RsDiagnostic.addToHolder(new RsDiagnostic.UnknownCfgPredicate(path, itemName, fixes), holder, false);
                break;
            }
        }
    }

    private static void checkDeriveAttr(@NotNull RsMetaItem element, @NotNull RsAnnotationHolder holder) {
        RsMetaItemArgs metaItemArgs = element.getMetaItemArgs();
        if (metaItemArgs == null) return;
        List<RsLitExpr> args = metaItemArgs.getLitExprList();
        for (RsLitExpr arg : args) {
            List<SubstituteTextFix> fixes = new ArrayList<>();
            String stringValue = RsLitExprUtil.getStringValue(arg);
            if (stringValue != null) {
                fixes.add(SubstituteTextFix.replace(RsBundle.message("intention.name.remove.quotes"), arg.getContainingFile(), arg.getTextRange(), stringValue));
            }
            RsDiagnostic.addToHolder(new RsDiagnostic.LiteralValueInsideDeriveError(arg, fixes), holder);
        }
    }

    private static void checkReprAttr(@NotNull RsMetaItem element, @NotNull RsAnnotationHolder holder, @NotNull ProcessingContext context) {
        RsMetaItemArgs metaItemArgs = element.getMetaItemArgs();
        if (metaItemArgs == null) return;
        List<RsMetaItem> args = metaItemArgs.getMetaItemList();
        RsDocAndAttributeOwner owner = RsMetaItemUtil.getOwner(element);
        if (owner == null) return;
        for (RsMetaItem arg : args) {
            checkReprArg(arg, owner, holder);
        }

        if (!(owner instanceof RsEnumItem)) return;
        RsEnumItem enumItem = (RsEnumItem) owner;
        RsEnumBody enumBody = enumItem.getEnumBody();
        if (enumBody != null) {
            List<RsEnumVariant> variants = enumBody.getEnumVariantList();
            if (variants != null && variants.isEmpty()) {
                RsDiagnostic.addToHolder(new RsDiagnostic.ReprForEmptyEnumError(context.get(RsPsiPattern.META_ITEM_ATTR)), holder);
            }
        }
    }

    private static void checkReprArg(@NotNull RsMetaItem reprArg, @NotNull RsDocAndAttributeOwner owner, @NotNull RsAnnotationHolder holder) {
        String reprName = reprArg.getName();
        if (reprName == null) return;

        if ("align".equals(reprName)) {
            checkReprAlign(reprArg, holder);
        }

        checkReprArgIsCorrectlyApplied(reprName, owner, reprArg, holder);
    }

    private static void checkReprArgIsCorrectlyApplied(@NotNull String reprName, @NotNull RsDocAndAttributeOwner owner, @NotNull RsMetaItem reprArg, @NotNull RsAnnotationHolder holder) {
        String errorText;
        switch (reprName) {
            case "C":
            case "transparent":
            case "align":
                if (owner instanceof RsStructItem || owner instanceof RsEnumItem) return;
                errorText = RsBundle.message("inspection.message.attribute.should.be.applied.to.struct.enum.or.union", reprName);
                break;
            case "packed":
            case "simd":
                if (owner instanceof RsStructItem) return;
                errorText = RsBundle.message("inspection.message.attribute.should.be.applied.to.struct.or.union", reprName);
                break;
            default:
                if (TyInteger.NAMES.contains(reprName)) {
                    if (owner instanceof RsEnumItem) return;
                    errorText = RsBundle.message("inspection.message.attribute.should.be.applied.to.enum", reprName);
                } else {
                    RsDiagnostic.addToHolder(new RsDiagnostic.UnrecognizedReprAttribute(reprArg, reprName), holder);
                    return;
                }
                break;
        }
        RsDiagnostic.addToHolder(new RsDiagnostic.ReprAttrUnsupportedItem(reprArg, errorText), holder);
    }

    private static void checkReprAlign(@NotNull RsMetaItem align, @NotNull RsAnnotationHolder holder) {
        RsMetaItemArgs metaItemArgs = align.getMetaItemArgs();
        List<RsLitExpr> args = metaItemArgs != null ? metaItemArgs.getLitExprList() : null;
        PsiElement eq = align.getEq();

        if (args == null && eq == null) {
            List<AddAttrParenthesesFix> fixes = Collections.singletonList(new AddAttrParenthesesFix(align, "align"));
            RsDiagnostic.addToHolder(new RsDiagnostic.InvalidReprAlign(
                align,
                RsBundle.message("inspection.message.align.needs.argument"),
                fixes
            ), holder);
        } else if (args == null) {
            RsDiagnostic.addToHolder(new RsDiagnostic.IncorrectlyDeclaredAlignRepresentationHint(
                align,
                RsBundle.message("inspection.message.incorrect.repr.align.attribute.format")
            ), holder);
        } else {
            checkReprAlignArgs(args, align, holder);
        }
    }

    private static void checkReprAlignArgs(@NotNull List<RsLitExpr> args, @NotNull RsMetaItem align, @NotNull RsAnnotationHolder holder) {
        if (args.size() != 1) {
            RsDiagnostic.addToHolder(new RsDiagnostic.IncorrectlyDeclaredAlignRepresentationHint(
                align,
                RsBundle.message("inspection.message.align.takes.exactly.one.argument.in.parentheses")
            ), holder);
            return;
        }

        RsLitExpr arg = args.get(0);
        RsLiteralKind kind = RsLiteralKindUtil.getKind(arg);
        if (kind == null || !(kind instanceof RsLiteralKind.IntegerLiteral) || ((RsLiteralKind.IntegerLiteral) kind).getSuffix() != null) {
            List<ConvertToUnsuffixedIntegerFix> fixes = new ArrayList<>();
            ConvertToUnsuffixedIntegerFix fix = ConvertToUnsuffixedIntegerFix.createIfCompatible(arg, RsBundle.message("intention.name.change.to1", "align(%s)"));
            if (fix != null) fixes.add(fix);
            RsDiagnostic.addToHolder(new RsDiagnostic.InvalidReprAlign(
                align,
                RsBundle.message("inspection.message.align.argument.must.be.unsuffixed.integer"),
                fixes
            ), holder);
            return;
        }

        Long value = RsLitExprUtil.getIntegerValue(arg);
        if (value == null) return;
        if (!StdextUtil.isPowerOfTwo(value)) {
            RsDiagnostic.addToHolder(new RsDiagnostic.InvalidReprAlign(
                align,
                RsBundle.message("inspection.message.align.argument.must.be.power.two")
            ), holder);
            return;
        }

        if (value > (1 << 29)) {
            RsDiagnostic.addToHolder(new RsDiagnostic.InvalidReprAlign(
                align,
                RsBundle.message("inspection.message.align.argument.must.not.be.larger.than")
            ), holder);
        }
    }
}
