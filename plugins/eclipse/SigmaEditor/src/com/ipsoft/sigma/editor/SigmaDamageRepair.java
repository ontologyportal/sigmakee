package com.ipsoft.sigma.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;

public class SigmaDamageRepair extends DefaultDamagerRepairer {

	/**
	 * The maximum length of a line to damage completely.
	 */
	public static final int MAX_COMPLETE_LINE_DAMAGE = 200;
	
	/**
	 * The number of chars to damage in front of and at the end of
	 * the offset.
	 */
	public static final int CHARS_TO_DAMAGE = 25;
	
	public SigmaDamageRepair(ITokenScanner scanner) {
		super(scanner);	
	}
	public void setDocument(IDocument document) {
		fDocument = document;
	}
	@Override
	public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent e, boolean documentPartitioningChanged){
		
		
		IRegion damagedRegion = null;
		try {
			IDocument doc = e.getDocument();
			IRegion damagedLine = doc.getLineInformationOfOffset(e.getOffset());
			
			// the damaged line is too long so calculate a smaller region to damage instead of the
			// whole line.
			if(damagedLine.getLength() > MAX_COMPLETE_LINE_DAMAGE ){
				damagedRegion = calculateRegionToDamage(e.getOffset(), doc);
			} else {
				damagedRegion = super.getDamageRegion(partition, e, documentPartitioningChanged);
			}
			
			
		} catch (BadLocationException ex) {
			//YEditLog.logException(ex, "Failed to get the region for a document change.");
		}
		
		return damagedRegion;
	}
	protected int endOfLineOf(int offset) throws BadLocationException {

		IRegion info = fDocument.getLineInformationOfOffset(offset);
		if (offset <= info.getOffset() + info.getLength())
			return info.getOffset() + info.getLength();

		int line = fDocument.getLineOfOffset(offset);
		try {
			info = fDocument.getLineInformation(line + 1);
			return info.getOffset() + info.getLength();
		} catch (BadLocationException x) {
			return fDocument.getLength();
		}
	}
	private IRegion calculateRegionToDamage(int offset, IDocument doc) throws BadLocationException{
		
		int damagedLineNum = doc.getLineOfOffset(offset);
		int lineOffset = doc.getLineOffset(damagedLineNum); 
		int damagedLineLength = doc.getLineLength(damagedLineNum);
		
		int startDamageOffset = offset - CHARS_TO_DAMAGE;
		if( startDamageOffset < lineOffset ){
			startDamageOffset = lineOffset;
		}
		
		int endDamageOffset = offset + CHARS_TO_DAMAGE;
		if( endDamageOffset > lineOffset + damagedLineLength ){
			endDamageOffset = lineOffset + damagedLineLength;
		}
		
		IRegion regionToDamage = new Region(startDamageOffset, endDamageOffset - startDamageOffset);
		return regionToDamage;
		
	}

}