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
import hibernate.entities.FriendshipId;
import hibernate.entities.Invitation;
import hibernate.entities.InvitationId;
import hibernate.entities.Manipulation;
import hibernate.entities.ManipulationId;
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
 * This is the OO-Hibernate BG Client. It does NOT use HQL.
 * It uses pure object oriented interface to implement the BG actions.
 * Each action follows a similar format, they all begin with opening 
 * a new session using the session factory object and beginning a new transaction
 * After the processing of the action, they commit the transaction and close the 
 *  session.
 *  This client runs slower than HQL-Hibernate (provided in this package) as it 
 *  issues more queries per BG action. The detail analysis is available in the
 *  paper published on the BG website.
 *  
 * @author Ankit Mutha (mutha.ankit@gmail.com)
 *
 */
public class HibernateBgClient extends DB implements HibernateConstants {

	private static final boolean verbose = false;

	private static final Semaphore imgctrl = new Semaphore(10, true);
	private static final Semaphore initCtrl = new Semaphore(1, true);
	private static final Semaphore cleanupCtrl = new Semaphore(1, true);

	private static boolean isInitDone = false;
	private static int numOfThreads = 0;

	protected String FSimagePath = "";
	private static Configuration config;
	protected static SessionFactory factory;

	public HibernateBgClient() {
	}

	/**
	 * Setup Hibernate configuration from the properties passed as command line argument.
	 * Create a session factory that will be used in each action to open a session.
	 * @return true if the configurations is successful, false otherwise.
	 * @throws edu.usc.bg.base.DBException
	 */
	@Override
	public boolean init() throws DBException {
		/*
		 * Set the required parameters for configuration using the command line 
		 * properties if available, or default otherwise.
		 */
		Properties props = getProperties();
		String urls = props.getProperty(HibernateConstants.CONNECTION_URL,
				DEFAULT_URL);
		String user = props.getProperty(HibernateConstants.CONNECTION_USER,
				DEFAULT_USER);
		String passwd = props.getProperty(HibernateConstants.CONNECTION_PASSWD,
				DEFAULT_PASSWD);
		String driver = props.getProperty(HibernateConstants.DRIVER_CLASS,
				DEFAULT_DRIVER);
		String sqlDialect = props.getProperty(HibernateConstants.SQL_DIALECT,
				DEFAULT_DIALECT);
		if (verbose) {
			System.out.println("init: url: " + urls);
			System.out.println("init: user:" + user);
			System.out.println("init: passwd:" + passwd);
			System.out.println("init: driver:" + driver);
			System.out.println("init: sqlDialect:" + sqlDialect);
		}

		FSimagePath = props.getProperty(HibernateConstants.FS_PATH, "");

		try {

			/*
			 * In multi-threaded setup (with threadcount>1)init will be called more than once,
			 * therefore this code is guarded.
			 */
			initCtrl.acquire();
			numOfThreads++;
			if (!isInitDone) {
				try {
					config = new Configuration();
					config = config.configure();
					config.setProperty(DRIVER_HCP, driver);
					config.setProperty(DIALECT_HCP, sqlDialect);
					config.setProperty(CONNECTION_URL_HCP, urls);
					config.setProperty(USER_HCP, user);
					config.setProperty(PASSWORD_HCP, passwd);
					factory = config.buildSessionFactory();
					isInitDone = true;
				} catch (HibernateException ex) {
					numOfThreads--;
					initCtrl.release();
					ex.printStackTrace();
					throw new Exception("Invalid Hibernate Configuration");
				}
			}
			initCtrl.release();
		} catch (Exception ex) {
			System.err.println("ERROR: Failed to create sessionFactory object."
					+ ex);
			ex.printStackTrace(System.out);
			return false;
		}
		return true;
	}

	@Override
	/**
	 * Closes the session factory created in the init method
	 * 
	 * @see edu.usc.bg.base.DB#cleanup(boolean)
	 */
	public void cleanup(boolean warmup) {
		try {
			/*
			 * Similar to init method, this code is guarded for
			 * multi-threaded setup
			 */
			cleanupCtrl.acquire();
			numOfThreads--;
			if (numOfThreads == 0) {
				boolean isDone = false;
				while (!isDone) {
					try {
						factory.close();
						isDone = true;
					} catch (HibernateException ex) {
					}
				}
			}
			cleanupCtrl.release();
		} catch (InterruptedException ex) {
			System.err
					.println("ERROR: Failed to cleanup sessionFactory object."
							+ ex);
			ex.printStackTrace(System.out);
		}
	}

	/**
	 * Saves a new member into the database with given primary identifier and other attributes
	 * @param entityPK primary identifier for the member
	 * @param values attributes of the member
	 * @param insertImage indicates if the image is to be stored
	 * @return 1 if the new member is saved successfully, false otherwise.
	 */
	public int insertUser(String entityPK,
			HashMap<String, ByteIterator> values, boolean insertImage) {
		Session session = null;
		Transaction tx = null;
		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			int userId = new Integer(entityPK);
			/*
			 * Create a new user entity instance using the given
			 * primary identifier and other attributes
			 */
			User user = new User(userId);
			for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("username")) {
					user.setUsername(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("pw")) {
					user.setPw(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("fname")) {
					user.setFirstname(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("lname")) {
					user.setLastname(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("gender")) {
					user.setGender(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("dob")) {
					user.setDob(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("jdate")) {
					user.setJdate(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("ldate")) {
					user.setLdate(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("address")) {
					user.setAddress(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("email")) {
					user.setEmail(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("tel")) {
					user.setTel(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("pic")) {
					/*
					 * If the insertImage is set as true, get the image from values
					 * and save it in the entity object if the fspath property is not 
					 * set, otherwise save the image on the given file system path.
					 */
					if (insertImage) {
						if (FSimagePath.equals("")) {
							byte[] pic = values.get("pic").toArray();
							user.setPic(Hibernate.getLobCreator(session).createBlob(pic));
						} else {
							byte[] profileImage = null;
							profileImage = ((ObjectByteIterator) values
									.get("pic")).toArray();
							StoreImageInFS(entityPK, profileImage, true);
						}
					}
				} else if (entry.getKey().equalsIgnoreCase("tpic")) {
					if (insertImage) {
						if (FSimagePath.equals("")) {
							byte[] tpic = values.get("tpic").toArray();
							user.setTpic(Hibernate.getLobCreator(session).createBlob(tpic));
						} else {
							byte[] thumbImage = ((ObjectByteIterator) values
									.get("tpic")).toArray();

							StoreImageInFS(entityPK, thumbImage, false);
						}
					}
				}
			}

			session.save(user);

			/*
			 * If the transaction commit fails for some reason, try to commit
			 * again for ten times before giving up.
			 */
			int isComplete = 10;
			while (isComplete>0) {
				try {
					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * Saves a new resource into the database with given primary identifier and attributes
	 * @param entityPK primary identifier of the resource
	 * @param values the attributes of the resource
	 * @return 1 if the new resource is saved successfully, 0 otherwise
	 */
	public int insertResource(String entityPK,
			HashMap<String, ByteIterator> values) {

		Session session = null;
		Transaction tx = null;
		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			/*
			 * In order to create a Resource entity instance,
			 * we need to fetch the required User entity instances
			 * first - the resource creator and the wall user.
			 */
			ByteIterator value= values.get("creatorid");
			int creatorid = new Integer(value.toString());
			User creator = (User) session.get(User.class, creatorid);
			if (creator == null) {
				return ERROR;
			}

			value = values.get("walluserid");
			int walluserid = new Integer(value.toString());
			User wallUser = (User) session.get(User.class, walluserid);
			if (wallUser == null) {
				return ERROR;
			}

			/*
			 * Create a new resource entity instance
			 * and populate its properties.
			 */
			int rid = new Integer(entityPK);
			Resource resource = new Resource(rid, creator, wallUser);

			for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("type")) {
					resource.setType(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("body")) {
					resource.setBody(entry.getValue().toString());
				} else if (entry.getKey().equalsIgnoreCase("doc")) {
					resource.setDoc(entry.getValue().toString());
				}
			}

			session.save(resource);

			int isComplete = 10;
			while (isComplete>0) {
				try {

					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This BG action is used in the BG load phase to insert either a Resource
	 * or a User into the database, this method calls the corresponding methods.
	 * 
	 * @see edu.usc.bg.base.DB#insertEntity(java.lang.String, java.lang.String, java.util.HashMap, boolean)
	 */
	@Override
	public int insertEntity(String entitySet, String entityPK,
			HashMap<String, ByteIterator> values, boolean insertImage) {
		if (verbose) {
			System.out
					.printf(System.nanoTime()
							+ " Thread: "
							+ Thread.currentThread().getId()
							+ " insertEntity(entitySet: %s, entityPK: %s, insertImage: %s)\n",
							entitySet, entityPK, insertImage);
		}
		if (entitySet == null || entityPK == null) {
			return ERROR;
		}
		if (entitySet.equalsIgnoreCase("users")) {
			return insertUser(entityPK, values, insertImage);
		} else if (entitySet.equalsIgnoreCase("resources")) {
			return insertResource(entityPK, values);
		} else {
			System.err.println("Incorrect entitySet value - " + entitySet);
			System.err.println("Excepted values - users or resources");
			return ERROR;
		}
	}

	/**
	 * This simulates the action of a member viewing her or someone else's 
	 * profile.
	 * The implementation retrieves the target member's entity instance along with
	 * the friends, friendsOf, pendingFriends and wallResource collections.
	 * 
	 * @see edu.usc.bg.base.DB#viewProfile(int, int, java.util.HashMap, boolean, boolean)
	 */
	@Override
	public int viewProfile(int requesterID, int profileOwnerID,
			HashMap<String, ByteIterator> result, boolean insertImage,
			boolean testMode) {

		Session session = null;
		Transaction tx = null;
		if (requesterID < 0 || profileOwnerID < 0) {
			return ERROR;
		}
		try {
			session = factory.openSession();
			boolean isTxnStarted = false;
			while (!isTxnStarted) {
				try {
					//tx = session.beginTransaction();
					isTxnStarted = true;
				} catch (HibernateException ex) {
					isTxnStarted = false;
					ex.printStackTrace();
				}
			}

			User user = (User) session.get(User.class, profileOwnerID);

			result.put(
					"friendcount",
					new ObjectByteIterator((user.getFriendCount() + "")
							.getBytes()));
			result.put(
					"resourcecount",
					new ObjectByteIterator((user.getWallResourceCount() + "")
							.getBytes()));
			if (requesterID == profileOwnerID) {
				result.put("pendingcount",
						new ObjectByteIterator(
								(user.getPendingFriendCount() + "").getBytes()));
			}

			if (verbose) {
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " viewProfile(profileOwnerID: %5d, friendcount: %5d, resourcecount: %5d, pendingcount: %5d, insertImage: %s)\n",
								profileOwnerID, user.getFriendCount(),
								user.getTotalResourceCount(),
								user.getPendingFriendCount(), insertImage);
			}

			result.put("userid",
					new ObjectByteIterator((user.getUserid() + "").getBytes()));
			result.put("username",
					new ObjectByteIterator((user.getUsername()).getBytes()));
			result.put("fname",
					new ObjectByteIterator((user.getFirstname()).getBytes()));
			result.put("lname",
					new ObjectByteIterator((user.getLastname()).getBytes()));
			result.put("gender",
					new ObjectByteIterator((user.getGender()).getBytes()));
			result.put("dob",
					new ObjectByteIterator((user.getDob()).getBytes()));
			result.put("jdate",
					new ObjectByteIterator((user.getJdate()).getBytes()));
			result.put("ldate",
					new ObjectByteIterator((user.getLdate()).getBytes()));
			result.put("address",
					new ObjectByteIterator((user.getAddress()).getBytes()));
			result.put("email",
					new ObjectByteIterator((user.getEmail()).getBytes()));
			result.put("tel",
					new ObjectByteIterator((user.getTel()).getBytes()));

			if (insertImage && FSimagePath.equals("")) {
				if (testMode) {
					FileOutputStream fos = new FileOutputStream(profileOwnerID
							+ "-proimage.bmp");
					fos.write(user.getPic().getBytes(1,(int)user.getPic().length()));
					fos.close();
				}
				result.put("pic", new ObjectByteIterator(user.getPic().getBytes(1,(int)user.getPic().length())));
			} else if (insertImage) {
				byte[] profileImage = GetImageFromFS(user.getUserid() + "",
						true);
				/*
				 * testMode is true in the BG command line tool.
				 * It save the retrieved image on the working directory of the 
				 * program. 
				 */
				if (testMode) {
					// dump to file
					FileOutputStream fos = new FileOutputStream(profileOwnerID
							+ "-proimage.bmp");
					fos.write(profileImage);
					fos.close();
				}
				result.put("pic", new ObjectByteIterator(profileImage));
			}

			int isComplete = 10;
			while (isComplete>0) {
				try {
					//tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This simulates the action of one member listing all his/her friends.
	 * 
	 * This implementation retrieves the members entity instance along with
	 * it's friends and friendsOf collections. Then it iterates over the 
	 * friendship collections and retrieves the friend member.
	 * 
	 * @see edu.usc.bg.base.DB#listFriends(int, int, java.util.Set, java.util.Vector, boolean, boolean)
	 */
	@Override
	public int listFriends(int requesterID, int profileOwnerID,
			Set<String> fields, Vector<HashMap<String, ByteIterator>> result,
			boolean insertImage, boolean testMode) {

		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " listFriends(requesterID: %5d, profileOwnerID: %5d)\n",
					requesterID, profileOwnerID);
		}

		Session session = null;
		Transaction tx = null;
		if (requesterID < 0 || profileOwnerID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			//tx = session.beginTransaction();

			User user = (User) session.get(User.class, profileOwnerID);
			List<Friendship> friends = new ArrayList<Friendship>(
					user.getFriends());

			/*
			 * The friendship relationship is modeled as one row in the database
			 * using two asymmetric relationships, friends and friendsOf.
			 * Together they make up all the friends of a given member entity instance.
			 */
			friends.addAll(user.getFriendOf());

			Iterator<Friendship> iterator = friends.iterator();
			int cnt = 0;

			while (iterator.hasNext()) {
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				Friendship f = iterator.next();
				User friend = user == f.getFriend1() ? f.getFriend2() : f
						.getFriend1();
				cnt++;
				if (fields != null) {
					for (String field : fields) {
						if (field.equalsIgnoreCase("userid")) {
							values.put(
									"userid",
									new ObjectByteIterator(
											(friend.getUserid() + "")
													.getBytes()));
						} else if (field.equalsIgnoreCase("username")) {
							values.put(field, new ObjectByteIterator(friend
									.getUsername().getBytes()));
						} else if (field.equalsIgnoreCase("fname")) {
							values.put(field, new ObjectByteIterator(friend
									.getFirstname().getBytes()));
						} else if (field.equalsIgnoreCase("lname")) {
							values.put(field, new ObjectByteIterator(friend
									.getLastname().getBytes()));
						} else if (field.equalsIgnoreCase("gender")) {
							values.put(field, new ObjectByteIterator(friend
									.getGender().getBytes()));
						} else if (field.equalsIgnoreCase("dob")) {
							values.put(field, new ObjectByteIterator(friend
									.getDob().getBytes()));
						} else if (field.equalsIgnoreCase("jdate")) {
							values.put(field, new ObjectByteIterator(friend
									.getJdate().getBytes()));
						} else if (field.equalsIgnoreCase("ldate")) {
							values.put(field, new ObjectByteIterator(friend
									.getLdate().getBytes()));
						} else if (field.equalsIgnoreCase("address")) {
							values.put(field, new ObjectByteIterator(friend
									.getAddress().getBytes()));
						} else if (field.equalsIgnoreCase("email")) {
							values.put(field, new ObjectByteIterator(friend
									.getEmail().getBytes()));
						} else if (field.equalsIgnoreCase("tel")) {
							values.put(field, new ObjectByteIterator(friend
									.getTel().getBytes()));
						} else if (field.equalsIgnoreCase("tpic")) {
							byte[] thumbImage = null;
							if (insertImage && !FSimagePath.equals("")) {
								thumbImage = GetImageFromFS(friend.getUserid()
										+ "", false);
							} else if (insertImage) {
								thumbImage = friend.getTpic().getBytes(1,(int)friend.getTpic().length());
							}

							values.put("tpic", new ObjectByteIterator(
									thumbImage));

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
					values.put(
							"userid",
							new ObjectByteIterator((friend.getUserid() + "")
									.getBytes()));
					values.put("username", new ObjectByteIterator(friend
							.getUsername().getBytes()));
					values.put("fname", new ObjectByteIterator(friend
							.getFirstname().getBytes()));
					values.put("lname", new ObjectByteIterator(friend
							.getLastname().getBytes()));
					values.put("gender", new ObjectByteIterator(friend
							.getGender().getBytes()));
					values.put("dob", new ObjectByteIterator(friend.getDob()
							.getBytes()));
					values.put("jdate", new ObjectByteIterator(friend
							.getJdate().getBytes()));
					values.put("ldate", new ObjectByteIterator(friend
							.getLdate().getBytes()));
					values.put("address", new ObjectByteIterator(friend
							.getAddress().getBytes()));
					values.put("email", new ObjectByteIterator(friend
							.getEmail().getBytes()));
					values.put("tel", new ObjectByteIterator(friend.getTel()
							.getBytes()));
					byte[] thumbImage = null;
					if (insertImage && !FSimagePath.equals("")) {
						thumbImage = GetImageFromFS(friend.getUserid() + "",
								false);
					} else if (insertImage) {
						thumbImage = friend.getTpic().getBytes(1,(int)friend.getTpic().length());
					}

					values.put("tpic", new ObjectByteIterator(thumbImage));

					if (testMode && thumbImage != null) {
						FileOutputStream fos = new FileOutputStream(
								profileOwnerID + "-" + cnt + "-thumbimage.bmp");
						fos.write(thumbImage);
						fos.close();
					}
				}
				result.add(values);
			}

			int isComplete = 10;
			while (isComplete>0) {
				try {

					//tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This simulates the action of one member viewing his/her own pending friendship
	 * invitations.
	 * 
	 *  This implementation retrieves the given member entity instance along with
	 *  the pendingFriends collection. It iterates through the collection
	 *  and retrieves the profiles of the pending friend members. 
	 * 
	 * @see edu.usc.bg.base.DB#viewFriendReq(int, java.util.Vector, boolean, boolean)
	 */
	@Override
	public int viewFriendReq(int profileOwnerID,
			Vector<HashMap<String, ByteIterator>> results, boolean insertImage,
			boolean testMode) {
		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " viewFriendReq(profileOwnerID: %5d)\n", profileOwnerID);
		}
		Session session = null;
		Transaction tx = null;

		if (profileOwnerID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			User user = (User) session.get(User.class, profileOwnerID);

			/*
			 * PendingFriends represents the asymmetric relation between
			 * two members A and B, where B invited A to be friends.
			 */
			List<Invitation> pendingFriends = user.getPendingFriends();
			Iterator<Invitation> iterator = pendingFriends.iterator();
			int cnt = 0;

			while (iterator.hasNext()) {
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				Invitation i = iterator.next();
				User friend = i.getInviter();
				cnt++;

				values.put("userid", new ObjectByteIterator(
						(friend.getUserid() + "").getBytes()));
				values.put("username", new ObjectByteIterator(friend
						.getUsername().getBytes()));
				values.put("fname", new ObjectByteIterator(friend
						.getFirstname().getBytes()));
				values.put("lname", new ObjectByteIterator(friend.getLastname()
						.getBytes()));
				values.put("gender", new ObjectByteIterator(friend.getGender()
						.getBytes()));
				values.put("dob", new ObjectByteIterator(friend.getDob()
						.getBytes()));
				values.put("jdate", new ObjectByteIterator(friend.getJdate()
						.getBytes()));
				values.put("ldate", new ObjectByteIterator(friend.getLdate()
						.getBytes()));
				values.put("address", new ObjectByteIterator(friend
						.getAddress().getBytes()));
				values.put("email", new ObjectByteIterator(friend.getEmail()
						.getBytes()));
				values.put("tel", new ObjectByteIterator(friend.getTel()
						.getBytes()));

				byte[] thumbImage = null;
				if (insertImage && !FSimagePath.equals("")) {
					thumbImage = GetImageFromFS(friend.getUserid() + "", false);
				} else if (insertImage) {
					thumbImage = friend.getTpic().getBytes(1,(int)friend.getTpic().length());
				}

				values.put("tpic", new ObjectByteIterator(thumbImage));

				if (testMode) {
					FileOutputStream fos = new FileOutputStream(profileOwnerID
							+ "-" + cnt + "-thumbimage.bmp");
					fos.write(thumbImage);
					fos.close();
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
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This simulates the action of a member accepting the friendship invitation
	 * of another member.
	 * 
	 * The implementation requires retrieving of the two member's entity instances
	 * It also requires to retrieve the instance of the invitation instance
	 * that was extended from the inviter member.
	 * The invitation instance is deleted and a new friendship instance is created
	 * with appropriate values and it is persisted in the database.
	 * 
	 * @see edu.usc.bg.base.DB#acceptFriend(int, int)
	 */
	@Override
	public int acceptFriend(int inviterID, int inviteeID) {

		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " acceptFriend(inviterID: %5d, inviteeID: %5d)\n",
					inviterID, inviteeID);
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
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " acceptFriend(inviterID: %5d, inviteeID: %5d). Before commit:\n",
								inviterID, inviteeID);
				printStatsForUser(inviter);
				printStatsForUser(invitee);
			}

			Friendship friendship = new Friendship(inviter, invitee);
			Invitation invitation = (Invitation) session.get(Invitation.class,
					new InvitationId(inviter, invitee));
			if (invitation == null) {
				System.err
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " ERROR: acceptFriend(inviterID: %5d, inviteeID: %5d). No invitations found.\n",
								inviterID, inviteeID);
				return ERROR;
			}
			session.delete(invitation);
			session.save(friendship);

			int isComplete = 10;
			while (isComplete>0) {
				try {
					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
					ex.printStackTrace(System.out);
				}
			}
			if (verbose) {
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " acceptFriend(inviterID: %5d, inviteeID: %5d). After commit:\n",
								inviterID, inviteeID);
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
	 * This simulates the action of a member reject friendship invitation of 
	 * another member.
	 * This implementation retrieves the inviter and the invitee member entity instances
	 * It then retrieves the invitation entity instance and deletes it.
	 * @see edu.usc.bg.base.DB#rejectFriend(int, int)
	 */
	@Override
	public int rejectFriend(int inviterID, int inviteeID) {
		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " rejectFriend(inviterID: %5d, inviteeID: %5d)\n",
					inviterID, inviteeID);
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
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " rejectFriend(inviterID: %5d, inviteeID: %5d). Before commit:\n",
								inviterID, inviteeID);
				printStatsForUser(inviter);
				printStatsForUser(invitee);
			}

			Invitation invitation = (Invitation) session.get(Invitation.class,
					new InvitationId(inviter, invitee));
			session.delete(invitation);

			int isComplete = 10;
			while (isComplete>0) {
				try {
					tx.commit();
					isComplete = -1;
				} catch (HibernateException ex) {
					isComplete--;
					System.err.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
					ex.printStackTrace(System.out);
				}
			}
			if (verbose) {
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " rejectFriend(inviterID: %5d, inviteeID: %5d). After commit: \n",
								inviterID, inviteeID);
				printStatsForUser(inviter);
				printStatsForUser(invitee);
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
	 * This simulates the action of one member inviting other member to be friends.
	 * 
	 * This implementation requires retrieving the member entity instances of the inviter
	 * and the invitee. It then creates a new invitation entity instance and persists it.
	 * 
	 * @see edu.usc.bg.base.DB#inviteFriend(int, int)
	 */
	@Override
	public int inviteFriend(int inviterID, int inviteeID) {

		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " inviteFriend(inviterID: %5d, inviteeID: %5d)\n",
					inviterID, inviteeID);
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
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " inviteFriend(inviterID: %5d, inviteeID: %5d). Before commit:\n",
								inviterID, inviteeID);
				printStatsForUser(inviter);
				printStatsForUser(invitee);
			}

			/*
			 * Additional checks can be added to first do a session.get()
			 * on the invitation object which will retrieve the object if it exists
			 * This will require one additional SQL query though.
			 * 
			 * The current implementation can throw an error if the invitation object
			 * already exists in the database.
			 */
			Invitation invitation = new Invitation(inviter, invitee);
			session.save(invitation);

			int isComplete = 10;
			while (isComplete>0) {
				try {

					tx.commit();
					isComplete = -1;
				} catch (HibernateException ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
					ex.printStackTrace(System.out);
				}
			}
			if (verbose) {
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " inviteFriend(inviterID: %5d, inviteeID: %5d). After commit: \n",
								inviterID, inviteeID);
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
	 * This simulates the action when a member un-friends another member.
	 * 
	 * This implementation requires to retrieve the two member's entity instances
	 * along with the friendship entity instances that involves them. It then
	 * deletes the friendship entity instances
	 * 
	 * @see edu.usc.bg.base.DB#thawFriendship(int, int)
	 */
	@Override
	public int thawFriendship(int friendid1, int friendid2) {
		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " thawFriendship(friendid1: %5d, friendid2: %5d)\n",
					friendid1, friendid2);
		}

		Session session = null;
		Transaction tx = null;

		if (friendid1 < 0 || friendid2 < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			User friend1 = (User) session.get(User.class, friendid1);
			User friend2 = (User) session.get(User.class, friendid2);

			if (verbose) {
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " thawFriendship(friendid1: %5d, friendid2: %5d). Before commit:\n",
								friendid1, friendid2);
				printStatsForUser(friend1);
				printStatsForUser(friend2);
			}

			/*
			 * Since the symmetric relationship of friendship is represented as two
			 * asymmetric relations friends and friendsOf, we need to check both
			 * the relations to find the required friendship entity instance. 
			 */
			Friendship friendship = (Friendship) session.get(Friendship.class,
					new FriendshipId(friend1, friend2));
			if (friendship == null) {
				friendship = (Friendship) session.get(Friendship.class,
						new FriendshipId(friend2, friend1));
			}
			if (friendship == null) {
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ "ERROR: thawFriendship(friendid1: %5d, friendid2: %5d). No friendship exists.\n",
								friendid1, friendid2);
				return ERROR;
			}
			session.delete(friendship);

			int isComplete = 10;
			while (isComplete>0) {
				try {
					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
					ex.printStackTrace(System.out);
				}
			}
			if (verbose) {
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " thawFriendship(friendid1: %5d, friendid2: %5d). After commit:\n",
								friendid1, friendid2);
				printStatsForUser(friend1);
				printStatsForUser(friend2);
			}

		} catch (Exception ex) {
			if (tx != null) {
				tx.rollback();
			}
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId() + " ");
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
	 * This method is used in the load phase to create friendship between two
	 * members.
	 * 
	 * This implementation retrieves the entity instances of the two members
	 * and creates a new friendship entity instances and persists it.
	 * 
	 * @see edu.usc.bg.base.DB#CreateFriendship(int, int)
	 */
	@Override
	public int CreateFriendship(int friendid1, int friendid2) {
		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " CreateFriendship(friendid1: %5d, friendid2: %5d)\n",
					friendid1, friendid2);
		}
		Session session = null;
		Transaction tx = null;

		if (friendid1 < 0 || friendid2 < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			User friend1 = (User) session.get(User.class, friendid1);
			User friend2 = (User) session.get(User.class, friendid2);

			Friendship friendship = new Friendship(friend1, friend2);

			session.save(friendship);

			int isComplete = 10;
			while (isComplete>0) {
				try {
					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This simulates the action of one member viewing the top resources of another member's
	 * wall.
	 * 
	 * This implementation requires retrieving the member entity instance of the member whose wall is being viewed.
	 * It then retrieves the wallResources collection of this instance that holds all the resources on the wall of the member.
	 * It sorts the collection based on the resource id and then iterates over the top k elements.
	 * 
	 * @see edu.usc.bg.base.DB#viewTopKResources(int, int, int, java.util.Vector)
	 */
	@Override
	public int viewTopKResources(int requesterID, int profileOwnerID, int k,
			Vector<HashMap<String, ByteIterator>> result) {
		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " viewTopKResources(profileOwnerID: %5d, k: %5d)\n",
					profileOwnerID, k);
		}
		Session session = null;
		Transaction tx = null;

		if (requesterID < 0 || profileOwnerID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			User user = (User) session.get(User.class, profileOwnerID);
			List<Resource> resourceList = new ArrayList<Resource>(
					user.getWallResources());

			if (resourceList.size() < k) {
				k = resourceList.size();
			}

			Collections.sort(resourceList, new Comparator<Resource>() {

				public int compare(Resource a, Resource b) {
					return Integer.signum(a.getRid() - b.getRid());
				}
			});
			Iterator<Resource> iterator = resourceList.subList(0, k).iterator();

			while (iterator.hasNext()) {
				Resource resource = iterator.next();
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

				values.put("rid", new ObjectByteIterator(
						(resource.getRid() + "").getBytes()));
				values.put("walluserid", new ObjectByteIterator((resource
						.getWallUser().getUserid() + "").getBytes()));
				values.put("creatorid", new ObjectByteIterator((resource
						.getCreator().getUserid() + "").getBytes()));
				values.put("type", new ObjectByteIterator(resource.getType()
						.getBytes()));
				values.put("body", new ObjectByteIterator(resource.getBody()
						.getBytes()));
				values.put("doc", new ObjectByteIterator(resource.getDoc()
						.getBytes()));

				result.add(values);
			}

			int isComplete = 10;
			while (isComplete>0) {
				try {

					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This method retrieves the resources created by a given member.
	 * This implementation requires retrieving the given member entity instance
	 * and the createdResources collection which holds all the resource entity instances created by
	 * the given member.
	 * 
	 * @see edu.usc.bg.base.DB#getCreatedResources(int, java.util.Vector)
	 */
	@Override
	public int getCreatedResources(int creatorID,
			Vector<HashMap<String, ByteIterator>> result) {
		if (verbose) {
			System.out.printf(System.nanoTime() + " Thread: "
					+ Thread.currentThread().getId()
					+ " getCreatedResources(creatorID: %5d)\n", creatorID);
		}
		Session session = null;
		Transaction tx = null;

		if (creatorID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			User user = (User) session.get(User.class, creatorID);
			List<Resource> createdResources = user.getCreatedResources();

			Iterator<Resource> iterator = createdResources.iterator();

			while (iterator.hasNext()) {
				Resource resource = iterator.next();
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

				values.put("rid", new ObjectByteIterator(
						(resource.getRid() + "").getBytes()));
				values.put("walluserid", new ObjectByteIterator((resource
						.getWallUser().getUserid() + "").getBytes()));
				values.put("creatorid", new ObjectByteIterator((resource
						.getCreator().getUserid() + "").getBytes()));
				values.put("type", new ObjectByteIterator(resource.getType()
						.getBytes()));
				values.put("body", new ObjectByteIterator(resource.getBody()
						.getBytes()));
				values.put("doc", new ObjectByteIterator(resource.getDoc()
						.getBytes()));

				result.add(values);
			}

			int isComplete = 10;
			while (isComplete>0) {
				try {
					tx.commit();
					isComplete =-1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This simulates the action of the a member viewing the comments on a given
	 * resource.
	 * 
	 * This requires retrieving the given resource entity instance and it's manipulation
	 * collection that holds the Manipulation entity instances that represent the comments 
	 * on the given resource.
	 * 
	 * @see edu.usc.bg.base.DB#viewCommentOnResource(int, int, int, java.util.Vector)
	 */
	@Override
	public int viewCommentOnResource(int requesterID, int profileOwnerID,
			int resourceID, Vector<HashMap<String, ByteIterator>> result) {
		if (verbose) {
			System.out
					.printf(System.nanoTime()
							+ " Thread: "
							+ Thread.currentThread().getId()
							+ " viewCommentOnResource(requesterID: %5d, profileOwnerID: %5d, resourceID: %5d)\n",
							requesterID, profileOwnerID, resourceID);
		}
		Session session = null;
		Transaction tx = null;

		if (requesterID < 0 || profileOwnerID < 0 || resourceID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			Resource resource = (Resource) session.get(Resource.class,
					resourceID);
			List<Manipulation> comments = resource.getManipulations();

			Iterator<Manipulation> iterator = comments.listIterator();
			while (iterator.hasNext()) {
				Manipulation comment = iterator.next();
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

				values.put("mid", new ObjectByteIterator(
						(comment.getMid() + "").getBytes()));
				values.put("creatorid",
						new ObjectByteIterator((comment.getResource()
								.getCreator().getUserid() + "").getBytes()));
				values.put("rid", new ObjectByteIterator((comment.getResource()
						.getRid() + "").getBytes()));
				values.put("modifierid", new ObjectByteIterator((comment
						.getModifier().getUserid() + "").getBytes()));
				values.put("timestamp", new ObjectByteIterator(comment
						.getTimestamp().getBytes()));
				values.put("type", new ObjectByteIterator(comment.getType()
						.getBytes()));
				values.put("content", new ObjectByteIterator(comment
						.getContent().getBytes()));

				result.add(values);
			}

			int isComplete = 10;
			while (isComplete>0) {
				try {

					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This simulates the action of member posting a comment on a given resource.
	 * 
	 * This implementation retrieves the given resource entity instance and creates 
	 * a new manipulation entity instance and persists it.
	 * @see edu.usc.bg.base.DB#postCommentOnResource(int, int, int, java.util.HashMap)
	 */
	@Override
	public int postCommentOnResource(int commentCreatorID,
			int resourceCreatorID, int resourceID,
			HashMap<String, ByteIterator> values) {
		if (verbose) {
			System.out
					.printf(System.nanoTime()
							+ " Thread: "
							+ Thread.currentThread().getId()
							+ " postCommentOnResource(commentCreatorID: %5d, resourceCreatorID: %5d, resourceID: %5d)\n",
							commentCreatorID, resourceCreatorID, resourceID);
		}
		Session session = null;
		Transaction tx = null;

		if (commentCreatorID < 0 || resourceCreatorID < 0 || resourceID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			Resource resource = (Resource) session.get(Resource.class,
					resourceID);

			int mid = Integer.parseInt(values.get("mid").toString());
			Manipulation comment = new Manipulation(mid, resource);

			if (verbose) {
				System.out
						.printf(System.nanoTime()
								+ " Thread: "
								+ Thread.currentThread().getId()
								+ " postCommentOnResource(commentCreatorID: %5d, resourceCreatorID: %5d, resourceID: %5d, mid: %5d)\n",
								commentCreatorID, resourceCreatorID,
								resourceID, mid);
			}

			comment.setContent(values.get("content").toString());
			comment.setTimestamp(values.get("timestamp").toString());
			comment.setType(values.get("type").toString());
			session.save(comment);

			int isComplete = 10;
			while (isComplete>0) {
				try {

					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This simulates the action of one member deleting a posted comment on a resource.
	 * 
	 * This implementation retrieves the given resource entity instance and the manipulation entity 
	 * instance and deletes the manipulation instance.
	 *  
	 * @see edu.usc.bg.base.DB#delCommentOnResource(int, int, int)
	 */
	@Override
	public int delCommentOnResource(int resourceCreatorID, int resourceID,
			int manipulationID) {
		if (verbose) {
			System.out
					.printf(System.nanoTime()
							+ " Thread: "
							+ Thread.currentThread().getId()
							+ " delCommentOnResource(resourceCreatorID: %5d, resourceID: %5d, manipulationID: %5d)\n",
							resourceCreatorID, resourceID, manipulationID);
		}
		Session session = null;
		Transaction tx = null;

		if (manipulationID < 0 || resourceCreatorID < 0 || resourceID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			Resource resource = (Resource) session.get(Resource.class,
					resourceID);
			Manipulation comment = (Manipulation) session.get(
					Manipulation.class, new ManipulationId(manipulationID,
							resource));
			session.delete(comment);

			int isComplete = 10;
			while (isComplete>0) {
				try {
					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * @see edu.usc.bg.base.DB#getInitialStats()
	 */
	@Override
	public HashMap<String, String> getInitialStats() {
		Session session = null;
		Transaction tx = null;
		HashMap<String, String> stats = new HashMap<String, String>();

		try {
			session = factory.openSession();
			session.setFlushMode(FlushMode.ALWAYS);
			tx = session.beginTransaction();

			// User count
			Long userCount = (Long) session
					.createQuery("SELECT COUNT(*) FROM User").list().get(0);
			stats.put("usercount", "" + userCount);

			// User offset
			int offset = (Integer) session
					.createQuery("SELECT MIN(userid) FROM User").list().get(0);

			// Resources per user
			User user = (User) session.get(User.class, offset);

			int resourcePerUser = user.getCreatedResourceCount();

			stats.put("resourcesperuser", "" + resourcePerUser);

			// Number of friends per User
			int friendsPerUser = user.getFriendCount();

			stats.put("avgfriendsperuser", "" + friendsPerUser);

			// Number of pending friends per User
			int pendingFriendsPerUser = user.getPendingFriendCount();

			stats.put("avgpendingperuser", "" + pendingFriendsPerUser);

			int isComplete = 10;
			while (isComplete>0) {
				try {

					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
					ex.printStackTrace(System.out);
				}
			}
		} catch (Exception ex) {
			if (tx != null) {
				tx.rollback();
			}
			ex.printStackTrace(System.out);
			return null;
		} finally {
			if (session != null) {
				session.close();
			}
		}

		return stats;
	}

	/**
	 * This method creates the schema using schema export.
	 * If the database does not exists or the client can not 
	 * connect to the database, appropriate exception is thrown.
	 * 
	 * @see edu.usc.bg.base.DB#createSchema(java.util.Properties)
	 */
	@Override
	public void createSchema(Properties props) {
		Session session = null;
		Transaction tx = null;

		try {
			session = factory.openSession();
			tx = session.beginTransaction();
			new SchemaExport(config).create(true, true);
			tx.commit();
		}catch (Exception ex) {
			if (tx != null) {
				tx.rollback();
			}
			System.err.println("Can not connect to specified database. Check if the database exists and the connection parameters are correct.");
		} finally {
			if (session != null) {
				session.close();
			}
		}
	}

	/**
	 * @see edu.usc.bg.base.DB#queryPendingFriendshipIds(int, java.util.Vector)
	 */
	@Override
	public int queryPendingFriendshipIds(int memberID,
			Vector<Integer> pendingIds) {
		Session session = null;
		Transaction tx = null;

		if (memberID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			User user = (User) session.get(User.class, memberID);

			Iterator<Invitation> iterator = user.getPendingFriends().iterator();

			while (iterator.hasNext()) {
				Invitation i = iterator.next();
				User friend = i.getInviter();
				pendingIds.add(friend.getUserid());
			}

			int isComplete = 10;
			while (isComplete>0) {
				try {

					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * @see edu.usc.bg.base.DB#queryConfirmedFriendshipIds(int, java.util.Vector)
	 */
	@Override
	public int queryConfirmedFriendshipIds(int memberID,
			Vector<Integer> confirmedIds) {
		Session session = null;
		Transaction tx = null;

		if (memberID < 0) {
			return ERROR;
		}

		try {
			session = factory.openSession();
			tx = session.beginTransaction();

			User user = (User) session.get(User.class, memberID);

			List<Friendship> friends = new ArrayList<Friendship>(
					user.getFriends());

			friends.addAll(user.getFriendOf());
			Iterator<Friendship> iterator = user.getFriends().iterator();

			while (iterator.hasNext()) {
				Friendship f = iterator.next();
				User friend = user == f.getFriend1() ? f.getFriend2() : f
						.getFriend1();
				confirmedIds.add(friend.getUserid());
			}

			int isComplete = 10;
			while (isComplete>0) {
				try {

					tx.commit();
					isComplete = -1;
				} catch (Exception ex) {
					isComplete--;
					System.out.printf(System.nanoTime() + " Thread: "
							+ Thread.currentThread().getId() + " ");
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
	 * This method stores a given image byte array onto the given file system path
	 * @param userid used to name the saved image
	 * @param image the image byte array
	 * @param profileimg true indicates if the image is a profile image else thumbnail image.
	 * @return true if the save operation is completed successfully, false otherwise.
	 */
	public boolean StoreImageInFS(String userid, byte[] image,
			boolean profileimg) {
		boolean result = true;
		String ext = "thumbnail";

		if (profileimg) {
			ext = "profile";
		}

		String ImageFileName = FSimagePath + "\\img" + userid + ext;

		File tgt = new File(ImageFileName);
		if (tgt.exists()) {
			if (!tgt.delete()) {
				System.out.println("Error, file exists and failed to delete");
				return false;
			}
		}

		// Write the file
		try {
			imgctrl.acquire();
			FileOutputStream fos = new FileOutputStream(ImageFileName);
			fos.write(image);
			fos.close();
		} catch (Exception ex) {
			System.out.println("Error in writing the file" + ImageFileName);
			ex.printStackTrace(System.out);
			imgctrl.release();
		} finally {
			imgctrl.release();
		}
		return result;
	}

	/**
	 * This method retrieves the image stored for the given userid.
	 * 
	 * @param userid identifies the name of the image to be retrieved
	 * @param profileimg true indcates if the it is a profile image, else it is a thumbnail image.
	 * @return the retrieved image byte array
	 */
	public byte[] GetImageFromFS(String userid, boolean profileimg) {
		int filelength = 0;
		String ext = "thumbnail";
		byte[] imgpayload = null;

		if (profileimg) {
			ext = "profile";
		}

		String ImageFileName = FSimagePath + "\\img" + userid + ext;
		int attempt = 100;
		while (attempt > 0) {
			try {
				imgctrl.acquire();
				FileInputStream fis = null;
				DataInputStream dis = null;
				File fsimage = new File(ImageFileName);
				filelength = (int) fsimage.length();
				imgpayload = new byte[filelength];
				fis = new FileInputStream(fsimage);
				dis = new DataInputStream(fis);
				int read = 0;
				int numRead = 0;
				while (read < filelength
						&& (numRead = dis.read(imgpayload, read, filelength
								- read)) >= 0) {
					read = read + numRead;
				}
				dis.close();
				fis.close();
				imgctrl.release();
				break;
			} catch (IOException e) {
				e.printStackTrace(System.out);
				attempt--;
				imgctrl.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
				imgctrl.release();
			} finally {
				imgctrl.release();
			}
		}
		return imgpayload;
	}

	/**
	 * A utility method to print the user information for debugging
	 * 
	 * @param user the user entity instance
	 */
	protected void printStatsForUser(User user) {
		System.out
				.printf(System.nanoTime()
						+ " Thread: "
						+ Thread.currentThread().getId()
						+ " userid: %5d, pendingFriends: %5d, invitedFriends: %5d, friendCount: %5d\n",
						user.getUserid(), user.getPendingFriendCount(), user
								.getInvitedFriends().size(), user
								.getFriendCount());
	}

	@Override
	public int getShortestPathLength(int requesterID, int profileID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int listCommonFriends(int requesterID, int profileID, int l,
			Set<String> fields, Vector<HashMap<String, ByteIterator>> result,
			boolean insertImage, boolean testMode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int listFriendsOfFriends(int requesterID, int profileID,
			Set<String> fields, Vector<HashMap<String, ByteIterator>> result,
			boolean insertImage, boolean testMode) {
		// TODO Auto-generated method stub
		return 0;
	}
}
