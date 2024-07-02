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

package hibernate;

import edu.usc.bg.base.ByteIterator;

import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import edu.usc.bg.base.DB;
import edu.usc.bg.base.DBException;
import edu.usc.bg.base.ObjectByteIterator;
import hibernate.entities.Friendship;
import hibernate.entities.Invitation;
import hibernate.entities.InvitationId;
import hibernate.entities.Manipulation;
import hibernate.entities.Resource;
import hibernate.entities.User;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * This is the HQL-Hibernate BG Client. It does uses HQL in actions where possible.
 * It's implementation is similar to JDBC BGClient - Basic1R2TMySQLClient
 * 
 * Each action follows a similar format, they all begin with opening 
 * a new session using the session factory object and beginning a new transaction
 * After the processing of the action, they commit the transaction and close the 
 *  session.
 *  
 *  This client only overrides the actions that can benefit from the use of HQL.
 *  Actions such as IF, PCR can not benefit from HQL as they involved insert operation
 *  and HQL does not support insert into..values syntax of the insert command.
 *  
 * @author Ankit Mutha (mutha.ankit@gmail.com)
 */
public class HibernateBgClientWithHQL
        extends HibernateBgClient implements HibernateConstants {

    private static final boolean verbose = false;

    public HibernateBgClientWithHQL() {
    }

    /**
     * This simulates the action of one member viewing the profile of another member(or her own).
     * 
     * This implementation uses HQL query to fetch only the required attributes of the given member profile.
     * The major difference from OO-Hibernate is that this implementation does not fetch the thumbnail image
     * for the member wich is not required. Also, this implementation retrieves only required aggregates
     * in contrast to OO-Hibernate where the complete collections for the relationship needed to be retrieved.
     * Also, it can retrieve the friends of the given member in a single HQL query as opposed to retrieving 
     * two collections in OO-Hibernate.
     * 
     * @see hibernate.HibernateBgClient#viewProfile(int, int, java.util.HashMap, boolean, boolean)
     */
    @Override
    public int viewProfile(int requesterID, int profileOwnerID, HashMap<String, ByteIterator> result, boolean insertImage, boolean testMode) {

        Session session = null;
        Transaction tx = null;
        if (requesterID < 0 || profileOwnerID < 0) {
            return ERROR;
        }
        try {
            session = factory.openSession();
            tx = session.beginTransaction();

            Query query = session.createQuery("SELECT count(*) FROM Friendship WHERE (id.friend1.userid = :profileOwnerID OR id.friend2.userid = :profileOwnerID)");
            long friendCount=(Long) query.setParameter("profileOwnerID", profileOwnerID).iterate().next();
			result.put("friendcount", new ObjectByteIterator((friendCount+"").getBytes()));
			
			query = session.createQuery("SELECT count(*) FROM Resource WHERE wallUser.userid = :profileOwnerID");
            long resourceCount=(Long)query.setParameter("profileOwnerID", profileOwnerID).iterate().next();
			result.put("resourcecount", new ObjectByteIterator((resourceCount+"").getBytes()));
			long pendingCount=0;
            if (requesterID == profileOwnerID) {
            	query = session.createQuery("SELECT count(*) FROM Invitation WHERE id.invitee.userid = :profileOwnerID");
                pendingCount=(Long)query.setParameter("profileOwnerID", profileOwnerID).iterate().next();
    			result.put("pendingcount", new ObjectByteIterator((pendingCount+"").getBytes()));
            }

            if (verbose) {
                System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " viewProfile(profileOwnerID: %5d, friendcount: %5d, resourcecount: %5d, pendingcount: %5d, insertImage: %s)\n", profileOwnerID, friendCount, resourceCount, pendingCount, insertImage);
            }

            
            if(insertImage&&!FSimagePath.equals(""))
            	query=session.createQuery("SELECT userid, username, firstname, lastname, gender, dob, jdate, ldate, address, email, tel FROM User WHERE userid = :profileOwnerID");
            else
            	query=session.createQuery("SELECT userid, username, firstname, lastname, gender, dob, jdate, ldate, address, email, tel, pic FROM User WHERE userid = :profileOwnerID");
            query.setParameter("profileOwnerID", profileOwnerID);
            Object[] userFields=(Object[]) query.list().get(0);
            	
            result.put("userid", new ObjectByteIterator((userFields[0] + "").getBytes()));
            result.put("username", new ObjectByteIterator((userFields[1]+"").getBytes()));
            result.put("fname", new ObjectByteIterator((userFields[2]+"").getBytes()));
            result.put("lname", new ObjectByteIterator((userFields[3]+"").getBytes()));
            result.put("gender", new ObjectByteIterator((userFields[4]+"").getBytes()));
            result.put("dob", new ObjectByteIterator((userFields[5]+"").getBytes()));
            result.put("jdate", new ObjectByteIterator((userFields[6]+"").getBytes()));
            result.put("ldate", new ObjectByteIterator((userFields[7]+"").getBytes()));
            result.put("address", new ObjectByteIterator((userFields[8]+"").getBytes()));
            result.put("email", new ObjectByteIterator((userFields[9]+"").getBytes()));
            result.put("tel", new ObjectByteIterator((userFields[10]+"").getBytes()));

            if (insertImage && FSimagePath.equals("")) {
                if (testMode) {
                    FileOutputStream fos = new FileOutputStream(
                            profileOwnerID + "-proimage.bmp");
                    fos.write((byte[])userFields[11]);
                    fos.close();
                }
                result.put("pic", new ObjectByteIterator((byte[])userFields[11]));
            } else if (insertImage) {
                byte[] profileImage
                        = GetImageFromFS(userFields[0] + "", true);
                if (testMode) {
                    // dump to file
                    FileOutputStream fos = new FileOutputStream(
                            profileOwnerID + "-proimage.bmp");
                    fos.write(profileImage);
                    fos.close();
                }
                result.put("pic", new ObjectByteIterator(profileImage));
            }

            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();
                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            System.err.println("Error in processing view profile: " + e);
            e.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This simulates the action of a member looking at all his/her friends.
     * 
     * This implementation retrieves all the required attributes of all the friends of a given member in a single HQL query.
     *  
     * @see hibernate.HibernateBgClient#listFriends(int, int, java.util.Set, java.util.Vector, boolean, boolean)
     */
    @Override
    public int listFriends(int requesterID, int profileOwnerID, Set<String> fields, Vector<HashMap<String, ByteIterator>> result, boolean insertImage, boolean testMode
    ) {

        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " listFriends(requesterID: %5d, profileOwnerID: %5d)\n", requesterID, profileOwnerID);
        }

        Session session = null;
        Transaction tx = null;
        if (requesterID < 0 || profileOwnerID < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();

            Query query;
            if(insertImage&&!FSimagePath.equals(""))
            	query=session.createQuery("SELECT user.userid, user.username, user.firstname, user.lastname, user.gender, "
            			+ "user.dob, user.jdate, user.ldate, user.address, user.email, user.tel FROM User user, Friendship friends "
            			+ "WHERE (friends.id.friend1.userid = :profileOwnerID AND user.userid = friends.id.friend2.userid) or "
            			+ "(friends.id.friend2.userid = :profileOwnerID AND user.userid = friends.id.friend1.userid)");
            else
            	query=session.createQuery("SELECT user.userid, user.username, user.firstname, user.lastname, user.gender, "
            			+ "user.dob, user.jdate, user.ldate, user.address, user.email, user.tel, user.tpic FROM User user, Friendship friends "
            			+ "WHERE (friends.id.friend1.userid = :profileOwnerID AND user.userid = friends.id.friend2.userid) or "
            			+ "(friends.id.friend2.userid = :profileOwnerID AND user.userid = friends.id.friend1.userid)");
            
            query.setParameter("profileOwnerID", profileOwnerID);
            Iterator<Object[]> iterator=query.iterate();
            
            int cnt = 0;

            while (iterator.hasNext()) {
                HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
                Object[] friendFields = (Object[]) iterator.next();
                cnt++;
                if (fields != null) {
                    for (String field : fields) {
                        if (field.equalsIgnoreCase("userid")) {
                            values.put("userid", new ObjectByteIterator((friendFields[0] + "").getBytes()));
                        } else if (field.equalsIgnoreCase("username")) {
                            values.put(field, new ObjectByteIterator((friendFields[1]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("fname")) {
                            values.put(field, new ObjectByteIterator((friendFields[2]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("lname")) {
                            values.put(field, new ObjectByteIterator((friendFields[3]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("gender")) {
                            values.put(field, new ObjectByteIterator((friendFields[4]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("dob")) {
                            values.put(field, new ObjectByteIterator((friendFields[5]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("jdate")) {
                            values.put(field, new ObjectByteIterator((friendFields[6]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("ldate")) {
                            values.put(field, new ObjectByteIterator((friendFields[7]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("address")) {
                            values.put(field, new ObjectByteIterator((friendFields[8]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("email")) {
                            values.put(field, new ObjectByteIterator((friendFields[9]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("tel")) {
                            values.put(field, new ObjectByteIterator((friendFields[10]+"").getBytes()));
                        } else if (field.equalsIgnoreCase("tpic")) {
                        	byte[] thumbImage = null;
                            if (insertImage && !FSimagePath.equals("")) {
                                thumbImage = GetImageFromFS(friendFields[0] + "", false);
                            } else if (insertImage) {
                                thumbImage = (byte[])friendFields[11];
                            }

                            values.put("tpic", new ObjectByteIterator(thumbImage));

                            if (testMode) {
                                FileOutputStream fos = new FileOutputStream(
                                        profileOwnerID + "-" + cnt
                                        + "-thumbimage.bmp");
                                fos.write(thumbImage);
                                fos.close();
                            }
                        }
                    }
                } else {
                        values.put("userid", new ObjectByteIterator((friendFields[0] + "").getBytes()));
                        values.put("username", new ObjectByteIterator((friendFields[1]+"").getBytes()));
                        values.put("fname", new ObjectByteIterator((friendFields[2]+"").getBytes()));
                        values.put("lname", new ObjectByteIterator((friendFields[3]+"").getBytes()));
                        values.put("gender", new ObjectByteIterator((friendFields[4]+"").getBytes()));
                        values.put("dob", new ObjectByteIterator((friendFields[5]+"").getBytes()));
                        values.put("jdate", new ObjectByteIterator((friendFields[6]+"").getBytes()));
                        values.put("ldate", new ObjectByteIterator((friendFields[7]+"").getBytes()));
                        values.put("address", new ObjectByteIterator((friendFields[8]+"").getBytes()));
                        values.put("email", new ObjectByteIterator((friendFields[9]+"").getBytes()));
                        values.put("tel", new ObjectByteIterator((friendFields[10]+"").getBytes()));
                    byte[] thumbImage = null;
                    if (insertImage && !FSimagePath.equals("")) {
                        thumbImage = GetImageFromFS(friendFields[0] + "", false);
                        values.put("tpic", new ObjectByteIterator(thumbImage));
                        if (testMode) {
                            FileOutputStream fos = new FileOutputStream(
                                    profileOwnerID + "-" + cnt
                                    + "-thumbimage.bmp");
                            fos.write(thumbImage);
                            fos.close();
                        }
                    } else if (insertImage) {
                        thumbImage = (byte[])friendFields[11];
                        values.put("tpic", new ObjectByteIterator(thumbImage));
                        if (testMode) {
                            FileOutputStream fos = new FileOutputStream(
                                    profileOwnerID + "-" + cnt
                                    + "-thumbimage.bmp");
                            fos.write(thumbImage);
                            fos.close();
                        }
                    }
                }
                result.add(values);
            }

            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();
                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            System.err.println("Error in processing insert to Users: " + e);
            e.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This simulates the action of a member viewing all the members who have invited to be friends with him/her.
     * 
     * This implementation retrieves all the required attributes of all the members who have invited the given member in a single HQL query.
     * @see hibernate.HibernateBgClient#viewFriendReq(int, java.util.Vector, boolean, boolean)
     */
    @Override
    public int viewFriendReq(int profileOwnerID, Vector<HashMap<String, ByteIterator>> results, boolean insertImage, boolean testMode
    ) {
        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " viewFriendReq(profileOwnerID: %5d)\n", profileOwnerID);
        }
        Session session = null;
        Transaction tx = null;

        if (profileOwnerID < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            Query query;
            if(insertImage&&!FSimagePath.equals(""))
            	query=session.createQuery("SELECT user.userid, user.username, user.firstname, user.lastname, user.gender, "
            			+ "user.dob, user.jdate, user.ldate, user.address, user.email, user.tel FROM User user, Invitation invitation "
            			+ "WHERE invitation.id.invitee.userid = :profileOwnerID AND user.userid = invitation.id.inviter.userid")
            			.setParameter("profileOwnerID", profileOwnerID);
            else
            	query=session.createQuery("SELECT user.userid, user.username, user.firstname, user.lastname, user.gender, "
                			+ "user.dob, user.jdate, user.ldate, user.address, user.email, user.tel, user.tpic  FROM User user, Invitation invitation "
                			+ "WHERE invitation.id.invitee.userid = :profileOwnerID AND user.userid = invitation.id.inviter.userid")
                			.setParameter("profileOwnerID", profileOwnerID);
            Iterator<Object[]> iterator=query.iterate();
            int cnt = 0;

            while (iterator.hasNext()) {
                HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
                Object[] pendingFriendFields=iterator.next();
                cnt++;

                values.put("userid", new ObjectByteIterator((pendingFriendFields[0] + "").getBytes()));
                values.put("username", new ObjectByteIterator((pendingFriendFields[1]+"").getBytes()));
                values.put("fname", new ObjectByteIterator((pendingFriendFields[2]+"").getBytes()));
                values.put("lname", new ObjectByteIterator((pendingFriendFields[3]+"").getBytes()));
                values.put("gender", new ObjectByteIterator((pendingFriendFields[4]+"").getBytes()));
                values.put("dob", new ObjectByteIterator((pendingFriendFields[5]+"").getBytes()));
                values.put("jdate", new ObjectByteIterator((pendingFriendFields[6]+"").getBytes()));
                values.put("ldate", new ObjectByteIterator((pendingFriendFields[7]+"").getBytes()));
                values.put("address", new ObjectByteIterator((pendingFriendFields[8]+"").getBytes()));
                values.put("email", new ObjectByteIterator((pendingFriendFields[9]+"").getBytes()));
                values.put("tel", new ObjectByteIterator((pendingFriendFields[10]+"").getBytes()));

                byte[] thumbImage = null;
                if (insertImage && !FSimagePath.equals("")) {
                    thumbImage = GetImageFromFS(pendingFriendFields[0] + "", false);
                    values.put("tpic", new ObjectByteIterator(thumbImage)); 
                    if (testMode) {
                        FileOutputStream fos = new FileOutputStream(
                                profileOwnerID + "-" + cnt
                                + "-thumbimage.bmp");
                        fos.write(thumbImage);
                        fos.close();
                    }
                } else if (insertImage) {
                    thumbImage = (byte[])pendingFriendFields[11];
                    values.put("tpic", new ObjectByteIterator(thumbImage)); 
                    if (testMode) {
                        FileOutputStream fos = new FileOutputStream(
                                profileOwnerID + "-" + cnt
                                + "-thumbimage.bmp");
                        fos.write(thumbImage);
                        fos.close();
                    }
                }

                results.add(values);
            }

            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();
                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            ex.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This simulates the action of a member accepting the friendship invitation from another member
     * 
     * This implementation is similar to OO-Hibernate implementation except for the part where it needs to delete
     * the invitation entity instance, which this implementation can accomplish in a single HQL query as opposed to
     * session.get() and session.delete() of the OO-Hibernate(which results in two SQL queries).
     * 
     * @see hibernate.HibernateBgClient#acceptFriend(int, int)
     */
    @Override
    public int acceptFriend(int inviterID, int inviteeID
    ) {

        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " acceptFriend(inviterID: %5d, inviteeID: %5d)\n", inviterID, inviteeID);
        }

        Session session = null;
        Transaction tx = null;

        if (inviterID < 0 || inviteeID < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();

            User inviter = (User) session.get(User.class, inviterID);
            User invitee = (User) session.get(User.class, inviteeID);

            if (verbose) {
                System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " acceptFriend(inviterID: %5d, inviteeID: %5d). Before commit:\n", inviterID, inviteeID);
                printStatsForUser(inviter);
                printStatsForUser(invitee);
            }

            Friendship friendship = new Friendship(inviter, invitee);
            Query query=session.createQuery("DELETE from Invitation WHERE id.inviter.userid=:inviterID AND id.invitee.userid=:inviteeID")
            		.setParameter("inviterID", inviterID)
            		.setParameter("inviteeID", inviteeID);
            query.executeUpdate();
          
            session.save(friendship);

            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();

                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
            if (verbose) {
                System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " acceptFriend(inviterID: %5d, inviteeID: %5d). After commit:\n", inviterID, inviteeID);
                printStatsForUser(inviter);
                printStatsForUser(invitee);
            }
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            ex.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This simulates the action of a member reject the friendship invitation of another member
     * 
     * This implementation deletes the invitation entity instance in a single HQL query.
     * 
     * @see hibernate.HibernateBgClient#rejectFriend(int, int)
     */
    @Override
    public int rejectFriend(int inviterID, int inviteeID
    ) {
        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " rejectFriend(inviterID: %5d, inviteeID: %5d)\n", inviterID, inviteeID);
        }
        Session session = null;
        Transaction tx = null;

        if (inviterID < 0 || inviteeID < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();

            session.createQuery("DELETE FROM Invitation WHERE id.invitee.userid=:inviteeID AND id.inviter.userid=:inviterID")
            .setParameter("inviterID", inviterID)
            .setParameter("inviteeID", inviteeID)
            .executeUpdate();

            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();
                    isComplete = -1;
                } catch (HibernateException ex) {
                    isComplete--;
                    System.err.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
        } catch (HibernateException ex) {
            if (tx != null) {
                tx.rollback();
            }
            ex.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This simulates the action when a member un-friends another member.
     * 
     * This implementation deletes the target friendship entity instance in a single HQL query
     * using a the DELETE command and the OR connective.
     * 
     * @see hibernate.HibernateBgClient#thawFriendship(int, int)
     */
    @Override
    public int thawFriendship(int friendid1, int friendid2
    ) {
        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " thawFriendship(friendid1: %5d, friendid2: %5d)\n", friendid1, friendid2);
        }

        Session session = null;
        Transaction tx = null;

        if (friendid1 < 0 || friendid2 < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();

            session.createQuery("DELETE FROM Friendship WHERE "
            		+ "(id.friend1.userid=:friend1 AND id.friend2.userid=:friend2)"
            		+ " OR (id.friend1.userid=:friend2 AND id.friend2.userid=:friend1)")
            		.setParameter("friend1", friendid1)
            		.setParameter("friend2",friendid2)
            		.executeUpdate();
            
            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();
                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }

        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
            ex.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This simulates the action of a member viewing the top resources on his/her wall.
     * 
     * This implementation retrieves all wall resources and orders them by descending resource id in a single HQL query using the ORDER BY clause.
     * The result is limited to k elements by using setMaxResults()
     * @see hibernate.HibernateBgClient#viewTopKResources(int, int, int, java.util.Vector)
     */
    @Override
    public int viewTopKResources(int requesterID, int profileOwnerID, int k, Vector<HashMap<String, ByteIterator>> result
    ) {
        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " viewTopKResources(profileOwnerID: %5d, k: %5d)\n", profileOwnerID, k);
        }
        Session session = null;
        Transaction tx = null;

        if (requesterID < 0 || profileOwnerID < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();

            Iterator<Object[]> iterator=session.createQuery("SELECT rid, wallUser.userid,creator.userid,type,body,doc "
            		+ "FROM Resource WHERE id.wallUser.userid = :profileOwnerID ORDER BY rid DESC")
            		.setParameter("profileOwnerID", profileOwnerID)
            		.setMaxResults(k)
            		.iterate();

            while (iterator.hasNext()) {
                Object[] resourceFields = iterator.next();
                HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

                values.put("rid", new ObjectByteIterator((resourceFields[0] + "").getBytes()));
                values.put("walluserid", new ObjectByteIterator((resourceFields[1] + "").getBytes()));
                values.put("creatorid", new ObjectByteIterator((resourceFields[2] + "").getBytes()));
                values.put("type", new ObjectByteIterator((resourceFields[3] + "").getBytes()));
                values.put("body", new ObjectByteIterator((resourceFields[4] + "").getBytes()));
                values.put("doc", new ObjectByteIterator((resourceFields[5] + "").getBytes()));

                result.add(values);
            }

            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();
                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            ex.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This method retrieves all the resources created by a given user
     * 
     * This implementation retrieves all the resources created by a given user in a single HQL query.
     * 
     * @see hibernate.HibernateBgClient#getCreatedResources(int, java.util.Vector)
     */
    @Override
    public int getCreatedResources(int creatorID, Vector<HashMap<String, ByteIterator>> result
    ) {
        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " getCreatedResources(creatorID: %5d)\n", creatorID);
        }
        Session session = null;
        Transaction tx = null;

        if (creatorID < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();

            Iterator<Object[]> iterator=session.createQuery("SELECT rid, wallUser.userid,creator.userid,type,body,doc "
            		+ "FROM Resource WHERE id.creator.userid = :profileOwnerID ORDER BY rid DESC")
            		.setParameter("profileOwnerID", creatorID)
            		.iterate();

            while (iterator.hasNext()) {
                Object[] resourceFields = iterator.next();
                HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

                values.put("rid", new ObjectByteIterator((resourceFields[0] + "").getBytes()));
                values.put("walluserid", new ObjectByteIterator((resourceFields[1] + "").getBytes()));
                values.put("creatorid", new ObjectByteIterator((resourceFields[2] + "").getBytes()));
                values.put("type", new ObjectByteIterator((resourceFields[3] + "").getBytes()));
                values.put("body", new ObjectByteIterator((resourceFields[4] + "").getBytes()));
                values.put("doc", new ObjectByteIterator((resourceFields[5] + "").getBytes()));

                result.add(values);
            }

            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();
                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            ex.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This simulates the action of a member viewing the comments on a resource.
     * 
     * This implementation retrieves all required attributes of all the manipulation entity instances 
     * associated with the given resource in a single HQL query.
     *   
     * @see hibernate.HibernateBgClient#viewCommentOnResource(int, int, int, java.util.Vector)
     */
    @Override
    public int viewCommentOnResource(int requesterID, int profileOwnerID, int resourceID, Vector<HashMap<String, ByteIterator>> result
    ) {
        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " viewCommentOnResource(requesterID: %5d, profileOwnerID: %5d, resourceID: %5d)\n", requesterID, profileOwnerID, resourceID);
        }
        Session session = null;
        Transaction tx = null;

        if (requesterID < 0 || profileOwnerID < 0 || resourceID < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();
            
                Iterator<Object[]> iterator = session.createQuery("SELECT id.mid,creator.userid,id.resource.rid,modifier.userid,timestamp,type,content"
                		+ " FROM Manipulation WHERE id.resource.rid=:rid")
                		.setParameter("rid", resourceID)
                		.iterate();
                while (iterator.hasNext()) {
                    Object[] commentFields = iterator.next();
                    HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

                    values.put("mid", new ObjectByteIterator((commentFields[0] + "").getBytes()));
                    values.put("creatorid", new ObjectByteIterator((commentFields[1] + "").getBytes()));
                    values.put("rid", new ObjectByteIterator((commentFields[2] + "").getBytes()));
                    values.put("modifierid", new ObjectByteIterator((commentFields[3] + "").getBytes()));
                    values.put("timestamp", new ObjectByteIterator((commentFields[4] + "").getBytes()));
                    values.put("type", new ObjectByteIterator((commentFields[5] + "").getBytes()));
                    values.put("content", new ObjectByteIterator((commentFields[6] + "").getBytes()));

                    result.add(values);
                }

            int isComplete = 10;
            while (isComplete>0) {
                try {

                    tx.commit();
                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            ex.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

    /**
     * This simulates the action of a member deleting a posted comment on a resource.
     * 
     * This implementation deletes the given posted comment from a resource in a single HQL query
     * 
     * @see hibernate.HibernateBgClient#delCommentOnResource(int, int, int)
     */
   @Override
    public int delCommentOnResource(int resourceCreatorID, int resourceID, int manipulationID
    ) {
        if (verbose) {
            System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " delCommentOnResource(resourceCreatorID: %5d, resourceID: %5d, manipulationID: %5d)\n", resourceCreatorID, resourceID, manipulationID);
        }
        Session session = null;
        Transaction tx = null;

        if (manipulationID < 0 || resourceCreatorID < 0 || resourceID < 0) {
            return ERROR;
        }

        try {
            session = factory.openSession();
            tx = session.beginTransaction();

            session.createQuery("DELETE FROM Manipulation WHERE id.mid=:mid AND id.resource.rid=:rid")
            .setParameter("mid", manipulationID)
            .setParameter("rid",resourceID)
            .executeUpdate();

            int isComplete = 10;
            while (isComplete>0) {
                try {
                    tx.commit();
                    isComplete = -1;
                } catch (Exception ex) {
                    isComplete--;
                    System.out.printf(System.nanoTime() + " Thread: " + Thread.currentThread().getId() + " ");
                    ex.printStackTrace(System.out);
                }
            }
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            ex.printStackTrace(System.out);
            return ERROR;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return SUCCESS;
    }

}




