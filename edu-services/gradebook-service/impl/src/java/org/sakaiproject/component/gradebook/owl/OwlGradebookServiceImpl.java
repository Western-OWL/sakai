package org.sakaiproject.component.gradebook.owl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.sakaiproject.component.gradebook.GradebookFrameworkServiceImpl;
import org.sakaiproject.component.gradebook.GradebookServiceHibernateImpl;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeApproval;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.OwlGradeSubmission;
import org.sakaiproject.service.gradebook.shared.owl.OwlGradebookService;
import org.sakaiproject.service.gradebook.shared.owl.anongrading.OwlAnonGradingID;
import org.sakaiproject.service.gradebook.shared.owl.finalgrades.report.FGChanges;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.gradebook.facades.owl.OwlAuthz;
import org.sakaiproject.user.api.UserDirectoryService;
import org.springframework.orm.hibernate5.HibernateCallback;

/**
 *
 * @author plukasew
 */
@RequiredArgsConstructor
public class OwlGradebookServiceImpl implements OwlGradebookService
{
	private final OwlAuthz owlAuthz;
	private final GradebookServiceHibernateImpl gbServ;
	private final SiteService siteServ;
	private final UserDirectoryService userDirServ;
	private FinalGradeChangesReporter reporter;

	@Override
	public OwlGradebookService owlDoNotCall()
	{
		return this;
	}

	@Override
	public OwlAuthz owlAuthz()
	{
		return owlAuthz;
	}

    @Override
	public List<OwlGradeSubmission> getAllCourseGradeSubmissionsForSectionInSite(final String sectionEid, final String siteId) throws IllegalArgumentException
	{
		if (siteId == null || siteId.trim().isEmpty())
		{
			throw new IllegalArgumentException("siteId cannot be null or blank");
		}
		if (sectionEid == null || sectionEid.trim().isEmpty())
		{
			throw new IllegalArgumentException("sectionEid cannot be null or blank");
		}

		HibernateCallback hc = new HibernateCallback()
		{
			public Object doInHibernate(Session session) throws HibernateException
			{
				List<OwlGradeSubmission> results = new ArrayList<>();
				String sid = siteId.trim();
				String eid = sectionEid.trim();
				String queryString = "FROM OwlGradeSubmission WHERE siteId = ?0 AND sectionEid = ?1 ORDER BY submissionDate DESC";
				Query query = session.createQuery(queryString);
				query.setString(0, sid);
				query.setString(1, eid);
				List<OwlGradeSubmission> submissions = query.list();

				for (OwlGradeSubmission sub : submissions)
				{
					int gradeCount = sub.getGradeData().size(); // this is really just here to prefetch the grade data while the Hibernate session is still open
					if (gradeCount > 0)
					{
						results.add(sub);
						// also prefetch any previous submission and/or approval
						if (sub.hasPrevSubmission())
						{
							sub.getPrevSubmission().getStatusCode();
						}
						if (sub.hasApproval())
						{
							sub.getApproval().getUploadedToRegistrar();
						}
					}

				}

				return results;
			}
		};

		return (List<OwlGradeSubmission>) gbServ.getHibernateTemplate().execute(hc);
	}

	@Override
	public OwlGradeSubmission getMostRecentCourseGradeSubmissionForSectionInSite(final String sectionEid, final String siteId) throws IllegalArgumentException
	{
		OwlGradeSubmission mostRecent = null;
		List<OwlGradeSubmission> results = getAllCourseGradeSubmissionsForSectionInSite(sectionEid, siteId);
		if (!results.isEmpty())
		{
			mostRecent = results.get(0); // results is already sorted by date in descending order
		}

		return mostRecent;
	}

	// OWL-1228  --plukasew
	@Override
	public boolean isSectionInSiteApproved(final String sectionEid, final String siteId) throws IllegalArgumentException
	{
		OwlGradeSubmission mostRecent = getMostRecentCourseGradeSubmissionForSectionInSite(sectionEid, siteId);

		return mostRecent != null && mostRecent.hasApproval();
	}

	@Override
	public boolean areAllSectionsInSiteApproved(final Set<String> sectionEids, final String siteId) throws IllegalArgumentException
	{
		if (sectionEids == null || sectionEids.isEmpty())
		{
			return false;
		}

		for (String eid : sectionEids)
		{
			if (!isSectionInSiteApproved(eid, siteId))
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public Long createSubmission(final OwlGradeSubmission sub) throws IllegalArgumentException
	{
		if (sub == null || sub.getStatusCode() == OwlGradeSubmission.UNDEFINED_STATUS)
		{
			throw new IllegalArgumentException("submission cannot be null or a \"null\" object");
		}

		HibernateCallback hc = new HibernateCallback()
		{
			public Object doInHibernate(Session session) throws HibernateException
			{
				Long id = (Long) session.save(sub);
				return id;
			}
		};

		return (Long) gbServ.getHibernateTemplate().execute(hc);

	}

	@Override
	public void updateSubmission(final OwlGradeSubmission sub) throws IllegalArgumentException
	{
		if (sub == null || sub.getStatusCode() == OwlGradeSubmission.UNDEFINED_STATUS)
		{
			throw new IllegalArgumentException("submission cannot be null or a \"null\" object");
		}

		HibernateCallback hc = new HibernateCallback()
		{
			public Object doInHibernate(Session session) throws HibernateException
			{
				session.update(sub);

				return null;
			}
		};

		gbServ.getHibernateTemplate().execute(hc);
	}

	@Override
	public Long createApproval(final OwlGradeApproval approval) throws IllegalArgumentException
	{
		if (approval == null)
		{
			throw new IllegalArgumentException("approval cannot be null");
		}

		HibernateCallback hc = new HibernateCallback()
		{
			public Object doInHibernate(Session session) throws HibernateException
			{
				return (Long) session.save(approval);
			}
		};

		return (Long) gbServ.getHibernateTemplate().execute(hc);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public boolean isOfficialRegistrarGradingSchemeInUse( final Long gradebookID )
	{
		if( gradebookID == null )
			return false;

		String mappingScaleUID = gbServ.getGradebook( gradebookID ).getSelectedGradeMapping().getGradingScale().getUid();
		return( mappingScaleUID != null && !"".equals( mappingScaleUID ) && GradebookFrameworkServiceImpl.OFFICIAL_REGISTRAR_SCALE_UID.equals( mappingScaleUID ) );
	}

		/* Begin OWL anonymous grading methods --plukasew
	 *
	 * These methods interface with the owl_anon_grading_id table and are
	 * for accessing and storing anonymous grading ids sourced from Registrar.
	 */

	@Override
	public List<OwlAnonGradingID> getAnonGradingIds()
	{
		HibernateCallback hc = (Session session)->
		{
			String queryString = "FROM OwlAnonGradingID";
			Query query = session.createQuery(queryString);
			return query.list();
		};

		return (List<OwlAnonGradingID>) gbServ.getHibernateTemplate().execute(hc);
	}

	@Override
	public List<OwlAnonGradingID> getAnonGradingIdsForSection(final String sectionEid)
	{
		if (StringUtils.isBlank(sectionEid))
		{
			return Collections.<OwlAnonGradingID>emptyList();
		}

		HibernateCallback hc = (Session session)->
		{
			String sid = sectionEid.trim();
			String queryString = "FROM OwlAnonGradingID Where sectionEid = ?0";
			Query query = session.createQuery(queryString);
			query.setString(0, sid);
			return query.list();
		};

		return (List<OwlAnonGradingID>) gbServ.getHibernateTemplate().execute(hc);
	}

	@Override
	public List<OwlAnonGradingID> getAnonGradingIDsByGradingIDs(final Collection<Integer> gradingIDs)
	{
		if (CollectionUtils.isEmpty(gradingIDs))
		{
			return Collections.<OwlAnonGradingID>emptyList();
		}

		HibernateCallback hc = (Session session)->
		{
			List<OwlAnonGradingID> allGradingIDs = new ArrayList<>();
			String queryString = "FROM OwlAnonGradingID WHERE anonGradingID IN (:gradingIDs)";
			// each query can only do 1000 at a time
			List<Integer> gradingIDSublist = new ArrayList<>();

			Iterator<Integer> itGradingIDs = gradingIDs.iterator();
			while (itGradingIDs.hasNext())
			{
				Integer currentGradingID = itGradingIDs.next();
				gradingIDSublist.add(currentGradingID);
				// query 1000 gradingIDs at a time (Oracle bug)
				if (gradingIDSublist.size() == 1000 || !itGradingIDs.hasNext())
				{
					List<OwlAnonGradingID> thisBatch = session.createQuery(queryString).setParameterList("gradingIDs", gradingIDSublist).list();
					allGradingIDs.addAll(thisBatch);
					gradingIDSublist.clear();
				}
			}
			return allGradingIDs;
		};

		return (List<OwlAnonGradingID>) gbServ.getHibernateTemplate().execute(hc);
	}

	@Override
	public List<OwlAnonGradingID> getAnonGradingIDsBySectionEIDs(final Collection<String> sectionEIDs)
	{
		if (CollectionUtils.isEmpty(sectionEIDs))
		{
			return Collections.<OwlAnonGradingID>emptyList();
		}

		HibernateCallback hc = (Session session)->
		{
			List<OwlAnonGradingID> allGradingIDs = new ArrayList<>();
			String queryString = "FROM OwlAnonGradingID where sectionEid IN (:sectionEIDs)";
			// each query can only do 1000 at a time (Oracle bug)
			List<String> sectionEIDSublist = new ArrayList<>();
			Iterator<String> itSectionEIDs = sectionEIDs.iterator();
			while (itSectionEIDs.hasNext())
			{
				String currentSectionEID = itSectionEIDs.next();
				sectionEIDSublist.add(currentSectionEID);
				//query 1000 sectionEIDs at a time (Oracle bug)
				if (sectionEIDSublist.size() == 1000 || !itSectionEIDs.hasNext())
				{
					List<OwlAnonGradingID> thisBatch = session.createQuery(queryString).setParameterList("sectionEIDs", sectionEIDSublist).list();
					allGradingIDs.addAll(thisBatch);
					sectionEIDSublist.clear();
				}
			}
			return allGradingIDs;
		};

		return (List<OwlAnonGradingID>) gbServ.getHibernateTemplate().execute(hc);
	}

	@Override
	public Map<String, Map<String, String>> getAnonGradingIdMapBySectionEids(final Set<String> sectionEids)
	{
		List<OwlAnonGradingID> anonIds = getAnonGradingIDsBySectionEIDs(sectionEids);

		Map<String, Map<String, String>> anonIdMap = new HashMap<>();

		for (OwlAnonGradingID id : anonIds)
		{
			String userEid = id.getUserEid();
			if (anonIdMap.containsKey(userEid))
			{
				anonIdMap.get(userEid).put(id.getSectionEid(), id.getAnonGradingID().toString());
			}
			else
			{
				Map<String, String> m = new HashMap<>();
				m.put(id.getSectionEid(), id.getAnonGradingID().toString());
				anonIdMap.put(userEid, m);
			}
		}

		return anonIdMap;
	}

	@Override
	public Optional<OwlAnonGradingID> getAnonGradingId(final String sectionEid, final String userEid) throws IllegalArgumentException
	{
		if (StringUtils.isBlank(sectionEid) || StringUtils.isBlank(userEid))
		{
			throw new IllegalArgumentException("sectionEid/userEid cannot be null or blank");
		}

		HibernateCallback hc = (Session session)->
		{
			String sid = sectionEid.trim();
			String uid = userEid.trim();
			String queryString = "FROM OwlAnonGradingID WHERE SectionEid = ?0 and userEid = ?1";
			Query query = session.createQuery(queryString);
			query.setString(0, sid);
			query.setString(1, uid);
			return query.uniqueResult();
		};

		OwlAnonGradingID id = (OwlAnonGradingID) gbServ.getHibernateTemplate().execute(hc);
		return Optional.ofNullable(id);
	}

	@Override
	public Long createAnonGradingId(final OwlAnonGradingID gradingId) throws IllegalArgumentException
	{
		if (gradingId == null || gradingId.getAnonGradingID() == 0)
		{
			throw new IllegalArgumentException("grading id cannot be null or a \"null\" object");
		}

		HibernateCallback hc = (Session session)->(Long) session.save(gradingId);

		return (Long) gbServ.getHibernateTemplate().execute(hc);
	}

	@Override
	public void updateAnonGradingId(final OwlAnonGradingID gradingId) throws IllegalArgumentException
	{
		if (gradingId == null || gradingId.getAnonGradingID() == 0)
		{
			throw new IllegalArgumentException("grading id cannot be null or a \"null\" object");
		}

		HibernateCallback hc = (Session session)->
		{
			session.update(gradingId);
			return null;
		};

		gbServ.getHibernateTemplate().execute(hc);
	}

	@Override
	public int createAnonGradingIds(final Set<OwlAnonGradingID> gradingIds) throws IllegalArgumentException
	{
		if (gradingIds == null)
		{
			throw new IllegalArgumentException("grading id set cannot be null");
		}

		HibernateCallback hc = (Session session)->
		{
			int createCount = 0;
			for (OwlAnonGradingID id : gradingIds)
			{
				if (id != null && id.getAnonGradingID() != 0)
				{
					session.save(id);
					++createCount;
				}
			}

			return createCount;
		};

		return ((Integer) gbServ.getHibernateTemplate().execute(hc));
	}

	@Override
	public int updateAnonGradingIds(final Set<OwlAnonGradingID> gradingIds) throws IllegalArgumentException
	{
		if (gradingIds == null)
		{
			throw new IllegalArgumentException("grading id set cannot be null");
		}

		HibernateCallback hc = (Session session)->
		{
			int createCount = 0;
			for (OwlAnonGradingID id : gradingIds)
			{
				if (id != null && id.getAnonGradingID() != 0)
				{
					session.update(id);
					++createCount;
				}
			}

			return createCount;
		};

		return ((Integer) gbServ.getHibernateTemplate().execute(hc));
	}

	@Override
	public void deleteAnonGradingId(final OwlAnonGradingID gradingId) throws IllegalArgumentException
	{
		if (gradingId == null || gradingId.getAnonGradingID() == 0)
		{
			throw new IllegalArgumentException("grading id cannot be null or a \"null\" object");
		}

		gbServ.getHibernateTemplate().delete(gbServ.getHibernateTemplate().merge(gradingId));
	}

	@Override
	public int deleteAnonGradingIds(final Set<OwlAnonGradingID> gradingIds) throws IllegalArgumentException
	{
		if (gradingIds == null)
		{
			throw new IllegalArgumentException("grading id set cannot be null");
		}

		List<OwlAnonGradingID> merged = mergeAll(gradingIds.stream().filter(id -> id != null && id.getAnonGradingID() != 0).collect(Collectors.toSet()));
		gbServ.getHibernateTemplate().deleteAll(merged);
		return merged.size();
	}

	private List<OwlAnonGradingID> mergeAll(final Set<OwlAnonGradingID> gradingIds)
	{
		List<OwlAnonGradingID> merged = new ArrayList<>();
		gradingIds.forEach(id -> merged.add(gbServ.getHibernateTemplate().merge(id)));
		return merged;
	}

	/** End OWL anonymous grading methods */

	/* Begin Final Grades Report Job methods */

	@Override
	public FGChanges getFinalGradeChanges(String siteId, String sectionId)
	{
		return getReporter().getChanges(siteId, sectionId);
	}

	private FinalGradeChangesReporter getReporter()
	{
		if (reporter == null)
		{
			reporter = new FinalGradeChangesReporter(gbServ, this, siteServ, userDirServ);
		}

		return reporter;
	}

	/* End Final Grades Report Job methods */
}
