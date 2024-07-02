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

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

/**
 *This class represents the embeddable composite primary key
 *of the Invitation entity.
 *
 * @author Ankit Mutha(mutha.ankit@gmail.com)
 */
@Embeddable
public class InvitationId implements Serializable {
    private User inviter;
    private User invitee;

    public InvitationId(){}
     public InvitationId(User inviter, User invitee){
         this.inviter=inviter;
         this.invitee=invitee;
     }
    
    /**
     * @return the inviter
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(name = "INVITERID", nullable=false)
    })
    @ForeignKey(name="HIBERNATE_INVITATION_USER_FK1")
    public User getInviter() {
        return inviter;
    }

    /**
     * @param inviter the inviter to set
     */
    public void setInviter(User inviter) {
        this.inviter = inviter;
    }

    /**
     * @return the invitee
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(name = "INVITEEID", nullable=false)
    })
    @ForeignKey(name="HIBERNATE_INVITATION_USER_FK2")
    public User getInvitee() {
        return invitee;
    }

    /**
     * @param invitee the invitee to set
     */
    public void setInvitee(User invitee) {
        this.invitee = invitee;
    }
}
