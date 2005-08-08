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

import java.io.IOException;

import com.marklogic.xqrunner.*;

public class su implements Command {
	public String getName() {
		return "su";
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("usage: su [username]" + Environment.NEWLINE);
		help.append("Switch to a different user (you will be prompted to enter in a password)." + Environment.NEWLINE);
		help.append("Set's the environment variables 'user' and 'password' upon successful" + Environment.NEWLINE);
		help.append("connection to Mark Logic. Will not change environment if failed to connect." + Environment.NEWLINE);
		return help.toString();
	}

	public void execute(Environment env, String user) {
		if( user != null && user.length() > 0 ) {
			if(env instanceof Shell) {
				Shell shell = (Shell)env;
				String password = null;
				try {
					password = shell.getConsole().readLine("password: ", new Character('*'));
				} catch(IOException ignored) { }
	
				if(password == null) {
					shell.printLine("Failed to switch user. Error reading password.");
				} else {
					try { 
						shell.checkConnection(user,
					                          password,
											shell.getProperties().getString("host"),
											shell.getProperties().getInt("port", Shell.DEFAULT_PORT)
											  );
											  	
						shell.getProperties().setProperty("user", user);
						shell.getProperties().setProperty("password", password);
					} catch(XQException e) {
						shell.printLine("Failed to switch to user: " + user);
						shell.printError(e);
					}
				}
			}
		} else {
			env.printLine("You must specify a user.");
		}
	}
}
