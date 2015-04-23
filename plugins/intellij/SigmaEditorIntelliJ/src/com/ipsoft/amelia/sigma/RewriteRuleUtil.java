package com.ipsoft.amelia.sigma;/*
Copyright 2014-2015 IPsoft

Author: Peigen You Peigen.You@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
*/
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.ipsoft.amelia.sigma.psi.RewriteRuleFile;
import com.ipsoft.amelia.sigma.psi.RewriteRuleKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RewriteRuleUtil {
    public static List<PsiElement> findProperties(Project project, String key) {

        System.out.println("INFO in findProperties:"+key);
        List<PsiElement> result = null;
        Collection<VirtualFile> virtualFiles = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, RewriteRuleFileType.INSTANCE,
                GlobalSearchScope.allScope(project));
        for (VirtualFile virtualFile : virtualFiles) {
            RewriteRuleFile RewriteRuleFile = (RewriteRuleFile) PsiManager.getInstance(project).findFile(virtualFile);
            if (RewriteRuleFile != null) {
                RewriteRuleKey[] properties = PsiTreeUtil.getChildrenOfType(RewriteRuleFile, RewriteRuleKey.class);
                if (properties != null) {
                    for (RewriteRuleKey property : properties) {
                        if (property.getText().contains(key)) {
                            if (result == null) {
                                result = new ArrayList<PsiElement>();
                            }
                            result.add(property);
                        }
                    }
                }
            }
        }
        return result != null ? result : Collections.<PsiElement>emptyList();
    }

    public static List<PsiElement> findProperties(Project project) {

        System.out.println("INFO in findProperties: no key");
        List<PsiElement> result = new ArrayList<PsiElement>();
        Collection<VirtualFile> virtualFiles = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, RewriteRuleFileType.INSTANCE,
                GlobalSearchScope.allScope(project));
        for (VirtualFile virtualFile : virtualFiles) {
            RewriteRuleFile RewriteRuleFile = (RewriteRuleFile) PsiManager.getInstance(project).findFile(virtualFile);
            if (RewriteRuleFile != null) {
                RewriteRuleKey[] properties = PsiTreeUtil.getChildrenOfType(RewriteRuleFile, RewriteRuleKey.class);
                if (properties != null) {
                    Collections.addAll(result, properties);
                }
            }
        }
        return result;
    }
}
