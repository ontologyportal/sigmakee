// This is a generated file. Not intended for manual editing.
package com.ipsoft.amelia.sigma.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RewriteRuleRule extends PsiElement {

  @Nullable
  RewriteRuleClause getClause();

  @Nullable
  RewriteRuleLhs getLhs();

  @Nullable
  RewriteRuleRhs getRhs();

  @Nullable
  PsiElement getSeperator();

}
