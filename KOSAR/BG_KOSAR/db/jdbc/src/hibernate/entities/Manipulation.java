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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;

/**
 * This entity class represents the comments posted on a resource.
 * This has a many-to-one association from Resource entity.
 * 
 * This has a composite primary key (mid,rid)
 *
 * @author Ankit Mutha (mutha.ankit@gmail.com)
 */
@Entity
@Table(name = "HIBERNATE_MANIPULATION")
public class Manipulation implements java.io.Serializable {

    private ManipulationId id;
    private User modifier;
    private User creator;
    private String timestamp;
    private String type;
    private String content;

    public Manipulation() {
    }

    public Manipulation(int mid,Resource resource) {
        this.id = new ManipulationId();
        this.id.setMid(mid);
        this.id.setResource(resource);
    }

    /**
     * @return the timestamp
     */
    @Column(name = "TIMESTAMP")
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the type
     */
    @Column(name = "TYPE")
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the content
     */
    @Column(name = "CONTENT")
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the id
     */
    @EmbeddedId
    public ManipulationId getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(ManipulationId id) {
        this.id = id;
    }

    /**
     * @return the modifier
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "MODIFIERID")
    @ForeignKey(name="HIBERNATE_MANIPULATION_USER_FK2")
    public User getModifier() {
        return modifier;
    }

    /**
     * @param modifier the modifier to set
     */
    public void setModifier(User modifier) {
        this.modifier = modifier;
    }

    @Transient
    public int getMid() {
        return this.id.getMid();
    }

    @Transient
    public Resource getResource() {
        return this.id.getResource();
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "CREATORID")
    @ForeignKey(name="HIBERNATE_MANIPULATION_USER_FK1")
	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

}
