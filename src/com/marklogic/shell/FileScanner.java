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
import java.util.*;
import java.util.regex.*;

import org.jdom.*;
import org.jdom.input.SAXBuilder;

public class FileScanner {
    
    private FileScanner() { }

    public static List findFiles(String path) {
        List fileList = new ArrayList();

        File file = new File(path);
        if( !file.exists() ) {
            File dir = file.getParentFile();
            if(dir == null) {
                dir = new File(System.getProperty("user.home"));
            }
            FileMatcher fileMatch = new FileMatcher(file.getName());

            File[] files = dir.listFiles(fileMatch);
            if( files != null ) {
                for(int i = 0; i < files.length; i++) {
                    if( isWellFormed(files[i]) ){
                        fileList.add(files[i]);
                    }
                }
            }
        } else if(file.isDirectory()) {
            FileMatcher fileMatch = new FileMatcher();

            File[] files = file.listFiles(fileMatch);
            if( files != null ) {
                for(int i = 0; i < files.length; i++) {
                    if( isWellFormed(files[i]) ){
                        fileList.add(files[i]);
                    }
                }
            }
        } else if(file.isFile()) {
            if(isWellFormed(file)) {
                fileList.add(file);
            }
        }

        return fileList;
    }

    private static boolean isWellFormed(File file) {
        SAXBuilder saxBuilder = new SAXBuilder(false);
        try {
            Document doc = saxBuilder.build(file);
        } catch (JDOMException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}

class FileMatcher implements FilenameFilter {
    
    Pattern regex;

    public FileMatcher() { }

    public FileMatcher(String pattern) { 
        StringBuffer buf = new StringBuffer("^");
        for(int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch(c) {
                case '*':  buf.append(".*"); break;
//                case '.':  buf.append("\\."); break;
                default:   buf.append(c); break;
            }
        }
        buf.append("$");
        try {
            regex = Pattern.compile(buf.toString());
        } catch(PatternSyntaxException e) {
            throw new RuntimeException("Failed to parse regex: " + buf.toString() + ": " + e.getMessage());
        }
    }

    public boolean accept(File dir, String fileName) {
        File f = new File(dir, fileName);

        if( regex != null ) {
            return regex.matcher(fileName).matches();
        }

        if( f.exists() && f.isFile() ) {
            return true;
        }

        return false;
    }
}
