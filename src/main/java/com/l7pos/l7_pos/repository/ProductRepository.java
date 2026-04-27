package com.l7pos.l7_pos.repository;

import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Locale;

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

    /**
     * 상품 가격 수정
     *
     * 같이 수정되는 대상:
     * 1. product.price
     * 2. sale_item.price
     * 3. sale_item.amount
     * 4. sale.total_amount
     *
     * 주의:
     * 과거 판매내역 가격까지 변경되므로
     * 과거 매출 통계도 현재 상품 가격 기준으로 바뀐다.
     */
    public void update(Long id, Integer price) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            em.getTransaction().begin();

            Product product = em.find(Product.class, id);

            if (product == null) {
                throw new IllegalArgumentException("수정할 상품이 존재하지 않습니다.");
            }

            if (price == null || price <= 0) {
                throw new IllegalArgumentException("가격은 0보다 커야 합니다.");
            }

            String productCode = normalizeProductCode(product.getProductCode());

            if (productCode.isEmpty()) {
                throw new IllegalArgumentException("상품코드가 없습니다.");
            }

            // 1. 상품 가격 수정
            product.setPrice(price);

            /*
             * 2. 기존 판매 상세 가격 수정
             *
             * 현재 sale_item 구조는 수량 컬럼이 없고
             * 한 행 = 한 개 판매 구조이므로
             * price와 amount를 동일하게 변경한다.
             */
            em.createQuery(
                            "UPDATE SaleItem si " +
                                    "SET si.price = :price, " +
                                    "    si.amount = :price " +
                                    "WHERE UPPER(si.productCode) = :productCode"
                    )
                    .setParameter("price", price)
                    .setParameter("productCode", productCode)
                    .executeUpdate();

            /*
             * 3. 판매 마스터 총 금액 재계산
             *
             * sale.totalAmount = 해당 sale_no의 sale_item.amount 합계
             */
            em.createQuery(
                            "UPDATE Sale s " +
                                    "SET s.totalAmount = (" +
                                    "    SELECT COALESCE(SUM(si.amount), 0) " +
                                    "    FROM SaleItem si " +
                                    "    WHERE si.sale = s" +
                                    ") " +
                                    "WHERE EXISTS (" +
                                    "    SELECT 1 " +
                                    "    FROM SaleItem si2 " +
                                    "    WHERE si2.sale = s " +
                                    "      AND UPPER(si2.productCode) = :productCode" +
                                    ")"
                    )
                    .setParameter("productCode", productCode)
                    .executeUpdate();

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
        return productCode == null
                ? ""
                : productCode.trim().toUpperCase(Locale.ROOT);
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