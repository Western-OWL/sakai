package org.sakaiproject.gradebookng.tool.owl.model;

import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.sakaiproject.gradebookng.business.GbCategoryType;
import org.sakaiproject.gradebookng.business.model.GbGradeInfo;
import org.sakaiproject.gradebookng.business.owl.OwlBusinessService;
import org.sakaiproject.gradebookng.business.owl.OwlGbStudentGradeInfo;
import org.sakaiproject.gradebookng.business.owl.anon.OwlAnonGradingService.AnonStatus;
import org.sakaiproject.gradebookng.business.owl.finalgrades.FinalGradeFormatter;
import org.sakaiproject.gradebookng.business.util.EventHelper;
import org.sakaiproject.gradebookng.business.util.FormatHelper;
import org.sakaiproject.gradebookng.tool.owl.panels.importExport.OwlExportPanel;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.CategoryDefinition;
import org.sakaiproject.service.gradebook.shared.SortType;

/**
 *
 * @author plukasew
 */
public class OwlExportFileBuilder implements Serializable
{
	@Builder
	public static class CsvType
	{
		public boolean anon, cg; // is anon / course grade allowed (with respect to anon status)
	}

	public enum OwlExportFormat { CSV, ZIP };

	public static final String IGNORE_COLUMN_PREFIX = "#";
	public static final String COMMENTS_COLUMN_PREFIX = "*";
	public static final char CSV_SEMICOLON_SEPARATOR = ';';

	private final String decimalSeparator, decimalEnd;

	public OwlExportFileBuilder(String decimalSeparator)
	{
		this.decimalSeparator = decimalSeparator;
		decimalEnd = decimalSeparator + "0";
	}

	public File buildFile(OwlExportPanel panel, OwlExportConfig config, List<Assignment> allItems, List<CategoryDefinition> cats)
	{
		return buildFile(panel, config, false, false, allItems, cats);
	}

	public File buildCustomFile(OwlExportPanel panel, OwlExportConfig config, List<Assignment> allItems, List<CategoryDefinition> cats)
	{
		return buildFile(panel, config, true, false, allItems, cats);
	}

	public File buildRevealedFile(OwlExportPanel panel, OwlExportConfig config, List<Assignment> allItems, List<CategoryDefinition> cats)
	{
		return buildFile(panel, config, false, true, allItems, cats);
	}

	public File buildCustomRevealedFile(OwlExportPanel panel, OwlExportConfig config, List<Assignment> allItems, List<CategoryDefinition> cats)
	{
		return buildFile(panel, config, true, true, allItems, cats);
	}

	private File buildFile(OwlExportPanel panel, OwlExportConfig config, boolean isCustom, boolean isRevealed,
			List<Assignment> allItems, List<CategoryDefinition> cats)
	{
		OwlBusinessService owlbus = panel.getOwlbus();

		// figure out what kind of export we have (normal/anon/mixed)
		AnonStatus anonStatus = isRevealed ? AnonStatus.NORMAL : owlbus.anon.detectAnonStatus(allItems);

		// tell the panel to build the file name
		OwlExportFormat format = anonStatus == AnonStatus.MIXED ? OwlExportFormat.ZIP : OwlExportFormat.CSV;
		panel.buildFileName(format, isCustom);

		// Step 3: Determine where the course grade goes
		boolean isCourseGradePureAnon = false;
		if (anonStatus == AnonStatus.MIXED)
		{
			isCourseGradePureAnon = owlbus.anon.isCourseGradePureAnonForAllAssignments(allItems);
		}

		// Step 4: create the file
		File tempFile;
		try
		{
			String extension = "." + format.name().toLowerCase();
			tempFile = File.createTempFile("gradebookTemplate", extension);
			String isoCharsetName = StandardCharsets.ISO_8859_1.name();

			if (anonStatus == AnonStatus.MIXED) // zip file
			{
				// partition the items and categories into anon (true) and normal (false)
				Map<Boolean, List<Assignment>> itemMap = allItems.stream().collect(Collectors.partitioningBy(Assignment::isAnon));
				List<Long> catsWithNormalItems = itemMap.get(false).stream().map(i -> i.getCategoryId()).filter(id -> id != null && id >= 0).collect(Collectors.toList());
				List<Long> pureAnonCats = itemMap.get(true).stream().map(i -> i.getCategoryId())
						.filter(id -> id != null && id >= 0 && !catsWithNormalItems.contains(id)).collect(Collectors.toList());
				Map<Boolean, List<CategoryDefinition>> catMap = cats.stream().collect((Collectors.partitioningBy(c -> pureAnonCats.contains(c.getId()))));

				try (FileOutputStream fos = new FileOutputStream(tempFile);
						ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.ISO_8859_1);
						OutputStreamWriter normalWriter = new OutputStreamWriter(zos, isoCharsetName);
						CSVWriter normalCsv = getCsvWriter(normalWriter);
						OutputStreamWriter anonWriter = new OutputStreamWriter(zos, isoCharsetName);
						CSVWriter anonCsv = getCsvWriter(anonWriter))
				{
						zos.putNextEntry(new ZipEntry("gradebookExport_normal.csv"));
						CsvType type = CsvType.builder().anon(false).cg(!isCourseGradePureAnon).build();
						writeLines(normalCsv, getCsvContents(owlbus, type, config, panel, itemMap.get(false), catMap.get(false)));
						normalCsv.flush();
						zos.closeEntry();

						zos.putNextEntry(new ZipEntry("gradebookExport_anonymous.csv"));
						type = CsvType.builder().anon(true).cg(isCourseGradePureAnon).build();
						writeLines(anonCsv, getCsvContents(owlbus, type, config, panel, itemMap.get(true), catMap.get(true)));
						anonCsv.flush();
						zos.closeEntry();
				}
			}
			else // normal or pure anon csv file
			{
				try (FileOutputStream fos = new FileOutputStream(tempFile);
						OutputStreamWriter osw = new OutputStreamWriter(fos, isoCharsetName);
						CSVWriter csvWriter = getCsvWriter(osw))
				{
					CsvType type = CsvType.builder().anon(anonStatus == AnonStatus.ANON).cg(true).build();
					writeLines(csvWriter, getCsvContents(owlbus, type, config, panel, allItems, cats));
				}
			}
		}
		catch (IOException ioe)
		{
			Session.get().error("Unable to create export file");
			throw new RuntimeException(ioe);
		}

		EventHelper.postExportEvent(panel.getGradebook(), isCustom);

		return tempFile;
	}

	private void writeLines(final CSVWriter writer, List<List<String>> lines)
	{
		lines.stream().forEach(line -> { writer.writeNext(line.toArray(new String[] {})); });
	}

	private CSVWriter getCsvWriter(OutputStreamWriter outstream)
	{
		char csvDelimiter = ".".equals(decimalSeparator) ? CSVWriter.DEFAULT_SEPARATOR : CSV_SEMICOLON_SEPARATOR;
		return new CSVWriter(outstream, csvDelimiter, CSVWriter.DEFAULT_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.RFC4180_LINE_END);
	}

	private List<List<String>> getCsvContents(OwlBusinessService owlbus, CsvType type, OwlExportConfig config, OwlExportPanel panel,
			List<Assignment> items, List<CategoryDefinition> cats)
	{
		List<List<String>> csvContents = new ArrayList<>();

		buildHeader(type, csvContents, items, cats, config, panel);

		List<OwlGbStudentGradeInfo> matrix = owlbus.buildGradeMatrixForImportExport(items, config.group, type.anon);
		buildRows(type, csvContents, items, cats, matrix, config, panel);

		return csvContents;
	}

	/**
	 *
	 * @param type
	 * @param contents
	 * @param items the items to include
	 * @param config
	 * @param panel
	 */
	private void buildHeader(CsvType type, List<List<String>> contents, List<Assignment> items, List<CategoryDefinition> cats,
			OwlExportConfig config, OwlExportPanel panel)
	{
		List<String> header = new ArrayList<>();

		if (type.anon)
		{
			addAnonUserDataToHeader(header, panel);
		}
		else
		{
			addNormalUserDataToHeader(header, config, panel);
		}

		// no gradebook items, give a template
		if (items.isEmpty())
		{
			// with points
			header.add(String.join(" ", panel.getString("importExport.export.csv.headers.example.points"), "[100]"));

			// no points
			header.add(panel.getString("importExport.export.csv.headers.example.nopoints"));

			// points and comments
			header.add(String.join(" ", COMMENTS_COLUMN_PREFIX, panel.getString("importExport.export.csv.headers.example.pointscomments"), "[50]"));

			// ignore
			header.add(String.join(" ", IGNORE_COLUMN_PREFIX, panel.getString("importExport.export.csv.headers.example.ignore")));
		}

		// build column header
		for (int i = 0; i < items.size(); ++i)
		{
			final Assignment item = items.get(i);
			final String itemPoints = FormatHelper.formatGradeForDisplay(item.getPoints().toString());
			String externalPrefix = "";
			if (item.isExternallyMaintained()) {
				externalPrefix = IGNORE_COLUMN_PREFIX;
			}
			if (config.includeGradeItemScores)
			{
				header.add(externalPrefix + item.getName() + " [" + StringUtils.removeEnd(itemPoints, decimalEnd) + "]");
			}
			if (config.includeGradeItemComments)
			{
				header.add(String.join(" ", externalPrefix, COMMENTS_COLUMN_PREFIX, item.getName()));
			}
			if (config.includeCategoryAverages && panel.getSortType() == SortType.SORT_BY_CATEGORY)
			{
				// read ahead and insert a heading for the category if necessary
				Optional<Assignment> nextItem = (i + 1) < items.size() ? Optional.of(items.get(i + 1)) : Optional.empty();
				if (item.getCategoryId() != null && (!nextItem.isPresent() || !item.getCategoryId().equals(nextItem.get().getCategoryId())))
				{
					Optional<CategoryDefinition> cat = cats.stream().filter(c -> item.getCategoryId().equals(c.getId())).findAny();
					cat.ifPresent(c -> addCategoryAvgToHeader(header, c, panel));
				}
			}
		}

		if (config.includeCategoryAverages && panel.getSortType() != SortType.SORT_BY_CATEGORY)
		{
			for (CategoryDefinition cat : cats)
			{
				// skip categories that have no associated items
				if (items.stream().map(i -> i.getCategoryId()).filter(Objects::nonNull).anyMatch(id -> cat.getId().equals(id)))
				{
					addCategoryAvgToHeader(header, cat, panel);
				}
			}
		}

		if (type.cg)
		{
			addCourseGradeDataToHeader(header, config, panel);
		}

		contents.add(header);
	}

	private void addCategoryAvgToHeader(List<String> header, CategoryDefinition cat, OwlExportPanel panel)
	{
		String weight = "";
		if (panel.getCatType() == GbCategoryType.WEIGHTED_CATEGORY && cat.getWeight() != null)
		{
			weight = "(" + FormatHelper.formatDoubleAsPercentage(cat.getWeight() * 100) + ")";
		}
		// Add the category name plus weight if available
		header.add(String.join(" ", IGNORE_COLUMN_PREFIX, panel.getString("label.category"), cat.getName(), weight));
	}

	private void addNormalUserDataToHeader(List<String> header, OwlExportConfig config, OwlExportPanel panel)
	{
		if (config.includeStudentId)
		{
			header.add(panel.getString("importExport.export.csv.headers.studentId"));
		}
		if (config.includeStudentName)
		{
			header.add(panel.getString("importExport.export.csv.headers.studentName"));
		}
		if (config.includeStudentNumber)
		{
			header.add(String.join(" ", IGNORE_COLUMN_PREFIX, panel.getString("importExport.export.csv.headers.studentNumber")));
		}
	}

	private void addAnonUserDataToHeader(List<String> header, OwlExportPanel panel)
	{
		header.add(panel.getString("importExport.export.csv.headers.anonId"));
	}

	private void addCourseGradeDataToHeader(List<String> header, OwlExportConfig config, OwlExportPanel panel)
	{
		if (config.includePoints)
		{
			header.add(String.join(" ", IGNORE_COLUMN_PREFIX, panel.getString("importExport.export.csv.headers.points")));
		}
		if (config.includeCalculatedGrade)
		{
			header.add(String.join(" ", IGNORE_COLUMN_PREFIX, panel.getString("importExport.export.csv.headers.calculatedGrade")));
		}
		if (config.includeFinalGrade)
		{
			header.add(String.join(" ", IGNORE_COLUMN_PREFIX, panel.getString("importExport.export.csv.headers.finalGrade")));
		}
		if (config.includeGradeOverride)
		{
			header.add(String.join(" ", IGNORE_COLUMN_PREFIX, panel.getString("importExport.export.csv.headers.gradeOverride")));
		}
		if (config.includeLastLogDate)
		{
			header.add(String.join(" ", IGNORE_COLUMN_PREFIX, panel.getString("importExport.export.csv.headers.lastLogDate")));
		}
	}

	/**
	 *
	 * @param type
	 * @param contents
	 * @param items the gradebook items to include
	 * @param matrix the users to include
	 * @param config
	 * @param panel
	 */
	private void buildRows(CsvType type, List<List<String>> contents, List<Assignment> items, List<CategoryDefinition> cats,
			List<OwlGbStudentGradeInfo> matrix,	OwlExportConfig config, OwlExportPanel panel)
	{
		final int cols = contents.get(0).size(); // get the numbers of columns in the header
		for (OwlGbStudentGradeInfo student : matrix)
		{
			List<String> row = new ArrayList<>(cols);

			if (type.anon)
			{
				addAnonUserData(row, student);
			}
			else
			{
				addNormalUserData(row, student, config);
			}

			if (items.isEmpty()) // no gradebook items, give a template, add empty cell for each example column
			{
				row.add("");
				row.add("");
				row.add("");
				row.add("");
			}
			else if (config.includeGradeItemScores || config.includeGradeItemComments || config.includeCategoryAverages)
			{
				final Map<Long, Double> catAvgs = student.info.getCategoryAverages();
				List<Long> catIds = cats.stream().map(CategoryDefinition::getId).collect(Collectors.toList());
				for (int i = 0; i < items.size(); ++i)
				{
					final Assignment item = items.get(i);
					final GbGradeInfo gradeInfo = student.info.getGrades().get(item.getId());
					if (config.includeGradeItemScores)
					{
						String grade = gradeInfo == null ? "" : FormatHelper.formatGradeForDisplay(gradeInfo.getGrade());
						row.add(StringUtils.removeEnd(grade, decimalEnd));
					}
					if (config.includeGradeItemComments)
					{
						row.add(gradeInfo == null ? "" : gradeInfo.getGradeComment());
					}
					if (config.includeCategoryAverages && panel.getSortType() == SortType.SORT_BY_CATEGORY)
					{
						// read ahead and insert the category avg if necessary
						Optional<Assignment> nextItem = (i + 1) < items.size() ? Optional.of(items.get(i + 1)) : Optional.empty();
						if (catIds.contains(item.getCategoryId()) && (!nextItem.isPresent() || !item.getCategoryId().equals(nextItem.get().getCategoryId())))
						{
							addCategoryAvgToRow(row, catAvgs.get(item.getCategoryId()));
						}
					}
				}

				if (config.includeCategoryAverages && panel.getSortType() != SortType.SORT_BY_CATEGORY)
				{
					for (CategoryDefinition cat : cats)
					{
						// skip categories that have no associated items
						if (items.stream().map(i -> i.getCategoryId()).filter(Objects::nonNull).anyMatch(id -> cat.getId().equals(id)))
						{
							addCategoryAvgToRow(row, catAvgs.get(cat.getId()));
						}
					}
				}
			}

			if (type.cg)
			{
				addCourseGradeData(row, config, student, panel);
			}

			contents.add(row);
		}
	}

	private void addCategoryAvgToRow(List<String> row, Double catAvg)
	{
		row.add(StringUtils.removeEnd(FormatHelper.formatGradeForDisplay(catAvg), decimalEnd));
	}

	private void addNormalUserData(List<String> row, OwlGbStudentGradeInfo student, OwlExportConfig config)
	{
		if (config.includeStudentId)
		{
			row.add(student.info.getStudentEid());
		}
		if (config.includeStudentName)
		{
			row.add(FormatHelper.htmlUnescape(student.info.getStudentLastName()) + ", " + FormatHelper.htmlUnescape(student.info.getStudentFirstName()));
		}
		if (config.includeStudentNumber)
		{
			row.add(student.info.getStudentNumber());
		}
	}

	private void addAnonUserData(List<String> row, OwlGbStudentGradeInfo student)
	{
		row.add(String.valueOf(student.anonId));
	}

	private void addCourseGradeData(List<String> row, OwlExportConfig config, OwlGbStudentGradeInfo student, OwlExportPanel panel)
	{
		if (config.includePoints)
		{
			row.add(FormatHelper.formatGradeForDisplay(FormatHelper.formatDoubleToDecimal(student.info.getCourseGrade().getCourseGrade().getPointsEarned())));
		}
		if (config.includeCalculatedGrade)
		{
			row.add(FormatHelper.formatGradeForDisplay(student.info.getCourseGrade().getCourseGrade().getCalculatedGrade()));
		}
		if (config.includeFinalGrade)
		{
			row.add(FinalGradeFormatter.format(student.info.getCourseGrade()));
		}
		if (config.includeGradeOverride)
		{
			row.add(FormatHelper.formatGradeForDisplay(student.getGradeOverride().orElse("")));
		}
		if (config.includeLastLogDate)
		{
			Date overrideDate = student.info.getCourseGrade().getCourseGrade().getDateRecorded();
			row.add(overrideDate == null ? "" : panel.formatDateTime(overrideDate));
		}
	}
}
