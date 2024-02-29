/**********************************************************************************
 * $URL: $
 * $Id: $
 ***********************************************************************************
 *
 * Author: Eric Jeney, jeney@rutgers.edu
 *
 * Copyright (c) 2010 Rutgers, the State University of New Jersey
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");                                                                
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/


package org.sakaiproject.lessonbuildertool.tool.view;

import uk.org.ponder.rsf.viewstate.SimpleViewParameters;

public class ExportCCViewParameters extends SimpleViewParameters {

	public static final String VERSION_DEFAULT = "1.3";
	public static final String BANK_DEFAULT = "0";
	public static final String DRAFT_DEFAULT = "0";
	public static final String TOOLS_DEFAULT = "asn,frm,lsn,lti,res,sam";

	private boolean exportcc = false;
	private String version = VERSION_DEFAULT;
	private String bank = BANK_DEFAULT;
	private String draft = DRAFT_DEFAULT;
	private String tools = TOOLS_DEFAULT;

	public ExportCCViewParameters() {
		super();
	}

	public ExportCCViewParameters(String VIEW_ID) {
		super(VIEW_ID);
	}

        public void setExportcc(boolean b) {
	    exportcc = b;
	}

	public boolean getExportcc() {
	    return exportcc;
	}

        public void setVersion(String s) {
	    version = s;
	}

	public String getVersion() {
	    return version;
	}

        public void setBank(String s) {
	    bank = s;
	}

	public String getBank() {
	    return bank;
	}

	public void setDraft(String s) {
		draft = s;
	}

	public String getDraft() {
		return draft;
	}

	public void setTools(String s) {
		tools = s;
	}

	public String getTools() {
		return tools;
	}
}
