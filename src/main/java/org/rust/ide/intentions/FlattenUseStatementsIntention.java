/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAttr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsPathUtil;

public class FlattenUseStatementsIntention extends RsElementBaseIntentionAction<FlattenUseStatementsIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.flatten.use.statements");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public interface Context {
        List<RsUseSpeck> getUseSpecks();
        PsiElement getRoot();
        PsiElement getFirstOldElement();
        List<PsiElement> createElements(List<String> paths, Project project);
        List<PsiElement> getOldElements();
        int getCursorOffset();
        String getBasePath();
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsUseGroup useGroupOnCursor = RsPsiJavaUtil.ancestorStrict(element, RsUseGroup.class);
        if (useGroupOnCursor == null) return null;
        boolean isNested = RsPsiJavaUtil.ancestorStrict(useGroupOnCursor, RsUseGroup.class) != null;

        RsUseSpeck useSpeckOnCursor;
        if (element instanceof RsUseSpeck) {
            useSpeckOnCursor = (RsUseSpeck) element;
        } else {
            PsiElement sibling = null;
            for (PsiElement s = element.getPrevSibling(); s != null; s = s.getPrevSibling()) {
                if (s instanceof RsUseSpeck) { sibling = s; break; }
            }
            if (sibling == null) {
                for (PsiElement s = element.getNextSibling(); s != null; s = s.getNextSibling()) {
                    if (s instanceof RsUseSpeck) { sibling = s; break; }
                }
            }
            if (sibling != null) {
                useSpeckOnCursor = (RsUseSpeck) sibling;
            } else {
                useSpeckOnCursor = RsPsiJavaUtil.ancestorStrict(element, RsUseSpeck.class);
                if (useSpeckOnCursor == null) return null;
            }
        }

        List<RsUseSpeck> useSpeckList = new ArrayList<>();
        RsUseSpeck parentUseSpeck = RsUseSpeckUtil.getParentUseSpeck(useGroupOnCursor);
        if (parentUseSpeck == null) return null;
        RsPath path = parentUseSpeck.getPath();
        if (path == null) return null;
        String basePath = path.getText();

        // Collect left siblings
        for (PsiElement s = useSpeckOnCursor.getPrevSibling(); s != null; s = s.getPrevSibling()) {
            if (s instanceof RsUseSpeck) useSpeckList.add(0, (RsUseSpeck) s);
        }
        useSpeckList.add(useSpeckOnCursor);
        // Collect right siblings
        for (PsiElement s = useSpeckOnCursor.getNextSibling(); s != null; s = s.getNextSibling()) {
            if (s instanceof RsUseSpeck) useSpeckList.add((RsUseSpeck) s);
        }

        if (useSpeckList.size() == 1) return null;

        if (isNested) {
            return new PathInNestedGroup(useGroupOnCursor, useSpeckList, basePath);
        } else {
            return new PathInGroup(useGroupOnCursor, useSpeckList, basePath);
        }
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        List<String> separatedPaths = makeSeparatedPath(ctx.getBasePath(), ctx.getUseSpecks());
        List<PsiElement> createdElements = ctx.createElements(separatedPaths, project);
        List<PsiElement> paths = new ArrayList<>();
        for (PsiElement el : createdElements) {
            paths.add(ctx.getRoot().addBefore(el, ctx.getFirstOldElement()));
        }

        for (PsiElement elem : ctx.getOldElements()) {
            elem.delete();
        }

        if (!paths.isEmpty()) {
            PsiElement lastPath = paths.get(paths.size() - 1);
            boolean nextUseSpeckExists = false;
            for (PsiElement s = lastPath.getNextSibling(); s != null; s = s.getNextSibling()) {
                if (s instanceof RsUseSpeck) { nextUseSpeckExists = true; break; }
            }
            if (!nextUseSpeckExists) {
                for (PsiElement s = lastPath.getNextSibling(); s != null; s = s.getNextSibling()) {
                    if ("\n".equals(s.getText())) { s.delete(); break; }
                }
            }
        }

        PsiElement firstPath = paths.isEmpty() ? null : paths.get(0);
        if (firstPath != null) {
            org.rust.openapiext.Editor.moveCaretToOffset(editor, firstPath, firstPath.getTextRange().getStartOffset() + ctx.getCursorOffset());
        }
    }

    private List<String> makeSeparatedPath(String basePath, List<RsUseSpeck> useSpecks) {
        List<String> result = new ArrayList<>();
        for (RsUseSpeck speck : useSpecks) {
            List<RsUseSpeck> innerList = speck.getUseGroup() != null ? speck.getUseGroup().getUseSpeckList() : null;
            RsPath speckPath = speck.getPath();
            if (innerList != null && speckPath != null) {
                result.addAll(makeSeparatedPath(basePath + "::" + speckPath.getText(), innerList));
            } else {
                result.add(addBasePath(speck.getText(), basePath));
            }
        }
        return result;
    }

    private String addBasePath(String localPath, String basePath) {
        if ("self".equals(localPath)) return basePath;
        return basePath + "::" + localPath;
    }

    public static class PathInNestedGroup implements Context {
        private final RsUseGroup useGroup;
        private final List<RsUseSpeck> useSpecks;
        private final String basePath;
        private final PsiElement firstOldElement;
        private final List<PsiElement> oldElements;
        private final PsiElement root;

        public PathInNestedGroup(RsUseGroup useGroup, List<RsUseSpeck> useSpecks, String basePath) {
            this.useGroup = useGroup;
            this.useSpecks = useSpecks;
            this.basePath = basePath;
            this.firstOldElement = useGroup.getParent();
            List<PsiElement> old = new ArrayList<>();
            old.add(firstOldElement);
            PsiElement next = firstOldElement.getNextSibling();
            if (next != null && RsPsiJavaUtil.elementType(next) == RsElementTypes.COMMA) {
                old.add(next);
            }
            this.oldElements = old;
            PsiElement p = useGroup.getParent();
            this.root = p != null ? p.getParent() : null;
        }

        @Override public List<RsUseSpeck> getUseSpecks() { return useSpecks; }
        @Override public PsiElement getRoot() { return root; }
        @Override public PsiElement getFirstOldElement() { return firstOldElement; }
        @Override public List<PsiElement> getOldElements() { return oldElements; }
        @Override public int getCursorOffset() { return 0; }
        @Override public String getBasePath() { return basePath; }

        @Override
        public List<PsiElement> createElements(List<String> paths, Project project) {
            RsPsiFactory psiFactory = new RsPsiFactory(project);
            List<PsiElement> result = new ArrayList<>();
            for (String path : paths) {
                RsUseSpeck useSpeck = psiFactory.createUseSpeck(path);
                useSpeck.add(psiFactory.createComma());
                useSpeck.add(psiFactory.createNewline());
                result.add(useSpeck);
            }
            return result;
        }
    }

    public static class PathInGroup implements Context {
        private final RsUseGroup useGroup;
        private final List<RsUseSpeck> useSpecks;
        private final String basePath;
        private final PsiElement firstOldElement;
        private final List<PsiElement> oldElements;
        private final PsiElement root;
        private final Collection<RsAttr> attrs;
        private final String visibility;

        public PathInGroup(RsUseGroup useGroup, List<RsUseSpeck> useSpecks, String basePath) {
            this.useGroup = useGroup;
            this.useSpecks = useSpecks;
            this.basePath = basePath;
            PsiElement p = useGroup.getParent();
            this.firstOldElement = p != null ? p.getParent() : null;
            this.oldElements = List.of(firstOldElement);
            this.root = firstOldElement != null ? firstOldElement.getParent() : null;
            this.attrs = firstOldElement != null ? RsPsiJavaUtil.descendantsOfType(firstOldElement, RsAttr.class) : List.of();
            this.visibility = firstOldElement instanceof RsUseItem ? (((RsUseItem) firstOldElement).getVis() != null ? ((RsUseItem) firstOldElement).getVis().getText() : null) : null;
        }

        @Override public List<RsUseSpeck> getUseSpecks() { return useSpecks; }
        @Override public PsiElement getRoot() { return root; }
        @Override public PsiElement getFirstOldElement() { return firstOldElement; }
        @Override public List<PsiElement> getOldElements() { return oldElements; }
        @Override public String getBasePath() { return basePath; }

        @Override
        public int getCursorOffset() {
            int offset = "use ".length();
            if (visibility != null) offset += visibility.length() + 1;
            for (RsAttr attr : attrs) {
                offset += attr.getTextLength();
            }
            return offset;
        }

        @Override
        public List<PsiElement> createElements(List<String> paths, Project project) {
            RsPsiFactory psiFactory = new RsPsiFactory(project);
            List<PsiElement> result = new ArrayList<>();
            for (String path : paths) {
                RsUseItem item = psiFactory.createUseItem(path, visibility != null ? visibility : "", null);
                List<RsAttr> attrList = new ArrayList<>(attrs);
                java.util.Collections.reverse(attrList);
                for (RsAttr attr : attrList) {
                    RsOuterAttr attrPsi = psiFactory.createOuterAttr(attr.getMetaItem().getText());
                    item.addBefore(attrPsi, item.getFirstChild());
                }
                result.add(item);
            }
            return result;
        }
    }
}
