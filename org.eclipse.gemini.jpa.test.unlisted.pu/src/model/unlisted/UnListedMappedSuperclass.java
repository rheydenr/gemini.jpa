/*******************************************************************************
 * Copyright (c) 2010 Oracle.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at 
 *     http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *     mkeith - Gemini JPA tests 
 ******************************************************************************/
package model.unlisted;

import javax.persistence.MappedSuperclass;

/**
 * Test JPA model class
 * 
 * @author mkeith
 */
@MappedSuperclass
public class UnListedMappedSuperclass {

    String mappedState;
    
    public String getMappedState() { return mappedState; }
    public void setMappedState(String state) { this.mappedState = state; }

}
