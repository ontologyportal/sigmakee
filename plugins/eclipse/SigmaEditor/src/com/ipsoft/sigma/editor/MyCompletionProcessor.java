package com.ipsoft.sigma.editor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class MyCompletionProcessor implements IContentAssistProcessor {

	private final String [] funcs=new String[]{};
	private final ICompletionProposal[] NO_COMPLETIONS={ };
	private final IContextInformation[] NO_CONTEXTS = { };
	private final String[] terms;
	public MyCompletionProcessor(){
		HashSet<String> hs=new HashSet<String>();
		String[] terms=SigmaUtil.getCNFTerms();
		for(String k:terms)
			hs.add(k);
		terms= SigmaUtil.getSUMOTerms();
		for(String k:terms){
			hs.add(k);
		}
		SigmaLogger.log("----------sumo term size is "+terms.length);
		this.terms=hs.toArray(new String[hs.size()]);
	}
	
	
	
	
    @Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int offset) {
		try{
			IDocument document=viewer.getDocument();
		ArrayList<ICompletionProposal> result=new ArrayList<ICompletionProposal>();
		String prefix = lastWord(document, offset);
        //String indent = lastIndent(document, offset);
        for(String k:terms){
        	if(k.startsWith(prefix))
    			result.add(new CompletionProposal(k,offset-prefix.length(),prefix.length(),k.length()));
        }
        
        
        return (ICompletionProposal[]) result.toArray(new ICompletionProposal[result.size()]);
     } catch (Exception e) {
        // ... log the exception ...
        return NO_COMPLETIONS;
     }
  }
  private String lastWord(IDocument doc, int offset) {
     try {
        for (int n = offset-1; n >= 0; n--) {
          char c = doc.getChar(n);
          if (!Character.isJavaIdentifierPart(c))
            return doc.get(n + 1, offset-n-1);
        }
     } catch (BadLocationException e) {
        // ... log the exception ...
     }
     return "";
  }
  private String lastIndent(IDocument doc, int offset) {
     try {
        int start = offset-1; 
        while (start >= 0 && doc.getChar(start)!= '\n') start--;
        int end = start;
        while (end < offset && Character.isSpaceChar(doc.getChar(end))) end++;
        return doc.get(start+1, end-start-1);
     } catch (BadLocationException e) {
        e.printStackTrace();
     }
     return "";
  }
	@Override
	public IContextInformation[] computeContextInformation(ITextViewer arg0,
			int arg1) {
		// TODO Auto-generated method stub
		return NO_CONTEXTS;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}
	
	public static void main(String[] args){
		MyCompletionProcessor a=new MyCompletionProcessor();
		
	}

}
