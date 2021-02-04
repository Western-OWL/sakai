package org.sakaiproject.tool.assessment.data.ifc.assessment;

import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.sakaiproject.tool.assessment.shared.api.assessment.SecureDeliveryServiceAPI.Phase;
import org.sakaiproject.tool.assessment.shared.api.assessment.SecureDeliveryServiceAPI.PhaseStatus;

/**
 * @author Luis Camargo (lcamargo@respondus.com)
 */
public interface SecureDeliveryModuleIfc {
	
	/**
	 * This method is called by the SecureDeliveryService after the module is loaded from the
	 * plugin jar.
	 * 
	 * @return true if the module initialized successfully. If this method returns false, the
	 *         module won't be available for selection. 
	 * 
	 * @param logFactory
	 */
	public boolean initialize();
	
	/**
	 * @returns true if the module is globally enabled. How this is determined is up to
	 * each module implementation. 
	 */
	public boolean isEnabled();
	
	/**
	 * @returns the name of the module for the given locale	 */
	public String getModuleName( Locale locale );
	
	/**
	 * @param locale
	 * @return the string for the given locale to be appended to the assessment title
	 * when the asssessment is configured to require the use of this module 
	 */
	public String getTitleDecoration( Locale locale );
	
	/**
	 * Validates the phase. 
	 * 
	 * @param phase
	 * @param assessment
	 * @param request
	 * @return
	 */
	public PhaseStatus validatePhase( Phase phase, PublishedAssessmentIfc assessment, HttpServletRequest request );
	
	/**
	 * Returns the initial HTML fragment to be inserted on the assessment list
	 * 
	 * @param request
	 * @param locale
	 * @return
	 */
	public String getInitialHTMLFragment(  HttpServletRequest request, Locale locale );
	
	/**
	 * Returns the HTML fragment to be inserted during delivery
	 * 
	 * 
	 * @param assessment
	 * @param request
	 * @param fragmentId one of SecureDeliveryServiceAPI.SUCCESS_FRAGMENT or SecureDeliveryServiceAPI.FAILURE_FRAGMENT
	 * @param locale
	 * @return
	 */
	public String getHTMLFragment( PublishedAssessmentIfc assessment, HttpServletRequest request, Phase phase, PhaseStatus status, Locale locale );
	
	/**
	 * Validates the context object passed as parameter.
	 * 
	 * 
	 * @param context
	 * @return
	 */
	public boolean validateContext( Object context );
	
	/**
	 * Encrypts the password passed as parameter. The encryption method is up to the module implementation.
	 *  
	 * @param password
	 * @return the encrypted password
	 */
	public String encryptPassword( String password );
	
	/**
	 * Decrypts the passoword passed as parameter. The encryption method is up to the module implementation.
	 * 
	 * @param password
	 * @return the plain text password
	 */
	public String decryptPassword( String password );

	/**
	 * Returns an absolute URL to an alternative location to take the assessment.
	 * For example, a cloud proctoring company could use this URL to embed the Samigo assessment in an iframe.
	 * @param assessmentId
	 * @param uid
	 * @return
	 */
	default Optional<String> getAlternativeDeliveryUrl(Long assessmentId, String uid) {
		return Optional.empty();
	}

	/**
	 * Returns an absolute URL to an alternative location to review the student's taking of the        proctored assessment.
	 * @param assessmentId
	 * @param studentId (internal user id)
	 * @return
	 */
	default Optional<String> getInstructorReviewUrl(Long assessmentId, String studentId) {
		return Optional.empty();
	}

	/**
	 * Returns whether the SecureDelivery service is implemented for this one assessment
	 * Some institutions may want to limit what assessments or sites are allowed to use the service.
	 * @param assessment
	 * @return
	 */
	default boolean isEnabled(Long assessmentId) {
		return isEnabled();
	}

}
