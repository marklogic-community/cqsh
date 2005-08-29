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
import java.text.DecimalFormat;
import java.util.Iterator;

import org.apache.commons.cli.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import com.marklogic.xqrunner.*;
import com.marklogic.xdbc.XDBCXQueryException;

/**
 * Shell Environment for Mark Logic. You can run the shell in batch
 * mode or intercative mode. You can set configuration options by creating a
 * file in your $HOME directory named ".cqshrc".
 *
 * @author Andrew Bruno <aeb@qnot.org>
 */

public class Shell implements Environment {
	/**
	 * Shell version
	 */
    public static final String VERSION = "0.0.2";
    
    /**
     * Default system package to search for commands
     */
    protected static final String SYSTEM_PATH = "com.marklogic.shell";
    
    /**
     * Default number of lines to scroll output
     */
    protected static final int DEFAULT_SCROLL = 50;
    
    /**
     * Default port to use when connecting to Mark Logic
     */
	protected static final int DEFAULT_PORT = 8003;
	
	/**
	 * Print writer to force utf-8 output
	 */
	private static PrintWriter utf8Out = null;
	
	static {
		try {
			utf8Out = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.err.println("Failed to create UTF-8 Print writer.");
			e.printStackTrace();
		}
	}

    private Options options = new Options();
    private PropertiesConfiguration properties;
	private jline.ConsoleReader console;
    private File historyFile = new File(System.getProperty("user.home") + 
	                                             File.separatorChar + ".cqsh_history");

    /**
     * Create a new Shell
     */
    public Shell() { 
        Option user = OptionBuilder.withLongOpt("user")
                                   .hasArg()
                                   .withDescription("user to use to connect to Marklogic")
                                   .create("u");
        Option password = OptionBuilder.withLongOpt("password")
                                       .hasArg()
                                       .withDescription("password to use to connect to Marklogic")
                                       .create("p");
        Option host = OptionBuilder.withLongOpt("host")
                                   .hasArg()
                                   .withDescription("host to use to connect to Marklogic")
                                   .create("H");
        Option port = OptionBuilder.withLongOpt("port")
                                   .hasArg()
                                   .withType(new java.lang.Integer(800))
                                   .withDescription("port to use to connect to Marklogic. Defaults to 8003")
                                   .create("P");
        Option help = OptionBuilder.withLongOpt("help")
                                   .withDescription("print help")
                                   .create("h");
        Option loadOption = OptionBuilder.withLongOpt("load")
                                   .withDescription("load files into database")
                                   .create("l");
        Option fileOption = OptionBuilder.withLongOpt("file")
                                   .withDescription("read xquery from file")
                                   .hasArg()
                                   .create("f");
        Option formatOption = OptionBuilder.withLongOpt("format")
                                   .withDescription("pretty print xml output")
                                   .create("F");
        Option uriOption = OptionBuilder.withLongOpt("uriprefix")
                                   .withDescription("uri prefix to append onto file names when loading")
                                   .hasArg()
                                   .create("i");

        options.addOption(user);
        options.addOption(password);
        options.addOption(host);
        options.addOption(port);
        options.addOption(help);
        options.addOption(loadOption);
        options.addOption(uriOption);
        options.addOption(fileOption);
        options.addOption(formatOption);

        
    		properties = new PropertiesConfiguration();
        try {
            properties.load(".cqshrc");
        } catch(ConfigurationException ignored) { }
    }

    public static void main(String[] args) {
        Shell shell = new Shell();
        shell.run(args);
    }

    private void run(String[] args) {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse( options, args);
        } catch(ParseException e) {
            exitWithError(e.getMessage());
        }

        if(cmd.hasOption("h") ) {
            printHelp();
        }

        String user = cmd.getOptionValue("u");
        String password = cmd.getOptionValue("p");
        String host = cmd.getOptionValue("H");
        Integer port = null;
        try {
            port = new Integer(cmd.getOptionValue("P"));
        } catch(NumberFormatException ignored) { }

        if(user == null || password == null || port == null || host == null){
            if(user == null) user = properties.getString("user");
            if(password == null) password = properties.getString("password");
            if(host == null) host = properties.getString("host");
            if(port == null) {
                try {    
                    port = new Integer(properties.getInt("port", DEFAULT_PORT));   
                } catch(ConversionException e) {
                    printHelp("Invalid port number: " + properties.getString("port"));
                }
            }

            if(user == null || password == null || port == null || host == null ){
                printHelp("You must provide a user, password, host and port.");
            }
        }

        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("host", host);
        properties.setProperty("port", port.toString());
        if(properties.getString("scroll") == null || properties.getString("scroll").length() <= 0) {
            properties.setProperty("scroll", String.valueOf(DEFAULT_SCROLL));
        }
        
        if(cmd.hasOption("F") ) {
            properties.setProperty("pretty-print-xml", "true");
        }
            
        String xqueryFile = cmd.getOptionValue("f");
        InputStream in = null;
        if( xqueryFile != null ) {
            try {
                in = new FileInputStream(new File(xqueryFile));
            } catch(FileNotFoundException e) {
                exitWithError("File " + xqueryFile + " not found: " + e.getMessage());
            }
        } else {
            in = System.in;
        }
        int stdinBytes = 0;
        try {
            stdinBytes = in.available();   
        } catch(IOException ignored) { }
        
        if(cmd.hasOption("l")) {
        		printLine("Loading files...");
        		load loader = new load();
        		for(Iterator i = cmd.getArgList().iterator(); i.hasNext();) {
        			File file = new File(i.next().toString());
        			String uri = loader.getUri(cmd.getOptionValue("i"), file.getName());
        			loader.loadDocument(this, uri, file);
        		}
        } else if(stdinBytes > 0 ) {
            StringBuffer xquery = new StringBuffer();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                char[] b = new char[4*1024];
                int n;
                while((n = reader.read(b)) > 0 ) {
                    xquery.append(b, 0, n);
                }
                try {
                		ShellQuery squery = new ShellQuery(xquery.toString(), this);
					runXQuery(squery.asXQuery(), false, false);
				} catch(XQException e) {
                		printError(e);
                }
            } catch(IOException e) {
                exitWithError("Failed to read query from stdin: " + e.getMessage());
            }
		} else {
			try {
				checkConnection(
				               properties.getString("user"),
				               properties.getString("password"),
							  properties.getString("host"),
							  properties.getInt("port", DEFAULT_PORT) );
			} catch(XQException e) {
				printLine("Failed to connect to Mark Logic. Invalid connection information.");
				printError(e);
				exitWithError("Goodbye.");
			}
			printWelcome();
			printLine("");
            try {
	            startShell();
            } catch(Exception e) {
                printLine("Shell exited abnormally with exception: " + e.getClass().toString());
                printLine("Error message: " + e.getMessage());
                printLine("Goodbye.");
            }
        }
    }


    private void startShell() {
        try {
			console = new jline.ConsoleReader();
			console.setHistory(new jline.History(historyFile));
        } catch(IOException e) {
            throw new RuntimeException("Can't run shell. Failed to get a console. "
			                          +"Your platform does not seem to be suppored. Error: " + e.getMessage());
        }
        boolean exit = false;
        while(!exit) {
            try {
                String line = console.readLine("cqsh> ");
                if( line != null ) {
					// Default exit command
					if("exit".equals(line) ||
					   "q".equals(line) ||
					   "quit".equals(line) ) {
						exit = true;
					} else {
						runCommand(line);
					}
                }
            } catch(IOException e) {
                exitWithError(e.getMessage());
            }
        }
        printLine("Goodbye.");
    }

    private void runCommand(String line) {
        if(line == null || line.length() == 0) {
            return;
        }

        String commandString = null;
        String options = null;
        if( line.indexOf(' ') == -1 ) {
            commandString = line;
        } else {
            commandString = line.substring(0, line.indexOf(' '));
            options = line.substring(line.indexOf(' ')+1);
        }

		com.marklogic.shell.Command command = null;
		try {
			String[] path = properties.getStringArray("path");
			if( path != null && path.length > 0 ) {
				for(int i = 0; i < path.length; i++) {
					try {
						Class commandClass = Class.forName(path[i] + "." + commandString);
						command = (com.marklogic.shell.Command)commandClass.newInstance(); 
						break;
					} catch(Exception ignored) {
						command = null;
					}
				}
			}
		} catch(Exception ignored) { }

		// Try the default system path as a last attempt.
		if( command == null ) {
			try {
				Class commandClass = Class.forName(SYSTEM_PATH + "." + commandString);
				command = (com.marklogic.shell.Command)commandClass.newInstance(); 
			} catch(Exception ignored) {
				command = null;
			}
		}

        if( command != null) {
            try {
            		if(options != null && options.length() > 0 && options.charAt(options.length()-1) == ';') {
            			options = options.substring(0, options.length()-1);
            		}
				command.execute(this, options);
			} catch(Exception e) {
				if(debug()) {
					e.printStackTrace();
				}
                printLine("Failed to run command '" + commandString + "': " + e.getMessage());
            }
        } else {
            StringBuffer xquery = new StringBuffer();
            boolean clearBuffer = false;
            while( ';' != line.charAt(line.length()-1)) {
                if( line.length() >= 2) {
                    if( '\\' == line.charAt(line.length()-2) &&
                        'c' == line.charAt(line.length()-1)) {
                            clearBuffer = true;
                            break;
                    }
                }
                xquery.append(line);
                try {
                    line = console.readLine("   -> ");
                    if( line == null || line.length() == 0 ) {
                        line = " ";
                    }
					line = " " + line;
                } catch(IOException e) {
                    exitWithError(e.getMessage());
                }
            }

            xquery.append(line.substring(0, line.length()-1));
            if( !clearBuffer ) {
            		try {
            			ShellQuery squery = new ShellQuery(xquery.toString(), this);
					runXQuery(squery.asXQuery());
            		} catch(XQException e) {
            			printError(e);
            		}
            }
        }
    }

    /**
     * Runs an XQuery statement scrolling the result and displays the execution time
     * in seconds for the query.
     */
    public void runXQuery(XQuery xquery) {
        runXQuery(xquery, true, true);
    }

    /**
     * Runs an XQuery statement and optionally scrolls the result and optionally shows the 
     * execution time in seconds for the query.
     * @param xquery
     * @param scrollResult
     * @param showTime
     */
    public void runXQuery(XQuery xquery, boolean scrollResult, boolean showTime) {
        try {
            XQDataSource dataSource = getDataSource();
            XQRunner runner = dataSource.newSyncRunner();
            long start = System.currentTimeMillis();
            XQResult result = runner.runQuery(xquery);
            long end = System.currentTimeMillis();
            double total = (double)(end - start)/1000;

            try {
				boolean stop = false;
				int lineCount = 0;
				for( int x = 0; x < result.getSize(); x++) {
					XQResultItem item = result.getItem(x);
					
					BufferedReader reader = getReader(item);
					String line = null;
					while((line = reader.readLine()) != null) {
						lineCount++;
						printLine(line);
						if( scrollResult && checkStopScroll(lineCount) ) {
							stop = true;
							break;
						}
					}
					reader.close();
					if(stop) {
						break;
					}
				}
            } catch(IOException e) {
                printLine("Failed to read result string: " + e.getMessage());
            }

            DecimalFormat format = new DecimalFormat("###,##0.00");
            if(showTime) {
                printLine("\nDone (" + format.format(total) + " sec)");
            }
        } catch(XQException e) {
			printError(e);
        }
    }
    
    protected BufferedReader getReader(XQResultItem item) throws XQException, UnsupportedEncodingException {
		BufferedReader reader;
		if(item.getType().equals(XQVariableType.NODE) && "true".equals(properties.getString("pretty-print-xml"))) {
			Document doc = item.asJDom();
			Format format = Format.getPrettyFormat();
			format.setLineSeparator(NEWLINE);
			format.setOmitDeclaration(true);
			format.setOmitEncoding(true);
			XMLOutputter xmlout = new XMLOutputter(format);
			String docString = xmlout.outputString(doc);
			reader = new BufferedReader(new StringReader(docString));
		} else {
			// needed to add UTF-8 instead of relying on the default char set for the system
			reader = new BufferedReader(new InputStreamReader(item.asStream(), "UTF-8"));
		}
		
		return reader;
	}
    
    protected boolean debug() {
    		return "true".equals(properties.getString("debug"));
    }
    
    protected boolean checkStopScroll(int lineCount) {
    		boolean stop = false;
    		
		int scroll = DEFAULT_SCROLL;
		try {    
			scroll = properties.getInt("scroll");   
		} catch(ConversionException e) { }
		
		if(scroll == 0) {
			return false;
		}
		
		if(scroll < 0) {
			scroll = DEFAULT_SCROLL;
		}
		
		if( (lineCount % scroll) == 0 ) {
			try {
				print("***** press <space> to continue ******");
				int key = console.readVirtualKey();
				if(key != 32) {
					stop = true;
				}
				printLine("");
			} catch(IOException e) { }
		}
		
		return stop;
    }
    
    /**
     * Returns an XQDataSource using the user, password, host, and port defined
     * in the properties.
     * @throws XQException
     */
    public XQDataSource getDataSource() throws XQException {
        XQFactory factory = new XQFactory();
        return factory.newDataSource(
                                     properties.getString("host"), 
                                     properties.getInt("port", DEFAULT_PORT), 
                                     properties.getString("user"),
                                     properties.getString("password")
                                     );
    }

    /**
     * Tests the connection to Mark Logic.  
     * @param user user name
     * @param password password
     * @param host host
     * @param port port (defaults to 8004)
     * @throws XQException
     */
	public void checkConnection(String user, String password, String host, int port) throws XQException {
		XQFactory factory = new XQFactory();
		XQDataSource dataSource = factory.newDataSource( host, port, user, password);
		XQRunner runner = dataSource.newSyncRunner();
		XQResult result = runner.runQuery(dataSource.newQuery("xdmp:database-name(xdmp:database())"));
		XQResultItem item = result.nextItem();
		properties.setProperty("default-database", item.asString());
	}

	/**
	 * Prints an error to the console.
	 */
	public void printError(String message) {
		printLine(message);
	}

	/**
	 * Print an error to the screen that was caused by an Exception. If the Exception is
	 * an instance of XDBCXQueryException it will display some verbose information about
	 * the XQuery error including line number, context item/postion, uri, variable bindings.
	 * For all other exceptions just the message is displayed. 
	 */
	public void printError(Exception e) {
		Throwable t = e.getCause();

		if( t instanceof XDBCXQueryException) {
			printLine("XQuery error: ");
			XDBCXQueryException ex = (XDBCXQueryException)e.getCause();
			printLine("Format String: " + ex.getFormatString());
			printLine("--STACK DUMP--");
			XDBCXQueryException.Frame[] data = ex.getStack();
			if( data != null ) {
				for(int i = 0; i < data.length; i++) {
					printLine("line number:  " + data[i].getLine());
					printLine("context item:  " + data[i].getContextItem());
					printLine("context position:  " + data[i].getContextPosition());
					printLine("uri:  " + data[i].getURI());
					printLine("variable bindings: ");
					XDBCXQueryException.Variable[] vars = data[i].getVariables();
					if( vars != null ) {
						for(int x = 0; x < vars.length; x++) {
							printLine(vars[x].getName() + " = " + vars[x].getValue());
						}
					}
				}
			}
		} else {
			printLine("Error: " + e.getMessage());
		}
	}

	/**
	 * Print the welcome message for the shell.
	 */
    private void printWelcome() {
        printLine("Welcome to cqsh " + VERSION + ".  Commands end with ';'");
        printLine("Connected to host: " + properties.getString("host") + ":" + properties.getInt("port"));
        printLine("");
        printLine("Type 'help' for command help. Type '\\c' to clear the buffer.");
    }

    /**
     * Print the default help screen which will list all the command line options
     * supported by the shell and a brief description of each.
     */
    public void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "cqsh", options );
        System.exit(0);
    }

    /**
     * Print a message along with the default help screen.
     */
    public void printHelp(String message) {
        printLine(message);
        printHelp();
    }

    /**
     * Print a message to the console.
     */
    public void print(String message) {
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
    public void printLine(String message) {
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
		printLine("Goodbye.");
		System.exit(0);
	}

	/**
	 * The console reader used to read lines of input from the user.
	 */
	public jline.ConsoleReader getConsole() {
		return console;
	}

	/**
	 * The history file used to store command line history. The default
	 * for this file is in $HOME/.cqsh_history.
	 */
	public File getHistoryFile() {
		return historyFile;
	}

	/**
	 * The command line options passed into the shell when it was first launched.
	 */
	public Options getOptions() {
		return options;
	}

	/**
	 * The configuration properties for the shell environment. When the shell is first 
	 * created the file $HOME/.cqshrc is read in if exists. While the shell is running
	 * the properties can be used as a place to store environment variables similar to 
	 * unix shells.
	 */
	public PropertiesConfiguration getProperties() {
		return properties;
	}
}
