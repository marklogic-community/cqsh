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

import java.util.Iterator;

public class env implements Command {
	public String getName() {
		return "env";
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("usage: env" + Environment.NEWLINE);
		help.append("Prints out the configuration environment." + Environment.NEWLINE);
		return help.toString();
	}

	public void execute(Environment env, String none) {
		for(Iterator i = env.getProperties().getKeys(); i.hasNext();) {
			String key = (String)i.next();
			if("password".equals(key)) {
				env.printLine(key + " = ********");
			} else  {
				env.printLine(key + " = " + env.getProperties().getString(key));
			}
		}
	}
}
