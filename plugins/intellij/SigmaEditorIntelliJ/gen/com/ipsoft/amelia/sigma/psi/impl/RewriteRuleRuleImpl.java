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

public class RewriteRuleRuleImpl extends ASTWrapperPsiElement implements RewriteRuleRule {

  public RewriteRuleRuleImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RewriteRuleVisitor) ((RewriteRuleVisitor)visitor).visitRule(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public RewriteRuleClause getClause() {
    return findChildByClass(RewriteRuleClause.class);
  }

  @Override
  @Nullable
  public RewriteRuleLhs getLhs() {
    return findChildByClass(RewriteRuleLhs.class);
  }

  @Override
  @Nullable
  public RewriteRuleRhs getRhs() {
    return findChildByClass(RewriteRuleRhs.class);
  }

  @Override
  @Nullable
  public PsiElement getSeperator() {
    return findChildByType(SEPERATOR);
  }

}
