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
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.ForeignKey;

/**
 * This entity class represents the Resource entity.
 * 
 * @author Ankit Mutha (mutha.ankit@gmail.com)  
 */
@Entity
@Table(name = "HIBERNATE_RESOURCE")

public class Resource implements Serializable {

    //rid, creatorid, walluserid, type, body, doc
    private int rid;
    private User creator;
    private User wallUser;
    private String type;
    private String body;
    private String doc;

    private List<Manipulation> manipulations = new ArrayList<Manipulation>();

    public Resource() {
    }

    public Resource(int rid, User creator, User wallUser) {
        this.rid = rid;
        this.creator = creator;
        this.wallUser = wallUser;
    }

    public Resource(int rid, User creator, User wallUser, String type, String body, String doc) {
        this.rid = rid;
        this.creator = creator;
        this.wallUser = wallUser;
        this.type = type;
        this.body = body;
        this.doc = doc;
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
     * @return the body
     */
    @Column(name = "BODY")
    public String getBody() {
        return body;
    }

    /**
     * @param body the body to set
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * @return the doc
     */
    @Column(name = "DOC")
    public String getDoc() {
        return doc;
    }

    /**
     * @param doc the doc to set
     */
    public void setDoc(String doc) {
        this.doc = doc;
    }

    /**
     * @return the manipulations
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "id.resource")
    public List<Manipulation> getManipulations() {
        return manipulations;
    }

    /**
     * @param manipulations the manipulations to set
     */
    public void setManipulations(List<Manipulation> manipulations) {
        this.manipulations = manipulations;
    }

    @Id
    @Column(name = "RID")
    public int getRid() {
        return rid;
    }

    /**
     * @param rid the rid to set
     */
    public void setRid(int rid) {
        this.rid = rid;
    }

    /**
     * @return the creator
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "CREATORID")
    })
    @ForeignKey(name="HIBERNATE_RESOURCE_USER_FK1")
    public User getCreator() {
        return creator;
    }

    /**
     * @param creator the creator to set
     */
    public void setCreator(User creator) {
        this.creator = creator;
    }

    /**
     * @return the wallUser
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
        @JoinColumn(name = "WALLUSERID")
    })
    @ForeignKey(name="HIBERNATE_RESOURCE_USER_FK2")
    public User getWallUser() {
        return wallUser;
    }

    /**
     * @param wallUser the wallUser to set
     */
    public void setWallUser(User wallUser) {
        this.wallUser = wallUser;
    }

}
