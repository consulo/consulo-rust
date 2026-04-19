/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.docs;

import com.intellij.codeInsight.documentation.DocumentationManagerUtil;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.ide.presentation.PresentationInfo;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import org.rust.lang.core.types.ty.TyPrimitive;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.doc.RsDocRenderMode;
import org.rust.lang.doc.RsDocPipeline;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.openapiext.Testmark;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.CollectionsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;
import org.rust.lang.core.psi.ext.RsTypeReferenceUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsRefLikeTypeUtil;
import org.rust.lang.core.psi.ext.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.doc.psi.RsQualifiedName;
import org.rust.lang.core.psi.ext.RsFileUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import com.intellij.psi.PsiFile;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.ide.presentation.PresentationUtil;

@SuppressWarnings("UnstableApiUsage")
public class RsDocumentationProvider extends AbstractDocumentationProvider {

    public static final String STD_DOC_HOST = "https://doc.rust-lang.org";
    public static final String EXTERNAL_DOCUMENTATION_URL_SETTING_KEY = "org.rust.external.doc.url";

    @Nullable
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        StringBuilder buffer = new StringBuilder();
        if (element instanceof RsTypeParameter) {
            definition(buffer, b -> generateDoc((RsTypeParameter) element, b));
        } else if (element instanceof RsConstParameter) {
            definition(buffer, b -> generateDoc((RsConstParameter) element, b));
        } else if (element instanceof RsDocAndAttributeOwner) {
            generateDoc((RsDocAndAttributeOwner) element, buffer);
        } else if (element instanceof RsPatBinding) {
            definition(buffer, b -> generateDoc((RsPatBinding) element, b));
        } else if (element instanceof RsPath) {
            generateDoc((RsPath) element, buffer);
        } else {
            generateCustomDoc(element, buffer);
        }
        return buffer.isEmpty() ? null : buffer.toString();
    }

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, @Nullable PsiElement originalElement) {
        StringBuilder sb = new StringBuilder();
        if (element instanceof RsPatBinding) {
            generateDoc((RsPatBinding) element, sb);
        } else if (element instanceof RsTypeParameter) {
            generateDoc((RsTypeParameter) element, sb);
        } else if (element instanceof RsConstParameter) {
            generateDoc((RsConstParameter) element, sb);
        } else if (element instanceof RsConstant) {
            PresentationInfo info = PresentationInfo.getPresentationInfo((RsConstant) element);
            if (info != null) sb.append(info.getQuickDocumentationText());
        } else if (element instanceof RsMod) {
            PresentationInfo info = PresentationInfo.getPresentationInfo((RsMod) element);
            if (info != null) sb.append(info.getQuickDocumentationText());
        } else if (element instanceof RsItemElement || element instanceof RsMacro) {
            header((RsDocAndAttributeOwner) element, sb);
            signature((RsDocAndAttributeOwner) element, sb);
        } else if (element instanceof RsNamedElement) {
            PresentationInfo info = PresentationInfo.getPresentationInfo((RsNamedElement) element);
            if (info != null) sb.append(info.getQuickDocumentationText());
        } else {
            return null;
        }
        return sb.toString();
    }

    @Override
    public void collectDocComments(@NotNull PsiFile file, @NotNull Consumer<? super PsiDocCommentBase> sink) {
        if (!(file instanceof RsFile)) return;
        for (PsiElement element : SyntaxTraverser.psiTraverser(file)) {
            if (element instanceof RsDocComment) {
                sink.accept((RsDocComment) element);
            }
        }
    }

    private void generateDoc(RsDocAndAttributeOwner element, StringBuilder buffer) {
        definition(buffer, b -> {
            header(element, b);
            signature(element, b);
        });
        String text = RsDocPipeline.documentationAsHtml(element);
        if ((text == null || text.isEmpty()) && element instanceof RsAbstractable &&
            ((RsAbstractable) element).getOwner().isTraitImpl()) {
            RsAbstractable superItem = ((RsAbstractable) element).getSuperItem();
            if (superItem instanceof RsDocAndAttributeOwner) {
                text = RsDocPipeline.documentationAsHtml((RsDocAndAttributeOwner) superItem);
            }
        }
        if (text == null || text.isEmpty()) return;
        buffer.append("\n");
        final String finalText = text;
        content(buffer, b -> b.append(finalText));
    }

    private void generateDoc(RsPatBinding element, StringBuilder buffer) {
        PresentationInfo presentationInfo = PresentationInfo.getPresentationInfo(element);
        if (presentationInfo == null) return;
        String type = OpenApiUtil.getEscaped(org.rust.ide.presentation.TypeRendering.render(RsTypesUtil.getType(element)));
        buffer.append(presentationInfo.getType());
        buffer.append(" ");
        buffer.append("<b>").append(presentationInfo.getName()).append("</b>");
        buffer.append(": ");
        buffer.append(type);
    }

    private void generateDoc(RsTypeParameter element, StringBuilder buffer) {
        String name = element.getName();
        if (name == null) return;
        buffer.append("type parameter ");
        buffer.append("<b>").append(name).append("</b>");
        var typeBounds = RsTypeParameterUtil.getBounds(element);
        if (!typeBounds.isEmpty()) {
            buffer.append(": ");
            for (int i = 0; i < typeBounds.size(); i++) {
                if (i > 0) buffer.append(" + ");
                generateDocumentation(typeBounds.get(i), buffer, "", "");
            }
        }
        if (element.getTypeReference() != null) {
            generateDocumentation(element.getTypeReference(), buffer, " = ", "");
        }
    }

    private void generateDoc(RsConstParameter element, StringBuilder buffer) {
        String name = element.getName();
        if (name == null) return;
        buffer.append("const parameter ");
        buffer.append("<b>").append(name).append("</b>");
        if (element.getTypeReference() != null) {
            generateDocumentation(element.getTypeReference(), buffer, ": ", "");
        }
        if (element.getExpr() != null) {
            generateDocumentation(element.getExpr(), buffer, " = ", "");
        }
    }

    private void generateDoc(RsPath element, StringBuilder buffer) {
        TyPrimitive primitive = TyPrimitive.fromPath(element);
        if (primitive == null) return;
        RsFile primitiveDocs = findFileInStdCrate(element.getProject(), "primitive_docs.rs");
        if (primitiveDocs == null) return;

        RsModItem mod = null;
        for (RsModItem m : RsPsiJavaUtil.childrenOfType(primitiveDocs, RsModItem.class)) {
            if (m.getQueryAttributes().hasAttributeWithValue("rustc_doc_primitive", primitive.getName()) ||
                m.getQueryAttributes().hasAttributeWithKeyValue("doc", "primitive", primitive.getName())) {
                mod = m;
                break;
            }
        }
        if (mod == null) return;

        RsModItem finalMod = mod;
        definition(buffer, b -> {
            b.append(AutoInjectedCrates.STD);
            b.append("\n");
            b.append("primitive type ");
            b.append("<b>").append(primitive.getName()).append("</b>");
        });
        content(buffer, b -> b.append(RsDocPipeline.documentationAsHtml(finalMod, element)));
    }

    private void generateCustomDoc(PsiElement element, StringBuilder buffer) {
        if (isKeywordLike(element)) {
            RsFile keywordDocs = findFileInStdCrate(element.getProject(), "keyword_docs.rs");
            if (keywordDocs == null) return;
            String keywordName = element.getText();
            RsModItem mod = null;
            for (RsModItem m : RsPsiJavaUtil.childrenOfType(keywordDocs, RsModItem.class)) {
                if (m.getQueryAttributes().hasAttributeWithKeyValue("doc", "keyword", keywordName)) {
                    mod = m;
                    break;
                }
            }
            if (mod == null) return;

            RsModItem finalMod = mod;
            definition(buffer, b -> {
                b.append(AutoInjectedCrates.STD);
                b.append("\n");
                b.append("keyword ");
                b.append("<b>").append(keywordName).append("</b>");
            });
            content(buffer, b -> b.append(RsDocPipeline.documentationAsHtml(finalMod, element)));
        }
    }

    @Nullable
    @Override
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        RsElement element = null;
        if (context instanceof RsElement) {
            element = (RsElement) context;
        } else if (context.getParent() instanceof RsElement) {
            element = (RsElement) context.getParent();
        }
        if (element == null) return null;

        RsQualifiedName qualifiedName = RsQualifiedName.from(link);
        if (qualifiedName == null) {
            RsPath path = new RsCodeFragmentFactory(context.getProject()).createPath(link, element);
            if (path != null && path.getReference() != null) {
                return RsPathReferenceImpl.resolveTypeAliasToImpl(path.getReference());
            }
            return null;
        } else {
            return qualifiedName.findPsiElement(psiManager, element);
        }
    }

    @Override
    public List<String> getUrlFor(PsiElement element, @Nullable PsiElement originalElement) {
        RsQualifiedName qualifiedName = null;
        PackageOrigin origin = null;

        if (element instanceof RsDocAndAttributeOwner && element instanceof RsQualifiedNamedElement &&
            hasExternalDocumentation((RsDocAndAttributeOwner) element)) {
            origin = ((RsElement) element).getContainingCrate().getOrigin();
            qualifiedName = RsQualifiedName.from((RsQualifiedNamedElement) element);
        } else {
            qualifiedName = RsQualifiedName.from(element);
            if (qualifiedName == null) return Collections.emptyList();
            origin = PackageOrigin.STDLIB;
        }

        String baseUrl = getExternalDocumentationBaseUrl();
        String pagePrefix;
        if (origin == PackageOrigin.STDLIB) {
            pagePrefix = STD_DOC_HOST;
        } else if (origin == PackageOrigin.DEPENDENCY || origin == PackageOrigin.STDLIB_DEPENDENCY) {
            if (!(element instanceof RsElement)) return Collections.emptyList();
            var pkg = RsElementUtil.getContainingCargoPackage(element);
            if (pkg == null) return Collections.emptyList();
            if (pkg.getSource() == null) {
                Testmarks.PkgWithoutSource.hit();
                return Collections.emptyList();
            }
            pagePrefix = baseUrl + pkg.getName() + "/" + pkg.getVersion();
        } else {
            Testmarks.NonDependency.hit();
            return Collections.emptyList();
        }

        if (qualifiedName == null) return Collections.emptyList();
        String pagePath = qualifiedName.toUrlPath();
        if (pagePath == null) return Collections.emptyList();
        return List.of(pagePrefix + "/" + pagePath);
    }

    @Nullable
    @Override
    public PsiElement getCustomDocumentationElement(
        @NotNull Editor editor,
        @NotNull PsiFile file,
        @Nullable PsiElement contextElement,
        int targetOffset
    ) {
        if (contextElement != null && isKeywordLike(contextElement) && !(contextElement.getParent() instanceof RsPath)) {
            return contextElement;
        }
        return null;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Nullable
    @Override
    public String generateRenderedDoc(@NotNull PsiDocCommentBase comment) {
        if (comment instanceof RsDocComment) {
            return RsDocPipeline.documentationAsHtml((RsDocComment) comment, RsDocRenderMode.INLINE_DOC_COMMENT);
        }
        return null;
    }

    private boolean hasExternalDocumentation(RsDocAndAttributeOwner element) {
        if (element.getQueryAttributes().isDocHidden()) {
            Testmarks.DocHidden.hit();
            return false;
        }

        if (element instanceof RsVisible) {
            if (element instanceof RsAbstractable) {
                var owner = ((RsAbstractable) element).getOwner();
                if (owner instanceof RsAbstractableOwner.Trait) {
                    return hasExternalDocumentation(((RsAbstractableOwner.Trait) owner).getTrait());
                }
                if (owner instanceof RsAbstractableOwner.Impl) {
                    var implOwner = (RsAbstractableOwner.Impl) owner;
                    if (implOwner.isInherent()) {
                        return ((RsVisible) element).getVisibility() == RsVisibility.Public.INSTANCE;
                    } else {
                        var traitRef = implOwner.getImpl().getTraitRef();
                        if (traitRef != null) {
                            var trait = RsTraitRefUtil.resolveToTrait(traitRef);
                            return trait != null && hasExternalDocumentation(trait);
                        }
                        return false;
                    }
                }
            } else {
                if (((RsVisible) element).getVisibility() != RsVisibility.Public.INSTANCE) return false;
            }
        }

        if (element instanceof RsMacro) {
            return RsMacroUtil.getHasMacroExport((RsMacro) element);
        }

        for (RsMod mod : element.getContainingMod().getSuperMods()) {
            if (!mod.isPublic()) return false;
        }
        return true;
    }

    @Nullable
    private static RsFile findFileInStdCrate(Project project, String name) {
        var crates = CrateGraphService.crateGraph(project).getTopSortedCrates();
        for (var crate : crates) {
            if (crate.getOrigin() == PackageOrigin.STDLIB && AutoInjectedCrates.STD.equals(crate.getNormName())) {
                var rootMod = crate.getRootMod();
                if (rootMod != null && rootMod.getParent() != null) {
                    var file = rootMod.getParent().findFile(name);
                    if (file != null) return RsFileUtil.getRustFile(file);
                }
                return null;
            }
        }
        return null;
    }

    public static void signature(RsDocAndAttributeOwner element, StringBuilder builder) {
        List<String> rawLines;
        if (element instanceof RsNamedFieldDecl) {
            PresentationInfo info = PresentationInfo.getPresentationInfo((RsNamedFieldDecl) element);
            rawLines = info != null ? List.of(info.getSignatureText()) : Collections.emptyList();
        } else if (element instanceof RsFunction) {
            rawLines = buildFunctionSignature((RsFunction) element);
        } else if (element instanceof RsConstant) {
            rawLines = buildConstantSignature((RsConstant) element);
        } else if (element instanceof RsStructOrEnumItemElement || element instanceof RsTraitItem || element instanceof RsTypeAlias) {
            rawLines = buildGenericDeclSignature(element);
        } else if (element instanceof RsMacro) {
            rawLines = List.of("macro <b>" + ((RsMacro) element).getName() + "</b>");
        } else if (element instanceof RsMacro2) {
            rawLines = buildMacro2Signature((RsMacro2) element);
        } else if (element instanceof RsImplItem) {
            rawLines = getImplDeclarationText((RsImplItem) element);
        } else {
            rawLines = Collections.emptyList();
        }
        joinTo(rawLines, builder, "<br>");
    }

    private static List<String> buildFunctionSignature(RsFunction fn) {
        StringBuilder buffer = new StringBuilder();
        joinTo(getDeclarationModifiers(fn), buffer, " ");
        if (!getDeclarationModifiers(fn).isEmpty()) buffer.append(" ");
        buffer.append("<b>").append(fn.getName()).append("</b>");
        if (fn.getTypeParameterList() != null) {
            generateDocumentation(fn.getTypeParameterList(), buffer, "", "");
        }
        if (fn.getValueParameterList() != null) {
            generateDocumentation(fn.getValueParameterList(), buffer, "", "");
        }
        if (fn.getRetType() != null) {
            generateDocumentation(fn.getRetType(), buffer, "", "");
        }
        List<String> result = new ArrayList<>();
        result.add(buffer.toString());
        result.addAll(getWherePredsDocText(RsFunctionUtil.getWherePreds(fn)));
        return result;
    }

    private static List<String> buildConstantSignature(RsConstant c) {
        StringBuilder buffer = new StringBuilder();
        joinTo(getDeclarationModifiers(c), buffer, " ");
        if (!getDeclarationModifiers(c).isEmpty()) buffer.append(" ");
        buffer.append("<b>").append(c.getName()).append("</b>");
        if (c.getTypeReference() != null) {
            generateDocumentation(c.getTypeReference(), buffer, ": ", "");
        }
        if (c.getExpr() != null) {
            generateDocumentation(c.getExpr(), buffer, " = ", "");
        }
        return List.of(buffer.toString());
    }

    private static List<String> buildGenericDeclSignature(RsDocAndAttributeOwner element) {
        String name = ((RsNamedElement) element).getName();
        if (name == null) return Collections.emptyList();
        StringBuilder buffer = new StringBuilder();
        joinTo(getDeclarationModifiers((RsItemElement) element), buffer, " ");
        if (!getDeclarationModifiers((RsItemElement) element).isEmpty()) buffer.append(" ");
        buffer.append("<b>").append(name).append("</b>");
        if (element instanceof RsGenericDeclaration) {
            var typeParamList = ((RsGenericDeclaration) element).getTypeParameterList();
            if (typeParamList != null) {
                generateDocumentation(typeParamList, buffer, "", "");
            }
        }
        if (element instanceof RsTypeAlias) {
            var typeRef = ((RsTypeAlias) element).getTypeReference();
            if (typeRef != null) {
                generateDocumentation(typeRef, buffer, " = ", "");
            }
        }
        List<String> result = new ArrayList<>();
        result.add(buffer.toString());
        if (element instanceof RsGenericDeclaration) {
            result.addAll(getWherePredsDocText(RsGenericDeclarationUtil.getWherePreds((RsGenericDeclaration) element)));
        }
        return result;
    }

    private static List<String> buildMacro2Signature(RsMacro2 macro) {
        StringBuilder buffer = new StringBuilder();
        joinTo(getDeclarationModifiers(macro), buffer, " ");
        if (!getDeclarationModifiers(macro).isEmpty()) buffer.append(" ");
        buffer.append("<b>").append(macro.getName()).append("</b>");
        return List.of(buffer.toString());
    }

    private static List<String> getImplDeclarationText(RsImplItem impl) {
        var typeRef = impl.getTypeReference();
        if (typeRef == null) return Collections.emptyList();
        StringBuilder buffer = new StringBuilder("impl");
        if (impl.getTypeParameterList() != null) {
            generateDocumentation(impl.getTypeParameterList(), buffer, "", "");
        }
        buffer.append(" ");
        var traitRef = impl.getTraitRef();
        if (traitRef != null) {
            generateDocumentation(traitRef, buffer, "", "");
            buffer.append(" for ");
        }
        generateDocumentation(typeRef, buffer, "", "");
        List<String> result = new ArrayList<>();
        result.add(buffer.toString());
        result.addAll(getWherePredsDocText(RsGenericDeclarationUtil.getWherePreds(impl)));
        return result;
    }

    private static void header(RsDocAndAttributeOwner element, StringBuilder buffer) {
        List<String> rawLines;
        if (element instanceof RsNamedFieldDecl) {
            var parent = ((RsNamedFieldDecl) element).getParent();
            if (parent != null) parent = parent.getParent();
            if (parent instanceof RsDocAndAttributeOwner) {
                String qn = PresentationUtil.getPresentableQualifiedName((RsDocAndAttributeOwner) parent);
                rawLines = qn != null ? List.of(qn) : Collections.emptyList();
            } else {
                rawLines = Collections.emptyList();
            }
        } else if (element instanceof RsStructOrEnumItemElement || element instanceof RsTraitItem || element instanceof RsMacroDefinitionBase) {
            String modName = getPresentableQualifiedModName(element);
            rawLines = modName != null ? List.of(modName) : Collections.emptyList();
        } else if (element instanceof RsAbstractable) {
            var owner = ((RsAbstractable) element).getOwner();
            if (owner == RsAbstractableOwner.Foreign || owner == RsAbstractableOwner.Free) {
                String modName = getPresentableQualifiedModName(element);
                rawLines = modName != null ? List.of(modName) : Collections.emptyList();
            } else if (owner instanceof RsAbstractableOwner.Impl) {
                List<String> result = new ArrayList<>();
                String modName = getPresentableQualifiedModName(element);
                if (modName != null) result.add(modName);
                result.addAll(getImplDeclarationText(((RsAbstractableOwner.Impl) owner).getImpl()));
                rawLines = result;
            } else if (owner instanceof RsAbstractableOwner.Trait) {
                rawLines = getTraitDeclarationText(((RsAbstractableOwner.Trait) owner).getTrait());
            } else {
                rawLines = Collections.emptyList();
            }
        } else {
            String qn = PresentationUtil.getPresentableQualifiedName(element);
            rawLines = qn != null ? List.of(qn) : Collections.emptyList();
        }
        joinTo(rawLines, buffer, "<br>");
        if (!rawLines.isEmpty()) {
            buffer.append("\n");
        }
    }

    private static List<String> getTraitDeclarationText(RsTraitItem trait) {
        String name = PresentationUtil.getPresentableQualifiedName(trait);
        if (name == null) return Collections.emptyList();
        StringBuilder buffer = new StringBuilder(name);
        if (trait.getTypeParameterList() != null) {
            generateDocumentation(trait.getTypeParameterList(), buffer, "", "");
        }
        List<String> result = new ArrayList<>();
        result.add(buffer.toString());
        result.addAll(getWherePredsDocText(RsGenericDeclarationUtil.getWherePreds(trait)));
        return result;
    }

    private static List<String> getDeclarationModifiers(RsItemElement element) {
        List<String> modifiers = new ArrayList<>();
        if (element.getVis() != null) {
            modifiers.add(element.getVis().getText());
        }
        if (element instanceof RsFunction) {
            RsFunction fn = (RsFunction) element;
            if (RsFunctionUtil.isAsync(fn)) modifiers.add("async");
            if (RsFunctionUtil.isConst(fn)) modifiers.add("const");
            if (fn.isUnsafe()) modifiers.add("unsafe");
            if (RsFunctionUtil.isActuallyExtern(fn)) {
                modifiers.add("extern");
                String abi = RsFunctionUtil.getLiteralAbiName(fn);
                if (abi != null) modifiers.add("\"" + abi + "\"");
            }
            modifiers.add("fn");
        } else if (element instanceof RsStructItem) {
            modifiers.add("struct");
        } else if (element instanceof RsEnumItem) {
            modifiers.add("enum");
        } else if (element instanceof RsConstant) {
            modifiers.add(RsConstantUtil.isConst((RsConstant) element) ? "const" : "static");
        } else if (element instanceof RsTypeAlias) {
            modifiers.add("type");
        } else if (element instanceof RsTraitItem) {
            if (((RsTraitItem) element).isUnsafe()) modifiers.add("unsafe");
            modifiers.add("trait");
        } else if (element instanceof RsMacro2) {
            modifiers.add("macro");
        }
        return modifiers;
    }

    private static List<String> getWherePredsDocText(List<RsWherePred> preds) {
        if (preds == null || preds.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        result.add("where");
        for (RsWherePred pred : preds) {
            StringBuilder buffer = new StringBuilder();
            var lifetime = pred.getLifetime();
            var typeReference = pred.getTypeReference();
            if (lifetime != null) {
                generateDocumentation(lifetime, buffer, "", "");
                if (pred.getLifetimeParamBounds() != null) {
                    generateDocumentation(pred.getLifetimeParamBounds(), buffer, "", "");
                }
            } else if (typeReference != null) {
                generateDocumentation(typeReference, buffer, "", "");
                if (pred.getTypeParamBounds() != null) {
                    generateDocumentation(pred.getTypeParamBounds(), buffer, "", "");
                }
            } else {
                continue;
            }
            result.add("&nbsp;&nbsp;&nbsp;&nbsp;" + buffer + ",");
        }
        return result;
    }

    @Nullable
    private static String getPresentableQualifiedModName(RsDocAndAttributeOwner element) {
        String qn = PresentationUtil.getPresentableQualifiedName(element);
        if (qn == null) return null;
        String name = ((RsNamedElement) element).getName();
        if (name != null && qn.endsWith("::" + name)) {
            return qn.substring(0, qn.length() - name.length() - 2);
        }
        return qn;
    }

    private static void generateDocumentation(PsiElement element, StringBuilder buffer, String prefix, String suffix) {
        buffer.append(prefix);
        if (element instanceof RsPath) {
            generatePathDocumentation((RsPath) element, buffer);
        } else if (element instanceof RsAssocTypeBinding) {
            var atb = (RsAssocTypeBinding) element;
            generateDocumentation(atb.getPath(), buffer, "", "");
            if (atb.getTypeReference() != null) {
                generateDocumentation(atb.getTypeReference(), buffer, " = ", "");
            }
        } else if (element instanceof RsTraitRef) {
            generateDocumentation(((RsTraitRef) element).getPath(), buffer, "", "");
        } else if (element instanceof RsLifetimeParamBounds) {
            var lifetimes = ((RsLifetimeParamBounds) element).getLifetimeList();
            buffer.append(": ");
            for (int i = 0; i < lifetimes.size(); i++) {
                if (i > 0) buffer.append(" + ");
                generateDocumentation(lifetimes.get(i), buffer, "", "");
            }
        } else if (element instanceof RsTypeParamBounds) {
            var polybounds = ((RsTypeParamBounds) element).getPolyboundList();
            if (!polybounds.isEmpty()) {
                buffer.append(": ");
                for (int i = 0; i < polybounds.size(); i++) {
                    if (i > 0) buffer.append(" + ");
                    generateDocumentation(polybounds.get(i), buffer, "", "");
                }
            }
        } else if (element instanceof RsPolybound) {
            var polybound = (RsPolybound) element;
            if (polybound.getQ() != null) {
                buffer.append("?");
            }
            var bound = polybound.getBound();
            if (bound.getLifetime() != null) {
                generateDocumentation(bound.getLifetime(), buffer, "", "");
            } else if (bound.getTraitRef() != null) {
                generateDocumentation(bound.getTraitRef(), buffer, "", "");
            }
        } else if (element instanceof RsTypeArgumentList) {
            var tal = (RsTypeArgumentList) element;
            List<PsiElement> children = new ArrayList<>();
            children.addAll(tal.getLifetimeList());
            children.addAll(tal.getTypeReferenceList());
            children.addAll(tal.getExprList());
            children.addAll(tal.getAssocTypeBindingList());
            children.sort((a, b) -> Integer.compare(a.getTextOffset(), b.getTextOffset()));
            buffer.append("&lt;");
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) buffer.append(", ");
                generateDocumentation(children.get(i), buffer, "", "");
            }
            buffer.append("&gt;");
        } else if (element instanceof RsTypeParameterList) {
            var tpl = (RsTypeParameterList) element;
            var params = RsTypeParameterListUtil.getGenericParameters(tpl);
            buffer.append("&lt;");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) buffer.append(", ");
                generateDocumentation(params.get(i), buffer, "", "");
            }
            buffer.append("&gt;");
        } else if (element instanceof RsValueParameterList) {
            var vpl = (RsValueParameterList) element;
            List<PsiElement> params = new ArrayList<>();
            if (vpl.getSelfParameter() != null) params.add(vpl.getSelfParameter());
            params.addAll(vpl.getValueParameterList());
            if (vpl.getVariadic() != null) params.add(vpl.getVariadic());
            buffer.append("(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) buffer.append(", ");
                generateDocumentation(params.get(i), buffer, "", "");
            }
            buffer.append(")");
        } else if (element instanceof RsLifetimeParameter) {
            var lp = (RsLifetimeParameter) element;
            buffer.append(OpenApiUtil.getEscaped(lp.getQuoteIdentifier().getText()));
            if (lp.getLifetimeParamBounds() != null) {
                generateDocumentation(lp.getLifetimeParamBounds(), buffer, "", "");
            }
        } else if (element instanceof RsTypeParameter) {
            var tp = (RsTypeParameter) element;
            buffer.append(tp.getName());
            if (tp.getTypeParamBounds() != null) {
                generateDocumentation(tp.getTypeParamBounds(), buffer, "", "");
            }
            if (tp.getTypeReference() != null) {
                generateDocumentation(tp.getTypeReference(), buffer, " = ", "");
            }
        } else if (element instanceof RsConstParameter) {
            var cp = (RsConstParameter) element;
            buffer.append("const ");
            buffer.append(cp.getName());
            if (cp.getTypeReference() != null) {
                generateDocumentation(cp.getTypeReference(), buffer, ": ", "");
            }
        } else if (element instanceof RsValueParameter) {
            var vp = (RsValueParameter) element;
            if (vp.getPat() != null) {
                generateDocumentation(vp.getPat(), buffer, "", ": ");
            }
            if (vp.getTypeReference() != null) {
                generateDocumentation(vp.getTypeReference(), buffer, "", "");
            }
        } else if (element instanceof RsVariadic) {
            var v = (RsVariadic) element;
            if (v.getPat() != null) {
                generateDocumentation(v.getPat(), buffer, "", ": ");
            }
            buffer.append(v.getDotdotdot().getText());
        } else if (element instanceof RsTypeReference) {
            generateTypeReferenceDocumentation((RsTypeReference) element, buffer);
        } else if (element instanceof RsRetType) {
            if (((RsRetType) element).getTypeReference() != null) {
                generateDocumentation(((RsRetType) element).getTypeReference(), buffer, " -&gt; ", "");
            }
        } else if (element instanceof RsTypeQual) {
            var tq = (RsTypeQual) element;
            buffer.append("&lt;");
            generateDocumentation(tq.getTypeReference(), buffer, "", "");
            if (tq.getTraitRef() != null) {
                generateDocumentation(tq.getTraitRef(), buffer, " as ", "");
            }
            buffer.append("&gt;::");
        } else {
            buffer.append(OpenApiUtil.getEscaped(element.getText()));
        }
        buffer.append(suffix);
    }

    private static void generatePathDocumentation(RsPath element, StringBuilder buffer) {
        var path = element.getPath();
        if (path != null) {
            buffer.append(OpenApiUtil.getEscaped(path.getText()));
            buffer.append("::");
        }
        if (element.getTypeQual() != null) {
            generateDocumentation(element.getTypeQual(), buffer, "", "");
        }

        String name = element.getReferenceName() != null ? element.getReferenceName() : "";
        if (isLinkNeeded(element)) {
            createLink(buffer, getLink(element), name);
        } else {
            buffer.append(name);
        }

        var typeArgumentList = element.getTypeArgumentList();
        var valueParameterList = element.getValueParameterList();
        if (typeArgumentList != null) {
            generateDocumentation(typeArgumentList, buffer, "", "");
        } else if (valueParameterList != null) {
            generateDocumentation(valueParameterList, buffer, "", "");
            if (element.getRetType() != null) {
                generateDocumentation(element.getRetType(), buffer, "", "");
            }
        }
    }

    private static void generateTypeReferenceDocumentation(RsTypeReference element, StringBuilder buffer) {
        var typeElement = RsTypeReferenceUtil.skipParens(element);
        if (typeElement instanceof RsUnitType) {
            buffer.append("()");
        } else if (typeElement instanceof RsNeverType) {
            buffer.append("!");
        } else if (typeElement instanceof RsInferType) {
            buffer.append("_");
        } else if (typeElement instanceof RsPathType) {
            var path = ((RsPathType) typeElement).getPath();
            if (RsPathUtil.getHasCself(path)) {
                buffer.append("Self");
            } else {
                generateDocumentation(path, buffer, "", "");
            }
        } else if (typeElement instanceof RsTupleType) {
            var refs = ((RsTupleType) typeElement).getTypeReferenceList();
            buffer.append("(");
            for (int i = 0; i < refs.size(); i++) {
                if (i > 0) buffer.append(", ");
                generateDocumentation(refs.get(i), buffer, "", "");
            }
            buffer.append(")");
        } else if (typeElement instanceof RsArrayType) {
            var arr = (RsArrayType) typeElement;
            buffer.append("[");
            if (arr.getTypeReference() != null) {
                generateDocumentation(arr.getTypeReference(), buffer, "", "");
            }
            if (!RsArrayTypeUtil.isSlice(arr)) {
                buffer.append("; ");
                var arraySize = RsArrayTypeUtil.getArraySize(arr);
                if (arraySize != null) {
                    buffer.append(arraySize);
                } else if (arr.getExpr() != null) {
                    buffer.append(OpenApiUtil.getEscaped(arr.getExpr().getText()));
                } else {
                    buffer.append("<unknown>");
                }
            }
            buffer.append("]");
        } else if (typeElement instanceof RsRefLikeType) {
            var ref = (RsRefLikeType) typeElement;
            if (RsRefLikeTypeUtil.isRef(ref)) {
                buffer.append("&amp;");
                if (ref.getLifetime() != null) {
                    generateDocumentation(ref.getLifetime(), buffer, "", " ");
                }
                if (RsRefLikeTypeUtil.getMutability(ref).isMut()) {
                    buffer.append("mut ");
                }
            } else {
                buffer.append("*");
                buffer.append(RsRefLikeTypeUtil.getMutability(ref).isMut() ? "mut " : "const ");
            }
            if (ref.getTypeReference() != null) {
                generateDocumentation(ref.getTypeReference(), buffer, "", "");
            }
        } else if (typeElement instanceof RsFnPointerType) {
            buffer.append("fn");
            if (((RsFnPointerType) typeElement).getValueParameterList() != null) {
                generateDocumentation(((RsFnPointerType) typeElement).getValueParameterList(), buffer, "", "");
            }
            if (((RsFnPointerType) typeElement).getRetType() != null) {
                generateDocumentation(((RsFnPointerType) typeElement).getRetType(), buffer, "", "");
            }
        } else {
            buffer.append(OpenApiUtil.getEscaped(element.getText()));
        }
    }

    private static boolean isLinkNeeded(RsPath path) {
        if (path.getReference() == null) return false;
        var element = path.getReference().resolve();
        return !(element == null || element instanceof RsTypeParameter);
    }

    private static String getLink(RsPath path) {
        var parentPath = path.getPath();
        String prefix = null;
        if (parentPath != null) {
            prefix = OpenApiUtil.getEscaped(parentPath.getText()) + "::";
        } else if (path.getTypeQual() != null) {
            prefix = OpenApiUtil.getEscaped(path.getTypeQual().getText());
        }
        String refName = path.getReferenceName() != null ? path.getReferenceName() : "";
        return prefix != null ? prefix + refName : refName;
    }

    private static void createLink(StringBuilder buffer, String refText, String text) {
        DocumentationManagerUtil.createHyperlink(buffer, refText, text, true);
    }

    private static void definition(StringBuilder buffer, Consumer<StringBuilder> block) {
        buffer.append(DocumentationMarkup.DEFINITION_START);
        block.accept(buffer);
        buffer.append(DocumentationMarkup.DEFINITION_END);
    }

    private static void content(StringBuilder buffer, Consumer<StringBuilder> block) {
        buffer.append(DocumentationMarkup.CONTENT_START);
        block.accept(buffer);
        buffer.append(DocumentationMarkup.CONTENT_END);
    }

    private static boolean isKeywordLike(PsiElement element) {
        return PsiElementUtil.isKeywordLike(element);
    }

    private static void joinTo(List<String> items, StringBuilder buffer, String separator) {
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) buffer.append(separator);
            buffer.append(items.get(i));
        }
    }

    public static String getExternalDocumentationBaseUrl() {
        String url = AdvancedSettings.getString(EXTERNAL_DOCUMENTATION_URL_SETTING_KEY);
        return url.endsWith("/") ? url : url + "/";
    }

    @TestOnly
    public static void withExternalDocumentationBaseUrl(String url, Runnable action) {
        String originalUrl = getExternalDocumentationBaseUrl();
        try {
            AdvancedSettings.setString(EXTERNAL_DOCUMENTATION_URL_SETTING_KEY, url);
            action.run();
        } finally {
            AdvancedSettings.setString(EXTERNAL_DOCUMENTATION_URL_SETTING_KEY, originalUrl);
        }
    }

    public static final class Testmarks {
        public static final Testmark DocHidden = new Testmark();
        public static final Testmark NotExportedMacro = new Testmark();
        public static final Testmark PkgWithoutSource = new Testmark();
        public static final Testmark NonDependency = new Testmark();
    }
}
