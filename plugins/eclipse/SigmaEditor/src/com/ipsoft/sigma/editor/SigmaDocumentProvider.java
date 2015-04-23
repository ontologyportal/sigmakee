package com.ipsoft.sigma.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;


public class SigmaDocumentProvider extends FileDocumentProvider{
	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		return document;
	}
}
