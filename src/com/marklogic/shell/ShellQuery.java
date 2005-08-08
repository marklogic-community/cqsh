/*
 * Copyright 2005 Andrew Bruno <aeb@qnot.org> 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at 
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.marklogic.xqrunner.XQDataSource;
import com.marklogic.xqrunner.XQVariableType;
import com.marklogic.xqrunner.XQuery;
import com.marklogic.xqrunner.XQException;

public class ShellQuery {
	private String query;
	private List vars;
	private Shell shell;
	
	public ShellQuery(String query, Shell shell) {
		this.query = query;
		this.shell = shell;
		this.vars = new ArrayList();
		vars.add("define variable $query as xs:string external ");
		String db = shell.getProperties().getString("database");
		if(db != null && db.length() > 0) {
			vars.add("define variable $db as xs:string external ");
		}
	}
	
	public ShellQuery(Shell shell) {
		this(null, shell);
	}
	
	public void addVariable(String name, String type) {
		vars.add("define variable " + name + " as " + type + " external ");
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public List getVars() {
		return vars;
	}
	
	public XQuery asXQuery() throws XQException {
		StringBuffer queryStr = new StringBuffer();
		for(Iterator i = vars.iterator(); i.hasNext();) {
			queryStr.append((String)i.next());
		}
		String db = shell.getProperties().getString("database");
		if(db != null && db.length() > 0) {
			queryStr.append("xdmp:eval-in($query, xdmp:database($db)) ");
		} else {
			queryStr.append("xdmp:eval-in($query, xdmp:database()) ");
		}	
		XQDataSource dataSource = shell.getDataSource();
		XQuery xquery = dataSource.newQuery(queryStr.toString());
		xquery.setVariable(dataSource.newVariable("query", XQVariableType.XS_STRING, query));
		if(db != null && db.length() > 0) {
			xquery.setVariable(dataSource.newVariable("db", XQVariableType.XS_STRING, db));
		}
		
		return xquery;
	}
}
