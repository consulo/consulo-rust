// This is a generated file. Not intended for manual editing.
package vektah.rust.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static vektah.rust.psi.RustTokens.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import vektah.rust.psi.*;

public class RustTraitBoundsImpl extends ASTWrapperPsiElement implements RustTraitBounds {

  public RustTraitBoundsImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RustVisitor) ((RustVisitor)visitor).visitTraitBounds(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public RustRegionBound getRegionBound() {
    return findChildByClass(RustRegionBound.class);
  }

}
