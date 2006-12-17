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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;

import com.marklogic.shell.command.Command;
import com.marklogic.shell.command.load;
import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.XQueryException;
import com.marklogic.xcc.exceptions.XQueryStackFrame;
import com.marklogic.xcc.types.XdmElement;
import com.marklogic.xcc.types.XdmVariable;

/**
 * Shell Environment for Mark Logic. You can run the shell in batch mode or
 * intercative mode. You can set configuration options by creating a file in
 * your $HOME directory named ".cqshrc".
 * 
 * @author Andrew Bruno <aeb@qnot.org>
 */

public class Shell extends AbstractEnvironment {
    /**
     * Shell version
     */
    public static final String VERSION = "0.5.0";

    /**
     * Default number of lines to scroll output
     */
    public static final int DEFAULT_SCROLL = 50;

    /**
     * Print writer to force utf-8 output
     */
    private static PrintWriter utf8Out = null;

    static {
        try {
            utf8Out = new PrintWriter(new OutputStreamWriter(System.out,
                    "UTF-8"));
        } catch(UnsupportedEncodingException e) {
            System.err.println("Failed to create UTF-8 Print writer.");
            e.printStackTrace();
        }
    }

    private Options options;
    private jline.ConsoleReader console;
    private File historyFile;

    /**
     * Create a new Shell
     */
    public Shell() {
        Option user = OptionBuilder.withLongOpt("user").hasArg()
                .withDescription("user to use to connect to Marklogic").create(
                        "u");
        Option password = OptionBuilder.withLongOpt("password").hasArg()
                .withDescription("password to use to connect to Marklogic")
                .create("p");
        Option host = OptionBuilder.withLongOpt("host").hasArg()
                .withDescription("host to use to connect to Marklogic").create(
                        "H");
        Option db = OptionBuilder.withLongOpt("database").hasArg()
                .withDescription("default database").create("d");
        Option port = OptionBuilder.withLongOpt("port").hasArg().withType(
                new java.lang.Integer(800)).withDescription(
                "port to use to connect to Marklogic. Defaults to 8003")
                .create("P");
        Option help = OptionBuilder.withLongOpt("help").withDescription(
                "print help").create("h");
        Option loadOption = OptionBuilder.withLongOpt("load").withDescription(
                "load files into database").create("l");
        Option fileOption = OptionBuilder.withLongOpt("file").withDescription(
                "read xquery from file").hasArg().create("f");
        Option formatOption = OptionBuilder.withLongOpt("format")
                .withDescription("pretty print xml output").create("F");

        options = new Options();
        options.addOption(user);
        options.addOption(password);
        options.addOption(host);
        options.addOption(db);
        options.addOption(port);
        options.addOption(help);
        options.addOption(loadOption);
        options.addOption(fileOption);
        options.addOption(formatOption);

        //XXX hack to support loading from command line
        load loader = new load();
        for(Iterator i = loader.getOptions().getOptions().iterator(); i
                .hasNext();) {
            Option o = (Option) i.next();
            options.addOption(o);
        }

        historyFile = new File(System.getProperty("user.home")
                + File.separatorChar + ".cqsh_history");
    }

    public static void main(String[] args) {
        Shell shell = new Shell();
        shell.run(args);
    }

    private void run(String[] args) {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch(ParseException e) {
            exitWithError(e.getMessage());
        }

        if(cmd.hasOption("h")) {
            printHelp();
        }

        String user = cmd.getOptionValue("u");
        String password = cmd.getOptionValue("p");
        String host = cmd.getOptionValue("H");
        String database = cmd.getOptionValue("d");
        Integer port = null;
        try {
            port = new Integer(cmd.getOptionValue("P"));
        } catch(NumberFormatException ignored) {
        }

        if(user == null)
            user = properties.getString("user");
        if(password == null)
            password = properties.getString("password");
        if(host == null)
            host = properties.getString("host");
        if(port == null) {
            try {
                port = new Integer(properties.getInt("port", DEFAULT_PORT));
            } catch(ConversionException e) {
                printHelp("Invalid port number: "
                        + properties.getString("port"));
            }
        }

        if(user == null || password == null || port == null || host == null) {
            printHelp("You must provide a user, password, host and port.");
        }

        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("host", host);
        properties.setProperty("database", database);
        properties.setProperty("port", port.toString());
        if(properties.getString("scroll") == null
                || properties.getString("scroll").length() <= 0) {
            properties.setProperty("scroll", String.valueOf(DEFAULT_SCROLL));
        }

        if(cmd.hasOption("F")) {
            properties.setProperty("pretty-print-xml", "true");
        }

        String xqueryFile = cmd.getOptionValue("f");
        InputStream in = null;
        if(xqueryFile != null) {
            try {
                in = new FileInputStream(new File(xqueryFile));
            } catch(FileNotFoundException e) {
                exitWithError("File " + xqueryFile + " not found: "
                        + e.getMessage());
            }
        } else {
            in = System.in;
        }
        int stdinBytes = 0;
        try {
            stdinBytes = in.available();
        } catch(IOException ignored) { }

        if(cmd.hasOption("l")) {
            // XXX this is a hack to support loading from command line without
            // duplication of code
            // XXX make sure load command doesn't have conflicting args
            load loader = new load(this.options);
            loader.execute(this, args);
        } else if(stdinBytes > 0) {
            StringBuffer xquery = new StringBuffer();
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in));
                char[] b = new char[4 * 1024];
                int n;
                while((n = reader.read(b)) > 0) {
                    xquery.append(b, 0, n);
                }
            } catch(IOException e) {
                exitWithError("Failed to read query from stdin: "
                        + e.getMessage());
            }

            Session session = getContentSource().newSession();
            AdhocQuery request = session.newAdhocQuery(xquery.toString());
            try {
                outputResultSequence(session.submitRequest(request), false);
            } catch(RequestException e) {
                outputException(e);
            }
        } else {
            try {
                checkConnection(properties.getString("user"), properties
                        .getString("password"), properties.getString("host"),
                        properties.getInt("port", DEFAULT_PORT));
            } catch(ShellException e) {
                outputLine("Failed to connect to Mark Logic. Invalid connection information.");
                outputException(e);
                exitWithError("Goodbye.");
            }
            printWelcome();
            outputLine("");
            try {
                startShell();
            } catch(Exception e) {
                e.printStackTrace();
                outputLine("Shell exited abnormally with exception: "
                        + e.getClass().toString());
                outputLine("Error message: " + e.getMessage());
                outputLine("Goodbye.");
            }
        }
    }

    private void startShell() {
        try {
            console = new jline.ConsoleReader();
            console.setHistory(new jline.History(historyFile));
        } catch(IOException e) {
            throw new RuntimeException(
                    "Can't run shell. Failed to get a console. "
                            + "Your platform does not seem to be suppored. Error: "
                            + e.getMessage());
        }
        boolean exit = false;
        while(!exit) {
            try {
                String line = console.readLine("cqsh> ");
                if(line != null) {
                    // Default exit command
                    if("exit".equals(line) || "q".equals(line)
                            || "quit".equals(line)) {
                        exit = true;
                    } else {
                        runCommand(line);
                    }
                }
            } catch(IOException e) {
                exitWithError(e.getMessage());
            }
        }
        outputLine("Goodbye.");
    }

    private void runCommand(String line) {
        if(line == null || line.length() == 0) {
            return;
        }

        String commandString = null;
        String options = null;
        if(line.indexOf(' ') == -1) {
            commandString = line;
        } else {
            commandString = line.substring(0, line.indexOf(' '));
            options = line.substring(line.indexOf(' ') + 1);
        }

        Command command = null;
        String[] path = properties.getStringArray("path");
        if(path != null && path.length > 0) {
            for(int i = 0; i < path.length; i++) {
                try {
                    Class commandClass = Class.forName(path[i] + "."
                            + commandString);
                    command = (Command) commandClass.newInstance();
                    break;
                } catch(Exception ignored) {
                }
            }
        }

        // Try the default system path as a last attempt.
        if(command == null) {
            try {
                Class commandClass = Class.forName(Environment.SYSTEM_PATH
                        + "." + commandString);
                command = (Command) commandClass.newInstance();
            } catch(Exception ignored) {
            }
        }

        if(command != null) {
            try {
                if(options != null && options.length() > 0
                        && options.charAt(options.length() - 1) == ';') {
                    options = options.substring(0, options.length() - 1);
                }
                command.execute(this, options);
                outputLine("");
            } catch(Exception e) {
                if(debug()) {
                    e.printStackTrace();
                }
                outputLine("Failed to run command '" + commandString + "': "
                        + e.getMessage());
            }
        } else {
            StringBuffer xquery = new StringBuffer();
            boolean clearBuffer = false;
            while(';' != line.charAt(line.length() - 1)) {
                if(line.length() >= 2) {
                    if('\\' == line.charAt(line.length() - 2)
                            && 'c' == line.charAt(line.length() - 1)) {
                        clearBuffer = true;
                        break;
                    }
                }
                xquery.append(line);
                try {
                    line = console.readLine("   -> ");
                    if(line == null || line.length() == 0) {
                        line = " ";
                    }
                    line = " " + line;
                } catch(IOException e) {
                    exitWithError(e.getMessage());
                }
            }

            xquery.append(line.substring(0, line.length() - 1));
            if(!clearBuffer) {
                Session session = getContentSource().newSession();
                AdhocQuery request = session.newAdhocQuery(xquery.toString());
                try {
                    long start = System.currentTimeMillis();
                    ResultSequence result = session.submitRequest(request);
                    long end = System.currentTimeMillis();
                    double total = (double) (end - start) / 1000;
                    outputResultSequence(result);
                    DecimalFormat format = new DecimalFormat("###,##0.00");
                    outputLine("\nDone (" + format.format(total) + " sec)");
                } catch(RequestException e) {
                    outputException(e);
                }
            } else {
                outputLine("");
            }
        }
    }

    public void outputResultSequence(ResultSequence result) {
        outputResultSequence(result, true);
    }

    public void outputResultSequence(ResultSequence result, boolean scrollResult) {
        boolean stop = false;
        int lineCount = 0;
        while(result.hasNext()) {
            ResultItem item = result.next();

            try {
                BufferedReader reader = getResultItemReader(item);
                String line = null;
                while((line = reader.readLine()) != null) {
                    lineCount++;
                    outputLine(line);
                    if(scrollResult && checkStopScroll(lineCount)) {
                        stop = true;
                        break;
                    }
                }
                reader.close();
            } catch(IOException e) {
                outputError("I/O error. Failed to print result: "
                        + e.getMessage());
            } catch(ShellException e) {
                outputError("Failed to print result: " + e.getMessage());
            }

            if(stop) {
                break;
            }
        }
    }

    private BufferedReader getResultItemReader(ResultItem item)
            throws ShellException {
        BufferedReader reader;
        if((item instanceof XdmElement)
                && "true".equals(properties.getString("pretty-print-xml"))) {
            XdmElement xdmElement = (XdmElement) item;
            DOMBuilder domBuilder = new DOMBuilder();
            Document doc = null;
            try {
                doc = domBuilder.build(xdmElement.asW3cDocument());
            } catch(ParserConfigurationException e) {
                throw new ShellException(
                        "ParserConfig Error. Failed to pretty print xml", e);
            } catch(IOException e) {
                throw new ShellException(
                        "I/O Error. Failed to pretty print xml", e);
            } catch(SAXException e) {
                throw new ShellException(
                        "SAX Error. Failed to pretty print xml", e);
            }
            Format format = Format.getPrettyFormat();
            format.setLineSeparator(NEWLINE);
            format.setOmitDeclaration(true);
            format.setOmitEncoding(true);
            XMLOutputter xmlout = new XMLOutputter(format);
            String docString = xmlout.outputString(doc);
            reader = new BufferedReader(new StringReader(docString));
        } else {
            reader = new BufferedReader(item.asReader());
        }

        return reader;
    }

    public boolean debug() {
        return "true".equals(properties.getString("debug"));
    }

    public boolean checkStopScroll(int lineCount) {
        boolean stop = false;

        int scroll = DEFAULT_SCROLL;
        try {
            scroll = properties.getInt("scroll");
        } catch(ConversionException e) {
        }

        if(scroll == 0) {
            return false;
        }

        if(scroll < 0) {
            scroll = DEFAULT_SCROLL;
        }

        if((lineCount % scroll) == 0) {
            try {
                output("***** press <space> to continue ******");
                int key = console.readVirtualKey();
                if(key != 32) {
                    stop = true;
                }
                outputLine("");
            } catch(IOException e) {
            }
        }

        return stop;
    }

    /**
     * Tests the connection to Mark Logic.
     * 
     * @param user
     *            user name
     * @param password
     *            password
     * @param host
     *            host
     * @param port
     *            port (defaults to 8004)
     * @throws XQException
     */
    public void checkConnection(String user, String password, String host,
            int port) throws ShellException {
        if(user == null)
            user = properties.getString("user");
        if(password == null)
            password = properties.getString("password");
        if(host == null)
            host = properties.getString("host");
        if(port == -1)
            port = properties.getInt("port", DEFAULT_PORT);

        ContentSource contentSource = ContentSourceFactory.newContentSource(
                host, port, user, password);

        Session session = contentSource.newSession();
        Request request = session
                .newAdhocQuery("xdmp:database-name(xdmp:database())");
        try {
            ResultSequence rs = session.submitRequest(request);
            if(rs.hasNext()) {
                String db = properties.getString("database");
                if(db == null) {
                    properties.setProperty("database", rs.next().asString());
                }
            } else {
                throw new ShellException("Failed to fetch default database.");
            }
        } catch(RequestException e) {
            throw new ShellException("Connection failed", e);
        }
    }

    public void checkConnection() throws ShellException {
        checkConnection(null, null, null, -1);
    }

    /**
     * Prints an error to the console.
     */
    public void outputError(String message) {
        outputLine(message);
    }

    /**
     * Print an error to the screen that was caused by an Exception. If the
     * Exception is an instance of XDBCXQueryException it will display some
     * verbose information about the XQuery error including line number, context
     * item/postion, uri, variable bindings. For all other exceptions just the
     * message is displayed.
     */
    public void outputException(Exception e) {
        Throwable t = e.getCause();

        if(e instanceof XQueryException) {
            XQueryException xqe = (XQueryException) e;
            outputXQueryException(xqe);
        } else if(t instanceof XQueryException) {
            XQueryException xqe = (XQueryException) t;
            outputXQueryException(xqe);
        } else {
            outputLine("Error: " + e.getMessage());
        }
    }

    private void outputXQueryException(XQueryException e) {
        outputLine("");
        outputLine("---------------------------");
        outputLine("      XQuery Error      ");
        outputLine("---------------------------");
        outputLine("Message: " + e.getFormatString());
        XQueryStackFrame[] stack = e.getStack();
        if(stack != null && stack.length > 0) {
            outputLine("--STACK DUMP--");
            for(int i = 0; i < stack.length; i++) {
                outputLine("line number:  " + stack[i].getLineNumber());
                outputLine("context item:  " + stack[i].getContextItem());
                outputLine("context position:  "
                        + stack[i].getContextPosition());
                outputLine("uri:  " + stack[i].getUri());
                outputLine("variable bindings:");
                XdmVariable vars[] = stack[i].getVariables();
                if(vars != null) {
                    for(int j = 0; j < vars.length; j++) {
                        outputLine(vars[j].getName() + " = "
                                + vars[j].getValue());
                    }
                }
            }
        }
        outputLine("");
    }

    /**
     * Print the welcome message for the shell.
     */
    private void printWelcome() {
        outputLine("Welcome to cqsh " + VERSION + ".  Commands end with ';'");
        outputLine("Connected to host: " + properties.getString("host") + ":"
                + properties.getInt("port"));
        outputLine("");
        outputLine("Type 'help' for command help. Type '\\c' to clear the buffer.");
    }

    /**
     * Print the default help screen which will list all the command line
     * options supported by the shell and a brief description of each.
     */
    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("cqsh", options);
        System.exit(0);
    }

    /**
     * Print a message along with the default help screen.
     */
    private void printHelp(String message) {
        outputLine(message);
        printHelp();
    }

    /**
     * Print a message to the console.
     */
    public void output(String message) {
        try {
            utf8Out.print(message);
            utf8Out.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Print a message to the console with a newline.
     */
    public void outputLine(String message) {
        try {
            utf8Out.println(message);
            utf8Out.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Exit the shell with an error message.
     */
    public void exitWithError(String message) {
        System.err.println(message);
        System.exit(1);
    }

    /**
     * Exit the shell cleanly.
     */
    public void exit() {
        outputLine("Goodbye.");
        System.exit(0);
    }

    /**
     * The console reader used to read lines of input from the user.
     */
    public jline.ConsoleReader getConsole() {
        return console;
    }

    /**
     * The history file used to store command line history. The default for this
     * file is in $HOME/.cqsh_history.
     */
    public File getHistoryFile() {
        return historyFile;
    }

    /**
     * The command line options passed into the shell when it was first
     * launched.
     */
    public Options getOptions() {
        return options;
    }

    /**
     * The configuration properties for the shell environment. When the shell is
     * first created the file $HOME/.cqshrc is read in if exists. While the
     * shell is running the properties can be used as a place to store
     * environment variables similar to unix shells.
     */
    public PropertiesConfiguration getProperties() {
        return properties;
    }
}
