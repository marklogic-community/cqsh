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
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.marklogic.xqrunner.*;

public class cp implements Command {
	private Options options = new Options();

	public cp() {
		Option force = OptionBuilder.withLongOpt("force")
		                            .withDescription("Copy even if the target document exists")
		                            .create("f");
		Option fromHost = OptionBuilder.withLongOpt("from-host")
		                            .withDescription("Host server and port to copy from (uses default database for that port)")
		                            .create("s");
		Option toHost = OptionBuilder.withLongOpt("to-host")
		                            .hasArg()
				                   .withDescription("Target server and port to copy to (uses default database for that port)")
				                   .create("d");

		options.addOption(fromHost);
		options.addOption(toHost);
		options.addOption(force);
	}

	public String getName() {
		return null;
	}

	public String getHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("usage: cp [options] <from uri> <to uri>" + Environment.NEWLINE);
		buffer.append("Copies the document specified by the <from uri> to the <to uri>" + Environment.NEWLINE);
		buffer.append("Options: " + Environment.NEWLINE);
        HelpFormatter formatter = new HelpFormatter();
        StringWriter help = new StringWriter();
        formatter.printOptions(new PrintWriter(help),
				               80,
				               options,
				               4,
				               8);
        buffer.append(help.toString());
		return buffer.toString();
	}

	public void execute(Environment env, String commandline) {
		if (commandline != null && commandline.length() > 0) {
			if (env instanceof Shell) {
				Shell shell = (Shell)env;
				String[] tokens = commandline.split("\\s+");
				CommandLineParser parser = new PosixParser();
				CommandLine cmd = null;
				try {
					cmd = parser.parse(options, tokens);
				} catch (ParseException e) {
					env.printError(e);
					return;
				}
				
				boolean force = cmd.hasOption("f");
				String fromHost = cmd.getOptionValue("s");
				String toHost = cmd.getOptionValue("d");

				if((fromHost != null && fromHost.length() > 0) || (toHost != null && fromHost.length() > 0)) {
					shell.printLine("Copying from or to remote hosts is not implmented yet");
				} else {
					if(cmd.getArgList().size() == 2) {
						String source = cmd.getArgList().get(0).toString();
						String destination = cmd.getArgList().get(1).toString();

						try {
							String query = null;
							if(force) {
								query = "if(doc('" + source + "')) then xdmp:document-insert('" + destination + "', doc('" + source + "')) "
										  	  + "else \"Source document not found.\"";
							} else {
								query = "if(doc('" + source + "') and not(doc('" + destination + "'))) then xdmp:document-insert('" + destination + "', doc('" + source + "')) "
										  	  + "else \"Source document not found or target document exists.\"";
							}
							ShellQuery squery = new ShellQuery(query, shell);
							shell.runXQuery(squery.asXQuery(), true, false);
						} catch (XQException e) {
							shell.printError(e);
						}

					} else {
						if(cmd.getArgList().size() == 1) {
							shell.printLine("Missing target document uri");
						} else if(cmd.getArgList().size() == 0) {
							shell.printLine("Missing source and target document uri");
						} else {
							shell.printLine("Too many arguments");
						}
					}
				}
			}
		} else {
			env.printLine("Please specify document to copy. See help cp.");
		}
	}
}
