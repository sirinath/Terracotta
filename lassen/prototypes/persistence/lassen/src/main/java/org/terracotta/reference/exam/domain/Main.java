package org.terracotta.reference.exam.domain;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class Main {

  public static void main(String arg[]) {
    // Add a user
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction tx = session.beginTransaction();
    
    User user = new User();
    user.setFirstName("Alex");
    user.setLastName("Miller");
    user.setEmail("email@something");
    Long userId = (Long) session.save(user);
    
    tx.commit();
    session.close();
    
    // Get all users
    Session newSession = HibernateUtil.getSessionFactory().openSession();
    Transaction newTransaction = newSession.beginTransaction();
    
    List<User> users = newSession.createQuery("from User u order by u.lastName asc").list();
    System.out.println(users.size() + " users found:");
    for(User readUser : users) {
      System.out.println( readUser );
    }
    
    newTransaction.commit();
    newSession.close();
    
    // Shut down
    HibernateUtil.shutdown();
  }
}
