/**                                                                                                                                                                                
 * Copyright (c) 2012 USC Database Laboratory All rights reserved. 
 *
 * Authors:  Ankit Mutha (mutha.ankit@gmail.com)
 *			 Sumita Barahmand 
 *			 Shahram Ghandeharizadeh  
 *                                                                                                                                                                                 
 * Licensed under the Apache License, Version 2.0 (the "License"); you                                                                                                             
 * may not use this file except in compliance with the License. You                                                                                                                
 * may obtain a copy of the License at                                                                                                                                             
 *                                                                                                                                                                                 
 * http://www.apache.org/licenses/LICENSE-2.0                                                                                                                                      
 *                                                                                                                                                                                 
 * Unless required by applicable law or agreed to in writing, software                                                                                                             
 * distributed under the License is distributed on an "AS IS" BASIS,                                                                                                               
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or                                                                                                                 
 * implied. See the License for the specific language governing                                                                                                                    
 * permissions and limitations under the License. See accompanying                                                                                                                 
 * LICENSE file.                                                                                                                                                                   
 */
 
package hibernate.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *This entity class represents the asymmetric relationship of 
 *invitation between two members.
 *It has a composite primary key (inviter,invitee)
 *
 * @author Ankit Mutha (mutha.ankit@gmail.com)
 */
@Entity
@Table(name = "HIBERNATE_INVITATION")
public class Invitation implements Serializable {

	/*
	 * Invitation has a composite primary key
	 */
    private InvitationId id;

    public Invitation() {
    }

    public Invitation(User inviter, User invitee) {
        id = new InvitationId(inviter, invitee);
    }

    /**
     * @return the id
     */
    @Id
    public InvitationId getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(InvitationId id) {
        this.id = id;
    }

    @Transient
    public User getInviter() {
        return this.id.getInviter();
    }

    @Transient
    public User getInvitee() {
        return this.id.getInvitee();
    }

}
