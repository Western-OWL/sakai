// 2012.07.19, bbailla2, New
// Generates a PDF file used for course grade approval at the department level
// 2012.07.20, plukasew, Modified
// add statistics and revision logic

package org.sakaiproject.gradebookng.business.owl.finalgrades;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.exception.IdNotFoundException;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmission;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmissionGrades;

/**
 *
 * @author bbailla2
 * @author plukasew
 */
public class CourseGradePdfGenerator
{
    private static final PDFont MASTER_FONT = PDType1Font.HELVETICA;
    private static final PDFont MASTER_FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    
    private static final float HEADER_MARGIN_Y = 34f;
    private static final float HEADER_ROW_HEIGHT=14f;
    private static final PDFont HEADER_FONT = MASTER_FONT;
    private static final int HEADER_FONT_SIZE = 11;
    
    private static final float LABEL1_MARGIN_X = 36f;
    private static final float LABEL1_MARGIN_Y = 22f;
    private static final float LABEL1_ROW_HEIGHT = 10f;
    private static final PDFont LABEL1_FONT = MASTER_FONT;
    private static final int LABEL1_FONT_SIZE = 9;
    
    private static final float TABLE_MARGIN_X = 36f;
    private static final float TABLE_MARGIN_Y = 18f;
    private static final float TABLE_LINE_HEIGHT = 10f;
    private static final float TABLE_ROW_MARGIN_HEIGHT = 3f;
    private static final float TABLE_COLUMN_SEPARATION = 8f;
    private static final float TABLE_ARROW_SEPARATION = 4f;
    private static final PDFont TABLE_FONT = MASTER_FONT;
    private static final PDFont TABLE_FONT_BOLD = MASTER_FONT_BOLD;
    private static final int TABLE_FONT_SIZE = 9;
    
    private static final float SIGNATURE_MARGIN_X = 32f;
    private static final float SIGNATURE_UPPER_MARGIN=20f;
    private static final float SIGNATURE_UPPER_PADDING=5f;
    private static final float SIGNATURE_HEIGHT = 130f;
    private static final float SIGNATURE_SEGMENT_MARGIN=26f;
    private static final float SIGNATURE_TEXT_MARGIN=36f;
    private static final PDFont SIGNATURE_FONT = MASTER_FONT;
    private static final int SIGNATURE_FONT_SIZE = 11;
    
    private static final float FOOT_Y = 40f;
    
    private static final float STAT_CAPTION_X = 40f;
    private static final float STAT_CAPTION_Y = PDRectangle.LETTER.getHeight() - 110f;
    private static final float STAT_LINE_HEIGHT = 13f;
    private static final float STAT_MARGIN_X = 55f;
    private static final float STAT_MARGIN_Y = 25f;
    private static final float STAT_COLUMN_SEPARATION=120f;
    private static final PDFont STAT_FONT = MASTER_FONT;
    private static final int STAT_FONT_SIZE=9;
    
    private static final String NGR = "NGR";
    
    private OwlGradeSubmission submission;
    private Set<OwlGradeSubmissionGrades> previousApprovedGrades;
    private List<GradeRow> gradeRows;
    
    public CourseGradePdfGenerator(OwlGradeSubmission sub)
    {
        submission = sub;
        previousApprovedGrades = new HashSet<>();
        gradeRows = new ArrayList<>();
    }
    
    public void setSubmission(OwlGradeSubmission value)
    {
        submission = value;
    }
    
    public void setPreviousApprovedGrades(Set<OwlGradeSubmissionGrades> value)
    {
        previousApprovedGrades = value;
    }
    
    private String[][] getTableContent()
    {
        // NOTE: This algorithm completely ignores students who were approved last time
        // but are missing this time. This is consistent with the WebCT export algorithm.
        
        boolean revision = !previousApprovedGrades.isEmpty();
        Map<String, OwlGradeSubmissionGrades> oldGradesMap = new HashMap<>(previousApprovedGrades.size());
        for (OwlGradeSubmissionGrades oldGrade : previousApprovedGrades)
        {
            oldGradesMap.put(oldGrade.getStudentNumber(), oldGrade);
        }
        
        for (OwlGradeSubmissionGrades grade : submission.getGradeData())
        {
            GradeRow row = new GradeRow();
            row.setStudentNumber(grade.getStudentNumber());
            row.setNewGrade(grade.getGrade());
            row.setStudentName(cleanUnprintableChars(grade.getStudentNameLastFirst()));
            
            if (revision)
            {
                OwlGradeSubmissionGrades oldGrade = oldGradesMap.get(grade.getStudentNumber());
                if (oldGrade == null)
                {
                    // new student, set old grade to NGR
                    row.setOldGrade(NGR);
                }
                else
                {
                    row.setOldGrade(oldGrade.getGrade());
                }
            }
            else
            {
                row.setOldGrade(grade.getGrade());
            }
            
            gradeRows.add(row);
        }
        
        Collections.sort(gradeRows);
        
        String[][] tableContent = new String[gradeRows.size()][5];

	for (int row = 0; row < gradeRows.size(); row++)
	{
	    GradeRow current = gradeRows.get(row);
            String col1 = current.getStudentNumber();
            String col2;
            String col3 = "";
            String col4 = "";
            String col5 = current.getStudentName();
            if (revision)
            {
                col2 = current.getOldGrade();
                col3 = "->";
                col4 = current.getNewGrade();
            }
            else
            {
                col2 = current.getNewGrade();
            }
            
	    tableContent[row] = new String[] {col1, col2, col3, col4, col5};
	}
        
        return tableContent;
    }
    
    private boolean[] getBoldFlags()
    {
        boolean[] flags = new boolean[gradeRows.size()];
        if (!previousApprovedGrades.isEmpty())
        {
            for (int i = 0; i < flags.length; ++i)
            {
                flags[i] = gradeRows.get(i).isRevision();
            }
        }
        
        return flags;
    }
    
    private List<String> getHeaderContent()
    {
        List<String> header = new ArrayList<>();
        header.add("THE UNIVERSITY OF WESTERN ONTARIO");
        boolean revision = !previousApprovedGrades.isEmpty();
        if (revision)
        {
            header.add("GRADE REVISION FOR " + getSectionName().toUpperCase());
        }
        else
        {
            header.add("GRADE SUBMISSION FOR " + getSectionName().toUpperCase());
        }
        header.add("REGISTRAR CODE: " + submission.getSectionEid());
        header.add(new SimpleDateFormat("MMM dd yyyy HH:mm:ss").format(submission.getSubmissionDate()));
        
        return header;
    }
    
    private List<String> getStatsContent()
    {
        CourseGradeStatistics stats = new CourseGradeStatistics(submission.getGradeData());

        List<String> statsContent = new ArrayList<>();
        statsContent.add("Statistics for " + getSectionName());
		addDoubleToStats(statsContent, stats.getStats().getMean());
		addDoubleToStats(statsContent, stats.getStats().getStandardDeviation());
		addDoubleToStats(statsContent, stats.getMedian());
		addDoubleToStats(statsContent, stats.getMode());
		addDoubleToStats(statsContent, stats.getStats().getMin());
		addDoubleToStats(statsContent, stats.getStats().getMax());
		addDoubleToStats(statsContent, stats.getStats().getSkewness());
        statsContent.add(Integer.toString(stats.getNonNumericCount()));
        statsContent.add(Integer.toString(stats.getNumericCount()));
        statsContent.add(stats.getCountForLetterGrade().get("A+").toString());
        statsContent.add(stats.getCountForLetterGrade().get("A").toString());
        statsContent.add(stats.getCountForLetterGrade().get("B").toString());
        statsContent.add(stats.getCountForLetterGrade().get("C").toString());
        statsContent.add(stats.getCountForLetterGrade().get("D").toString());
        statsContent.add(stats.getCountForLetterGrade().get("F").toString());
        
        return statsContent;
    }

    private void addDoubleToStats(List<String> stats, double stat)
    {
	Double dblVal = stat;
	//if (Double.NaN == stat) - this wasn't cutting it
	if ("NaN".equals(Double.toString(dblVal)))
	{
	    stats.add("--");
	}
	else
	{
	    stats.add(Double.toString(dblVal));
	}
    }

    private String getSectionName()
    {
		String sectionName = "";
        CourseManagementService cms = (CourseManagementService) ComponentManager.get(CourseManagementService.class);
        if (cms != null)
        {
            try
            {
                sectionName = cms.getSection(submission.getSectionEid()).getTitle();
            }
            catch (IdNotFoundException infe)
            {
                // don't care, do nothing
            }
        }
		return sectionName;
    }
        
    /**
     * Stores a PDF for final grade export into an OutputStream
     * @param headerContent the lines of the header that appears on every page except stats
     * @param tableContent the listing of grades. Outer index is the row, inner index is the column
     * @param bold bold[i] determines whether row[i] in tableContent is bold
     * @param statsContent index 0 is the caption, subsequent indices are the stats
     * @param os the output stream in which the pdf will be stored
     */
    public void generateIntoOutputStream(OutputStream os) throws IOException
    {
		try (PDDocument doc = new PDDocument())
		{
			drawDocument(doc, getHeaderContent(), getTableContent(), getBoldFlags(), getStatsContent());
			doc.save(os);
		}
    }
    
    private void drawHeader(PDPage page, PDPageContentStream contentStream, List<String> headerContent) throws IOException
    {
        contentStream.setFont( HEADER_FONT, HEADER_FONT_SIZE );
        contentStream.beginText();
            //center the text at the top
            float initX = page.getMediaBox().getWidth()/2;
            float initY = page.getMediaBox().getHeight()-HEADER_MARGIN_Y;
            contentStream.newLineAtOffset(initX, initY);

            //draw the header
            Iterator<String> it = headerContent.iterator();
            while (it.hasNext())
            {
                drawHeaderLine(it.next(), contentStream);
            }

        contentStream.endText();
    }
    
    private void drawHeaderLine(String text, PDPageContentStream contentStream) throws IOException
    {
        //get the width of the text in pixels
        /* I don't know why this needs to be divided by 1000f, 
         * it's just a magic number that shows up in all the examples*/
        float textWidth = getStringWidth(text, HEADER_FONT, HEADER_FONT_SIZE);
        //center the text
        contentStream.newLineAtOffset(-textWidth/2, 0);
        //draw the text
        contentStream.showText(text);
        //center the text position and move to the next line
        contentStream.newLineAtOffset(textWidth/2, -HEADER_ROW_HEIGHT);
    }
    
    private void drawLabel1(PDPage page, PDPageContentStream contentStream) throws IOException
    {
	//only draw this label if revisions exist
	boolean revisionsExist = !previousApprovedGrades.isEmpty();

	if (revisionsExist)
	{
	    contentStream.setFont(LABEL1_FONT, LABEL1_FONT_SIZE);
	    contentStream.beginText();
		float initX = LABEL1_MARGIN_X;
		float initY = page.getMediaBox().getHeight() - HEADER_MARGIN_Y - 
			4*HEADER_ROW_HEIGHT - LABEL1_MARGIN_Y;
		contentStream.newLineAtOffset(initX, initY);
		contentStream.showText("Students with revised grades since last approval are indicated in bold.");
	    contentStream.endText();
	}
    }
    
    private void drawSignature(PDPage page, PDPageContentStream contentStream) throws IOException
    {
		contentStream.setFont(SIGNATURE_FONT, SIGNATURE_FONT_SIZE);
		contentStream.beginText();
		//left side of the box's x coordinate
		float lx = SIGNATURE_MARGIN_X;
		//right side of the box's x coordinate
		float rx = page.getMediaBox().getWidth() - SIGNATURE_MARGIN_X;
		//top of the box's y coordinate
		float ty = FOOT_Y + SIGNATURE_HEIGHT;
		//bottom of the box's y coordinate
		float by = FOOT_Y;

		float center = page.getMediaBox().getWidth()/2f;

		//top
		drawLine(contentStream, lx, ty, rx, ty);
		//right
		drawLine(contentStream, rx,ty,rx,by);
		//bottom
		drawLine(contentStream, rx,by,lx,by);
		//left
		drawLine(contentStream, lx,by,lx,ty);
		//center
		drawLine(contentStream, center, ty, center, by);

		float cellMargin = 4f;
		float signatureRightMargin=4f;

		drawLine(contentStream, lx + cellMargin, ty - SIGNATURE_SEGMENT_MARGIN - SIGNATURE_UPPER_PADDING, center - cellMargin - signatureRightMargin, ty - SIGNATURE_SEGMENT_MARGIN - SIGNATURE_UPPER_PADDING);
		float toMoveX=SIGNATURE_MARGIN_X + cellMargin;
		float toMoveY=ty - SIGNATURE_TEXT_MARGIN -SIGNATURE_UPPER_PADDING;
		contentStream.newLineAtOffset(toMoveX, toMoveY);
		contentStream.showText("Instructor Name");
		//contentStream.newLineAtOffset(rx, by);

		String please="(Please Print)";
		float textWidth = getStringWidth(please, SIGNATURE_FONT, SIGNATURE_FONT_SIZE);

		toMoveX=-toMoveX + center - cellMargin - textWidth - signatureRightMargin;

		contentStream.newLineAtOffset(toMoveX, 0);
		contentStream.showText(please);

		toMoveX=-toMoveX;

		//Was 16f, requested to take it down two lines, so 16f + 2*12f =40f
		float downFromInstName = 40f;
		toMoveY-=downFromInstName;
		contentStream.newLineAtOffset(toMoveX, -downFromInstName);
		contentStream.showText("Grades of INC, SPC and AEGROTAT standing must");

		float textHeight=12f;
		toMoveY-=textHeight;
		contentStream.newLineAtOffset(0, -textHeight);
		contentStream.showText("be approved by the Dean of the faculty in which the");

		toMoveY-=textHeight;
		contentStream.newLineAtOffset(0, -textHeight);
		contentStream.showText("student is registered prior to the submission of the");

		toMoveY-=textHeight;
		contentStream.newLineAtOffset(0, -textHeight);
		contentStream.showText("grade to the Office of the Registrar.");

		drawLine(contentStream, center+cellMargin,
				ty - SIGNATURE_SEGMENT_MARGIN - SIGNATURE_UPPER_PADDING,
				page.getMediaBox().getWidth()-SIGNATURE_MARGIN_X-cellMargin-signatureRightMargin,
				ty-SIGNATURE_SEGMENT_MARGIN-SIGNATURE_UPPER_PADDING);

		// After upgrading to PDFBOX 2.x the offset math below this point broke so I reworked it  --plukasew
		String date = "Date";
		final float SIG_BEGIN_X = center + cellMargin;
		final float DATE_WIDTH = getStringWidth(date, SIGNATURE_FONT, SIGNATURE_FONT_SIZE);
		final float DATE_BEGIN_X = center - SIGNATURE_MARGIN_X - signatureRightMargin - DATE_WIDTH - 2*cellMargin;
		final float INSTRUCTOR_Y = ty - SIGNATURE_UPPER_PADDING - SIGNATURE_TEXT_MARGIN;
		final float CHAIR_Y = INSTRUCTOR_Y - SIGNATURE_TEXT_MARGIN;
		final float DEAN_Y = CHAIR_Y - SIGNATURE_TEXT_MARGIN;
		
		//toMoveX = center + cellMargin;
		toMoveX = SIG_BEGIN_X;
		//toMoveY = ty - SIGNATURE_TEXT_MARGIN - SIGNATURE_UPPER_PADDING;
		toMoveY = INSTRUCTOR_Y;
		contentStream.newLineAtOffset(toMoveX, toMoveY);
		contentStream.showText("Signature of Instructor");

		
		//textWidth=SIGNATURE_FONT.getStringWidth(date)/1000f * SIGNATURE_FONT_SIZE;
		//toMoveX=center - SIGNATURE_MARGIN_X - signatureRightMargin - textWidth - 2*cellMargin;
		toMoveX = DATE_BEGIN_X;
		contentStream.newLineAtOffset(toMoveX, 0);
		contentStream.showText(date);

		drawLine(contentStream, center+cellMargin, ty - SIGNATURE_SEGMENT_MARGIN - SIGNATURE_TEXT_MARGIN - SIGNATURE_UPPER_PADDING, page.getMediaBox().getWidth()-SIGNATURE_MARGIN_X-cellMargin-signatureRightMargin,ty-SIGNATURE_SEGMENT_MARGIN-SIGNATURE_TEXT_MARGIN-SIGNATURE_UPPER_PADDING);
		//toMoveX=-toMoveX;
		toMoveX = SIG_BEGIN_X;
		//toMoveY=-SIGNATURE_TEXT_MARGIN;
		toMoveY = CHAIR_Y;
		contentStream.newLineAtOffset(toMoveX, toMoveY);
		contentStream.showText("Signature of Chairperson");

		//toMoveX=-toMoveX;
		toMoveX = DATE_BEGIN_X;
		contentStream.newLineAtOffset(toMoveX, 0);
		contentStream.showText(date);

		drawLine(contentStream, center+cellMargin, ty - SIGNATURE_SEGMENT_MARGIN - 2*SIGNATURE_TEXT_MARGIN - SIGNATURE_UPPER_PADDING, page.getMediaBox().getWidth()-SIGNATURE_MARGIN_X-cellMargin-signatureRightMargin,ty-SIGNATURE_SEGMENT_MARGIN-2*SIGNATURE_TEXT_MARGIN-SIGNATURE_UPPER_PADDING);
		//toMoveX=-toMoveX;
		toMoveX = SIG_BEGIN_X;
		//toMoveY=-SIGNATURE_TEXT_MARGIN;
		toMoveY = DEAN_Y;
		contentStream.newLineAtOffset(toMoveX, toMoveY);
		contentStream.showText("Signature of Dean");

		//toMoveX=-toMoveX;
		toMoveX = DATE_BEGIN_X;
		contentStream.newLineAtOffset(toMoveX, 0);
		contentStream.showText(date);

		contentStream.endText();
    }
    
    private void drawStat(PDPageContentStream contentStream, String stat) throws IOException
    {
        contentStream.showText(stat);
        contentStream.newLineAtOffset(0, -STAT_LINE_HEIGHT);
    }
    
    /**
     * Calculates the number of characters in text that can fit within the maxWidth
     * @param text the string we are testing
     * @param maxWidth the width that the string has to fit within
     * @param font the font that the text would be rendered in
     * @param fontSize the font size that the text would be rendered in
     * @return the number of characters in text that can fit in maxWidth
     */
    private int numCharsThatFit(String text, float maxWidth, PDFont font, int fontSize) throws IOException
    {
        int lastFittingChar=0;
        String test;
        while (lastFittingChar<text.length())
        {
            test=text.substring(0, lastFittingChar);
            float width = getStringWidth(test, font, fontSize);
            if (width>maxWidth)
            {
                lastFittingChar--;
                break;
            }
            lastFittingChar++;
        }
        return lastFittingChar;
    }
    
    /**
     * @param page
     * @param contentStream
     * @param y the y-coordinate of the first row
     * @param margin the padding on left and right of table
     * @param content a 2d array containing the table data
     * @throws IOException
     */
    public void drawDocument(PDDocument doc, List<String> headerContent, String[][] content, boolean[] bold, List<String> statsContent) 
            throws IOException 
    {
        final int rows = content.length;
        
        /**
         * Approach 1:
         * Draw the first row left to right, draw the second row left to right
         * -problem: cell[0][0]'s width is different than cell[1][0],
         * so this means cell[0][1] will start at a different x coordinate than
         * cell[1][1]
         * 
         * Approach 2:
         * Draw the first column top to bottom, draw the second column top to 
         * bottom, record the width of each column as the maximum
         * -problem: the height of cell[0][0] is different than cell[0][2],
         * so this means cell[1][0] will start at a different y coordinate than
         * cell[1][2]
         * 
         * Approach 3:
         * Do two passes - one to get the width and height of all the cells 
         * with variable width/heights, and a second pass to draw them using
         * these maximum widths and heights to find where to draw the next cell
         */
        
        //the number of chars allowed per line in the first column
        int firstColumnWidthInChars=9;
        //the width of the first column in pixels
        float firstColumnWidth = 0;
        float firstColumnCellHeights[]=new float [rows];
        /*
         * With the firstColumnWidthInChars restriction, the cell contents will 
         * be broken up onto multiple lines. 
         * Outer index is the cell, inner index is the line number
         */
        ArrayList<ArrayList<String>> firstColumnLines = new ArrayList<>();
        
        /*
         * second column will never span multiple lines, so we just care about
         * the width
         */
        float secondColumnWidth = 0;
	float thirdColumnWidth = 0;
	float fourthColumnWidth = 0;
        
        //the width of the fifth column in pixels
        float fifthColumnWidth = 0;
        float fifthColumnCellHeights[]=new float [rows];
        //similar to firstColumnLines
        ArrayList<ArrayList<String>> fifthColumnLines = new ArrayList<>();
        
        /*
         * First pass - get the column width and the height of the cells
         */
        
        PDFont font = null;
        
        //Get information on the first column
        for (int i = 0; i < rows; i++)
        {
            if (bold[i])
            {
                font = TABLE_FONT_BOLD;
            }
            else
            {
                font = TABLE_FONT;
            }
            //If the cell's text is long, it gets parsed onto multiple lines
            ArrayList<String> cellLines=new ArrayList<>();
            //start a new line every firstColumnWidthInChars characters
            for (int j=0; j<content[i][0].length();j+=firstColumnWidthInChars)
            {
                //index of the last character on this line
                int lineEndCharIndex = Math.min(j+firstColumnWidthInChars, content[i][0].length());
                //get the line
                String line = content[i][0].substring(j,lineEndCharIndex);
                //use this line's width as the whole column's width if it's the longest line so far
                firstColumnWidth = Math.max(getStringWidth(line, font, TABLE_FONT_SIZE), firstColumnWidth);
                cellLines.add(line);
            }
            //add the new cell
            /*
             * for every line, the height gets extended by TABLE_LINE_HEIGHT
             * and then we throw a margin of TABLE_ROW_MARGIN_HEIGHT at the 
             * bottom of the cell
             */
            firstColumnCellHeights[i]=cellLines.size() * TABLE_LINE_HEIGHT + TABLE_ROW_MARGIN_HEIGHT;
            firstColumnLines.add(cellLines);
        }

        //Get information on the second, third, and 4th columns
        for (int i = 0; i < rows; i++)
        {
	    if (bold[i])
            {
                font=TABLE_FONT_BOLD;
            }
            else
            {
                font=TABLE_FONT;
            }
	    //we just care about its width
	    //use this cell's width as the whole column's width if it's the widest cell so far
	    secondColumnWidth = Math.max(getStringWidth(content[i][1], font, TABLE_FONT_SIZE), secondColumnWidth);
	    thirdColumnWidth = Math.max(thirdColumnWidth, getStringWidth("->", TABLE_FONT, TABLE_FONT_SIZE));
	    fourthColumnWidth = Math.max(getStringWidth(content[i][3], font, TABLE_FONT_SIZE), fourthColumnWidth);
        }
        
        //This is how far column5 is from the left side of the page
        float column5Margin = TABLE_MARGIN_X + firstColumnWidth + TABLE_COLUMN_SEPARATION + secondColumnWidth + TABLE_ARROW_SEPARATION + thirdColumnWidth + TABLE_ARROW_SEPARATION + fourthColumnWidth + TABLE_COLUMN_SEPARATION;
        
        /*
         * This is how much width is available for the fifth column: 
         * It's the distance from column5Margin to the center of the page, 
         * and we'll have a TABLE_COLUMN_SEPARATION as a margin on the 
         * right
         */
        //PAGE_SIZE_LETTER is the default size when you call new PDPage(), and it's what we're using
        float maxWidthFor5thColumn=PDRectangle.LETTER.getWidth()/2f - column5Margin - TABLE_COLUMN_SEPARATION;
        
        //Get information on the fifth column
        for (int i = 0; i < rows; i++)
        {
            if (bold[i])
            {
                font = TABLE_FONT_BOLD;
            }
            else
            {
                font = TABLE_FONT;
            }
            //All the lines in the cell
            ArrayList<String> cellLines = new ArrayList<>();
            //The text that hasn't been added to the cell yet
            String remainingText = content[i][4];
            while (remainingText.length()!=0)
            {
                /*
                 * Get the number of characters that can fit on the next line, 
                 * and add these characters to the next line
                 */
                int numCharsThatFit = numCharsThatFit(remainingText, maxWidthFor5thColumn, font, TABLE_FONT_SIZE);
                String line = remainingText.substring(0,numCharsThatFit);
                float width = getStringWidth(line, font, TABLE_FONT_SIZE);
                fifthColumnWidth = Math.max(fifthColumnWidth, width);
                remainingText=remainingText.substring(numCharsThatFit);
                cellLines.add(line);
            }
            //add the new cell
            fifthColumnCellHeights[i] = cellLines.size()*TABLE_LINE_HEIGHT + TABLE_ROW_MARGIN_HEIGHT;
            fifthColumnLines.add(cellLines);
        }
        
        //prepare to draw
        
        //create a page
        PDPage page = new PDPage();
        doc.addPage( page );

        //get a PDPageContentStream to draw with
        PDPageContentStream contentStream = new PDPageContentStream(doc, page);
        
        //start drawing
        drawHeader(page, contentStream, headerContent);

        drawLabel1(page, contentStream);
        
        drawSignature(page, contentStream);

        //prepare to draw the table
        
        //place the cursor
        float tableLeftX = TABLE_MARGIN_X;
        //start at the top, and skip past the header and label1
        float tableTop = page.getMediaBox().getHeight() - HEADER_MARGIN_Y - 
                4*HEADER_ROW_HEIGHT - LABEL1_MARGIN_Y - 
                LABEL1_ROW_HEIGHT - TABLE_MARGIN_Y;
        contentStream.beginText();
        contentStream.newLineAtOffset(tableLeftX, tableTop);
        
        //as we render the table, this keeps track of how tall it is
        float currentTableHeight=0;
        //this is the max height before the table overlaps the signature
        float allowedTableHeight = tableTop - FOOT_Y -SIGNATURE_HEIGHT - SIGNATURE_UPPER_MARGIN;
        //determins which side of the page we're drawing on
        boolean renderingLeft = true;
        
        //iterate through the table's rows and draw them
        for (int i=0 ; i<rows; i++)
        {
            if (bold[i])
            {
                font = TABLE_FONT_BOLD;
            }
            else
            {
                font = TABLE_FONT;
            }
            contentStream.setFont(font, TABLE_FONT_SIZE);
            
            //switch sides of the page / start a new page when the table gets long
            if (Math.max(currentTableHeight+firstColumnCellHeights[i], currentTableHeight+fifthColumnCellHeights[i])>allowedTableHeight)
            {
                if (renderingLeft)
                {
                    //move cursor to the right side
                    contentStream.endText();
                    contentStream.beginText();
                    contentStream.newLineAtOffset(page.getMediaBox().getWidth()/2f+TABLE_COLUMN_SEPARATION, tableTop);
                }
                else
                {
                    //start a new page
                    
                    contentStream.endText();
                    contentStream.close();
                    
                    page=new PDPage();
                    doc.addPage(page);
                    
                    contentStream=new PDPageContentStream(doc, page);
                    
                    //need to draw these on every page
                    drawHeader(page, contentStream, headerContent);

                    drawLabel1(page, contentStream);

                    drawSignature(page, contentStream);

                    //set the cursor again
                    contentStream.setFont( font, TABLE_FONT_SIZE );
                    contentStream.beginText();
                    contentStream.newLineAtOffset(tableLeftX, tableTop);
                }
                //the table is empty on the new side/page
                currentTableHeight=0;
                renderingLeft=!renderingLeft;
            }
            
            //draw the first column's cell
            Iterator<String> it = firstColumnLines.get(i).iterator();
            while (it.hasNext())
            {
                //draw this line, descend a line
                sanitizeAndShowText(it.next(), contentStream, font);
                contentStream.newLineAtOffset(0, -TABLE_LINE_HEIGHT);
            }
            
            //move back up the first cell's height and over to the second column
            contentStream.newLineAtOffset(firstColumnWidth + TABLE_COLUMN_SEPARATION, -TABLE_ROW_MARGIN_HEIGHT + firstColumnCellHeights[i]);

            //draw the second column
            sanitizeAndShowText(content[i][1], contentStream, font);
            
            //move over to the third column
            contentStream.newLineAtOffset(secondColumnWidth + TABLE_ARROW_SEPARATION, 0);
            
            sanitizeAndShowText(content[i][2], contentStream, font);

	    contentStream.newLineAtOffset(thirdColumnWidth + TABLE_ARROW_SEPARATION, 0);

            sanitizeAndShowText(content[i][3], contentStream, font);

	    contentStream.newLineAtOffset(fourthColumnWidth + TABLE_COLUMN_SEPARATION, 0);

            //draw the fifth column's cell
            it = fifthColumnLines.get(i).iterator();
            while (it.hasNext())
            {
                //draw this line, descend a line
                sanitizeAndShowText(it.next(), contentStream, font);
                contentStream.newLineAtOffset(0, -TABLE_LINE_HEIGHT);
            }
            
            //move the cursor to the next row
            
            //add a margin before the next row
            float distanceToNextRow = -TABLE_ROW_MARGIN_HEIGHT;
            //adjust it if it's not currently correct (due to the first column's cell being taller)
            if (firstColumnCellHeights[i]>fifthColumnCellHeights[i])
            {
                distanceToNextRow += fifthColumnCellHeights[i] - firstColumnCellHeights[i];
            }
            contentStream.newLineAtOffset(-TABLE_COLUMN_SEPARATION - fourthColumnWidth - TABLE_ARROW_SEPARATION - thirdColumnWidth - TABLE_ARROW_SEPARATION - secondColumnWidth - TABLE_COLUMN_SEPARATION - firstColumnWidth, distanceToNextRow);
            
            //add this row's height the table's height to calculate when to switch sides/pages
            currentTableHeight+=Math.max(firstColumnCellHeights[i], fifthColumnCellHeights[i]);
        }
        
        contentStream.endText();
        contentStream.close();
        
        //Draw the stats page
        
        page=new PDPage();
        doc.addPage(page);

        contentStream=new PDPageContentStream(doc, page);
        
        contentStream.setFont(STAT_FONT, STAT_FONT_SIZE);
        contentStream.beginText();
        
        //draw the caption
        String caption = statsContent.get(0);
        contentStream.newLineAtOffset(STAT_CAPTION_X, STAT_CAPTION_Y);
        contentStream.showText(caption);
        
        //draw the keys
        contentStream.newLineAtOffset(STAT_MARGIN_X - STAT_CAPTION_X, -STAT_MARGIN_Y);
        drawStat(contentStream,"Mean:");
        drawStat(contentStream,"St. Dev:");
        drawStat(contentStream,"Median:");
        drawStat(contentStream,"Mode:");
        drawStat(contentStream,"Minimum:");
        drawStat(contentStream,"Maximum:");
        drawStat(contentStream,"Skewness:");
        drawStat(contentStream,"Excluded:");
        drawStat(contentStream,"N used:");
        contentStream.newLineAtOffset(0, -STAT_LINE_HEIGHT);
        drawStat(contentStream,"A+:");
        drawStat(contentStream,"A:");
        drawStat(contentStream,"B:");
        drawStat(contentStream,"C:");
        drawStat(contentStream,"D:");
        drawStat(contentStream,"F:");
        
        //draw the values
        contentStream.newLineAtOffset(STAT_COLUMN_SEPARATION, STAT_LINE_HEIGHT*16);
        for (int i=1; i<10; i++)
        {
            drawStat(contentStream, statsContent.get(i));
        }
        contentStream.newLineAtOffset(0, -STAT_LINE_HEIGHT);
        for (int i=10; i<statsContent.size(); i++)
        {
            drawStat(contentStream, statsContent.get(i));
        }
        
        contentStream.endText();
        contentStream.close();
    }
	
	private void drawLine(PDPageContentStream cs, float startX, float startY, float endX, float endY) throws IOException
	{
		cs.endText();
		cs.moveTo(startX, startY);
		cs.lineTo(endX, endY);
		cs.stroke();
		cs.beginText();
	}
	
	/**
	 * Some names sourced from LDAP can contain characters that are unprintable in the PDF font we're using,
	 * causing runtime exceptions. This method removes these characters and replaces them with underscores.
	 * @param input the input string
	 * @return the input string with all known unprintable characters replaced by underscores
	 */
	private String cleanUnprintableChars(String input)
	{
		return input.replaceAll("\uFFFD", "_");
	}

	/**
	 * Calculates the width of the specified line after any characters that cannot be encoded in the specified font are substituted
	 */
	private float getStringWidth(String line, PDFont font, int fontSize) throws IOException
	{
		float width = font.getStringWidth(sanitizeLine(line, font));
		return width/1000f * fontSize;
	}

	/**
	 * Draws the line into the specified content stream.
	 *
	 * NB: the font parameter does not specify which font will be used to draw the line; it is used only in the sanitization process. Make sure that this font is the same as that assigned to the content stream.
	 *
	 * @param line the line to draw. Characters that cannot be encoded will be substituted
	 * @param contentStream
	 * @param font used to determine which characters cannot be encoded. 
	 */
	private void sanitizeAndShowText(String line, PDPageContentStream contentStream, PDFont font) throws IOException
	{
		contentStream.showText(sanitizeLine(line, font));
	}

	/**
	 * Returns the line such that it's encodable in the given font.
	 * Any character that cannot be encoded will have accents stripped.
	 * If the resulting character still cannot be encoded, it will be substituted with a "?".
	 */
	private String sanitizeLine(String line, PDFont font) throws IOException
	{
		try
		{
			font.encode(line);
			return line;
		}
		catch (IllegalArgumentException e)
		{

		}

		StringBuilder sb = new StringBuilder();
		String[] chars = line.split("");
		for (String character : chars)
		{
			try
			{
				font.encode(character);
				sb.append(character);
			}
			catch (IllegalArgumentException e1)
			{
				try
				{
					// Try without an accent so that the character is still legible
					String noAccent = StringUtils.stripAccents(character);
					font.encode(noAccent);
					sb.append(noAccent);
				}
				catch (IllegalArgumentException e2)
				{
					// We can't encode the character in a legible way. Substitute with "?"
					sb.append("?");
				}
			}
		}
		return sb.toString();
	}
    
    /********************* BEGIN NESTED CLASSES ************************/
    
    private class GradeRow implements Comparable
    {
        private String studentNumber;
        private String oldGrade;
        private String newGrade;
        private String studentName;
        
        public GradeRow()
        {
            studentNumber = "";
            oldGrade = "";
            newGrade = "";
            studentName = "";
        }

        public String getNewGrade()
        {
            return newGrade;
        }

        public void setNewGrade(String newGrade)
        {
            this.newGrade = newGrade;
        }

        public String getOldGrade()
        {
            return oldGrade;
        }

        public void setOldGrade(String oldGrade)
        {
            this.oldGrade = oldGrade;
        }

        public String getStudentName()
        {
            return studentName;
        }

        public void setStudentName(String studentName)
        {
            this.studentName = studentName;
        }

        public String getStudentNumber()
        {
            return studentNumber;
        }

        public void setStudentNumber(String studentNumber)
        {
            this.studentNumber = studentNumber;
        }
        
        public boolean isRevision()
        {
            return !oldGrade.equals(newGrade);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final GradeRow other = (GradeRow) obj;
            if ((this.studentNumber == null) ? (other.studentNumber != null) : !this.studentNumber.equals(other.studentNumber))
            {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 89 * hash + (this.studentNumber != null ? this.studentNumber.hashCode() : 0);
            return hash;
        }
        
        public int compareTo(Object other)
        {
            GradeRow row = (GradeRow) other;
            return studentName.compareTo(row.studentName);
        }
        
        @Override
        public String toString()
        {
            return studentNumber + " " + oldGrade + " -> " + newGrade + " " + studentName;
        }
    }
    
    /********************* END NESTED CLASSES **************************/
    
} // end class
