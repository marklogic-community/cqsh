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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.marklogic.shell.Environment;
import com.marklogic.shell.FileScanner;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentCapability;
import com.marklogic.xcc.ContentCreateOptions;
import com.marklogic.xcc.ContentFactory;
import com.marklogic.xcc.ContentPermission;
import com.marklogic.xcc.DocumentFormat;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;

public class load implements Command {
    private Options options = new Options();

    public load(Options options) {
        this();
        for(Iterator i = options.getOptions().iterator(); i.hasNext();) {
            Option o = (Option) i.next();
            this.options.addOption(o);
        }
    }

    public load() {
        Option uriPrefixOption = OptionBuilder.withLongOpt("uriprefix")
                .withDescription(
                        "uri prefix to append to file names when loading")
                .hasArg().create("i");
        Option uriOption = OptionBuilder.withLongOpt("uri").withDescription(
                "uri of the document being loaded").hasArg().create("n");
        Option colOption = OptionBuilder.withLongOpt("collection").withDescription(
                "add document(s) to collection when loading").hasArg().create("c");
        Option qualityOption = OptionBuilder.withLongOpt("quality").withDescription(
                "add quality to document(s) when loading").hasArg().create("q");
        Option permOption = OptionBuilder.withLongOpt("permission").withDescription(
                "add permissions to document(s) when loading in the form of 'capability:role'. "+
                "Capability must be one of: execute,insert,read,update").hasArg().create("x");
        Option typeOption = OptionBuilder.withLongOpt("type").withDescription(
                "add document format to document(s) when loading. Must be one of "+
                "binary,text,xml").hasArg().create("t");

        options.addOption(uriPrefixOption);
        options.addOption(uriOption);
        options.addOption(colOption);
        options.addOption(permOption);
        options.addOption(typeOption);
        options.addOption(qualityOption);
    }

    public Options getOptions() {
        return this.options;
    }

    public String getName() {
        return "load";
    }

    public String getHelp() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("usage: load [options] [file path]"
                        + Environment.NEWLINE);
        buffer.append("Loads a document into Mark Logic from [file path]. The document uri defaults to"
                        + Environment.NEWLINE);
        buffer.append("the file name." + Environment.NEWLINE);
        buffer.append("Options: " + Environment.NEWLINE);
        HelpFormatter formatter = new HelpFormatter();
        StringWriter help = new StringWriter();
        formatter.printOptions(new PrintWriter(help), 80, options, 4, 8);
        buffer.append(help.toString());
        return buffer.toString();
    }

    public void execute(Environment env, String commandLine) {
        if(commandLine != null && commandLine.length() > 0) {
            String[] tokens = commandLine.split("\\s+");
            execute(env, tokens);
        } else {
            env.outputLine("You must specify a file path to load.");
        }
    }

    public void execute(Environment env, String[] args) {
        if(args != null && args.length > 0) {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);
            } catch(ParseException e) {
                env.outputException(e);
                return;
            }
            
            ContentCreateOptions contentOptions = new ContentCreateOptions();
            
            String quality = cmd.getOptionValue("q");
            if(quality != null) {
                try {
                    Integer q = Integer.valueOf(quality);
                    contentOptions.setQuality(q.intValue());
                } catch(NumberFormatException e) {
                    env.outputError("Invalid document quality (must be an int): "+quality);
                    return;
                }
            }
            
            String docType = cmd.getOptionValue("t");
            if(docType != null) {
	            if("binary".equals(docType)) {
	                contentOptions.setFormat(DocumentFormat.BINARY);
	            } else if("text".equals(docType)) {
	                contentOptions.setFormat(DocumentFormat.TEXT);
	            } else if("xml".equals(docType)) {
	                contentOptions.setFormat(DocumentFormat.XML);
	            } else {
	                env.outputError("Invalid document format. Must be: binary,text,xml");
	                return;
	            }
            }
            
            String[] cols = cmd.getOptionValues("c");
            if(cols != null && cols.length > 0) {
                contentOptions.setCollections(cols);
            }
            
            String[] perms = cmd.getOptionValues("x");
            if(perms != null && perms.length > 0) {
                List contentPermissions = new ArrayList();
                for(int i = 0; i < perms.length; i++) {
                    ContentCapability ccap = null;
                    String p = perms[i];
                    if(p == null) {
                        env.outputError("Invalid permission option. Must be of the form: 'capability:role'");
                        return;
                    }
                    String[] parts = p.split(":");
                    if(parts == null || parts.length != 2) {
                        env.outputError("Invalid permission option. Must be of the form: 'capability:role'");
                        return;
                    }
                    String capability = parts[0];
                    String role = parts[1];
                    if("execute".equals(capability)) {
                        ccap = ContentCapability.EXECUTE;
                    } else if("insert".equals(capability)) {
                        ccap = ContentCapability.INSERT;
                    } else if("read".equals(capability)) {
                        ccap = ContentCapability.READ;
                    } else if("update".equals(capability)) {
                        ccap = ContentCapability.UPDATE;
                    } else {
                        env.outputError("Invalid permission '"+p+"'. Capability must be one of:  execute,insert,read,update");
                        return;
                    }
                    
                    if(role == null || role.length() == 0) {
                        env.outputError("Invalid permission. Please provide a role");
                        return;
                    }
                    contentPermissions.add(new ContentPermission(ccap, role));
                }
                ContentPermission[] cperms = new ContentPermission[contentPermissions.size()];
                contentPermissions.toArray(cperms);
                contentOptions.setPermissions(cperms);
            }
            
            env.outputLine("Loading files...");
            int total = 0;
            for(Iterator i = cmd.getArgList().iterator(); i.hasNext();) {
                String path = i.next().toString();
                List files = FileScanner.findFiles(path);
                if(files != null && files.size() > 0) {
                    List list = new ArrayList();
                    for(Iterator it = files.iterator(); it.hasNext();) {
                        File f = (File) it.next();
                        String uri = cmd.getOptionValue("n");
                        if(uri == null || uri.length() == 0) {
                            uri = getUri(cmd.getOptionValue("i"), f.getName());
                        }
                        list.add(ContentFactory.newContent(uri, f, contentOptions));
                    }
                    Content[] contentList = new Content[list.size()];
                    list.toArray(contentList);
                    Session session = env.getContentSource().newSession();
                    try {
                        session.insertContent(contentList);
                        total += contentList.length;
                    } catch(RequestException e) {
                        env.outputException(e);
                    }
                } else {
                    env.outputLine("No file(s) found at location " + path
                                    + ".");
                }
            }
            if(total > 0) {
                env.outputLine("Done. Loaded " + total + " file(s).");
            }
        } else {
            env.outputLine("You must specify a file path to load.");
        }
    }

    public String getUri(String prefix, String filename) {
        String uri = "";
        if(prefix == null || prefix.length() == 0) {
            uri = filename;
        } else {
            if(prefix.endsWith("/")) {
                uri = prefix + filename;
            } else {
                uri = prefix + "/" + filename;
            }
        }

        return uri;
    }
}
