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

import com.marklogic.shell.Environment;
import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;

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
        if(arg != null && arg.length() > 0) {
            String query = "define variable $db as xs:string external "
                    + "let $test := xdmp:database($db) return ()";
            Session session = env.getContentSource().newSession();
            AdhocQuery request = session.newAdhocQuery(query);
            request.setNewStringVariable("db", arg);
            try {
                session.submitRequest(request);
                env.getProperties().setProperty("database", arg);
                env.outputLine("Database changed.");
            } catch(RequestException e) {
                env.outputError("Unknown database: " + arg);
                env.outputException(e);
            }
        } else {
            env.outputLine("Please specifiy a valid database name. See: show databases");
        }
    }
}
