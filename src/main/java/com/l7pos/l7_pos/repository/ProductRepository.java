package com.l7pos.l7_pos.repository;

import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ProductRepository {

    private static final int MAX_RESULT_COUNT = 500;
    private static final int QUERY_TIMEOUT_MS = 3000;

    public List<Product> findAll() {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                            "SELECT p FROM Product p ORDER BY p.id DESC",
                            Product.class
                    )
                    .setMaxResults(MAX_RESULT_COUNT)
                    .getResultList();

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
            String code = normalizeProductCode(productCode);

            if (code.isEmpty()) {
                return null;
            }

            List<Product> result = em.createQuery(
                            "SELECT p FROM Product p WHERE UPPER(p.productCode) = :productCode",
                            Product.class
                    )
                    .setParameter("productCode", code)
                    .setHint("jakarta.persistence.query.timeout", QUERY_TIMEOUT_MS)
                    .setMaxResults(1)
                    .getResultList();

            return result.isEmpty() ? null : result.get(0);

        } finally {
            em.close();
        }
    }

    public List<Product> searchByProductCode(String keyword) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            String searchKeyword = normalizeProductCode(keyword);

            if (searchKeyword.isEmpty()) {
                return findAll();
            }

            return em.createQuery(
                            "SELECT p FROM Product p " +
                                    "WHERE UPPER(p.productCode) LIKE :keyword " +
                                    "ORDER BY p.id DESC",
                            Product.class
                    )
                    .setParameter("keyword", searchKeyword + "%")
                    .setHint("jakarta.persistence.query.timeout", QUERY_TIMEOUT_MS)
                    .setMaxResults(MAX_RESULT_COUNT)
                    .getResultList();

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
            normalizeProductBeforeSave(product);

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

            if (product == null) {
                throw new IllegalArgumentException("삭제할 상품이 존재하지 않습니다.");
            }

            em.remove(product);
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

    private String normalizeProductCode(String productCode) {
        return productCode == null ? "" : productCode.trim().toUpperCase();
    }

    private void normalizeProductBeforeSave(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("저장할 상품 정보가 없습니다.");
        }

        String productCode = normalizeProductCode(product.getProductCode());

        if (productCode.isEmpty()) {
            throw new IllegalArgumentException("상품코드가 없습니다.");
        }

        product.setProductCode(productCode);
    }
}