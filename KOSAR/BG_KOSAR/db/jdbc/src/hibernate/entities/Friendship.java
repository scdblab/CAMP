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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 *
 * This entity class represents the symmetric relationship of friendship
 * between two members.
 * There are two (asymmetric) many-to-one associations from User entity to Friendship entity
 * making the friendship relationship.
 * It has a composite primary key (friend1, friend2)
 * 
 * @author Ankit Mutha (mutha.ankit@gmail.com)
 */
@Entity
@Table(name = "HIBERNATE_FRIENDSHIP")
public class Friendship implements Serializable {
    
	/*
	 * Friendship entity has a composite primary key
	 */
    private FriendshipId id;

    
    public Friendship(){}
    public Friendship(User friend1,User friend2)
    {
        id=new FriendshipId(friend1,friend2);
    }
    
    
    /**
     * @return the id
     */
    @EmbeddedId
    public FriendshipId getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(FriendshipId id) {
        this.id = id;
    }
    
    @Transient
    public User getFriend1()
    {
        return this.getId().getFriend1();
    }
    
    @Transient
    public User getFriend2()
    {
        return this.getId().getFriend2();
    }
    
}
