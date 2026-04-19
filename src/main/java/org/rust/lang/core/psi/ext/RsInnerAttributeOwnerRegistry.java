/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsAttributeOwnerStub;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsInnerAttrStub;
import org.rust.lang.core.stubs.RsMetaItemStub;

import java.util.*;
import java.util.stream.Stream;

public final class RsInnerAttributeOwnerRegistry {

    private RsInnerAttributeOwnerRegistry() {
    }

    private static final Map<IElementType, AttrInfo> REG = new HashMap<>();

    static {
        REG.put(RsElementTypes.FOREIGN_MOD_ITEM, AttrInfo.DIRECT);
        REG.put(RsElementTypes.MOD_ITEM, AttrInfo.DIRECT);
        REG.put(RsFileStub.Type, AttrInfo.DIRECT);
        REG.put(RsElementTypes.FUNCTION, new AttrInfo.Nested(RsElementTypes.BLOCK));
        REG.put(RsElementTypes.IMPL_ITEM, new AttrInfo.Nested(RsElementTypes.MEMBERS));
        REG.put(RsElementTypes.TRAIT_ITEM, new AttrInfo.Nested(RsElementTypes.MEMBERS));
    }

    private static final TokenSet ATTRS_ELEMENT_SET = RsTokenType.tokenSetOf(
        RsElementTypes.OUTER_ATTR, RsElementTypes.INNER_ATTR
    );

    @NotNull
    public static List<RsInnerAttr> innerAttrs(@NotNull RsInnerAttributeOwner psi) {
        AttrInfo info = REG.get(((PsiElement) psi).getNode().getElementType());
        if (info == AttrInfo.DIRECT) {
            return PsiElementUtil.stubChildrenOfType(psi, RsInnerAttr.class);
        }
        if (info instanceof AttrInfo.Nested) {
            PsiElement child = PsiElementUtil.stubChildOfElementType(
                psi, TokenSet.create(((AttrInfo.Nested) info).elementType), PsiElement.class
            );
            if (child != null) {
                return PsiElementUtil.stubChildrenOfType(child, RsInnerAttr.class);
            }
            return Collections.emptyList();
        }
        throw new IllegalStateException("Inner attributes for type " + psi + " are not registered");
    }

    @NotNull
    private static Stream<RsAttr> allAttrs(@NotNull RsDocAndAttributeOwner psi) {
        AttrInfo info = REG.get(((PsiElement) psi).getNode().getElementType());
        if (info == null || info == AttrInfo.DIRECT) {
            return PsiElementUtil.stubChildrenOfType(psi, RsAttr.class).stream();
        }
        if (info instanceof AttrInfo.Nested) {
            Stream<RsAttr> outer = Stream.empty();
            if (psi instanceof RsOuterAttributeOwner) {
                outer = ((RsOuterAttributeOwner) psi).getOuterAttrList().stream().map(a -> (RsAttr) a);
            }
            Stream<RsAttr> inner = Stream.empty();
            if (psi instanceof RsInnerAttributeOwner) {
                inner = ((RsInnerAttributeOwner) psi).getInnerAttrList().stream().map(a -> (RsAttr) a);
            }
            return Stream.concat(outer, inner);
        }
        return Stream.empty();
    }

    @NotNull
    public static Stream<RsMetaItem> rawMetaItems(@NotNull RsDocAndAttributeOwner psi) {
        return allAttrs(psi).map(RsAttr::getMetaItem);
    }

    @NotNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <S extends StubElement<?> & RsAttributeOwnerStub> Stream<StubElement<?>> allAttrsStub(@NotNull S stub) {
        AttrInfo info = REG.get(stub.getStubType());
        List rawChildren = stub.getChildrenStubs();
        List<StubElement<?>> children = (List<StubElement<?>>) rawChildren;
        if (info == null || info == AttrInfo.DIRECT) {
            return children.stream()
                .filter(s -> ATTRS_ELEMENT_SET.contains(s.getStubType()));
        }
        if (info instanceof AttrInfo.Nested) {
            Stream<StubElement<?>> outer = filterOuterAttrs(rawChildren);
            Stream<StubElement<?>> inner = children.stream()
                .filter(s -> s.getStubType() == ((AttrInfo.Nested) info).elementType)
                .findFirst()
                .map(s -> {
                    List rawInner = s.getChildrenStubs();
                    return ((List<StubElement<?>>) rawInner).stream()
                        .filter(c -> c instanceof RsInnerAttrStub);
                })
                .orElse(Stream.empty());
            return Stream.concat(outer, inner);
        }
        return Stream.empty();
    }

    @NotNull
    public static <S extends StubElement<?> & RsAttributeOwnerStub> Stream<RsMetaItemStub> rawMetaItemsStub(@NotNull S stub) {
        return allAttrsStub(stub).map(s -> s.findChildStubByType(RsMetaItemStub.Type)).filter(Objects::nonNull);
    }

    @NotNull
    public static <S extends StubElement<?> & RsAttributeOwnerStub> Stream<RsMetaItemStub> rawOuterMetaItemsStub(@NotNull S stub) {
        return filterOuterAttrs(stub.getChildrenStubs())
            .map(s -> s.findChildStubByType(RsMetaItemStub.Type))
            .filter(Objects::nonNull);
    }

    @NotNull
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Stream<StubElement<?>> filterOuterAttrs(@NotNull List childrenStubs) {
        return ((List<StubElement<?>>) childrenStubs).stream()
            .filter(s -> s.getStubType() == RsElementTypes.OUTER_ATTR);
    }

    private static abstract class AttrInfo {
        static final AttrInfo DIRECT = new AttrInfo() {};

        static final class Nested extends AttrInfo {
            @NotNull final IElementType elementType;

            Nested(@NotNull IElementType elementType) {
                this.elementType = elementType;
            }
        }
    }
}
