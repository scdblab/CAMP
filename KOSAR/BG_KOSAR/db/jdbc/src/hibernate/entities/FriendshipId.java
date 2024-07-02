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

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

/**
 *This class represents the embeddable composite 
 * primary key of the Friendship entity.
 * 
 * @author Ankit Mutha (mutha.ankit@gmail.com)
 */

@Embeddable
public class FriendshipId  implements java.io.Serializable{
    private User friend1;
    private User friend2;

    public FriendshipId(){}
    public FriendshipId(User friend1, User friend2) {
        this.friend1=friend1;
        this.friend2=friend2;
    }

    /**
     * This represents one (asymmetric) many-to-one relation
     * from User entity to Friendship entity.
     * 
     * @return the friend1
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(name = "FRIEND1", nullable=false)
    })
    @ForeignKey(name="HIBERNATE_FRIENDSHIP_USER_FK1")
    public User getFriend1() {
        return friend1;
    }

    /**
     * @param friend1 the friend1 to set
     */
    public void setFriend1(User friend1) {
        this.friend1 = friend1;
    }

    /**
     * This represents one (asymmetric) many-to-one relation
     * from User entity to Friendship entity.
     * 
     * @return the friend2
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(name = "FRIEND2", nullable=false)
    })
    @ForeignKey(name="HIBERNATE_FRIENDSHIP_USER_FK2")
    public User getFriend2() {
        return friend2;
    }

    /**
     * @param friend2 the friend2 to set
     */
    public void setFriend2(User friend2) {
        this.friend2 = friend2;
    }
    
}
