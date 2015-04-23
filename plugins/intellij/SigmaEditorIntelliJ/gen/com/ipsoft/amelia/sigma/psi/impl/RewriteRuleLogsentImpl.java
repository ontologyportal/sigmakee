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

public class RewriteRuleLogsentImpl extends ASTWrapperPsiElement implements RewriteRuleLogsent {

  public RewriteRuleLogsentImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RewriteRuleVisitor) ((RewriteRuleVisitor)visitor).visitLogsent(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<RewriteRuleKif> getKifList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RewriteRuleKif.class);
  }

}
