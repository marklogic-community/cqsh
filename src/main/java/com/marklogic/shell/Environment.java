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

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ResultSequence;

/**
 * The interface that Enviroments must implement in order to support running
 * commands.
 * 
 * @author Andrew Bruno <aeb@qnot.org>
 */
public interface Environment {
    /**
     * Platform dependent newline char.
     */
    public static final String NEWLINE = System.getProperty("line.separator");

    /**
     * Default system package to search for commands
     */
    public static final String SYSTEM_PATH = "com.marklogic.shell.command";

    /**
     * Ouput a message.
     * 
     * @param message
     */
    public void output(String message);

    /**
     * Ouput a message on single line.
     * 
     * @param message
     */
    public void outputLine(String message);

    /**
     * Output an error that was caused by an exception.
     * 
     * @param exception
     */
    public void outputException(Exception exception);

    /**
     * Output an error message.
     * 
     * @param message
     */
    public void outputError(String message);

    /**
     * Exit the environment cleanly.
     */
    public void exit();

    /**
     * Exit the environment with an error.
     * 
     * @param message
     */
    public void exitWithError(String message);

    /**
     * Outputs the results of a query
     * 
     * @param result
     */
    public void outputResultSequence(ResultSequence result);

    /**
     * Returns a ContentSource for interacting with the database
     */
    public ContentSource getContentSource();

    /**
     * The configuration properties for the environment.
     */
    public PropertiesConfiguration getProperties();
}
