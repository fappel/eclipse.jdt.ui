/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.util.Resources;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.internal.ui.compare.JavaTokenComparator;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;

/**
 * A proposal for quick fixes and quick assist that work on a single compilation unit.
 * Either a compilation unit change is directly passed in the constructor or method {@link #addEdits(IDocument, TextEdit)} is overridden
 * to provide the text changes that are applied to the document when the proposal is
 * evaluated.
 * <p>
 * The proposal takes care of the preview of the changes as proposal information.
 * </p>
 * @since 3.0
 */
public class CUCorrectionProposal extends ChangeCorrectionProposal  {

	private ICompilationUnit fCompilationUnit;
	private ImportRewrite fImportRewrite;
	private boolean fIsInitialized;
	
	/**
	 * Constructs a compilation unit correction proposal.
	 * @param name The name that is displayed in the proposal selection dialog.
	 * @param cu The compilation unit on that the change works. 
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public CUCorrectionProposal(String name, ICompilationUnit cu, int relevance, Image image) {
		super(name, createCompilationUnitChange(name, cu), relevance, image);
		fCompilationUnit= cu;
		fImportRewrite= null;
		fIsInitialized= false;
	}
	
	/**
	 * Constructs a compilation unit correction proposal.
	 * @param name The name that is displayed in the proposal selection dialog.
	 * @param change The change that is executed when the proposal is applied.  
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public CUCorrectionProposal(String name, CompilationUnitChange change, int relevance, Image image) {
		super(name, change, relevance, image);
		fCompilationUnit= change.getCompilationUnit();
		fIsInitialized= false;
	}
	
	private static Change createCompilationUnitChange(String name, ICompilationUnit cu) {
		CompilationUnitChange change= new CompilationUnitChange(name, cu);
		change.setEdit(new MultiTextEdit());
		change.setSaveMode(TextFileChange.LEAVE_DIRTY);
		return change;
	}
	
	/**
	 * Initializes the compilation unit change that is invoked when the proposal is
	 * applied. This method is only called once, either when the a preview of
	 * the change is requested or when the change is invoked. 
	 * The default implementation calls {@link #addEdits(IDocument, TextEdit)}.
	 * @throws CoreException Thrown when the initialization fails.
	 */
	protected void initializeCompilationUnitChange() throws CoreException {
		if (fIsInitialized) {
			return;
		}
		fIsInitialized= true;
		
		TextEdit rootEdit= getCompilationUnitChange().getEdit();
		if (rootEdit != null) {
			ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
			IPath path= getCompilationUnit().getPath();
			manager.connect(path, null);
			try {
				IDocument document= manager.getTextFileBuffer(path).getDocument();
				addEdits(document, rootEdit);
				if (fImportRewrite != null && !fImportRewrite.isEmpty()) {
					rootEdit.addChild(fImportRewrite.createEdit(document));
				}
			} finally {
				manager.disconnect(path, null);
			}
		}
	}
	
	/**
	 * Called when the <code>CompilationUnitChange</code> is initialized. Subclasses can override to
	 * add text edits to root edit of the change.
	 * @param document Content of the underlying compilation unit. To be accessed read only.
	 * @param editRoot The root edit to add all edits to
	 * @throws CoreException
	 */
	protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
		if (false) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, "Implementors can throw an exception", null)); //$NON-NLS-1$
		}
	}

	// import management

	/**
	 * Returns the import rewriter used for this compilation unit.
	 */
	public ImportRewrite getImportRewrite() throws CoreException {
		if (fImportRewrite == null) {
			fImportRewrite= new ImportRewrite(getCompilationUnit());
		}
		return fImportRewrite;
	}
	
	/**
	 * Sets the import rewriter used for this compilation unit.
	 */
	public void setImportRewrite(ImportRewrite rewrite) {
		fImportRewrite= rewrite;
	}
				
	/*
	 * @see ICompletionProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		StringBuffer buf= new StringBuffer();
		
		try {
			initializeCompilationUnitChange();
			CompilationUnitChange change= getCompilationUnitChange();

			IDocument previewContent= change.getPreviewDocument();
			String currentConentString= change.getCurrentContent();
			
			ITokenComparator leftSide= new JavaTokenComparator(previewContent.get(), true); 
			ITokenComparator rightSide= new JavaTokenComparator(currentConentString, true);
			
			RangeDifference[] differences= RangeDifferencer.findRanges(leftSide, rightSide);
			for (int i= 0; i < differences.length; i++) {
				RangeDifference curr= differences[i];
				int start= leftSide.getTokenStart(curr.leftStart());
				int end= leftSide.getTokenStart(curr.leftEnd());
				if (curr.kind() == RangeDifference.CHANGE && curr.leftLength() > 0) {
					buf.append("<b>"); //$NON-NLS-1$
					appendContent(previewContent, start, end, buf, false);
					buf.append("</b>"); //$NON-NLS-1$
				} else if (curr.kind() == RangeDifference.NOCHANGE) {
					appendContent(previewContent, start, end, buf, true);
				}
			}			
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		return buf.toString();
	}
	
	private final int surroundLines= 1;

	private void appendContent(IDocument text, int startOffset, int endOffset, StringBuffer buf, boolean surroundLinesOnly) throws BadLocationException {
		int startLine= text.getLineOfOffset(startOffset);
		int endLine= text.getLineOfOffset(endOffset);
		
		boolean dotsAdded= false;
		if (surroundLinesOnly && startOffset == 0) { // no surround lines for the top no-change range
			startLine= Math.max(endLine - surroundLines, 0);
			buf.append("...<br>"); //$NON-NLS-1$
			dotsAdded= true;
		}
		
		for (int i= startLine; i <= endLine; i++) {
			if (surroundLinesOnly) {
				if ((i - startLine > surroundLines) && (endLine - i > surroundLines)) {
					if (!dotsAdded) {
						buf.append("...<br>"); //$NON-NLS-1$
						dotsAdded= true;
					} else if (endOffset == text.getLength()) {
						return; // no surround lines for the bottom no-change range
					}
					continue;
				}
			}
			
			IRegion lineInfo= text.getLineInformation(i);
			int start= lineInfo.getOffset();
			int end= start + lineInfo.getLength();

			int from= Math.max(start, startOffset);
			int to= Math.min(end, endOffset);
			String content= text.get(from, to - from);
			if (surroundLinesOnly && (from == start) && Strings.containsOnlyWhitespaces(content)) {
				continue; // ignore empty lines except when range started in the middle of a line
			}
			buf.append(content);
			if (to == end && to != endOffset) { // new line when at the end of the line, and not end of range
				buf.append("<br>"); //$NON-NLS-1$
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal#performChange(org.eclipse.jface.text.IDocument, org.eclipse.ui.IEditorPart)
	 */
	protected void performChange(IEditorPart activeEditor, IDocument document) throws CoreException {
		initializeCompilationUnitChange();
		super.performChange(activeEditor, document);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(IDocument document) {
		try {
			boolean canEdit= performValidateEdit();
			if (!canEdit) {
				return;
			}
			ICompilationUnit unit= getCompilationUnit();
			IEditorPart part= EditorUtility.isOpenInEditor(unit);
			if (part == null) {
				part= EditorUtility.openInEditor(unit, true);
				if (part != null) {
					document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
				}
			}
			IWorkbenchPage page= JavaPlugin.getActivePage();
			if (page != null && part != null) {
				page.bringToTop(part);
			}
			if (part != null) {
				part.setFocus();
			}
			performChange(part, document);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, CorrectionMessages.getString("CUCorrectionProposal.error.title"), CorrectionMessages.getString("CUCorrectionProposal.error.message"));  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	private boolean performValidateEdit() {
		ICompilationUnit unit= getCompilationUnit();
		IStatus status= Resources.makeCommittable(unit.getResource(), null);
		if (!status.isOK()) {
			String label= CorrectionMessages.getString("CUCorrectionProposal.error.title"); //$NON-NLS-1$
			String message= CorrectionMessages.getString("CUCorrectionProposal.error.message"); //$NON-NLS-1$
			ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), label, message, status);
			return false;
		}
		return true;
	}

	/**
	 * Gets the compilation unit change that is invoked when the change is applied.
	 * @return Returns a CompilationUnitChange
	 */
	public CompilationUnitChange getCompilationUnitChange() {
		return (CompilationUnitChange) getChange();
	}
	
	/**
	 * Returns the compilationUnit.
	 * @return ICompilationUnit
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/**
	 * @return Returns the preview of the changed compilation unit
	 */
	public String getPreviewContent() throws CoreException {
		initializeCompilationUnitChange();
		CompilationUnitChange change= getCompilationUnitChange();
		return change.getPreviewContent();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		try {
			return getPreviewContent();
		} catch (CoreException e) {
		}
		return super.toString();
	}


}
