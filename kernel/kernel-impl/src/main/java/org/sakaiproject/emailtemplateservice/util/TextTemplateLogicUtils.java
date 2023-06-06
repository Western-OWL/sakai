/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright 2006, 2007 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
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

package org.sakaiproject.emailtemplateservice.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This is a weird location but it will have to do for now,
 * this handles processing of text templates
 * 
 * @author Aaron Zeckoski (aaronz@vt.edu)
 */
public class TextTemplateLogicUtils {

    private static Map<String, Object> trimNullValuesToEmptyString(Map<String, Object> replacementValues) {
        Map<String, Object> retMap = new HashMap(replacementValues.size());
        for (Entry<String, Object> entry : replacementValues.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                retMap.put(entry.getKey(), StringUtils.trimToEmpty((String) value));
            } else if (value == null) {
                retMap.put(entry.getKey(), "");
            } else {
                retMap.put(entry.getKey(), entry.getValue());
            }
        }

        return retMap;
    }

   /**
    * Handles the replacement of the variable strings within textual templates and
    * also allows the setting of variables for the control of logical branching within
    * the text template as well<br/>
    * Uses and expects freemarker (http://freemarker.org/) style templates 
    * (that is using ${name} as the marker for a replacement)<br/>
    * NOTE: These should be compatible with Velocity (http://velocity.apache.org/) templates
    * 
    * @param textTemplate a freemarker/velocity style text template,
    * cannot be null or empty string
    * @param replacementValues a set of replacement values which are in the map like so:<br/>
    * key => value (String => Object)<br/>
    * username => aaronz<br/>
    * course_title => Math 1001 Differential Equations<br/>
    * @return the processed template
    */
   public static String processTextTemplate(String textTemplate, Map<String, Object> replacementValues, String templateName) {
      if (MapUtils.isEmpty(replacementValues)) {
         return textTemplate;
      } else {
          replacementValues = trimNullValuesToEmptyString(replacementValues);
      }

      if (StringUtils.isBlank(textTemplate)) {
         throw new IllegalArgumentException("The textTemplate cannot be null or empty string, " +
         		"please pass in at least something in the template or do not call this method");
      }

      // setup freemarker
      Configuration cfg = new Configuration();

      // Specify how templates will see the data-model
      cfg.setObjectWrapper(new DefaultObjectWrapper()); 

      // get the template
      Template template;
      try {
         template = new Template(templateName, new StringReader(textTemplate), cfg);
      } catch (IOException e) {
         throw new RuntimeException("Failure while creating freemarker template", e);
      }

      Writer output = new StringWriter();
      try {
         template.process(replacementValues, output);
      } catch (TemplateException e) {
         throw new RuntimeException("Failure while processing freemarker template", e);
      } catch (IOException e) {
         throw new RuntimeException("Failure while sending freemarker output to stream", e);
      }

      return output.toString();
   }
}
