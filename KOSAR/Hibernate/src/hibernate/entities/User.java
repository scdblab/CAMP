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
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * This entity class represents the User.
 * 
 * @author Ankit Mutha (mutha.ankit@gmail.com)        
 */
@Entity
@Table(name = "HIBERNATE_USER")
public class User implements Serializable {
    //userid, username, pw, firstname, lastname, gender, dob, jdate, ldate, address, email, tel)

    private int userid;
    private String username;
    private String pw;
    private String firstname;
    private String lastname;
    private String gender;
    private String dob;
    private String jdate;
    private String ldate;
    private String address;
    private String email;
    private String tel;
    private Blob profileImage;
    private Blob thumbnailImage;

    private List<Invitation> pendingFriends = new ArrayList<Invitation>();
    private List<Invitation> invitedFriends = new ArrayList<Invitation>();

    private List<Friendship> friends = new ArrayList<Friendship>();
    private List<Friendship> friendOf = new ArrayList<Friendship>();

    private List<Resource> createdResources = new ArrayList<Resource>();
    private List<Resource> wallResources = new ArrayList<Resource>();
    
    private List<Manipulation> createdManipulations=new ArrayList<Manipulation>();
    private List<Manipulation> modifiedManipulations = new ArrayList<Manipulation>();

    public User() {
    }

    public User(int id) {
        this.userid = id;
    }

    /**
     * @return the userid
     */
    @Id
    @Column(name = "USERID")
    public int getUserid() {
        return userid;
    }

    /**
     * @param userid the userid to set
     */
    public void setUserid(int userid) {
        this.userid = userid;
    }

    /**
     * @return the username
     */
    @Column(name = "USERNAME")
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the pw
     */
    @Column(name = "PW")
    public String getPw() {
        return pw;
    }

    /**
     * @param pw the pw to set
     */
    public void setPw(String pw) {
        this.pw = pw;
    }

    /**
     * @return the firstname
     */
    @Column(name = "FIRSTNAME")
    public String getFirstname() {
        return firstname;
    }

    /**
     * @param firstname the firstname to set
     */
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    /**
     * @return the lastname
     */
    @Column(name = "LASTNAME")
    public String getLastname() {
        return lastname;
    }

    /**
     * @param lastname the lastname to set
     */
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
     * @return the gender
     */
    @Column(name = "GENDER")
    public String getGender() {
        return gender;
    }

    /**
     * @param gender the gender to set
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * @return the dob
     */
    @Column(name = "DOB")
    public String getDob() {
        return dob;
    }

    /**
     * @param dob the dob to set
     */
    public void setDob(String dob) {
        this.dob = dob;
    }

    /**
     * @return the jdate
     */
    @Column(name = "JDATE")
    public String getJdate() {
        return jdate;
    }

    /**
     * @param jdate the jdate to set
     */
    public void setJdate(String jdate) {
        this.jdate = jdate;
    }

    /**
     * @return the ldate
     */
    @Column(name = "LDATE")
    public String getLdate() {
        return ldate;
    }

    /**
     * @param ldate the ldate to set
     */
    public void setLdate(String ldate) {
        this.ldate = ldate;
    }

    /**
     * @return the address
     */
    @Column(name = "ADDRESS")
    public String getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return the email
     */
    @Column(name = "EMAIL")
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the tel
     */
    @Column(name = "TEL")
    public String getTel() {
        return tel;
    }

    /**
     * @param tel the tel to set
     */
    public void setTel(String tel) {
        this.tel = tel;
    }

    /**
     * @return the pic
     */
    @Column(name = "PROFILEIMAGE")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    public Blob getPic() {
        return profileImage;
    }

    /**
     * @return the tpic
     */
    @Column(name = "THUMNAILIMAGE")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    public Blob getTpic() {
        return thumbnailImage;
    }

    /**
     * @return the createdResources
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "creator")
    public List<Resource> getCreatedResources() {
        return createdResources;
    }

    /**
     * @param createdResources the createdResources to set
     */
    public void setCreatedResources(List<Resource> createdResources) {
        this.createdResources = createdResources;
    }

    /**
     * @return the wallResources
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "wallUser")
    public List<Resource> getWallResources() {
        return wallResources;
    }

    /**
     * @param wallResources the wallResources to set
     */
    public void setWallResources(List<Resource> wallResources) {
        this.wallResources = wallResources;
    }

    /**
     * @return the modifications
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "modifier")
    public List<Manipulation> getModifiedManipulations() {
        return modifiedManipulations;
    }

    /**
     * @param modifiedManipulations the modifications to set
     */
    public void setModifiedManipulations(List<Manipulation> modifiedManipulations) {
        this.modifiedManipulations = modifiedManipulations;
    }

    /**
     * @param pic the pic to set
     */
    public void setPic(Blob pic) {
        this.profileImage = pic;
    }

    /**
     * @param tpic the tpic to set
     */
    public void setTpic(Blob tpic) {
        this.thumbnailImage = tpic;
    }

    /**
     * @return the pendingFriends
     */
    @OneToMany(mappedBy="id.invitee",fetch = FetchType.LAZY)
    public List<Invitation> getPendingFriends() {
        return pendingFriends;
    }

    /**
     * @param pendingFriends the pendingFriends to set
     */
    public void setPendingFriends(List<Invitation> pendingFriends) {
        this.pendingFriends = pendingFriends;
    }

    /**
     * @return the invitedFriends
     */
    @OneToMany(mappedBy="id.inviter",fetch = FetchType.LAZY)
    public List<Invitation> getInvitedFriends() {
        return invitedFriends;
    }

    /**
     * @param invitedFriends the invitedFriends to set
     */
    public void setInvitedFriends(List<Invitation> invitedFriends) {
        this.invitedFriends = invitedFriends;
    }

    /**
     * @return the friends
     */
    @OneToMany(mappedBy="id.friend1",fetch = FetchType.LAZY)
    public List<Friendship> getFriends() {
        return friends;
    }

    /**
     * @param friends the friends to set
     */
    public void setFriends(List<Friendship> friends) {
        this.friends = friends;
    }

    /**
     * @return the friendOf
     */
    @OneToMany(mappedBy="id.friend2", fetch = FetchType.LAZY)
    public List<Friendship> getFriendOf() {
        return friendOf;
    }

    /**
     * @param friendOf the friendOf to set
     */
    public void setFriendOf(List<Friendship> friendOf) {
        this.friendOf = friendOf;
    }

    @Transient
    public int getFriendCount() {
        return this.friends.size()+this.friendOf.size();
    }

    @Transient
    public int getCreatedResourceCount() {
        return this.createdResources.size();
    }

    @Transient
    public int getWallResourceCount() {
        return this.wallResources.size();
    }

    @Transient
    public int getTotalResourceCount() {
        return getCreatedResourceCount()
                + getWallResourceCount();
    }

    @Transient
    public int getPendingFriendCount() {
        return this.pendingFriends.size();
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "creator")
	public List<Manipulation> getCreatedManipulations() {
		return createdManipulations;
	}

	public void setCreatedManipulations(List<Manipulation> createdManipulations) {
		this.createdManipulations = createdManipulations;
	}

}
