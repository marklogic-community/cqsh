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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FileScanner {

    private FileScanner() {
    }

    public static List findFiles(String path) {
        List fileList = new ArrayList();

        File file = new File(path);
        if(!file.exists()) {
            File dir = file.getParentFile();
            if(dir == null) {
                dir = new File(System.getProperty("user.dir"));
            }
            FileMatcher fileMatch = new FileMatcher(file.getName());

            File[] files = dir.listFiles(fileMatch);
            if(files != null) {
                for(int i = 0; i < files.length; i++) {
                    fileList.add(files[i]);
                }
            }
        } else if(file.isDirectory()) {
            FileMatcher fileMatch = new FileMatcher();

            File[] files = file.listFiles(fileMatch);
            if(files != null) {
                for(int i = 0; i < files.length; i++) {
                    fileList.add(files[i]);
                }
            }
        } else if(file.isFile()) {
            fileList.add(file);
        }

        return fileList;
    }
}

class FileMatcher implements FilenameFilter {

    private Pattern regex;

    public FileMatcher() {
    }

    public FileMatcher(String pattern) {
        StringBuffer buf = new StringBuffer("^");
        for(int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch(c) {
            case '*':
                buf.append(".*");
                break;
            // case '.': buf.append("\\."); break;
            default:
                buf.append(c);
                break;
            }
        }
        buf.append("$");
        try {
            regex = Pattern.compile(buf.toString());
        } catch(PatternSyntaxException e) {
            throw new RuntimeException("Failed to parse regex: "
                    + buf.toString() + ": " + e.getMessage());
        }
    }

    public boolean accept(File dir, String fileName) {
        File f = new File(dir, fileName);

        if(regex != null) {
            return regex.matcher(fileName).matches();
        }

        if(f.exists() && f.isFile()) {
            return true;
        }

        return false;
    }
}
