package com.ipsoft.sigma.editor;

import java.util.Iterator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

import com.ipsoft.sigma.editor.scanner.ColorConstants;
import com.ipsoft.sigma.editor.scanner.ColorManager;
import com.ipsoft.sigma.editor.scanner.SigmaScanner;
import com.ipsoft.sigma.editor.scanner.SigmaTokens;

public class SigmaConfiguration extends TextSourceViewerConfiguration {
	private SigmaScanner scanner;
	private ColorManager colorManager;
	private SourceViewerDecorationSupport fSourceViewerDecorationSupport;

	public SigmaConfiguration (ColorManager colorManager,IPreferenceStore ps) {
		super(ps);
		this.colorManager = colorManager;
	}

	

	protected SigmaScanner getScanner () {
		if (scanner == null) {
			scanner = new SigmaScanner();
			scanner.setDefaultReturnToken(SigmaTokens.def);
		}
		return scanner;
	}

	public IPresentationReconciler getPresentationReconciler (
			ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr;
		dr = new SigmaDamageRepair(getScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		return reconciler;
	}
	public IContentAssistant getContentAssistant (ISourceViewer sourceViewer) {
		ContentAssistant a = new ContentAssistant();
		a.setContentAssistProcessor(new MyCompletionProcessor(),
				IDocument.DEFAULT_CONTENT_TYPE);
		a.enableAutoActivation(true);
		a.setAutoActivationDelay(500);
		a.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
		a.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		return a;
	}
	
}