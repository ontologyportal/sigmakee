// This is a generated file. Not intended for manual editing.
package com.ipsoft.amelia.sigma.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface RewriteRuleKif extends PsiElement {

  @Nullable
  RewriteRuleKifkey getKifkey();

  @NotNull
  List<RewriteRuleKifvalue> getKifvalueList();

  @Nullable
  RewriteRuleLogsent getLogsent();

  @Nullable
  RewriteRuleQuantsent getQuantsent();

  @Nullable
  RewriteRuleRelsent getRelsent();

}
