/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.openapiext.EditorExt;

public class NestUseStatementsIntention extends RsElementBaseIntentionAction<NestUseStatementsIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.nest.use.statements");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public interface Context {
        @NotNull
        List<RsUseSpeck> getUseSpecks();
        @NotNull
        PsiElement getRoot();
        @NotNull
        PsiElement getFirstOldElement();
        @NotNull
        PsiElement createElement(@NotNull String path, @NotNull Project project);
        @NotNull
        List<PsiElement> getOldElements();
        int getCursorOffset();
        @NotNull
        String getBasePath();
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsUseItem useItemOnCursor = PsiElementExt.ancestorStrict(element, RsUseItem.class);
        if (useItemOnCursor == null) return null;
        RsUseGroup useGroupOnCursor = PsiElementExt.ancestorStrict(element, RsUseGroup.class);
        RsUseSpeck useSpeckOnCursor = PsiElementExt.ancestorStrict(element, RsUseSpeck.class);
        if (useSpeckOnCursor == null) return null;

        if (useGroupOnCursor != null) {
            return PathInGroup.create(useGroupOnCursor, useSpeckOnCursor);
        } else {
            return PathInUseItem.create(useItemOnCursor, useSpeckOnCursor);
        }
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        String path = makeGroupedPath(ctx.getBasePath(), ctx.getUseSpecks());

        PsiElement inserted = ctx.getRoot().addAfter(ctx.createElement(path, project), ctx.getFirstOldElement());

        for (PsiElement prevElement : ctx.getOldElements()) {
            PsiElement firstChild = prevElement.getFirstChild();
            if (firstChild instanceof PsiComment) {
                ctx.getRoot().addBefore(firstChild, inserted);
            }
            prevElement.delete();
        }

        int nextUseSpeckCount = 0;
        PsiElement sibling = inserted.getNextSibling();
        while (sibling != null) {
            if (sibling instanceof RsUseSpeck) nextUseSpeckCount++;
            sibling = sibling.getNextSibling();
        }
        if (nextUseSpeckCount > 0) {
            ctx.getRoot().addAfter(new RsPsiFactory(project).createComma(), inserted);
        }

        EditorExt.moveCaretToOffset(editor, inserted, PsiElementExt.getStartOffset(inserted) + ctx.getCursorOffset());
    }

    @NotNull
    private String makeGroupedPath(@NotNull String basePath, @NotNull List<RsUseSpeck> useSpecks) {
        List<String> useSpecksInGroup = new ArrayList<>();
        for (RsUseSpeck useSpeck : useSpecks) {
            RsPath path = useSpeck.getPath();
            if (path != null && basePath.equals(path.getText())) {
                RsUseGroup useGroup = useSpeck.getUseGroup();
                if (useGroup != null) {
                    for (RsUseSpeck inner : useGroup.getUseSpeckList()) {
                        useSpecksInGroup.add(inner.getText());
                    }
                    continue;
                }
                RsAlias alias = useSpeck.getAlias();
                if (alias != null) {
                    useSpecksInGroup.add("self " + alias.getText());
                    continue;
                }
            }
            useSpecksInGroup.add(deleteBasePath(useSpeck.getText(), basePath));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(basePath).append("::{\n");
        for (int i = 0; i < useSpecksInGroup.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(useSpecksInGroup.get(i));
        }
        sb.append("\n}");
        return sb.toString();
    }

    @NotNull
    private String deleteBasePath(@NotNull String fullPath, @NotNull String basePath) {
        if (fullPath.equals(basePath)) return "self";
        if (fullPath.startsWith(basePath)) return fullPath.substring(basePath.length() + 2); // +2 for "::"
        return fullPath;
    }

    public static class PathInUseItem implements Context {
        private final RsUseItem myUseItem;
        private final List<RsUseItem> myUseItems;
        private final String myBasePath;

        public PathInUseItem(@NotNull RsUseItem useItem, @NotNull List<RsUseItem> useItems, @NotNull String basePath) {
            myUseItem = useItem;
            myUseItems = useItems;
            myBasePath = basePath;
        }

        @Nullable
        public static PathInUseItem create(@NotNull RsUseItem useItemOnCursor, @NotNull RsUseSpeck useSpeck) {
            RsPath path = useSpeck.getPath();
            if (path == null) return null;
            String basePath = getBasePathFromPath(path);
            if (basePath == null) return null;
            PsiElement visibility = useItemOnCursor.getVis();

            List<RsUseItem> useItemList = new ArrayList<>();

            // left siblings
            PsiElement left = useItemOnCursor.getPrevSibling();
            while (left != null) {
                if (left instanceof RsUseItem) {
                    RsUseItem item = (RsUseItem) left;
                    RsUseSpeck speck = item.getUseSpeck();
                    if (speck != null && speck.getPath() != null) {
                        String bp = getBasePathFromPath(speck.getPath());
                        if (basePath.equals(bp) && isVisibilityEqual(item.getVis(), visibility)) {
                            useItemList.add(0, item);
                        }
                    }
                }
                left = left.getPrevSibling();
            }

            useItemList.add(useItemOnCursor);

            // right siblings
            PsiElement right = useItemOnCursor.getNextSibling();
            while (right != null) {
                if (right instanceof RsUseItem) {
                    RsUseItem item = (RsUseItem) right;
                    RsUseSpeck speck = item.getUseSpeck();
                    if (speck != null && speck.getPath() != null) {
                        String bp = getBasePathFromPath(speck.getPath());
                        if (basePath.equals(bp) && isVisibilityEqual(item.getVis(), visibility)) {
                            useItemList.add(item);
                        }
                    }
                }
                right = right.getNextSibling();
            }

            if (useItemList.size() == 1) return null;
            return new PathInUseItem(useItemOnCursor, useItemList, basePath);
        }

        private static boolean isVisibilityEqual(@Nullable PsiElement vis1, @Nullable PsiElement vis2) {
            if (vis1 == null && vis2 == null) return true;
            if (vis1 == null || vis2 == null) return false;
            return vis1.getText().equals(vis2.getText());
        }

        @NotNull
        @Override
        public PsiElement createElement(@NotNull String path, @NotNull Project project) {
            PsiElement vis = myUseItem.getVis();
            String visText = vis != null ? vis.getText() : "";
            return new RsPsiFactory(project).createUseItem(path, visText, null);
        }

        @NotNull
        @Override
        public List<RsUseSpeck> getUseSpecks() {
            List<RsUseSpeck> result = new ArrayList<>();
            for (RsUseItem item : myUseItems) {
                RsUseSpeck speck = item.getUseSpeck();
                if (speck != null) result.add(speck);
            }
            return result;
        }

        @NotNull
        @Override
        public List<PsiElement> getOldElements() {
            return new ArrayList<>(myUseItems);
        }

        @NotNull
        @Override
        public PsiElement getFirstOldElement() {
            return myUseItems.get(0);
        }

        @NotNull
        @Override
        public PsiElement getRoot() {
            return myUseItem.getParent();
        }

        @Override
        public int getCursorOffset() {
            return "use ".length();
        }

        @NotNull
        @Override
        public String getBasePath() {
            return myBasePath;
        }
    }

    public static class PathInGroup implements Context {
        private final RsUseGroup myUseGroup;
        private final List<RsUseSpeck> myUseSpecks;
        private final String myBasePath;

        public PathInGroup(@NotNull RsUseGroup useGroup, @NotNull List<RsUseSpeck> useSpecks, @NotNull String basePath) {
            myUseGroup = useGroup;
            myUseSpecks = useSpecks;
            myBasePath = basePath;
        }

        @Nullable
        public static PathInGroup create(@NotNull RsUseGroup useGroup, @NotNull RsUseSpeck useSpeckOnCursor) {
            RsPath path = useSpeckOnCursor.getPath();
            if (path == null) return null;
            String basePath = getBasePathFromPath(path);
            if (basePath == null) return null;

            List<RsUseSpeck> useSpeckList = new ArrayList<>();

            // left siblings
            PsiElement left = useSpeckOnCursor.getPrevSibling();
            while (left != null) {
                if (left instanceof RsUseSpeck) {
                    RsUseSpeck speck = (RsUseSpeck) left;
                    if (speck.getPath() != null && basePath.equals(getBasePathFromPath(speck.getPath()))) {
                        useSpeckList.add(0, speck);
                    }
                }
                left = left.getPrevSibling();
            }

            useSpeckList.add(useSpeckOnCursor);

            // right siblings
            PsiElement right = useSpeckOnCursor.getNextSibling();
            while (right != null) {
                if (right instanceof RsUseSpeck) {
                    RsUseSpeck speck = (RsUseSpeck) right;
                    if (speck.getPath() != null && basePath.equals(getBasePathFromPath(speck.getPath()))) {
                        useSpeckList.add(speck);
                    }
                }
                right = right.getNextSibling();
            }

            if (useSpeckList.size() == 1) return null;
            return new PathInGroup(useGroup, useSpeckList, basePath);
        }

        @NotNull
        @Override
        public PsiElement createElement(@NotNull String path, @NotNull Project project) {
            return new RsPsiFactory(project).createUseSpeck(path);
        }

        @NotNull
        @Override
        public List<RsUseSpeck> getUseSpecks() {
            return myUseSpecks;
        }

        @NotNull
        @Override
        public List<PsiElement> getOldElements() {
            List<PsiElement> result = new ArrayList<>();
            for (RsUseSpeck speck : myUseSpecks) {
                result.add(speck);
                PsiElement next = speck.getNextSibling();
                if (next != null && PsiElementExt.getElementType(next) == RsElementTypes.COMMA) {
                    result.add(next);
                }
            }
            return result;
        }

        @NotNull
        @Override
        public PsiElement getFirstOldElement() {
            return myUseSpecks.get(0);
        }

        @NotNull
        @Override
        public PsiElement getRoot() {
            return myUseGroup;
        }

        @Override
        public int getCursorOffset() {
            return 0;
        }

        @NotNull
        @Override
        public String getBasePath() {
            return myBasePath;
        }
    }

    /**
     * Get base path.
     * If the path starts with :: contains it
     *
     * ex) a::b::c -> a
     * ex) ::a::b::c -> ::a
     */
    @Nullable
    public static String getBasePathFromPath(@NotNull RsPath path) {
        RsPath basePath = RsPathUtil.basePath(path);
        PsiElement coloncolon = basePath.getColoncolon();
        String refName = basePath.getReferenceName();
        if (refName == null) refName = "";

        if (coloncolon != null) {
            return "::" + refName;
        } else {
            return refName;
        }
    }
}
