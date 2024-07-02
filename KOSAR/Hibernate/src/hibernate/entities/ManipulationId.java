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
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ForeignKey;

/**
 * This class represents the embeddable composite primary key
 * of the Manipulation entity
 * @author Ankit Mutha (mutha.ankit@gmail.com)
 */
@Embeddable
public class ManipulationId implements java.io.Serializable {

    private int mid;
    private Resource resource;

    public ManipulationId(){}
    public ManipulationId(int manipulationID, Resource resource) {
        this.mid=manipulationID;
        this.resource=resource;
    }

    /**
     * @return the mid
     */
    @Column(name = "MID", nullable=false)
    public int getMid() {
        return mid;
    }

    /**
     * @param mid the mid to set
     */
    public void setMid(int mid) {
        this.mid = mid;
    }

    /**
     * @return the resource
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(name = "RID", nullable=false)
    })
    @ForeignKey(name="HIBERNATE_MANIPULATION_RESOURCE_FK1")
    public Resource getResource() {
        return resource;
    }

    /**
     * @param resource the resource to set
     */
    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
