// This is a generated file. Not intended for manual editing.
package com.ipsoft.amelia.sigma.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.ipsoft.amelia.sigma.psi.*;

public class RewriteRuleClauseImpl extends ASTWrapperPsiElement implements RewriteRuleClause {

  public RewriteRuleClauseImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RewriteRuleVisitor) ((RewriteRuleVisitor)visitor).visitClause(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public RewriteRuleKey getKey() {
    return findNotNullChildByClass(RewriteRuleKey.class);
  }

  @Override
  @NotNull
  public List<RewriteRuleValue> getValueList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RewriteRuleValue.class);
  }

}
