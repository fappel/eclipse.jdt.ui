/**
 * Copyright (c) 2014 - 2016 Frank Appel
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Frank Appel - initial API and implementation
 */
package org.eclipse.jdt.internal.junit5.runner;

import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.IVisitsTestTrees;
import org.eclipse.jdt.internal.junit.runner.TestExecution;
import org.junit.gen5.launcher.Launcher;
import org.junit.gen5.launcher.TestDiscoveryRequest;
import org.junit.gen5.launcher.TestPlan;
import org.junit.gen5.launcher.main.LauncherFactory;

class TestReference implements ITestReference {
  
  private TestDiscoveryRequest fDiscoveryRequest;
  private Launcher fLauncher;
  private TestPlan fTestPlan;
  private String fName;

  TestReference(String name, TestDiscoveryRequest discoveryRequest) {
    fName = name;
    fLauncher = LauncherFactory.create();
    fDiscoveryRequest = discoveryRequest;
    fTestPlan = fLauncher.discover( discoveryRequest );
  }

  @Override
  public int countTestCases() {
    return ( int )fTestPlan.countTestIdentifiers( identifier -> fName.equals(identifier.getDisplayName()));
  }

  @Override
  public void sendTree( IVisitsTestTrees notified ) {
    notified.visitTreeEntry(getIdentifier(), false, 1);
  }

  @Override
  public void run( TestExecution execution ) {
    fLauncher.registerTestExecutionListeners(new StatusReporter(execution.getListener()));
    fLauncher.execute(fDiscoveryRequest);
  }

  @Override
  public ITestIdentifier getIdentifier() {
    return () -> fName;
  }
}