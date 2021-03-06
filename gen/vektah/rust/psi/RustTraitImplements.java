// This is a generated file. Not intended for manual editing.
package vektah.rust.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RustTraitImplements extends PsiElement {

  @NotNull
  List<RustFunctionType> getFunctionTypeList();

  @NotNull
  List<RustTypeBasic> getTypeBasicList();

  @NotNull
  List<RustTypeClosure> getTypeClosureList();

  @NotNull
  List<RustTypeProc> getTypeProcList();

  @NotNull
  List<RustTypeTuple> getTypeTupleList();

  @NotNull
  List<RustTypeUnit> getTypeUnitList();

  @NotNull
  List<RustTypeVector> getTypeVectorList();

}
