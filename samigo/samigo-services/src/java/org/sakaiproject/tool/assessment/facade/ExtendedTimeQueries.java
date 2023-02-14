/*
 * Copyright (c) 2016, The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.sakaiproject.tool.assessment.facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.StringType;
import org.hibernate.type.spi.TypeConfiguration;
import org.sakaiproject.tool.assessment.data.dao.assessment.ExtendedTime;
import org.sakaiproject.tool.assessment.data.ifc.assessment.AssessmentBaseIfc;
import org.sakaiproject.tool.assessment.data.ifc.assessment.PublishedAssessmentIfc;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;

/**
 * @author Leonardo Canessa [lcanessa1 (at) udayton (dot) edu]
 */
@Slf4j
public class ExtendedTimeQueries extends HibernateDaoSupport implements ExtendedTimeQueriesAPI {

    private static final int MAX_IN_CLAUSE_SIZE = 1000;

    /**
     * init
     */
    public void init () {
        log.info("init()");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<ExtendedTime> getEntriesForAss(AssessmentBaseIfc ass) {
        log.debug("getEntriesForAss " + ass.getAssessmentBaseId());

        try {
            HibernateCallback hcb = (Session s) -> {
                Query q = s.getNamedQuery(QUERY_GET_ENTRIES_FOR_ASSESSMENT);
                q.setParameter(ASSESSMENT_ID, ass, new ManyToOneType(TypeConfiguration::new, "org.sakaiproject.tool.assessment.data.dao.assessment.AssessmentBaseData"));
                return q.list();
            };
            return (List<ExtendedTime>) getHibernateTemplate().execute(hcb);
        } catch (DataAccessException e) {
            log.error("Failed to get Extended TimeEntries for Assessment: " + ass.getAssessmentBaseId(), e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<ExtendedTime> getEntriesForPub(PublishedAssessmentIfc pub) {
        log.debug("getEntriesForPub " + pub.getPublishedAssessmentId());
        Long pubId = pub.getPublishedAssessmentId();

        try {
            HibernateCallback hcb = (Session s) -> {
                Query q = s.getNamedQuery(QUERY_GET_ENTRIES_FOR_PUBLISHED);
                q.setParameter(PUBLISHED_ID, pubId);
                return q.list();
            };

            return (List<ExtendedTime>) getHibernateTemplate().execute(hcb);
        } catch (DataAccessException e) {
            log.error("Failed to get Extended Time Entries for Published Assessment: " + pub.getPublishedAssessmentId(), e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ExtendedTime getEntryForPubAndUser(PublishedAssessmentIfc pub, String userId) {
        log.debug("getEntryForPubAndUser, pub: '" + pub.getPublishedAssessmentId() + "' User: " + userId);

        return getPubAndX(QUERY_GET_ENTRY_FOR_PUB_N_USER, pub, USER_ID, userId);
    }

    /**
     * {@inheritDoc}
     */
    public ExtendedTime getEntryForPubAndGroup (PublishedAssessmentIfc pub, String groupId) {
        log.debug("getEntryForPubAndGroup, pub: '" + pub.getPublishedAssessmentId() + "' group: " + groupId);

        return getPubAndX(QUERY_GET_ENTRY_FOR_PUB_N_GROUP, pub, GROUP, groupId);
    }

        /**
     * {@innheritDoc}
     */
    public List<ExtendedTime> getEntriesForPubAndUserOrGroups(PublishedAssessmentIfc pub, String userId, Collection<String> groupIDs) {
        log.debug("getEntriesForPubAndUserOrGroups, pub: '" + pub.getPublishedAssessmentId() + "', User: " + userId + ", groups: " + groupIDs);
        Long pubId = pub.getPublishedAssessmentId();

        List<ExtendedTime> entries;

        // Query groupIDs up to 1000 at a time (Oracle in clause limit)
        Collection<String> queryGroupIDs;
        Iterator<String> itGroupIDs = groupIDs.iterator();
        if (groupIDs.size() < MAX_IN_CLAUSE_SIZE) {
            queryGroupIDs = groupIDs;
        } else {
            queryGroupIDs = new ArrayList<>(MAX_IN_CLAUSE_SIZE);
            while (itGroupIDs.hasNext() && queryGroupIDs.size() < MAX_IN_CLAUSE_SIZE) {
                queryGroupIDs.add(itGroupIDs.next());
            }
        }

        try {
            HibernateCallback hcb = (Session s) -> {
                Query q = s.getNamedQuery(QUERY_GET_ENTRIES_FOR_PUB_USER_N_GROUPS);
                q.setParameter(PUBLISHED_ID, pubId);
                q.setParameter(USER_ID, userId);
                q.setParameterList(GROUPS, queryGroupIDs);
                return q.list();
            };
            entries = (List<ExtendedTime>)getHibernateTemplate().execute(hcb);
        } catch (DataAccessException de) {
            log.error("Failed to get extended time for pub: '{}', User: {}, groupIDs: {}", pub.getPublishedAssessmentId(), userId, queryGroupIDs);
            return Collections.emptyList();
        }

        if (groupIDs.size() >= MAX_IN_CLAUSE_SIZE) {
            // Use itGroupIDs to continue where we left off
            entries.addAll(getEntriesForPubAndGroups(pubId, itGroupIDs));
        }

        return entries;
    }

    private List<ExtendedTime> getEntriesForPubAndGroups(Long pubId, Iterator<String> itGroupIDs) {
        log.debug("getEntriesForPubAndGroups, pub: '{}'; using groupId iterator", pubId);
        if (!itGroupIDs.hasNext()) {
            return Collections.emptyList();
        }
        List<ExtendedTime> entries = new ArrayList<>();

        List<String> queryGroupIDs = new ArrayList<>(MAX_IN_CLAUSE_SIZE);
        while (itGroupIDs.hasNext()) {
            queryGroupIDs.clear();
            while (itGroupIDs.hasNext() && queryGroupIDs.size() < MAX_IN_CLAUSE_SIZE) {
                queryGroupIDs.add(itGroupIDs.next());
            }

            try {
                HibernateCallback hcb = (Session s) -> {
                    Query q = s.getNamedQuery(QUERY_GET_ENTRIES_FOR_PUB_N_GROUPS);
                    q.setParameter(PUBLISHED_ID, pubId);
                    q.setParameterList(GROUPS, queryGroupIDs);
                    return q.list();
                };
                log.debug("getEntriesForPubAndGroups, pub: '{}', groups: {}", pubId, queryGroupIDs);
                entries.addAll((List<ExtendedTime>)getHibernateTemplate().execute(hcb));
            } catch (DataAccessException de) {
                log.error("Failed to get extended time for pug: '{}', groupIDs: {}", pubId, queryGroupIDs);
                return Collections.emptyList();
            }
        }

        return entries;
    }

    /**
     * {@inheritDoc}
     */
    public boolean updateEntry(ExtendedTime e) {
        log.debug("updating entry assessment: '" + e.getAssessmentId() + "' pubId: '" + e.getPubAssessmentId() + "' user: '" + e.getUser() + "' group: " + e.getGroup());

        try {
            getHibernateTemplate().saveOrUpdate(e);
            return true;
        } catch (DataAccessException de) {
            log.error("Error updating extended time entry" , de);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateEntries(List<ExtendedTime> entries) {
        entries.forEach(this::updateEntry);
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteEntry(final ExtendedTime e) {
        log.debug("Removing ExtendedTime entry id: " + e.getId());

       try {
           getHibernateTemplate().delete(getHibernateTemplate().merge(e));
           return true;
       } catch (DataAccessException de) {
           log.error("Failed to delete extendedTime entry, id: " + e.getId() + ".", de);
           return false;
       }
    }

    @SuppressWarnings("unchecked")
    private ExtendedTime getPubAndX(final String query, final PublishedAssessmentIfc pub, final String secondParam, final String secondParamValue) {
        Long pubId = pub.getPublishedAssessmentId();
        try{
            HibernateCallback hcb = (Session s) -> {
                Query q = s.getNamedQuery(query);
                q.setParameter(PUBLISHED_ID, pubId);
                q.setParameter(secondParam, secondParamValue, new StringType());
                return q.uniqueResult();
            };

            return (ExtendedTime) getHibernateTemplate().execute(hcb);
        } catch (DataAccessException e) {
            log.error("Failed to get extended time for pub: " + pub.getPublishedAssessmentId() + " and user/group: " + secondParamValue, e);
            return null;
        }
    }
}
