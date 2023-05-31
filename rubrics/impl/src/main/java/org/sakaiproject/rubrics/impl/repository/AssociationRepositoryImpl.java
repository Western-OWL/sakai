/**********************************************************************************
 *
 * Copyright (c) 2017 The Sakai Foundation
 *
 * Original developers:
 *
 *   Unicon
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.rubrics.impl.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.sakaiproject.rubrics.api.model.Rubric;
import org.sakaiproject.rubrics.api.model.ToolItemRubricAssociation;
import org.sakaiproject.rubrics.api.repository.AssociationRepository;
import org.sakaiproject.springframework.data.SpringCrudRepositoryImpl;
import org.springframework.util.CollectionUtils;

public class AssociationRepositoryImpl extends SpringCrudRepositoryImpl<ToolItemRubricAssociation, Long> implements AssociationRepository {

    public Optional<ToolItemRubricAssociation> findByToolIdAndItemId(String toolId, String itemId) {

        return Optional.ofNullable(findByToolIdAndItemIds(toolId, Collections.singleton(itemId)).get(itemId));
    }

    public Map<String, ToolItemRubricAssociation> findByToolIdAndItemIds(String toolId, Set<String> itemIds) {
        return findByToolIdsAndItemIds(Collections.singleton(toolId), itemIds);
    }

    public Map<String, ToolItemRubricAssociation> findByToolIdsAndItemIds(Set<String> toolIds, Set<String> itemIds) {
        if (CollectionUtils.isEmpty(toolIds) || CollectionUtils.isEmpty(itemIds)) {
            return Collections.emptyMap();
        }

        Session session = sessionFactory.getCurrentSession();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ToolItemRubricAssociation> query = cb.createQuery(ToolItemRubricAssociation.class);
        Root<ToolItemRubricAssociation> ass = query.from(ToolItemRubricAssociation.class);
        query.where(cb.and(ass.get("toolId").in(toolIds),
                            ass.get("itemId").in(itemIds),
                            cb.equal(ass.get("active"), Boolean.TRUE)));
        return session.createQuery(query).getResultList().stream().collect(Collectors.toMap(ToolItemRubricAssociation::getItemId, tira -> tira));
    }

    public Optional<ToolItemRubricAssociation> findByItemIdAndRubricId(String itemId, Long rubricId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ToolItemRubricAssociation> query = cb.createQuery(ToolItemRubricAssociation.class);
        Root<ToolItemRubricAssociation> ass = query.from(ToolItemRubricAssociation.class);
        query.where(cb.and(cb.equal(ass.get("itemId"), itemId),
                            cb.equal(ass.get("rubric"), rubricId)));

        return session.createQuery(query).uniqueResultOptional();
    }

    public List<ToolItemRubricAssociation> findByRubricId(Long rubricId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ToolItemRubricAssociation> query = cb.createQuery(ToolItemRubricAssociation.class);
        Root<ToolItemRubricAssociation> ass = query.from(ToolItemRubricAssociation.class);
        query.where(cb.equal(ass.get("rubric"), rubricId));

        return session.createQuery(query).list();
    }
	
    public List<ToolItemRubricAssociation> findByItemIdPrefix(String toolId, String itemId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ToolItemRubricAssociation> query = cb.createQuery(ToolItemRubricAssociation.class);
        Root<ToolItemRubricAssociation> ass = query.from(ToolItemRubricAssociation.class);
        query.where(cb.and(cb.equal(ass.get("toolId"), toolId),
                            cb.like(ass.get("itemId"), itemId + "%")));

        return session.createQuery(query).list();
    }

    public int deleteBySiteId(String siteId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ToolItemRubricAssociation> query = cb.createQuery(ToolItemRubricAssociation.class);
        Root<ToolItemRubricAssociation> root = query.from(ToolItemRubricAssociation.class);
        Join<ToolItemRubricAssociation, Rubric> join = root.join("rubric");
        query.where(cb.equal(join.get("ownerId"), siteId));
        List<ToolItemRubricAssociation> associations = session.createQuery(query).list();

        associations.forEach(session::delete);

        return associations.size();
    }

    public int deleteByRubricId(Long rubricId) {

        Session session = sessionFactory.getCurrentSession();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaDelete<ToolItemRubricAssociation> delete = cb.createCriteriaDelete(ToolItemRubricAssociation.class);
        Root<ToolItemRubricAssociation> ass = delete.from(ToolItemRubricAssociation.class);
        delete.where(cb.equal(ass.get("rubric"), rubricId));
        
        return session.createQuery(delete).executeUpdate();
    }
}
