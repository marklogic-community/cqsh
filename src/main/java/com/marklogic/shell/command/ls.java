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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.marklogic.shell.Environment;
import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;

public class ls implements Command {
    private Options options = new Options();

    public ls() {
        Option longOption = OptionBuilder.withLongOpt("long").withDescription(
                "List files in long format with permissions").create("l");

        options.addOption(longOption);
    }

    public String getName() {
        return "ls";
    }

    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("usage: ls [xpath]" + Environment.NEWLINE);
        help.append("List base-uri's for nodes returned in [xpath]. By default this command is"
                        + Environment.NEWLINE);
        help.append("equivalent to running: for $i in input() order by base-uri($i) return base-uri($i) "
                        + Environment.NEWLINE);
        help.append("Options: " + Environment.NEWLINE);
        HelpFormatter formatter = new HelpFormatter();
        StringWriter buf = new StringWriter();
        formatter.printOptions(new PrintWriter(buf), 80, options, 4, 8);
        help.append(buf.toString());
        return help.toString();
    }

    public void execute(Environment env, String commandLine) {
        if(commandLine == null || commandLine.length() == 0) {
            commandLine = "input()";
        }

        String[] tokens = commandLine.split("\\s+");
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, tokens);
        } catch(ParseException e) {
            env.outputException(e);
            return;
        }

        String xpath = "";

        String[] args = cmd.getArgs();
        if(args != null && args.length > 0) {
            for(int i = 0; i < args.length; i++) {
                xpath += args[i] + " ";
            }
        } else {
            xpath = "input()";
        }

        StringBuffer xquery = new StringBuffer();
        // XXX format this better or abstract out into a file.
        if(cmd.hasOption("l")) {
            xquery.append("let $roles := xdmp:eval-in('<roles>{ ");
            xquery.append("for $i in //sec:role ");
            xquery.append("return <role id=\"{ data($i/sec:role-id) }\" name=\"{ ");
            xquery.append("data($i/sec:role-name) }\">{ data($i/sec:description) }</role> ");
            xquery.append("}</roles>', xdmp:database(\"Security\")) ");
            xquery.append("let $max := max(for $i in input() return string-length(base-uri($i))) ");
            xquery.append("for $i in " + xpath + " ");
            xquery.append("let $name := string(base-uri($i)) ");
            xquery.append("order by $name ");
            xquery.append("return concat($name, string-join(for $z in (1 to $max - ");
            xquery.append("string-length($name) + 3) return \" \", \"\"), string-join( ");
            xquery.append("for $perm in xdmp:document-get-permissions($name) ");
            xquery.append("return ");
            xquery.append("concat(substring($perm/sec:capability, 1, 1), \":\", ");
            xquery.append("$roles/role[@id = $perm/sec:role-id]/@name), \",\")) ");
        } else {
            xquery.append("distinct-values(for $i in "
                            + xpath
                            + " let $name := string(base-uri($i)) order by $name return $name)");
        }

        Session session = env.getContentSource().newSession();
        AdhocQuery request = session.newAdhocQuery(xquery.toString());
        try {
            env.outputResultSequence(session.submitRequest(request));
        } catch(RequestException e) {
            env.outputException(e);
        }
    }
}
