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
package org.eclipse.jdt.junit.model;

/**
 * Represents a test case element.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * <strong>EXPERIMENTAL</strong> This class or interface has been added as part
 * of a work in progress. This API may change at any given time. Please do not
 * use this API without consulting with the JDT/UI team.
 * 
 * @since 3.3
 */
public interface ITestCaseElement extends ITestElement {
	
	/**
	 * Returns the name of the test method.
	 * 
	 * @return returns the name of the test method.
	 */
	public String getTestMethodName();

	/**
	 * Returns the qualified type name of the class the test is contained in.
	 * 
	 * @return the qualified type name of the class the test is contained in.
	 */
	public String getTestClassName();
	
}