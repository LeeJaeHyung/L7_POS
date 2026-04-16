package com.l7pos.l7_pos.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("l7pos-unit");

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
}