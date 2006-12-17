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
package com.marklogic.shell.command;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.marklogic.shell.Environment;
import com.marklogic.shell.Shell;
import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;

public class rm implements Command {
    private Options options = new Options();

    public rm() {
        Option force = OptionBuilder.withLongOpt("force").withDescription(
                "Attempt to remove files without confirmation").create("f");
        Option xpath = OptionBuilder.withLongOpt("xpath").hasArg()
                .withDescription("Remove files that match xpath").create("x");

        options.addOption(force);
        options.addOption(xpath);
    }

    public String getName() {
        return null;
    }

    public String getHelp() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("usage: rm [options] [uri uri ...]"
                        + Environment.NEWLINE);
        buffer.append("Remove documents from the database."
                + Environment.NEWLINE);
        buffer.append("Options: " + Environment.NEWLINE);
        HelpFormatter formatter = new HelpFormatter();
        StringWriter help = new StringWriter();
        formatter.printOptions(new PrintWriter(help), 80, options, 4, 8);
        buffer.append(help.toString());
        return buffer.toString();
    }

    public void execute(Environment env, String commandline) {
        if(commandline != null && commandline.length() > 0) {
            // XXX need to be a shell environment because we are getting input
            // from the user
            if(env instanceof Shell) {
                Shell shell = (Shell) env;
                String[] tokens = commandline.split("\\s+");
                CommandLineParser parser = new PosixParser();
                CommandLine cmd = null;
                try {
                    cmd = parser.parse(options, tokens);
                } catch(ParseException e) {
                    env.outputException(e);
                    return;
                }

                String xpath = cmd.getOptionValue("x");
                if(xpath != null && xpath.length() > 0) {
                    String key = "n";
                    if(cmd.hasOption("f")) {
                        key = "y";
                    } else {
                        try {
                            key = shell.getConsole().readLine(
                                    "remove all documents matching '" + xpath
                                            + "'? (N|y) ");
                        } catch(IOException e) {
                            shell.outputLine("Failed to read char from console");
                            shell.outputException(e);
                        }
                    }
                    if(key.equalsIgnoreCase("y")) {
                        String query = "concat(xs:string(count(for $n in ("
                                + xpath
                                + ")"
                                + " return (xdmp:document-delete(base-uri($n)), <done/>))), "
                                + "\" documents removed.\");";
                        Session session = shell.getContentSource().newSession();
                        AdhocQuery request = session.newAdhocQuery(query);
                        try {
                            shell.outputResultSequence(session.submitRequest(request));
                        } catch(RequestException e) {
                            shell.outputException(e);
                        }
                    } else {
                        shell.outputLine("");
                    }
                } else {
                    for(Iterator i = cmd.getArgList().iterator(); i.hasNext();) {
                        String uri = (String) i.next();
                        String key = "n";
                        if(cmd.hasOption("f")) {
                            key = "y";
                        } else {
                            try {
                                key = shell.getConsole().readLine(
                                        "remove '" + uri + "'? (N|y) ");
                            } catch(IOException e) {
                                shell.outputLine("Failed to read char from console");
                                shell.outputException(e);
                            }
                        }
                        if(key.equalsIgnoreCase("y")) {
                            String query = "if(doc(\"" + uri
                                    + "\")) then xdmp:document-delete(\"" + uri
                                    + "\") " + "else \"Document not found.\"";
                            Session session = shell.getContentSource().newSession();
                            AdhocQuery request = session.newAdhocQuery(query);
                            try {
                                shell.outputResultSequence(session.submitRequest(request));
                            } catch(RequestException e) {
                                shell.outputException(e);
                            }
                        } else {
                            shell.outputLine("");
                        }
                    }
                }
            }
        } else {
            env.outputLine("Please specify document(s) to remove. See help rm.");
        }
    }
}
