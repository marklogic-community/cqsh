/*
 * Copyright 2005 Andrew Bruno <aeb@qnot.org> 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.marklogic.shell;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.marklogic.xqrunner.XQDataSource;
import com.marklogic.xqrunner.XQException;
import com.marklogic.xqrunner.XQuery;

/**
 * The interface that Enviroments must implement in order to support running commands.
 *
 * @author Andrew Bruno <aeb@qnot.org>
 */
public interface Environment {
	/**
	 * Platform dependent newline char.
	 */
	public static final String NEWLINE = System.getProperty("line.separator");

	/**
	 * Prints the default help message.
	 */
    public void printHelp();

    /**
     * Prints a message along with the default help message.
     * @param message
     */
    public void printHelp(String message);

    /**
     * Prints a message.
     * @param message
     */
    public void print(String message);

    /**
     * Prints a message with a line break.
     * @param message
     */
    public void printLine(String message);

    /**
     * Prints an error that was caused by an exception.
     * @param exception
     */
	public void printError(Exception exception);

	/**
	 * Prints an error message.
	 * @param message
	 */
	public void printError(String message);

	/**
	 * Exit the envronment cleanly.
	 */
	public void exit();

	/**
	 * Exit the environment with an error.
	 * @param message
	 */
	public void exitWithError(String message);

	/**
	 * Run an XQuery statement.
	 * @param xquery
	 */
    public void runXQuery(XQuery xquery);
    
    /**
     * Returns an XQDataSource.
     * @throws XQException
     */
    public XQDataSource getDataSource() throws XQException;
    
    /**
     * The configuration properties for the environment.
     */
    public PropertiesConfiguration getProperties();
}
