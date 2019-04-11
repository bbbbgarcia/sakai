package org.sakaiproject.office365.repository;

import java.util.List;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Transactional;
import org.sakaiproject.serialization.BasicSerializableRepository;

import org.sakaiproject.office365.model.OfficeUser;
import org.sakaiproject.office365.repository.OfficeUserRepository;

/**
 * Created by bgarcia
 */
@Transactional(readOnly = true)
public class OfficeUserRepositoryImpl extends BasicSerializableRepository<OfficeUser, String> implements OfficeUserRepository {

	@Override
	public OfficeUser findBySakaiId(String sakaiId){
        return (OfficeUser) startCriteriaQuery()
                .add(Restrictions.eq("sakaiUserId", sakaiId))
                .uniqueResult();
    }
}