/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
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

package org.sakaiproject.tool.assessment.integration.helper.ifc;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import org.sakaiproject.service.gradebook.shared.GradebookExternalAssessmentService;
import org.sakaiproject.service.gradebook.shared.GradebookExternalAssessmentService.ExternalAssignmentInfo;
import org.sakaiproject.tool.assessment.data.dao.assessment.PublishedAssessmentData;
import org.sakaiproject.tool.assessment.data.dao.grading.AssessmentGradingData;
import org.sakaiproject.tool.assessment.data.ifc.assessment.PublishedAssessmentIfc;

/**
 * <p>Description:
 * This is a context implementation helper delegate interface for
 * the GradebookService class.  Using Spring injection via the
 * integrationContext.xml selected by the build process for the implementation.
 * </p>
 * <p>Sakai Project Copyright (c) 2005</p>
 * <p> </p>
 * @author Ed Smiley <esmiley@stanford.edu>
 */
public interface GradebookServiceHelper extends Serializable
{
  public enum ExternalTitleValidationResult { VALID, INVALID_CHARS, DUPLICATE_TITLE };

  public boolean gradebookExists(String gradebookUId, GradebookExternalAssessmentService g);
  
  public boolean isGradebookExist(String SiteId);

  public void removeExternalAssessment(String gradebookUId,
     String publishedAssessmentId, GradebookExternalAssessmentService g) throws Exception;

  public boolean addToGradebook(PublishedAssessmentData publishedAssessment, Long categoryId,
		  GradebookExternalAssessmentService g) throws Exception;

  public boolean updateGradebook(PublishedAssessmentIfc publishedAssessment,
		  GradebookExternalAssessmentService g) throws Exception;

  public boolean isAssignmentDefined(String assessmentTitle,
		  GradebookExternalAssessmentService g) throws Exception;


  public Optional<ExternalAssignmentInfo> getExternalAssignmentInfo(String gradebookUId, String publishedAssessmentId,
		  GradebookExternalAssessmentService g) throws Exception;

  public void updateExternalAssessmentScore(AssessmentGradingData ag,
		  GradebookExternalAssessmentService g) throws Exception;
  
  public void updateExternalAssessmentScores(Long publishedAssessmentId, final Map<String, Double> studentUidsToScores,
		  GradebookExternalAssessmentService g) throws Exception;
  
  public void updateExternalAssessmentComment(Long publishedAssessmentId, String studentUid, String comment, 
		  GradebookExternalAssessmentService g) throws Exception;
  
  public String getAppName();

  /**
   * Validates the assessment title against the Gradebook's rules for item names. Intended for avoiding
   * exceptions before enabling a gradebook integration for the first time, or changing an assessment title
   * that is already integrated. Do not use this if you have an existing gb item and have not changed the title,
   * the result will be duplicate name because it will match against your existing item.
   * OWLTODO: consider if refactoring this and the related GEAS call to not match against itself is valuable.
   * @param gradebookUid the gradebook UUID
   * @param assessmentTitle the assessment title
   * @param g the GradebookExternalAssessmentService
   * @return enum value representing the validation result
   * @throws Exception if gradebook with given UUID is not found
   */
  public ExternalTitleValidationResult validateNewExternalTitle(String gradebookUid, String assessmentTitle,
		  GradebookExternalAssessmentService g) throws Exception;

}
