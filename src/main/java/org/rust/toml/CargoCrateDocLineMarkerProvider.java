/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.ide.docs.RsDocumentationProvider;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.lineMarkers.RsLineMarkerInfoUtils;
import org.rust.lang.core.psi.ext.RsElement;
import org.toml.lang.psi.*;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

public class CargoCrateDocLineMarkerProvider extends LineMarkerProviderDescriptor {

    @NotNull
    @Override
    public String getName() {
        return RsBundle.message("gutter.rust.open.documentation.toml.name");
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return RsIcons.DOCS_MARK;
    }

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                        @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (!Util.tomlPluginIsAbiCompatible()) return;
        if (elements.isEmpty()) return;
        PsiElement firstElement = elements.get(0);
        PsiElement file = firstElement.getContainingFile();
        if (!file.getContainingFile().getName().equalsIgnoreCase(CargoConstants.MANIFEST_FILE)) return;

        for (PsiElement element : elements) {
            PsiElement parent = element.getParent();
            if (parent instanceof TomlKeySegment) {
                TomlKeySegment keySegment = (TomlKeySegment) parent;
                PsiElement keyValueCandidate = keySegment.getParent();
                if (keyValueCandidate == null) continue;
                PsiElement keyValueParent = keyValueCandidate.getParent();
                if (!(keyValueParent instanceof TomlKeyValue)) continue;
                TomlKeyValue keyValue = (TomlKeyValue) keyValueParent;
                PsiElement tableCandidate = keyValue.getParent();
                if (!(tableCandidate instanceof TomlTable)) continue;
                TomlTable table = (TomlTable) tableCandidate;
                if (!Util.isDependencyListHeader(table.getHeader())) continue;
                if (keySegment.getFirstChild() != null && keySegment.getFirstChild().getNextSibling() != null) continue;

                String pkgVersion = getVersion(keyValue);
                if (pkgVersion == null) continue;
                String pkgName = getCrateName(keyValue);
                if (pkgName == null) continue;

                result.add(genLineMarkerInfo(element, pkgName, pkgVersion));
            } else if (RsElementUtil.getElementType(element) == TomlElementTypes.L_BRACKET) {
                if (!(parent instanceof TomlTableHeader)) continue;
                TomlTableHeader header = (TomlTableHeader) parent;
                TomlKey key = header.getKey();
                List<TomlKeySegment> names = key != null ? key.getSegments() : List.of();
                if (names.size() < 2) continue;
                TomlKeySegment secondToLast = names.get(names.size() - 2);
                if (!Util.isDependencyKey(secondToLast)) continue;

                PsiElement tableCandidate = parent.getParent();
                if (!(tableCandidate instanceof TomlTable)) continue;
                TomlTable table = (TomlTable) tableCandidate;
                String pkgVersion = null;
                for (TomlKeyValue entry : table.getEntries()) {
                    if ("version".equals(entry.getKey().getText())) {
                        TomlValue val_ = entry.getValue();
                        pkgVersion = Util.getStringValue(val_);
                        break;
                    }
                }
                if (pkgVersion == null) continue;
                TomlKeySegment lastSegment = names.get(names.size() - 1);
                String pkgName = lastSegment.getName();
                if (pkgName == null) continue;

                result.add(genLineMarkerInfo(element, pkgName, pkgVersion));
            }
        }
    }

    @NotNull
    private LineMarkerInfo<PsiElement> genLineMarkerInfo(@NotNull PsiElement anchor,
                                                          @NotNull String name,
                                                          @NotNull String version) {
        String urlVersion;
        if (version.isEmpty()) {
            urlVersion = "*";
        } else if (Character.isDigit(version.charAt(0))) {
            urlVersion = "^" + version;
        } else {
            urlVersion = version;
        }

        String baseUrl = RsDocumentationProvider.getExternalDocumentationBaseUrl();
        return RsLineMarkerInfoUtils.create(
            anchor,
            anchor.getTextRange(),
            RsIcons.DOCS_MARK,
            (e, elt) -> BrowserUtil.browse(baseUrl + name + "/" + URLUtil.encodeURIComponent(urlVersion)),
            GutterIconRenderer.Alignment.LEFT,
            () -> RsBundle.message("gutter.rust.open.documentation.for", name + "@" + urlVersion)
        );
    }

    @Nullable
    private static String getCrateName(@NotNull TomlKeyValue keyValue) {
        TomlValue rootValue = keyValue.getValue();
        if (rootValue instanceof TomlInlineTable) {
            TomlInlineTable inlineTable = (TomlInlineTable) rootValue;
            for (TomlKeyValue entry : inlineTable.getEntries()) {
                if ("package".equals(entry.getKey().getText())) {
                    String val_ = Util.getStringValue(entry.getValue());
                    if (val_ != null) return val_;
                }
            }
            List<TomlKeySegment> segments = keyValue.getKey().getSegments();
            if (segments.size() == 1) {
                return segments.get(0).getName();
            }
            return null;
        } else {
            List<TomlKeySegment> segments = keyValue.getKey().getSegments();
            if (segments.size() == 1) {
                return segments.get(0).getName();
            }
            return null;
        }
    }

    @Nullable
    private static String getVersion(@NotNull TomlKeyValue keyValue) {
        TomlValue value = keyValue.getValue();
        if (value instanceof TomlLiteral) {
            return Util.getStringValue(value);
        } else if (value instanceof TomlInlineTable) {
            TomlInlineTable inlineTable = (TomlInlineTable) value;
            for (TomlKeyValue entry : inlineTable.getEntries()) {
                if ("version".equals(entry.getKey().getText())) {
                    return Util.getStringValue(entry.getValue());
                }
            }
            return null;
        } else {
            return null;
        }
    }
}
