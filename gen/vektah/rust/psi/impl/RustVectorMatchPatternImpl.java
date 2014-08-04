// This is a generated file. Not intended for manual editing.
package vektah.rust.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static vektah.rust.psi.RustTokens.*;
import vektah.rust.psi.*;

public class RustVectorMatchPatternImpl extends RustMatchPatternImpl implements RustVectorMatchPattern {

  public RustVectorMatchPatternImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RustVisitor) ((RustVisitor)visitor).visitVectorMatchPattern(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RustMatchPattern> getMatchPatternList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RustMatchPattern.class);
  }

}
