/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyPackageFragmentRootChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CopyResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

public class CopyRefactoring extends ReorgRefactoring {

	private Set fAutoGeneratedNewNames;
	private final INewNameQueries fCopyQueries;
	private final IPackageFragmentRootManipulationQuery fRootManipulationQuery;
	
	private CopyRefactoring(List elements, INewNameQueries copyQueries, IPackageFragmentRootManipulationQuery updateClasspathQuery){
		super(elements);
		Assert.isNotNull(copyQueries);
		fCopyQueries= copyQueries;
		fAutoGeneratedNewNames=  new HashSet(2);
		fRootManipulationQuery= updateClasspathQuery;
	}
	
	public static CopyRefactoring create(List elements, INewNameQueries copyQueries, IPackageFragmentRootManipulationQuery updateClasspathQuery) throws JavaModelException{
		if (! isAvailable(elements))
			return null;
		return new CopyRefactoring(elements, copyQueries, updateClasspathQuery);
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("CopyRefactoring.copy_elements"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public final RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			RefactoringStatus result= new RefactoringStatus();
			result.merge(validateModifiesFiles());
			return result;
		} catch (JavaModelException e){	
			throw e;
		} catch (CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}	
	}
	
	private IFile[] getAllFilesToModify() throws CoreException{
		List result= new ArrayList();
		if (getDestination() instanceof IPackageFragment)
			result.addAll(Arrays.asList(ResourceUtil.getFiles(collectCus())));
		return (IFile[]) result.toArray(new IFile[result.size()]);
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	boolean isValidDestinationForCusAndFiles(Object dest) throws JavaModelException {
		return getDestinationForCusAndFiles(dest) != null;
	}
	
	//-----
	private static boolean isNewNameOk(IPackageFragment dest, String newName) {
		return ! dest.getCompilationUnit(newName).exists();
	}
	
	private static boolean isNewNameOk(IContainer container, String newName) {
		return container.findMember(newName) == null;
	}

	private static boolean isNewNameOk(IPackageFragmentRoot root, String newName) {
		return ! root.getPackageFragment(newName).exists() ;
	}
	
	private String createNewName(ICompilationUnit cu, IPackageFragment dest){
		if (isNewNameOk(dest, cu.getElementName()))
			return null;
		if (! ReorgUtils.isParent(cu, dest))
			return null;
		int i= 1;
		while (true){
			String newName;
			if (i == 1)
				newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.cu.copyOf1", //$NON-NLS-1$
							cu.getElementName());
			else	
				newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.cu.copyOfMore", //$NON-NLS-1$
							new String[]{String.valueOf(i), cu.getElementName()});
			if (isNewNameOk(dest, newName) && ! fAutoGeneratedNewNames.contains(newName)){
				fAutoGeneratedNewNames.add(newName);
				return newName;
			}
			i++;
		}
	}
	
	private String createNewName(IResource res, IContainer container){
		if (isNewNameOk(container, res.getName()))
			return null;
		if (! ReorgUtils.isParent(res, container))
			return null;
		int i= 1;
		while (true){
			String newName;
			if (i == 1)
				newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.resource.copyOf1", //$NON-NLS-1$
							res.getName());
			else
				newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.resource.copyOfMore", //$NON-NLS-1$
							new String[]{String.valueOf(i), res.getName()});
			if (isNewNameOk(container, newName) && ! fAutoGeneratedNewNames.contains(newName)){
				fAutoGeneratedNewNames.add(newName);
				return newName;
			}
			i++;
		}	
	}
	
	private String createNewName(IPackageFragment pack, IPackageFragmentRoot root){
		if (isNewNameOk(root, pack.getElementName()))
			return null;
		if (! ReorgUtils.isParent(pack, root))
			return null;
		int i= 1;
		while (true){
			String newName;
			if (i == 0)
				newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.package.copyOf1", //$NON-NLS-1$
							pack.getElementName());
			else
				newName= RefactoringCoreMessages.getFormattedString("CopyRefactoring.package.copyOfMore", //$NON-NLS-1$
							new String[]{String.valueOf(i), pack.getElementName()});
			if (isNewNameOk(root, newName) && ! fAutoGeneratedNewNames.contains(newName)){
				fAutoGeneratedNewNames.add(newName);
				return newName;
			}
			i++;
		}	
	}

	IChange createChange(IProgressMonitor pm, IPackageFragmentRoot root) throws JavaModelException{
		IResource res= root.getResource();
		IProject destinationProject= getDestinationForSourceFolders(getDestination());
		String newName= createNewName(res, destinationProject);
		if (newName == null )
			newName= res.getName();
		INewNameQuery nameQuery= fCopyQueries.createStaticQuery(newName);
		return new CopyPackageFragmentRootChange(root, destinationProject, nameQuery,  fRootManipulationQuery);
	}
	
	IChange createChange(IProgressMonitor pm, IPackageFragment pack) throws JavaModelException{
		IPackageFragmentRoot root= getDestinationForPackages(getDestination());
		String newName= createNewName(pack, root);
		if (newName == null || JavaConventions.validatePackageName(newName).getSeverity() < IStatus.ERROR){
			INewNameQuery nameQuery;
			if (newName == null)
				nameQuery= fCopyQueries.createNullQuery();
			else
				nameQuery= fCopyQueries.createNewPackageNameQuery(pack);
			return new CopyPackageChange(pack, root, nameQuery);
		} else {
			if (root.getResource() instanceof IContainer){
				IContainer dest= (IContainer)root.getResource();
				IResource res= pack.getResource();
				INewNameQuery nameQuery= fCopyQueries.createNewResourceNameQuery(res);
				return new CopyResourceChange(res, dest, nameQuery);
			}else
				return new NullChange();
		}	
	}

	IChange createChange(IProgressMonitor pm, final IResource res) throws JavaModelException{
		IContainer dest= getDestinationForResources(getDestination());
		INewNameQuery nameQuery;
		if (createNewName(res, dest) == null)
			nameQuery= fCopyQueries.createNullQuery();
		else
			nameQuery= fCopyQueries.createNewResourceNameQuery(res);
		return new CopyResourceChange(res, dest, nameQuery);
	}
	
	IChange createChange(IProgressMonitor pm, ICompilationUnit cu) throws JavaModelException{
		Object dest= getDestinationForCusAndFiles(getDestination());
		if (dest instanceof IPackageFragment)
			return copyCuToPackage(cu, (IPackageFragment)dest);
		 else 
			return copyFileToContainer(cu, (IContainer)dest); //cast should be checked before - in preconditions
	}
	
	private IChange copyFileToContainer(ICompilationUnit cu, IContainer dest) {
		IResource res= ResourceUtil.getResource(cu);
		INewNameQuery nameQuery;
		if (createNewName(res, dest) == null)
			nameQuery= fCopyQueries.createNullQuery();
		else	
			nameQuery= fCopyQueries.createNewResourceNameQuery(res);
		return new CopyResourceChange(res, dest, nameQuery);
	}
	
	private IChange copyCuToPackage(ICompilationUnit cu, IPackageFragment dest) {
		//XXX workaround for bug 31998 we will have to disable renaming of linked packages (and cus)
		IResource res= ResourceUtil.getResource(cu);
		if (res != null && res.isLinked()){
			if (ResourceUtil.getResource(dest) instanceof IContainer)
				return copyFileToContainer(cu, (IContainer)ResourceUtil.getResource(dest));
		}
		
		String newName= createNewName(cu, dest);
		Change simpleCopy= new CopyCompilationUnitChange(cu, dest, fCopyQueries.createStaticQuery(newName));
		if (newName == null || newName.equals(cu.getElementName()))
			return simpleCopy;
		
		try {
			IPath newPath= ResourceUtil.getResource(cu).getParent().getFullPath().append(newName);				
			INewNameQuery nameQuery= fCopyQueries.createNewCompilationUnitNameQuery(cu);
			return new CreateCopyOfCompilationUnitChange(newPath, cu.getSource(), cu, nameQuery); //XXX
		} catch(CoreException e) {
			return simpleCopy; //fallback - no ui here
		}
	}
}
