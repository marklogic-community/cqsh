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

public class cat implements Command {
	public String getName() {
		return "cat";
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("usage: cat [uri]" + Environment.NEWLINE);
		help.append("Display the xml content located at [uri]. Output will be paged 50 lines" + Environment.NEWLINE);
		help.append("at a time by default. You can configure the number of lines to scroll by" + Environment.NEWLINE);
		help.append("setting the 'scroll' environment variable. See help set." + Environment.NEWLINE);
		return help.toString();
	}

	public void execute(Environment env, String arg) {
		if( arg != null && arg.length() > 0 ) {
			if(env instanceof Shell) {
				Shell shell = (Shell)env;
				String query = "if(doc(\"" + arg + "\")) then doc(\"" + arg + "\") else \"Document not found.\"";
				ShellQuery squery = new ShellQuery(query, shell);
				try {
					env.runXQuery(squery.asXQuery());
				} catch (XQException e) {
					env.printError(e);
				}
			}
		} else {
				env.printLine("Please specify a document to view: cat [document uri]");
		}
	}
}
