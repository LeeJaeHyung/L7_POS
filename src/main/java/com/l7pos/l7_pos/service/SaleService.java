package com.l7pos.l7_pos.service;

import com.l7pos.l7_pos.dto.SaleRow;
import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.entity.Sale;
import com.l7pos.l7_pos.entity.SaleItem;
import com.l7pos.l7_pos.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SaleService {

    public Product findProductByCode(String productCode) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                            "SELECT p FROM Product p WHERE p.productCode = :productCode",
                            Product.class
                    )
                    .setParameter("productCode", productCode)
                    .getSingleResult();

        } catch (NoResultException e) {
            throw new IllegalArgumentException("등록되지 않은 상품입니다.\n품번: " + productCode);
        } finally {
            em.close();
        }
    }

    public void saveSale(String saleNo, List<SaleRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("등록할 판매 품목이 없습니다.");
        }

        int totalQuantity = rows.size();

        int totalAmount = rows.stream()
                .mapToInt(SaleRow::getAmount)
                .sum();

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Sale sale = new Sale(
                    saleNo,
                    LocalDateTime.now(),
                    totalQuantity,
                    totalAmount
            );

            for (SaleRow row : rows) {
                sale.addItem(toSaleItem(row));
            }

            em.persist(sale);
            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;

        } finally {
            em.close();
        }
    }

    public List<Sale> findAllSales() {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                    "SELECT s FROM Sale s ORDER BY s.saleDate DESC",
                    Sale.class
            ).getResultList();

        } finally {
            em.close();
        }
    }

    public List<Sale> findSalesByDateRange(LocalDateTime startDateTime,
                                           LocalDateTime endDateTime) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                            "SELECT s FROM Sale s " +
                                    "WHERE s.saleDate >= :startDateTime " +
                                    "AND s.saleDate < :endDateTime " +
                                    "ORDER BY s.saleDate DESC",
                            Sale.class
                    )
                    .setParameter("startDateTime", startDateTime)
                    .setParameter("endDateTime", endDateTime)
                    .getResultList();

        } finally {
            em.close();
        }
    }

    public Sale findSaleWithItems(String saleNo) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                            "SELECT DISTINCT s FROM Sale s " +
                                    "LEFT JOIN FETCH s.saleItems " +
                                    "WHERE s.saleNo = :saleNo",
                            Sale.class
                    )
                    .setParameter("saleNo", saleNo)
                    .getSingleResult();

        } catch (NoResultException e) {
            throw new IllegalArgumentException("판매 내역을 찾을 수 없습니다.\n판매번호: " + saleNo);

        } finally {
            em.close();
        }
    }

    public void deleteSale(String saleNo) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Sale sale = em.find(Sale.class, saleNo);

            if (sale == null) {
                throw new IllegalArgumentException("삭제할 판매 내역이 없습니다.");
            }

            em.remove(sale);
            tx.commit();

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;

        } finally {
            em.close();
        }
    }

    public void updateSale(String saleNo, List<SaleRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("수정 저장할 품목이 없습니다.");
        }

        int totalQuantity = rows.size();

        int totalAmount = rows.stream()
                .mapToInt(SaleRow::getAmount)
                .sum();

        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Sale sale = em.createQuery(
                            "SELECT DISTINCT s FROM Sale s " +
                                    "LEFT JOIN FETCH s.saleItems " +
                                    "WHERE s.saleNo = :saleNo",
                            Sale.class
                    )
                    .setParameter("saleNo", saleNo)
                    .getSingleResult();

            sale.clearItems();

            for (SaleRow row : rows) {
                sale.addItem(toSaleItem(row));
            }

            sale.setTotalQuantity(totalQuantity);
            sale.setTotalAmount(totalAmount);

            tx.commit();

        } catch (NoResultException e) {
            if (tx.isActive()) {
                tx.rollback();
            }

            throw new IllegalArgumentException("수정할 판매 내역이 없습니다.\n판매번호: " + saleNo);

        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }

            throw e;

        } finally {
            em.close();
        }
    }

    private SaleItem toSaleItem(SaleRow row) {
        return new SaleItem(
                row.getBarcode(),
                row.getProductCode(),
                row.getProductName(),
                row.getColor(),
                row.getSize(),
                row.getPrice()
        );
    }

    public String createSaleNo() {
        return "SALE" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    public List<String> findBarcodesByDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery("""
                SELECT si.barcode
                FROM SaleItem si
                JOIN si.sale s
                WHERE s.saleDate >= :startDateTime
                  AND s.saleDate < :endDateTime
                ORDER BY s.saleDate ASC, si.saleItemId ASC
                """, String.class)
                    .setParameter("startDateTime", startDateTime)
                    .setParameter("endDateTime", endDateTime)
                    .getResultList();

        } finally {
            em.close();
        }
    }
}