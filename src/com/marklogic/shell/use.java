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

import com.marklogic.xqrunner.*;

public class use implements Command {
	
	public String getName() {
		return "user";
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("usage: use [database]" + Environment.NEWLINE);
		help.append("Eval xquery in specified database." + Environment.NEWLINE);
		return help.toString();
	}

	public void execute(Environment env, String arg) {
		if( arg != null && arg.length() > 0 ) {
			if(env instanceof Shell) {
				Shell shell = (Shell)env;
				try {
					XQDataSource dataSource = shell.getDataSource();
					String query = "define variable $db as xs:string external "
						          +"let $test := xdmp:database($db) return ()";
					XQuery xquery = dataSource.newQuery(query);
					xquery.setVariable(dataSource.newVariable("db", XQVariableType.XS_STRING, arg));
					XQRunner runner = dataSource.newSyncRunner();
					XQResult result = runner.runQuery(xquery);
					env.printLine("Database changed.");
					shell.getProperties().setProperty("database", arg);
				} catch(XQException e) {
					env.printLine("Failed to change database: " + e.getMessage());
				}
			}
		} else {
			env.printLine("Please specifiy a valid database name. See: show databases");
		}
	}
}
