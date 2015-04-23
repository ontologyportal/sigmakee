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

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.ipsoft.amelia.sigma.psi.RewriteRuleTypes;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Scanner;

public class RewriteRuleCompletionContributor extends CompletionContributor {
    private static String[] cnfterms;

    public RewriteRuleCompletionContributor(){
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(RewriteRuleTypes.WORD).withLanguage(RewriteRuleLanguage.INSTANCE),
                new CompletionProvider<CompletionParameters>() {
                    public void addCompletions(@NotNull CompletionParameters parameters,
                                               ProcessingContext context,
                                               @NotNull CompletionResultSet resultSet) {
                        String[] terms=getCNFTerms();
                        System.out.println("Completion Contributor called");
                        for(String s:terms){
                            resultSet.addElement(LookupElementBuilder.create(s));
                        }
                    }
                }
        );
    }
    public static String[] getCNFTerms(){
        if(cnfterms==null)
            cnfterms=readTerms("CNFTerms.txt");
        return cnfterms;
    }
    public static String[] readTerms(String filename){
        Scanner in;
        HashSet<String> terms=new HashSet<String>();

        Reader paramReader = new InputStreamReader(RewriteRuleCompletionContributor.class.getResourceAsStream(filename));
        if(paramReader==null) System.out.println("resouce reader is null");
        in = new Scanner(paramReader);
        while(in.hasNextLine()){
            String k=in.nextLine();
            terms.add(k);
        }
        try {
            paramReader.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        in.close();
        return terms.toArray(new String[terms.size()]);
    }
}
