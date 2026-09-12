/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.openapiext.EditorExt;
import consulo.localize.LocalizeValue;

public class NestUseStatementsIntention extends RsElementBaseIntentionAction<NestUseStatementsIntention.Context> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.nest.use.statements"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public interface Context {
        @Nonnull
        List<RsUseSpeck> getUseSpecks();
        @Nonnull
        PsiElement getRoot();
        @Nonnull
        PsiElement getFirstOldElement();
        @Nonnull
        PsiElement createElement(@Nonnull String path, @Nonnull Project project);
        @Nonnull
        List<PsiElement> getOldElements();
        int getCursorOffset();
        @Nonnull
        String getBasePath();
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
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

    @Nonnull
    private String makeGroupedPath(@Nonnull String basePath, @Nonnull List<RsUseSpeck> useSpecks) {
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

    @Nonnull
    private String deleteBasePath(@Nonnull String fullPath, @Nonnull String basePath) {
        if (fullPath.equals(basePath)) return "self";
        if (fullPath.startsWith(basePath)) return fullPath.substring(basePath.length() + 2); // +2 for "::"
        return fullPath;
    }

    public static class PathInUseItem implements Context {
        private final RsUseItem myUseItem;
        private final List<RsUseItem> myUseItems;
        private final String myBasePath;

        public PathInUseItem(@Nonnull RsUseItem useItem, @Nonnull List<RsUseItem> useItems, @Nonnull String basePath) {
            myUseItem = useItem;
            myUseItems = useItems;
            myBasePath = basePath;
        }

        @Nullable
        public static PathInUseItem create(@Nonnull RsUseItem useItemOnCursor, @Nonnull RsUseSpeck useSpeck) {
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

        @Nonnull
        @Override
        public PsiElement createElement(@Nonnull String path, @Nonnull Project project) {
            PsiElement vis = myUseItem.getVis();
            String visText = vis != null ? vis.getText() : "";
            return new RsPsiFactory(project).createUseItem(path, visText, null);
        }

        @Nonnull
        @Override
        public List<RsUseSpeck> getUseSpecks() {
            List<RsUseSpeck> result = new ArrayList<>();
            for (RsUseItem item : myUseItems) {
                RsUseSpeck speck = item.getUseSpeck();
                if (speck != null) result.add(speck);
            }
            return result;
        }

        @Nonnull
        @Override
        public List<PsiElement> getOldElements() {
            return new ArrayList<>(myUseItems);
        }

        @Nonnull
        @Override
        public PsiElement getFirstOldElement() {
            return myUseItems.get(0);
        }

        @Nonnull
        @Override
        public PsiElement getRoot() {
            return myUseItem.getParent();
        }

        @Override
        public int getCursorOffset() {
            return "use ".length();
        }

        @Nonnull
        @Override
        public String getBasePath() {
            return myBasePath;
        }
    }

    public static class PathInGroup implements Context {
        private final RsUseGroup myUseGroup;
        private final List<RsUseSpeck> myUseSpecks;
        private final String myBasePath;

        public PathInGroup(@Nonnull RsUseGroup useGroup, @Nonnull List<RsUseSpeck> useSpecks, @Nonnull String basePath) {
            myUseGroup = useGroup;
            myUseSpecks = useSpecks;
            myBasePath = basePath;
        }

        @Nullable
        public static PathInGroup create(@Nonnull RsUseGroup useGroup, @Nonnull RsUseSpeck useSpeckOnCursor) {
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

        @Nonnull
        @Override
        public PsiElement createElement(@Nonnull String path, @Nonnull Project project) {
            return new RsPsiFactory(project).createUseSpeck(path);
        }

        @Nonnull
        @Override
        public List<RsUseSpeck> getUseSpecks() {
            return myUseSpecks;
        }

        @Nonnull
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

        @Nonnull
        @Override
        public PsiElement getFirstOldElement() {
            return myUseSpecks.get(0);
        }

        @Nonnull
        @Override
        public PsiElement getRoot() {
            return myUseGroup;
        }

        @Override
        public int getCursorOffset() {
            return 0;
        }

        @Nonnull
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
    public static String getBasePathFromPath(@Nonnull RsPath path) {
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
