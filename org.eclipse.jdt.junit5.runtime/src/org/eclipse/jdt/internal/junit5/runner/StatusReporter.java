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

import static org.eclipse.jdt.internal.junit.runner.MessageIds.ASSUMPTION_FAILED_TEST_PREFIX;
import static org.eclipse.jdt.internal.junit.runner.MessageIds.IGNORED_TEST_PREFIX;
import static org.eclipse.jdt.internal.junit.runner.MessageIds.TEST_ERROR;
import static org.eclipse.jdt.internal.junit.runner.MessageIds.TEST_FAILED;
import static org.junit.gen5.engine.TestExecutionResult.Status.FAILED;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;
import org.junit.gen5.engine.TestExecutionResult;
import org.junit.gen5.engine.TestExecutionResult.Status;
import org.junit.gen5.launcher.TestExecutionListener;
import org.junit.gen5.launcher.TestIdentifier;

class StatusReporter implements TestExecutionListener {

  private final IListensToTestExecutions fReceiver;

  public StatusReporter(IListensToTestExecutions receiver) {
    this.fReceiver = receiver;
  }

  @Override
  public void executionStarted(TestIdentifier identifier) {
    fReceiver.notifyTestStarted(identifier::getDisplayName);
  }
  
  @Override
  public void executionSkipped(TestIdentifier identifier, String reason) {
    fReceiver.notifyTestStarted(() -> IGNORED_TEST_PREFIX+identifier.getDisplayName()+"["+reason+"]");
    fReceiver.notifyTestEnded(identifier::getDisplayName);
  }
  
  @Override
  public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
    if( result.getStatus() == Status.ABORTED ) {
      ITestIdentifier id = () -> ASSUMPTION_FAILED_TEST_PREFIX+identifier.getDisplayName();
      fReceiver.notifyTestFailed( createFailure( id, result ) );
    } else if ( result.getStatus() == FAILED ) {
      fReceiver.notifyTestFailed( createFailure( identifier::getDisplayName, result ) );
    }
    fReceiver.notifyTestEnded(identifier::getDisplayName);
  }

  private TestReferenceFailure createFailure( ITestIdentifier id, TestExecutionResult result ) {
    return new TestReferenceFailure(id, getFailureType( result ), getTrace( result.getThrowable() ) );
  }

  private String getFailureType( TestExecutionResult result ) {
    Optional<Throwable> failureCause = result.getThrowable();
    return failureCause.isPresent() && failureCause.get() instanceof AssertionError ? TEST_FAILED : TEST_ERROR;
  }

  private static String getTrace(Optional<Throwable> failureCause) {
    if( !failureCause.isPresent() ) {
      return "";
    }
    StringWriter result = new StringWriter();
    PrintWriter writer = new PrintWriter(result);
    failureCause.get().printStackTrace(writer);
    return result.toString();
  }
}