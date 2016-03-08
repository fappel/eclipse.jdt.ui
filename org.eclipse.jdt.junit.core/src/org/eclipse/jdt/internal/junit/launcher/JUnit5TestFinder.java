package org.eclipse.jdt.internal.junit.launcher;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class JUnit5TestFinder implements ITestFinder {

	private static class Annotation {

		private static final Annotation TEST= new Annotation("org.junit.gen5.api.Test"); //$NON-NLS-1$

		private final String fName;

		private Annotation(String name) {
			fName= name;
		}

		private boolean annotates(IAnnotationBinding[] annotations) {
			for (int i= 0; i < annotations.length; i++) {
				ITypeBinding annotationType= annotations[i].getAnnotationType();
				if (annotationType != null && (annotationType.getQualifiedName().equals(fName))) {
					return true;
				}
			}
			return  false;
		}

		public boolean annotatesAtLeastOneMethod(ITypeBinding type) {
			while (type != null) {
				IMethodBinding[] declaredMethods= type.getDeclaredMethods();
				for (int i= 0; i < declaredMethods.length; i++) {
					IMethodBinding curr= declaredMethods[i];
					if (annotates(curr.getAnnotations())) {
						return true;
					}
				}
				type= type.getSuperclass();
			}
			return false;
		}
	}

	
	@Override
	public void findTestsInContainer(IJavaElement element, Set<IType> result, IProgressMonitor pm) throws CoreException {
	}

	@Override
	public boolean isTest(IType type) throws JavaModelException {
		return internalIsTest(type, null);
	}

	private boolean internalIsTest(IType type, IProgressMonitor monitor) throws JavaModelException {
		if (isAccessibleClass(type)) {
			ASTParser parser= ASTParser.newParser(AST.JLS8);
			if (type.getCompilationUnit() != null) {
				parser.setSource(type.getCompilationUnit());
			} else if (!isAvailable(type.getSourceRange())) { // class file with no source
				parser.setProject(type.getJavaProject());
				IBinding[] bindings= parser.createBindings(new IJavaElement[] { type }, monitor);
				if (bindings.length == 1 && bindings[0] instanceof ITypeBinding) {
					ITypeBinding binding= (ITypeBinding) bindings[0];
					return isTest(binding);
				}
				return false;
			} else {
				parser.setSource(type.getClassFile());
			}
			parser.setFocalPosition(0);
			parser.setResolveBindings(true);
			CompilationUnit root= (CompilationUnit) parser.createAST(monitor);
			ASTNode node= root.findDeclaringNode(type.getKey());
			if (node instanceof TypeDeclaration) {
				ITypeBinding binding= ((TypeDeclaration) node).resolveBinding();
				if (binding != null) {
					return isTest(binding);
				}
			}
		}
		return false;

	}
	
	public static boolean isAccessibleClass(IType type) throws JavaModelException {
		int flags= type.getFlags();
		if (Flags.isInterface(flags)) {
			return false;
		}
		IJavaElement parent= type.getParent();
		while (true) {
			if (parent instanceof ICompilationUnit || parent instanceof IClassFile) {
				return true;
			}
			if (!(parent instanceof IType) || !Flags.isStatic(flags) || Flags.isPrivate(flags)) {
				return false;
			}
			flags= ((IType) parent).getFlags();
			parent= parent.getParent();
		}
	}

    private static boolean isAvailable(ISourceRange range) {
		return range != null && range.getOffset() != -1;
	}


	private boolean isTest(ITypeBinding binding) {
		if (Modifier.isAbstract(binding.getModifiers()))
			return false;

		if (Annotation.TEST.annotatesAtLeastOneMethod(binding)) {
			return true;
		}
		return false;
	}
}
