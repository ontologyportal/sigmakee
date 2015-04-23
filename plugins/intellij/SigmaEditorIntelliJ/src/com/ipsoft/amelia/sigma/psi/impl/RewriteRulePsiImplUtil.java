package com.ipsoft.amelia.sigma.psi.impl;/*
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
import com.intellij.lang.ASTNode;
import com.ipsoft.amelia.sigma.psi.RewriteRuleRule;
import com.ipsoft.amelia.sigma.psi.RewriteRuleTypes;

public class RewriteRulePsiImplUtil {
    public static String getKey(RewriteRuleRule element) {
        ASTNode keyNode = element.getNode().findChildByType(RewriteRuleTypes.KEY);
        if (keyNode != null) {
            return keyNode.getText();
        } else {
            return null;
        }
    }

    public static String getValue(RewriteRuleRule element) {
        ASTNode valueNode = element.getNode().findChildByType(RewriteRuleTypes.VALUE);
        if (valueNode != null) {
            return valueNode.getText();
        } else {
            return null;
        }
    }
}
