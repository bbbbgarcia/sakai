package org.sakaiproject.office365.repository;

import org.sakaiproject.office365.model.OfficeUser;
import org.sakaiproject.serialization.SerializableRepository;

public interface OfficeUserRepository extends SerializableRepository<OfficeUser, String> {
	public OfficeUser findBySakaiId(String id);
}
