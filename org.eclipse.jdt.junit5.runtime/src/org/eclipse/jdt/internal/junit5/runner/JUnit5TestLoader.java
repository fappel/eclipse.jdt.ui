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

import static org.junit.gen5.engine.discovery.ClassSelector.forClass;
import static org.junit.gen5.engine.discovery.MethodSelector.forMethod;
import static org.junit.gen5.launcher.main.TestDiscoveryRequestBuilder.request;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.internal.junit.runner.ITestLoader;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.junit.gen5.engine.DiscoverySelector;

public class JUnit5TestLoader implements ITestLoader {

  @Override
  @SuppressWarnings("rawtypes")
  public ITestReference[] loadTests( Class[] testClasses,
                                     String testName,
                                     String[] failureNames,
                                     RemoteTestRunner listener )
  {
    Set<ITestReference> result = new HashSet<>();
    Stream.of(testClasses).forEach(testClass -> {
      if( !"".equals(testName) && testName != null ) {
        result.add(createTestReference(testName, forMethod(testClass, testName)));
      } else {
        result.add(createTestReference(testClass.getName(),forClass(testClass)));
      }
    } );
    return result.toArray( new ITestReference[ result.size() ] );
  }

  private TestReference createTestReference( String name, DiscoverySelector ... selectors ) {
    return new TestReference(name, request().select(selectors).build());
  }
}
