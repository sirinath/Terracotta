package org.terracotta.reference.exam.domain;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

public class HibernateUtil {

  private static SessionFactory sessionFactory;
  
  static { 
    try { 
      sessionFactory = new AnnotationConfiguration().configure().buildSessionFactory();
    } catch(Throwable t) {
      throw new ExceptionInInitializerError(t);
    }
  }
  
  public static SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public static void shutdown() {
    getSessionFactory().close();
  }

}
