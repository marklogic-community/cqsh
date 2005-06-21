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

public class set implements Command {
	public String getName() {
		return "set";
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("usage: set name = value" + NEWLINE);
		help.append("Set a configuration variable." + NEWLINE);
		return help.toString();
	}

	public void execute(Environment env, String arg) {
		if( arg != null && arg.length() > 0 ) {
			String[] tokens = arg.split("=");
			if(tokens.length == 2) {
				String name = tokens[0];
				String value = tokens[1];
				name = name.replaceAll("^\\s+", "");
				name = name.replaceAll("\\s+$", "");
				value = value.replaceAll("^\\s+", "");
				value = value.replaceAll("\\s+$", "");
				env.getProperties().setProperty(name, value);
			} else {
				env.printLine("Nothing to set. Try: set name=value");
			}
		} else {
			env.printLine("Nothing to set. Try: set name=value");
		}
	}
}
