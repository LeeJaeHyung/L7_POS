package com.l7pos.l7_pos.repository;

import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ProductRepository {

    public List<Product> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT p FROM Product p ORDER BY p.id DESC",
                    Product.class
            ).getResultList();
        } finally {
            em.close();
        }
    }

    public Product findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.find(Product.class, id);
        } finally {
            em.close();
        }
    }

    public Product findByProductCode(String productCode) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Product> result = em.createQuery(
                            "SELECT p FROM Product p WHERE p.productCode = :productCode",
                            Product.class
                    )
                    .setParameter("productCode", productCode)
                    .getResultList();

            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    public boolean existsByProductCode(String productCode) {
        return findByProductCode(productCode) != null;
    }

    public void save(Product product) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(product);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void update(Long id, Integer price) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Product product = em.find(Product.class, id);
            if (product == null) {
                throw new IllegalArgumentException("수정할 상품이 존재하지 않습니다.");
            }

            product.setPrice(price);
            em.merge(product);

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void deleteById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Product product = em.find(Product.class, id);
            if (product != null) {
                em.remove(product);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}