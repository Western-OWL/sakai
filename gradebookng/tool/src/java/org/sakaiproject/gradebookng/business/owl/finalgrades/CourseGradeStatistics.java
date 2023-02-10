// 2012.07.18, plukasew, New
// Calculates statistics for course grades

package org.sakaiproject.gradebookng.business.owl.finalgrades;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmissionGrades;

/**
 * Calculates statistics for course grades
 * @author plukasew
 */
public class CourseGradeStatistics implements Serializable
{
    private static final Log LOG = LogFactory.getLog(CourseGradeStatistics.class);
    private static final String LOG_PREFIX = "OWL: Final Grade Submission: ";
    
    public static final String CONVERT_LETTER_GRADES_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.stats.convertLetterGrades";
    public static final String KEYS_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.stats.gradeScale.keys";
    public static final String VALUES_SAKAI_PROPERTY = "gradebook.courseGradeSubmission.stats.gradeScale.values";
    
    private static boolean convertLetterGrades = ServerConfigurationService.getBoolean(CONVERT_LETTER_GRADES_SAKAI_PROPERTY, false);
    private static final SortedMap<Double, String> gradeScale;
    static // initialize the gradeScale map
    {
        gradeScale = new TreeMap<>(Collections.reverseOrder());
        String[] keys = ServerConfigurationService.getStrings(KEYS_SAKAI_PROPERTY);
        String[] values = ServerConfigurationService.getStrings(VALUES_SAKAI_PROPERTY);
        if (keys != null && values != null && keys.length == values.length)
        {
            for (int i = 0; i < keys.length; ++i)
            {
                try
                {
                    Double key = Double.valueOf(keys[i]);
                    gradeScale.put(key, values[i]);
                }
                catch (NumberFormatException nfe)
                {
                    // don't care, just skip it
                }
            }
        }
        else if (LOG.isWarnEnabled())
        {
            LOG.warn(LOG_PREFIX + "Gradescale for stats failed to initialize. Check sakai.properties for " + KEYS_SAKAI_PROPERTY + " and " + VALUES_SAKAI_PROPERTY);
        }
    }
    private final Map<String, MutableInt> gradeScaleCount;
    private final DescriptiveStatistics stats;
    private Double mode;
    private final int numericCount;
    private int nonNumericCount;
    
    public CourseGradeStatistics(Set<OwlGradeSubmissionGrades> grades)
    {
        nonNumericCount = 0;
        gradeScaleCount = new HashMap<>(gradeScale.size());
        // set all counts to zero
        for (String key : gradeScale.values())
        {
            gradeScaleCount.put(key, new MutableInt(0));
        }
       
        List<Double> gradeList = new ArrayList<>();
        for (OwlGradeSubmissionGrades grade : grades)
        {
            String g = grade.getGrade().trim();
            
            // try to parse double from grade
            Double d = convertStringToDouble(g);
            // if can't parse, try to convert
            if (d.isNaN() && convertLetterGrades)
            {
                d = convertLetterGradeToDouble(g);
            }
            // if we have a double now, count it
            if (!d.isNaN())
            {
                countGrade(d);
                gradeList.add(d);
            }
        }
        
        mode = mode(gradeList.toArray(new Double[0]));
        if (mode == null)
        {
            mode = Double.NaN;
        }
        numericCount = gradeList.size();
        nonNumericCount = grades.size() - numericCount;
        
        stats = new DescriptiveStatistics(new SynchronizedDescriptiveStatistics());
        for (Double d : gradeList)
        {
            stats.addValue(d);
        }
        
    }
    
    public DescriptiveStatistics getStats()
    {
        return stats;
    }
    
    public String getMeanForDisplay()
    {
        return roundAndDisplay(stats.getMean());
    }
    
    public String getStandardDeviationForDisplay()
    {
        return roundAndDisplay(stats.getStandardDeviation());
    }
    
    public String getMedianForDisplay()
    {
        return roundAndDisplay(getMedian());
    }
    
    public String getModeForDisplay()
    {
        return roundAndDisplay(getMode());
    }
    
    public String getMinimumForDisplay()
    {
        return roundAndDisplay(stats.getMin());
    }
    
    public String getMaximumForDisplay()
    {
        return roundAndDisplay(stats.getMax());
    }
    
    public String getSkewnessForDisplay()
    {
        return roundAndDisplay(stats.getSkewness());
    }
    
    public Map<String, MutableInt> getCountForLetterGrade()
    {
        return gradeScaleCount;
    }
    
    public Double getMode()
    {
        return mode;
    }
    
    public double getMedian()
    {
        return stats.getPercentile(50);
    }
    
    /**
     * Number of non-numeric grades in the grade data
     * @return 
     */
    public int getNonNumericCount()
    {
        return nonNumericCount;
    }
    
    /**
     * Number of numeric grades in the data
     * @return 
     */
    public int getNumericCount()
    {
        return numericCount;
    }
    
    private Double convertLetterGradeToDouble(String letter)
    {
        Double d = Double.NaN;
        String l = letter.trim();
        for (Double k : gradeScale.keySet())
        {
            if (l.equals(gradeScale.get(k)))
            {
                d = k;
            }
        }
        
        return d;
    }
    
    private Double convertStringToDouble(String number)
    {
        Double d = Double.NaN;
        try
        {
            d = Double.valueOf(number);
        }
        catch (NumberFormatException nfe)
        {
            // do nothing
        }
        
        return d;
    }
    
    private void countGrade(Double d)
    {
        for (Double k : gradeScale.keySet())
        {
            if (d >= k)
            {
                String gradeLetter = gradeScale.get(k);
                gradeScaleCount.get(gradeLetter).increment();
                break;
            }
        }
    }
    
    private String roundAndDisplay(Double d)
    {
        String display;
        
        if (d == null || d.isNaN())
        {
            display = "--";
        }
        else
        {
            DecimalFormat df = new DecimalFormat("##0.00");
            df.setRoundingMode(RoundingMode.HALF_UP);
            display = df.format(d);
        }
        
        return display;
    }
    
    // Mode -- taken from Apache Commons Lang ObjectUtils.java  --plukasew
    //-----------------------------------------------------------------------
    /**
     * Find the most frequently occurring item.
     * 
     * @param <T> type of values processed by this method
     * @param items to check
     * @return most populous T, {@code null} if non-unique or no items supplied
     * @since 3.0.1
     */
    private static <T> T mode(T... items) {
        if (ArrayUtils.isNotEmpty(items)) {
            HashMap<T, MutableInt> occurrences = new HashMap<>(items.length);
            for (T t : items) {
                MutableInt count = occurrences.get(t);
                if (count == null) {
                    occurrences.put(t, new MutableInt(1));
                } else {
                    count.increment();
                }
            }
            T result = null;
            int max = 0;
            for (Map.Entry<T, MutableInt> e : occurrences.entrySet()) {
                int cmp = e.getValue().intValue();
                if (cmp == max) {
                    result = null;
                } else if (cmp > max) {
                    max = cmp;
                    result = e.getKey();
                }
            }
            return result;
        }
        return null;
    }

    
}
