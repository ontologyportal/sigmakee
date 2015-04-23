package com.ipsoft.sigma.editor;

import java.awt.Composite;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

import com.ipsoft.sigma.editor.scanner.ColorManager;


public class SigmaEditor extends TextEditor implements IReusableEditor{

	private ColorManager colorManager;

	public SigmaEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new SigmaConfiguration(colorManager,getPreferenceStore()));
		setDocumentProvider(new SigmaDocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

	
	public void markErrors(){
		IDocument doc=this.getDocumentProvider().getDocument(this.getEditorInput());
		doc.get();
		IEditorInput input=this.getEditorInput();
	}
}
