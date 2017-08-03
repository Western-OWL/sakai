package org.sakaiproject.gradebookng.tool.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.sakaiproject.gradebookng.business.importExport.UserIdentificationReport;
import org.sakaiproject.gradebookng.business.model.ImportedSpreadsheetWrapper;
import org.sakaiproject.gradebookng.business.model.ProcessedGradeItem;
import org.sakaiproject.service.gradebook.shared.Assignment;

/**
 * Model object used for the import wizard panels
 */
public class ImportWizardModel implements Serializable {

	/**
	 * Representation of the spreadsheet
	 */
	@Getter
	@Setter
	private ImportedSpreadsheetWrapper spreadsheetWrapper;

	/**
	 * List of items that have been uploaded
	 */
	@Getter
	@Setter
	private List<ProcessedGradeItem> processedGradeItems;

	/**
	 * List of items that have been selected to import
	 */
	@Getter
	@Setter
	private List<ProcessedGradeItem> selectedGradeItems;

	/**
	 * Which step is the new gb item creation currently on
	 */
	@Getter
	@Setter
	private int step;

	/**
	 * How many total steps are in the new gb item creation portion of the wizard
	 */
	@Getter
	@Setter
	private int totalSteps;

	/**
	 * List of items from the spreadsheet that need to be created first
	 */
	@Getter
	@Setter
	private List<ProcessedGradeItem> itemsToCreate;

	/**
	 * List of items from the spreadsheet that just need their data updated
	 */
	@Getter
	@Setter
	private List<ProcessedGradeItem> itemsToUpdate;

	/**
	 * List of items from the spreadsheet that need to have the assignment updated and their data updated
	 */
	@Getter
	@Setter
	private List<ProcessedGradeItem> itemsToModify;

	/**
	 * Maps items from the spreadsheet to the assignments that need to be created once the wizard has been completed
	 */
	@Getter
	@Setter
	private Map<ProcessedGradeItem, Assignment> assignmentsToCreate = new LinkedHashMap<>();

	/**
	 * The {@link UserIdentificationReport} generated during parsing of the raw import file
	 */
	@Getter
	@Setter
	private UserIdentificationReport userReport;

	/**
	 * Whether the imported spreadsheet represents users as anonymous grading IDs
	 */
	@Getter
	@Setter
	private boolean isContextAnonymous;
}
