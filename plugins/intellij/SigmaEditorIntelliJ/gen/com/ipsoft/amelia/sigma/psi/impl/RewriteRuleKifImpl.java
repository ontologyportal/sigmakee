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

public class RewriteRuleKifImpl extends ASTWrapperPsiElement implements RewriteRuleKif {

  public RewriteRuleKifImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof RewriteRuleVisitor) ((RewriteRuleVisitor)visitor).visitKif(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public RewriteRuleKifkey getKifkey() {
    return findChildByClass(RewriteRuleKifkey.class);
  }

  @Override
  @NotNull
  public List<RewriteRuleKifvalue> getKifvalueList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, RewriteRuleKifvalue.class);
  }

  @Override
  @Nullable
  public RewriteRuleLogsent getLogsent() {
    return findChildByClass(RewriteRuleLogsent.class);
  }

  @Override
  @Nullable
  public RewriteRuleQuantsent getQuantsent() {
    return findChildByClass(RewriteRuleQuantsent.class);
  }

  @Override
  @Nullable
  public RewriteRuleRelsent getRelsent() {
    return findChildByClass(RewriteRuleRelsent.class);
  }

}
