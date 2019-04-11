package org.sakaiproject.office365.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "OFFICE_USER")
@Data @NoArgsConstructor
//@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfficeUser {

    @Id
	@JsonProperty("id")
    private String officeUserId;

    private String sakaiUserId;

	@Lob
    private String token;

	@Lob
    private String refreshToken;
    
    @JsonProperty("userPrincipalName")
    private String officeName;

}
