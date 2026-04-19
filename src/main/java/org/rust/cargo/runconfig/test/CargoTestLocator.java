/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.impl.CargoProjectsServiceImpl;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.stubs.index.RsNamedElementIndex;
import org.rust.openapiext.OpenApiUtil;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CargoTestLocator implements SMTestLocator {

    public static final CargoTestLocator INSTANCE = new CargoTestLocator();

    private static final String NAME_SEPARATOR = "::";
    private static final String TEST_PROTOCOL = "cargo:test";
    private static final Pattern LINE_NUM_REGEX = Pattern.compile("(.*?)(#\\d+)?$");

    private CargoTestLocator() {
    }

    @NotNull
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Location> getLocation(
        @NotNull String protocol,
        @NotNull String path,
        @NotNull Project project,
        @NotNull GlobalSearchScope scope
    ) {
        if (!TEST_PROTOCOL.equals(protocol)) return Collections.emptyList();
        QualifiedNameResult qnResult = toQualifiedName(path);
        String qualifiedName = qnResult.qualifiedName;
        Integer lineNum = qnResult.lineNum;

        if (!qualifiedName.contains(NAME_SEPARATOR)) {
            List<Location> result = new ArrayList<>();
            for (CargoWorkspace.Target target : org.rust.cargo.project.model.impl.CargoProjectsServiceImplUtil.getAllTargets(CargoProjectServiceUtil.getCargoProjects(project))) {
                if (target.getPkg().getOrigin() != PackageOrigin.WORKSPACE) continue;
                if (!target.getNormName().equals(qualifiedName)) continue;
                VirtualFile crateRoot = target.getCrateRoot();
                if (crateRoot == null) continue;
                PsiElement psiFile = OpenApiUtil.toPsiFile(crateRoot, project);
                if (psiFile == null) continue;
                result.add(getLocation(project, psiFile, lineNum));
            }
            return result;
        }

        List<Location> result = new ArrayList<>();
        String name = qualifiedName.substring(qualifiedName.lastIndexOf(NAME_SEPARATOR) + NAME_SEPARATOR.length());
        for (PsiElement element : RsNamedElementIndex.findElementsByName(project, name, scope)) {
            PsiElement sourceElement;
            if (element instanceof RsModDeclItem) {
                sourceElement = ((RsModDeclItem) element).getReference().resolve();
                if (!(sourceElement instanceof RsFile)) sourceElement = null;
            } else if (element instanceof RsQualifiedNamedElement) {
                sourceElement = element;
            } else {
                sourceElement = null;
            }
            if (sourceElement instanceof RsQualifiedNamedElement) {
                String elemQualName = org.rust.lang.core.psi.ext.RsQualifiedNamedElementUtil.getQualifiedName((RsQualifiedNamedElement) sourceElement);
                if (qualifiedName.equals(elemQualName)) {
                    result.add(getLocation(project, sourceElement, lineNum));
                }
            }
        }
        return result;
    }

    @NotNull
    private static Location<PsiElement> getLocation(@NotNull Project project, @NotNull PsiElement element, @Nullable Integer lineNum) {
        if (lineNum != null) {
            final int line = lineNum;
            return new PsiLocation<PsiElement>(project, element) {
                @Nullable
                @Override
                public OpenFileDescriptor getOpenFileDescriptor() {
                    VirtualFile vf = getVirtualFile();
                    if (vf == null) return null;
                    return new OpenFileDescriptor(project, vf, line - 1, 0);
                }
            };
        } else {
            return PsiLocation.fromPsiElement(element);
        }
    }

    @NotNull
    public static String getTestUrl(@NotNull String name) {
        QualifiedNameResult qnResult = toQualifiedName(name);
        String url = TEST_PROTOCOL + "://" + qnResult.qualifiedName;
        if (qnResult.lineNum != null) {
            url += "#" + qnResult.lineNum;
        }
        return url;
    }

    @NotNull
    public static String getTestUrl(@NotNull RsQualifiedNamedElement function) {
        String qualifiedName = org.rust.lang.core.psi.ext.RsQualifiedNamedElementUtil.getQualifiedName(function);
        return getTestUrl(qualifiedName != null ? qualifiedName : "");
    }

    @NotNull
    public static String getTestUrl(@NotNull DocTestContext ctx) {
        String ownerQualifiedName = org.rust.lang.core.psi.ext.RsQualifiedNamedElementUtil.getQualifiedName(ctx.getOwner());
        String owner = ownerQualifiedName != null ? ownerQualifiedName : "";
        return getTestUrl(owner + "#" + ctx.getLineNumber());
    }

    @NotNull
    private static QualifiedNameResult toQualifiedName(@NotNull String path) {
        String targetName;
        int sepIdx = path.indexOf(NAME_SEPARATOR);
        if (sepIdx >= 0) {
            targetName = path.substring(0, sepIdx);
        } else {
            targetName = path;
        }
        int lastDash = targetName.lastIndexOf('-');
        if (lastDash >= 0) {
            targetName = targetName.substring(0, lastDash);
        }

        if (!path.contains(NAME_SEPARATOR)) {
            return new QualifiedNameResult(targetName, null);
        }

        Matcher matcher = LINE_NUM_REGEX.matcher(path);
        if (!matcher.matches()) {
            return new QualifiedNameResult(targetName, null);
        }

        String group1 = matcher.group(1);
        String qualifiedName = null;
        if (group1 != null) {
            int idx = group1.indexOf(NAME_SEPARATOR);
            if (idx >= 0) {
                qualifiedName = group1.substring(idx + NAME_SEPARATOR.length());
            }
        }

        Integer lineNum = null;
        String group2 = matcher.group(2);
        if (group2 != null) {
            lineNum = Integer.parseInt(group2.substring(1));
        }

        return new QualifiedNameResult(targetName + NAME_SEPARATOR + qualifiedName, lineNum);
    }

    private static final class QualifiedNameResult {
        @NotNull
        final String qualifiedName;
        @Nullable
        final Integer lineNum;

        QualifiedNameResult(@NotNull String qualifiedName, @Nullable Integer lineNum) {
            this.qualifiedName = qualifiedName;
            this.lineNum = lineNum;
        }
    }
}
