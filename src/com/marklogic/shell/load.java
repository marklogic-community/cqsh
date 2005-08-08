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

import java.io.*;
import java.util.*;

import com.marklogic.xqrunner.*;
import com.marklogic.xqrunner.xdbc.*;
import com.marklogic.xdbc.XDBCException;
import com.marklogic.xdmp.XDMPDocInsertStream;

public class load implements Command {
	public String getName() {
		return "load";
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("usage: load [file path]" + Environment.NEWLINE);
		help.append("Loads a document into Mark Logic from [file path]. The document uri defaults to" + Environment.NEWLINE);
		help.append("the file path." + Environment.NEWLINE);
		return help.toString();
	}

	public void execute(Environment env, String arg) {
		if( arg != null && arg.length() > 0 ) {
			if(env instanceof Shell) {
				Shell shell = (Shell)env;
				//XXX currently we can only load documents into the default database which the
				//user connected to. Need to figure out how to support loading documents into
				//any db.
				String currentDb = shell.getProperties().getString("database");
				String defaultDb = shell.getProperties().getString("default-database");
				if(defaultDb == null && currentDb == null) {
					env.printLine("Default database is not set. Please set and try again.");
					return;
				}
				if(currentDb == null && defaultDb != null) {
					currentDb = defaultDb;
				}
				String port = shell.getProperties().getString("port");
				if(!defaultDb.equals(currentDb)) {
					env.printLine("Current database '" + currentDb + "' is not the default database bound to port '" + port + "'.");
					env.printLine("Can only load documents into '" + defaultDb + "'. Please change current database to confirm.");
					return;
				}
				String[] paths = arg.split("\\s+");
				for(int i = 0; i < paths.length; i++) {
					List files = FileScanner.findFiles(paths[i]);
					if( files != null && files.size() > 0) {
						for(Iterator it = files.iterator(); it.hasNext();) {
							File f = (File)it.next();
							loadDocument(env, f.getAbsolutePath(), f);
						}
					} else {
						env.printLine("No well-formed XML file(s) found at location " + paths[i] + ".");
					}
				}
			}
		} else {
			env.printLine("You must specify a file path to load.");
		}
	}

    private void loadDocument(Environment env, String uri, File file) {
        try {
            env.printLine("Loading file " + file.getAbsolutePath() + "...");
            XdbcDataSource dataSource = (XdbcDataSource)env.getDataSource();
            XDMPDocInsertStream dis = dataSource.getDocInsertStream(uri);
            InputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[100000];
            int byteCount = 0;
            int rc;
            while ((rc = inputStream.read(buffer)) >= 0) {
                dis.write(buffer, 0, rc);
                byteCount += rc;
            }
            dis.flush();
            dis.commit();
            dis.close();
            inputStream.close();
            env.printLine("Done. Loaded " + byteCount + " bytes.");
        } catch(XDBCException e) {
            env.printLine("Failed to load document: " + e.getMessage());
        } catch(XQException e) {
            env.printLine("Failed to load document: " + e.getMessage());
        } catch(IOException e) {
            env.printLine("Failed to load document: " + e.getMessage());
        }
    }
}
