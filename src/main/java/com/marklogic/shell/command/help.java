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

public class help implements Command {
    public String getName() {
        return "help";
    }

    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("Help System: help [command] will display verbose info"
                + Environment.NEWLINE);
        help.append(Environment.NEWLINE);
        help.append("Commands" + Environment.NEWLINE);
        help.append(Environment.NEWLINE);
        help.append("--Document: " + Environment.NEWLINE);
        help.append("  load [file pattern] ..   loads files into database "
                + Environment.NEWLINE);
        help.append("  ls [xpath]               list base-uri()'s for nodes returned in 'xpath'"
                        + Environment.NEWLINE);
        help.append("  cat [document uri]       display a documents contents"
                + Environment.NEWLINE);
        help.append("  rm [-f -x] uri uri ..    remove document(s) from database"
                        + Environment.NEWLINE);
        help.append(Environment.NEWLINE);
        help.append("--Environment: " + Environment.NEWLINE);
        help.append("  echo [property]          echo the value of a configuration property"
                        + Environment.NEWLINE);
        help.append("  env                      displays the current configuration environment"
                        + Environment.NEWLINE);
        help.append("  set name=value           sets a configuration property"
                + Environment.NEWLINE);
        help.append("  su [username]            switch to a differnt user"
                + Environment.NEWLINE);
        help.append(Environment.NEWLINE);
        help.append("--System: " + Environment.NEWLINE);
        help.append("  show databases           lists all databases"
                + Environment.NEWLINE);
        help.append("  use [database]           Eval xquery in specified database"
                        + Environment.NEWLINE);
        help.append("  version                  displays MarkLogic version information"
                        + Environment.NEWLINE);
        help.append("  help [command]           display verbose information on a command"
                        + Environment.NEWLINE);
        help.append("  exit (quit or q)         quit the program"
                + Environment.NEWLINE);

        return help.toString();
    }

    public void execute(Environment env, String commandString) {
        if(commandString == null || commandString.length() == 0) {
            env.outputLine(getHelp());
            return;
        }

        Command command = null;

        String[] path = env.getProperties().getStringArray("path");
        if(path != null && path.length > 0) {
            for(int i = 0; i < path.length; i++) {
                Class commandClass;
                try {
                    commandClass = Class.forName(path[i] + "." + commandString);
                    command = (Command) commandClass.newInstance();
                    break;
                } catch(Exception ignored) {
                }
            }
        }

        if(command == null) {
            try {
                Class commandClass = Class.forName(Environment.SYSTEM_PATH
                        + "." + commandString);
                command = (Command) commandClass.newInstance();
            } catch(Exception ignored) {
            }
        }

        if(command != null) {
            env.outputLine(command.getHelp());
        } else {
            env.outputError("Command not found: " + commandString);
        }
    }
}
