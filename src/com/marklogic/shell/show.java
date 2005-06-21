/*
 * Copyright 2005 Andrew Bruno <aeb@qnot.org> 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at 
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

public class show implements Command {

	public String getName() {
		return "show";
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("usage: show databases" + NEWLINE);
		help.append("Shows all databases in Mark Logic in which the user has permissions to view." + NEWLINE);
		return help.toString();
	}

	public void execute(Environment env, String option) {
		if( "databases".equals(option) ) {
			try {
				XQDataSource dataSource = env.getDataSource();
				XQuery xquery = dataSource.newQuery("for $d in xdmp:databases() return xdmp:database-name($d)");
				env.runXQuery(xquery);
			} catch(XQException e) {
				env.printError(e);
			}
		} else {
			env.printLine("Unsupported option '" + option + "'.");
		}
	}
}
