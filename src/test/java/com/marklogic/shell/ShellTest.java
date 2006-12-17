package com.marklogic.shell;

import junit.framework.TestCase;

public class ShellTest extends TestCase {
    
    public void testConnect() throws Exception {
        Shell shell = new Shell();
        shell.checkConnection();
        try {
            // check user that doesn't exist
            shell.checkConnection("_xxx_", "123", null, -1);
        } catch(ShellException e) {
            //passed
        }
    }
    
    public void testRun() throws Exception {
        Shell.main(new String[]{"-h"});
    }
}
