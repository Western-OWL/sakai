package org.sakaiproject.gradebookng.business.owl.importExport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.gradebookng.business.model.GbUser;
import org.sakaiproject.gradebookng.business.model.ImportedColumn;
import org.sakaiproject.gradebookng.business.model.ImportedRow;
import org.sakaiproject.gradebookng.business.model.ImportedSpreadsheetWrapper;
import org.sakaiproject.gradebookng.business.util.ImportGradesHelper;

/**
 * Delegate for the ImportGradesHelper to handle DPC files.
 * 
 * @author plukasew
 */
@Slf4j
public class DpcDelegate
{
	private static final String DPC_FILE_EXT = ".dpc";
	private static final String DPC_STUDENT_ID_COLUMN_HEADER = "Student ID";
	public static final String DPC_DEFAULT_GRADE_ITEM_TITLE = "Gradebook Item Import";
	private static final String DPC_DEFAULT_MAX_GRADE = "100";
    private static final String TAB_CHAR = "\t";

	public static boolean isDpc(String filename)
	{
		return filename.endsWith(DPC_FILE_EXT);
	}

	/**
	 * Parse a DPC into a list of {@link ImportedRow} objects.
	 *
	 * @param is InputStream of the data to parse
	 * @return
	 * @throws IOException
	 */
	public static ImportedSpreadsheetWrapper parseDPC(final InputStream is, final Map<String, GbUser> studentNumMap) throws IOException {

		// Create the header row (which isn't included in the actual DPC file) with default values for the title and max points.
		// The user has the opportunity to change the title and max points in the confirmation step later on.
		Map<Integer, ImportedColumn> columnMapping = new LinkedHashMap<>(2);
		ImportedColumn studentNumColumn = new ImportedColumn();
		studentNumColumn.setType(ImportedColumn.Type.STUDENT_NUMBER);
		studentNumColumn.setColumnTitle(DPC_STUDENT_ID_COLUMN_HEADER);
		columnMapping.put(0, studentNumColumn);
		// assuming we have students, but only add the grade column later if we need it

		// Parse the file
		final List<String[]> parsedRows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {

				// Skip empty lines
				if (isEmptyLine(line)) {
					continue;
				}

				parsedRows.add(line.split(TAB_CHAR));
			}
		} catch (final IOException ex) {
			log.debug(ex.getMessage());
		}

		// detect if we have a grade column and add it to the header if so
		if (parsedRows.stream().anyMatch(row -> row.length > 1))
		{
			ImportedColumn gradebookItemColumn = new ImportedColumn();
			gradebookItemColumn.setType(ImportedColumn.Type.GB_ITEM_WITH_POINTS);
			gradebookItemColumn.setColumnTitle(DPC_DEFAULT_GRADE_ITEM_TITLE);
			gradebookItemColumn.setPoints(DPC_DEFAULT_MAX_GRADE);
			columnMapping.put(1, gradebookItemColumn);
		}

		// build the ImportedRow objects
		final List<ImportedRow> rows = new ArrayList<>(parsedRows.size());
		for (String[] lineValues : parsedRows)
		{
			final ImportedRow row = ImportGradesHelper.mapLine(lineValues, columnMapping, studentNumMap, null);
			if (row != null) {
				rows.add(row);
			}
		}

		// Create the ImportedSpreadsheetWrapper object
		final ImportedSpreadsheetWrapper importedGradeWrapper = new ImportedSpreadsheetWrapper();
		if (rows.isEmpty())	{
			importedGradeWrapper.setColumns(new ArrayList<>());
		} else {
			importedGradeWrapper.setColumns(new ArrayList<>(columnMapping.values()));
		}
		importedGradeWrapper.setRows(rows, studentNumMap);
		return importedGradeWrapper;
	}

	/**
     * Determines if the given string is "empty".
     *
     * @param line
     * @return
     */
    public static boolean isEmptyLine( String line )
    {
		// ignore double quote, tab, comma, and semicolon characters
        return StringUtils.trimToEmpty( line.replaceAll( "[\"\t,;]", "" ) ).isEmpty();
    }
}
