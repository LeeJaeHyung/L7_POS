package com.l7pos.l7_pos.service;

import com.l7pos.l7_pos.dto.ResolvedBarcode;
import com.l7pos.l7_pos.dto.SaleRow;
import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.entity.Sale;
import com.l7pos.l7_pos.entity.SaleItem;
import com.l7pos.l7_pos.util.BarcodeParser;
import com.l7pos.l7_pos.util.ParsedBarcode;
import com.l7pos.l7_pos.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SaleService {

    /**
     * 등록된 상품이면 돌려주고, 없으면 null.
     */
    public Product findProductByCodeOrNull(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            return null;
        }

        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                            "SELECT p FROM Product p WHERE UPPER(p.productCode) = :productCode",
                            Product.class
                    )
                    .setParameter("productCode", productCode.trim().toUpperCase(Locale.ROOT))
                    .setMaxResults(1)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

        } finally {
            em.close();
        }
    }

    /**
     * 바코드를 상품까지 확인해서 확정한다.
     *
     * 스캔 전에 키보드 위에 물건이 닿아 "." 이나 "ㅐ" 같은 글자가
     * 앞에 먼저 입력되는 경우가 있다.
     *
     * 1. 입력한 그대로 찾아본다.
     * 2. 품번이 없으면, 품번 접두사(CO)가 나오는 지점부터 잘라서 다시 찾는다.
     *    잡글자에도 CO 가 섞일 수 있으므로 마지막 CO 부터 거슬러 올라가며 시도한다.
     *
     * 등록된 상품을 찾은 후보만 인정하므로, 엉뚱하게 잘린 값이 통과하지 않는다.
     */
    public ResolvedBarcode resolveBarcode(String rawBarcode) {
        if (rawBarcode == null || rawBarcode.isBlank()) {
            throw new IllegalArgumentException("바코드를 입력하세요.");
        }

        String input = rawBarcode.trim();

        // 1. 입력한 그대로
        ResolvedBarcode direct = tryResolve(input, input, false);

        if (direct != null) {
            return direct;
        }

        // 2. 앞에 붙은 잘못된 글자를 떼어내고 재시도
        for (String candidate : BarcodeParser.recoverBarcodeCandidates(input)) {
            if (candidate.equalsIgnoreCase(input)) {
                continue;   // 1번에서 이미 해봤다
            }

            ResolvedBarcode recovered = tryResolve(candidate, input, true);

            if (recovered != null) {
                return recovered;
            }
        }

        throw new IllegalArgumentException("등록되지 않은 상품입니다.\n입력: " + input);
    }

    private ResolvedBarcode tryResolve(String barcode, String rawInput, boolean recovered) {
        ParsedBarcode parsed;

        try {
            parsed = BarcodeParser.parse(barcode);

        } catch (IllegalArgumentException e) {
            return null;   // 바코드 형식이 아니면 후보에서 제외
        }

        Product product = findProductByCodeOrNull(parsed.productCode());

        if (product == null) {
            return null;
        }

        return new ResolvedBarcode(parsed, product, rawInput, recovered);
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

    /**
     * 전체 판매 조회
     *
     * 목록에 바코드를 함께 보여주므로 상세 품목까지 같이 읽어온다.
     * (EntityManager 를 닫은 뒤에도 saleItems 에 접근할 수 있어야 한다)
     */
    public List<Sale> findAllSales() {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                    "SELECT DISTINCT s FROM Sale s " +
                            "LEFT JOIN FETCH s.saleItems " +
                            "WHERE s.deletedAt IS NULL " +
                            "ORDER BY s.saleDate DESC",
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
                            "SELECT DISTINCT s FROM Sale s " +
                                    "LEFT JOIN FETCH s.saleItems " +
                                    "WHERE s.deletedAt IS NULL " +
                                    "AND s.saleDate >= :startDateTime " +
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
                                    "WHERE s.saleNo = :saleNo " +
                                    "AND s.deletedAt IS NULL",
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

    /**
     * 판매 삭제 (소프트 딜리트)
     *
     * 행을 지우지 않고 삭제 시각만 남긴다.
     * 판매에 딸린 품목도 같은 시각으로 함께 표시해서
     * 나중에 바코드 단위로 무엇이 빠졌는지 볼 수 있게 한다.
     */
    public void deleteSale(String saleNo) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Sale sale = em.createQuery(
                            "SELECT DISTINCT s FROM Sale s " +
                                    "LEFT JOIN FETCH s.saleItems " +
                                    "WHERE s.saleNo = :saleNo " +
                                    "AND s.deletedAt IS NULL",
                            Sale.class
                    )
                    .setParameter("saleNo", saleNo)
                    .getResultStream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("삭제할 판매 내역이 없습니다."));

            sale.softDelete(LocalDateTime.now());

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

    /**
     * 판매 내역 수정 저장
     *
     * 화면에서 빠진 품목은 지우지 않고 삭제 표시만 한다.
     *
     * 같은 바코드가 여러 개 팔릴 수 있으므로
     * 바코드별 개수까지 맞춰서 비교한다.
     *   - 화면에 남아있는 만큼은 기존 품목을 그대로 둔다 (판매 시점 가격 보존)
     *   - 화면에서 빠진 나머지는 삭제 표시
     *   - 화면에만 있는 것은 새로 추가
     */
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
                                    "WHERE s.saleNo = :saleNo " +
                                    "AND s.deletedAt IS NULL",
                            Sale.class
                    )
                    .setParameter("saleNo", saleNo)
                    .getSingleResult();

            LocalDateTime now = LocalDateTime.now();

            // 화면에 남아있는 품목들 (여기서 하나씩 꺼내 쓴다)
            List<SaleRow> remaining = new ArrayList<>(rows);

            for (SaleItem item : new ArrayList<>(sale.getSaleItems())) {
                int index = indexOfBarcode(remaining, item.getBarcode());

                if (index >= 0) {
                    remaining.remove(index);   // 그대로 유지
                } else {
                    item.softDelete(now);      // 화면에서 빠짐 -> 삭제 표시
                }
            }

            // 화면에만 있는 것은 새로 추가된 품목
            for (SaleRow row : remaining) {
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

    /**
     * 같은 바코드를 가진 첫 번째 항목의 위치. 없으면 -1.
     */
    private int indexOfBarcode(List<SaleRow> rows, String barcode) {
        for (int i = 0; i < rows.size(); i++) {
            String rowBarcode = rows.get(i).getBarcode();

            if (rowBarcode != null && rowBarcode.equalsIgnoreCase(barcode)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 기간 안에 삭제된 판매 품목
     *
     * 바코드 단위로, 어느 판매번호에서 무엇이 언제 빠졌는지 돌려준다.
     */
    public List<SaleItem> findDeletedItemsByDateRange(LocalDateTime startDateTime,
                                                      LocalDateTime endDateTime) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                            "SELECT si FROM SaleItem si " +
                                    "JOIN FETCH si.sale " +
                                    "WHERE si.deletedAt >= :startDateTime " +
                                    "AND si.deletedAt < :endDateTime " +
                                    "ORDER BY si.deletedAt DESC, si.saleItemId ASC",
                            SaleItem.class
                    )
                    .setParameter("startDateTime", startDateTime)
                    .setParameter("endDateTime", endDateTime)
                    .getResultList();

        } finally {
            em.close();
        }
    }

    public String createSaleNo() {
        return "SALE" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    public List<String> findBarcodesByDateRange(LocalDateTime startDateTime,
                                                LocalDateTime endDateTime) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery("""
                    SELECT si.barcode
                    FROM SaleItem si
                    JOIN si.sale s
                    WHERE s.saleDate >= :startDateTime
                      AND s.saleDate < :endDateTime
                      AND s.deletedAt IS NULL
                      AND si.deletedAt IS NULL
                    ORDER BY s.saleDate ASC, si.saleItemId ASC
                    """, String.class)
                    .setParameter("startDateTime", startDateTime)
                    .setParameter("endDateTime", endDateTime)
                    .getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();

        } finally {
            em.close();
        }
    }

    /**
     * 품번으로 판매 내역 검색
     *
     * 컬러/사이즈는 바코드 뒷자리에만 들어있으므로
     * 품번(앞 10자리)으로 조회하면 컬러/사이즈와 관계없이 모두 검색된다.
     * 10자리보다 짧게 입력하면 앞자리 일치(prefix) 검색으로 동작한다.
     * 한글로 입력해도 영문 자판으로 변환되어 처리된다.
     *
     * startDateTime / endDateTime 이 null 이면 전체 기간이 대상이다.
     *
     * 해당 품번이 하나라도 들어있는 판매를 찾되,
     * 그 판매의 상세 품목은 전부 함께 읽어온다.
     * 목록에 판매 전체의 바코드를 보여줘야 하기 때문이다.
     */
    public List<Sale> findSalesByProductCode(String productCode,
                                             LocalDateTime startDateTime,
                                             LocalDateTime endDateTime) {
        String code = BarcodeParser.toProductCode(productCode);

        if (code.isEmpty()) {
            throw new IllegalArgumentException("검색할 품번을 입력하세요.");
        }

        boolean hasPeriod = startDateTime != null && endDateTime != null;

        EntityManager em = JPAUtil.getEntityManager();

        try {
            String jpql = "SELECT DISTINCT s FROM Sale s " +
                    "LEFT JOIN FETCH s.saleItems " +
                    "WHERE s.deletedAt IS NULL " +
                    "AND s.saleNo IN (" +
                    "    SELECT si.sale.saleNo FROM SaleItem si " +
                    "    WHERE UPPER(si.productCode) LIKE :productCode " +
                    "      AND si.deletedAt IS NULL" +
                    ") " +
                    (hasPeriod
                            ? "AND s.saleDate >= :startDateTime AND s.saleDate < :endDateTime "
                            : "") +
                    "ORDER BY s.saleDate DESC";

            TypedQuery<Sale> query = em.createQuery(jpql, Sale.class)
                    .setParameter("productCode", code + "%");

            if (hasPeriod) {
                query.setParameter("startDateTime", startDateTime)
                        .setParameter("endDateTime", endDateTime);
            }

            return query.getResultList();

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
}