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

public class echo implements Command {
	public String getName() {
		return "echo";
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("usage: echo" + Environment.NEWLINE);
		help.append("Echo a configuration variable to the console." + Environment.NEWLINE);
		return help.toString();
	}

	public void execute(Environment env, String arg) {
		if( arg != null && arg.length() > 0 ) {
			env.printLine(arg + " = " + env.getProperties().getString(arg));
		}
	}
}
