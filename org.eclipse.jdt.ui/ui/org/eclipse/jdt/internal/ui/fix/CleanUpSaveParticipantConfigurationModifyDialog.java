/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeFormatingTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CodeStyleTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MemberAccessesTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MissingCodeTabPage;
import org.eclipse.jdt.internal.ui.preferences.cleanup.UnnecessaryCodeTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.ModifyDialogTabPage.IModificationListener;

public class CleanUpSaveParticipantConfigurationModifyDialog extends StatusDialog implements IModificationListener {
	
	private static final String DS_KEY_PREFERRED_WIDTH= "clean_up_save_particpant_modify_dialog.preferred_width"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_HEIGHT= "clean_up_save_particpant_modify_dialog.preferred_height"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_X= "clean_up_save_particpant_modify_dialog.preferred_x"; //$NON-NLS-1$
	private static final String DS_KEY_PREFERRED_Y= "clean_up_save_particpant_modify_dialog.preferred_y"; //$NON-NLS-1$
	private static final String DS_KEY_LAST_FOCUS= "clean_up_save_particpant_modify_dialog.last_focus"; //$NON-NLS-1$
	
	private static final int APPLY_BUTTON_ID= IDialogConstants.CLIENT_ID;
	
	private final Map fWorkingValues;
	private Map fOrginalValues;
	private final List fTabPages;
	private final IDialogSettings fDialogSettings;
	private TabFolder fTabFolder;
	private Button fApplyButton;
	
	public CleanUpSaveParticipantConfigurationModifyDialog(Shell parentShell, Map settings, String title) {
		super(parentShell);
		
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		
		setTitle(title);
		fWorkingValues= settings;
		fOrginalValues= new HashMap(settings);
		setStatusLineAboveButtons(false);
		fTabPages= new ArrayList();
		fDialogSettings= JavaPlugin.getDefault().getDialogSettings();
	}
	
	public void create() {
		super.create();
		int lastFocusNr= 0;
		try {
			lastFocusNr= fDialogSettings.getInt(DS_KEY_LAST_FOCUS);
			if (lastFocusNr < 0)
				lastFocusNr= 0;
			if (lastFocusNr > fTabPages.size() - 1)
				lastFocusNr= fTabPages.size() - 1;
		} catch (NumberFormatException x) {
			lastFocusNr= 0;
		}
		
		fTabFolder.setSelection(lastFocusNr);
		((ModifyDialogTabPage)fTabFolder.getSelection()[0].getData()).setInitialFocus();
	}
	
	protected Control createDialogArea(Composite parent) {
		final Composite composite= (Composite)super.createDialogArea(parent);
		
		fTabFolder= new TabFolder(composite, SWT.NONE);
		fTabFolder.setFont(composite.getFont());
		fTabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		addTabPage(SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_CodeStyle_TabPage, new CodeStyleTabPage(this, fWorkingValues, true));
		addTabPage(SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_MemberAccesses_TabPage, new MemberAccessesTabPage(this, fWorkingValues, true));
		addTabPage(SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_UnnecessaryCode_TabPage, new UnnecessaryCodeTabPage(this, fWorkingValues, true));
		addTabPage(SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_MissingCode_TabPage, new MissingCodeTabPage(this, fWorkingValues, true));
		addTabPage(SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_CodeOrganizing_TabPage, new CodeFormatingTabPage(this, fWorkingValues, true));
		
		applyDialogFont(composite);
		
		fTabFolder.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			
			public void widgetSelected(SelectionEvent e) {
				final TabItem tabItem= (TabItem)e.item;
				final ModifyDialogTabPage page= (ModifyDialogTabPage)tabItem.getData();
				fDialogSettings.put(DS_KEY_LAST_FOCUS, fTabPages.indexOf(page));
				page.makeVisible();
			}
		});
		
		updateStatus(StatusInfo.OK_STATUS);
		
		return composite;
	}
	
	public void updateStatus(IStatus status) {
		if (status == null) {
			super.updateStatus(StatusInfo.OK_STATUS);
		} else {
			super.updateStatus(status);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#getInitialSize()
	 */
	protected Point getInitialSize() {
		Point initialSize= super.getInitialSize();
		try {
			int lastWidth= fDialogSettings.getInt(DS_KEY_PREFERRED_WIDTH);
			if (initialSize.x > lastWidth)
				lastWidth= initialSize.x;
			int lastHeight= fDialogSettings.getInt(DS_KEY_PREFERRED_HEIGHT);
			if (initialSize.y > lastHeight)
				lastHeight= initialSize.x;
			return new Point(lastWidth, lastHeight);
		} catch (NumberFormatException ex) {
		}
		return initialSize;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.window.Window#getInitialLocation(org.eclipse.swt.graphics.Point)
	 */
	protected Point getInitialLocation(Point initialSize) {
		try {
			return new Point(fDialogSettings.getInt(DS_KEY_PREFERRED_X), fDialogSettings.getInt(DS_KEY_PREFERRED_Y));
		} catch (NumberFormatException ex) {
			return super.getInitialLocation(initialSize);
		}
	}
	
	public boolean close() {
		final Rectangle shell= getShell().getBounds();
		
		fDialogSettings.put(DS_KEY_PREFERRED_WIDTH, shell.width);
		fDialogSettings.put(DS_KEY_PREFERRED_HEIGHT, shell.height);
		fDialogSettings.put(DS_KEY_PREFERRED_X, shell.x);
		fDialogSettings.put(DS_KEY_PREFERRED_Y, shell.y);
		
		return super.close();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		applyPressed();
		super.okPressed();
	}
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == APPLY_BUTTON_ID) {
			applyPressed();
		} else {
			super.buttonPressed(buttonId);
		}
	}
	
	private void applyPressed() {
		fOrginalValues= new HashMap(fWorkingValues);
		updateStatus(StatusInfo.OK_STATUS);
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		fApplyButton= createButton(parent, APPLY_BUTTON_ID, SaveParticipantMessages.CleanUpSaveParticipantConfigurationModifyDialog_Apply_Button, false);
		fApplyButton.setEnabled(false);
		
		GridLayout layout= (GridLayout)parent.getLayout();
		layout.numColumns++;
		layout.makeColumnsEqualWidth= false;
		Label label= new Label(parent, SWT.NONE);
		GridData data= new GridData();
		data.widthHint= layout.horizontalSpacing;
		label.setLayoutData(data);
		super.createButtonsForButtonBar(parent);
	}
	
	protected final void addTabPage(String title, ModifyDialogTabPage tabPage) {
		final TabItem tabItem= new TabItem(fTabFolder, SWT.NONE);
		applyDialogFont(tabItem.getControl());
		tabItem.setText(title);
		tabItem.setData(tabPage);
		tabItem.setControl(tabPage.createContents(fTabFolder));
		fTabPages.add(tabPage);
	}
	
	protected void updateButtonsEnableState(IStatus status) {
		super.updateButtonsEnableState(status);
		if (fApplyButton != null && !fApplyButton.isDisposed()) {
			fApplyButton.setEnabled(hasChanges() && !status.matches(IStatus.ERROR));
		}
	}
	
	private boolean hasChanges() {
		for (Iterator iterator= fWorkingValues.keySet().iterator(); iterator.hasNext();) {
			String key= (String)iterator.next();
			if (!fWorkingValues.get(key).equals(fOrginalValues.get(key)))
				return true;
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void valuesModified() {
		updateStatus(StatusInfo.OK_STATUS);
	}
}