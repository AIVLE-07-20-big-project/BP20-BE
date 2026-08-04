-- 효과 검증 최종 통합 테스트용 고정 시나리오
-- 점주 로그인: effect-owner@bp20.com / bp20test
-- 전략 실행일: 2026-05-01
-- 실행 전 기간: 2026-04-01 ~ 2026-05-01
-- 실행 후 기간: 2026-05-01 ~ 2026-05-31

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @owner_email = 'effect-owner@bp20.com';
SET @business_number = '9999900001';

SET @old_owner_id = (
    SELECT u.user_id
    FROM users u
    JOIN user_private_info p ON p.private_info_id = u.private_info_id
    WHERE p.email = @owner_email
    LIMIT 1
);
SET @old_store_id = (
    SELECT store_id
    FROM stores
    WHERE owner_user_id = @old_owner_id
    LIMIT 1
);

DELETE FROM effect_verification_result
WHERE airecommendationid IN ('final-sales-20260501', 'final-review-20260501');
DELETE FROM effect_verification_execution
WHERE airecommendationid IN ('final-sales-20260501', 'final-review-20260501');
DELETE ra
FROM review_analysis ra
JOIN reviews r ON r.review_id = ra.review_id
WHERE r.store_id = @old_store_id;
DELETE FROM reviews WHERE store_id = @old_store_id;
DELETE FROM coupons WHERE store_id = @old_store_id;
DELETE FROM customers WHERE store_id = @old_store_id;
DELETE FROM csv_daily_sales WHERE owner_id = @old_owner_id;
DELETE FROM stores WHERE store_id = @old_store_id;
DELETE FROM users WHERE user_id = @old_owner_id;
DELETE FROM user_private_info
WHERE email = @owner_email OR email LIKE 'effect-customer-%@bp20.test';

INSERT INTO user_private_info (
    email, name, password_hash, phone_number
) VALUES (
    @owner_email,
    '효과검증 점주',
    '$2a$10$URyON/JnIM.Stcxc9MNHFeJKjEuMEZWQ7vUxvo2VwEFknWryQ05jm',
    '01012345678'
);
SET @owner_private_id = LAST_INSERT_ID();

INSERT INTO users (
    created_at, updated_at, role, status, private_info_id
) VALUES (
    '2026-04-01 00:00:00',
    '2026-04-01 00:00:00',
    'STORE_OWNER',
    'ACTIVE',
    @owner_private_id
);
SET @owner_id = LAST_INSERT_ID();

INSERT INTO stores (
    created_at, updated_at, address, business_number, category,
    name, online_sales_status, phone_number, owner_user_id
) VALUES (
    '2026-04-01 00:00:00',
    '2026-04-01 00:00:00',
    '서울특별시 성동구 테스트로 20',
    @business_number,
    '카페',
    '효과검증 테스트 매장',
    'OPEN',
    '0212345678',
    @owner_id
);
SET @store_id = LAST_INSERT_ID();

-- 실행 전 신규 고객 2명, 실행 후 신규 고객 4명
INSERT INTO user_private_info (email, name, password_hash, phone_number) VALUES
('effect-customer-01@bp20.test', '테스트 고객 1', '', '01020000001'),
('effect-customer-02@bp20.test', '테스트 고객 2', '', '01020000002'),
('effect-customer-03@bp20.test', '테스트 고객 3', '', '01020000003'),
('effect-customer-04@bp20.test', '테스트 고객 4', '', '01020000004'),
('effect-customer-05@bp20.test', '테스트 고객 5', '', '01020000005'),
('effect-customer-06@bp20.test', '테스트 고객 6', '', '01020000006');

INSERT INTO customers (
    created_at, updated_at, status, private_info_id, store_id
)
SELECT
    CASE
        WHEN email IN ('effect-customer-01@bp20.test', 'effect-customer-02@bp20.test')
            THEN '2026-04-15 12:00:00'
        ELSE '2026-05-15 12:00:00'
    END,
    CASE
        WHEN email IN ('effect-customer-01@bp20.test', 'effect-customer-02@bp20.test')
            THEN '2026-04-15 12:00:00'
        ELSE '2026-05-15 12:00:00'
    END,
    'ACTIVE',
    private_info_id,
    @store_id
FROM user_private_info
WHERE email LIKE 'effect-customer-%@bp20.test';

SET @customer_1 = (
    SELECT c.customer_id
    FROM customers c
    JOIN user_private_info p ON p.private_info_id = c.private_info_id
    WHERE p.email = 'effect-customer-01@bp20.test'
);
SET @customer_2 = (
    SELECT c.customer_id
    FROM customers c
    JOIN user_private_info p ON p.private_info_id = c.private_info_id
    WHERE p.email = 'effect-customer-02@bp20.test'
);
SET @customer_3 = (
    SELECT c.customer_id
    FROM customers c
    JOIN user_private_info p ON p.private_info_id = c.private_info_id
    WHERE p.email = 'effect-customer-03@bp20.test'
);
SET @customer_4 = (
    SELECT c.customer_id
    FROM customers c
    JOIN user_private_info p ON p.private_info_id = c.private_info_id
    WHERE p.email = 'effect-customer-04@bp20.test'
);

-- 실행 전 쿠폰 사용률 25%, 실행 후 쿠폰 사용률 75%
INSERT INTO coupons (
    created_at, updated_at, discount_type, discount_value,
    expires_at, issued_at, name, status, used_at, customer_id, store_id
) VALUES
('2026-04-10 10:00:00', '2026-04-10 10:00:00', 'RATE', 10,
 '2026-06-30 23:59:59', '2026-04-10 10:00:00', '실행 전 쿠폰 1', 'USED',
 '2026-04-12 10:00:00', @customer_1, @store_id),
('2026-04-11 10:00:00', '2026-04-11 10:00:00', 'RATE', 10,
 '2026-06-30 23:59:59', '2026-04-11 10:00:00', '실행 전 쿠폰 2', 'ISSUED',
 NULL, @customer_1, @store_id),
('2026-04-12 10:00:00', '2026-04-12 10:00:00', 'RATE', 10,
 '2026-06-30 23:59:59', '2026-04-12 10:00:00', '실행 전 쿠폰 3', 'ISSUED',
 NULL, @customer_2, @store_id),
('2026-04-13 10:00:00', '2026-04-13 10:00:00', 'RATE', 10,
 '2026-06-30 23:59:59', '2026-04-13 10:00:00', '실행 전 쿠폰 4', 'ISSUED',
 NULL, @customer_2, @store_id),
('2026-05-10 10:00:00', '2026-05-10 10:00:00', 'RATE', 10,
 '2026-06-30 23:59:59', '2026-05-10 10:00:00', '실행 후 쿠폰 1', 'USED',
 '2026-05-11 10:00:00', @customer_1, @store_id),
('2026-05-11 10:00:00', '2026-05-11 10:00:00', 'RATE', 10,
 '2026-06-30 23:59:59', '2026-05-11 10:00:00', '실행 후 쿠폰 2', 'USED',
 '2026-05-12 10:00:00', @customer_2, @store_id),
('2026-05-12 10:00:00', '2026-05-12 10:00:00', 'RATE', 10,
 '2026-06-30 23:59:59', '2026-05-12 10:00:00', '실행 후 쿠폰 3', 'USED',
 '2026-05-13 10:00:00', @customer_3, @store_id),
('2026-05-13 10:00:00', '2026-05-13 10:00:00', 'RATE', 10,
 '2026-06-30 23:59:59', '2026-05-13 10:00:00', '실행 후 쿠폰 4', 'ISSUED',
 NULL, @customer_4, @store_id);

-- 실행 전 총매출 1,000,000원, 실행 후 총매출 1,300,000원
INSERT INTO csv_daily_sales (
    owner_id, product_code, product_name, sale_date,
    sales_quantity, unit_price, sales_amount
) VALUES
(@owner_id, 'MENU-001', '아메리카노', '2026-04-10', 50, 5000, 250000),
(@owner_id, 'MENU-002', '카페라떼', '2026-04-20', 75, 10000, 750000),
(@owner_id, 'MENU-001', '아메리카노', '2026-05-10', 80, 5000, 400000),
(@owner_id, 'MENU-002', '카페라떼', '2026-05-20', 90, 10000, 900000);

-- 실행 전: 평균 3.5점, 부정 리뷰 50%, food 부정 75%
INSERT INTO reviews (
    content, is_analyzed, rating, reviewed_date, store_id
) VALUES
('음식이 너무 짜고 아쉬웠어요.', b'1', 3.0, '2026-04-05 12:00:00', @store_id),
('메뉴 맛이 기대에 미치지 못했습니다.', b'1', 3.0, '2026-04-12 12:00:00', @store_id),
('음식은 괜찮지만 개선이 필요해요.', b'1', 4.0, '2026-04-19 12:00:00', @store_id),
('전반적으로 만족했습니다.', b'1', 4.0, '2026-04-26 12:00:00', @store_id);
SET @before_review_start = LAST_INSERT_ID();

INSERT INTO review_analysis (
    aspect, confidence, review_id, sentiment
) VALUES
('food', 0.95, @before_review_start, 'negative'),
('food', 0.92, @before_review_start + 1, 'negative'),
('food', 0.88, @before_review_start + 2, 'negative'),
('food', 0.90, @before_review_start + 3, 'positive');

-- 실행 후: 평균 4.5점, 부정 리뷰 25%, food 부정 25%
INSERT INTO reviews (
    content, is_analyzed, rating, reviewed_date, store_id
) VALUES
('간이 적당하고 음식이 맛있어졌어요.', b'1', 4.0, '2026-05-05 12:00:00', @store_id),
('메뉴 맛이 전보다 좋아졌습니다.', b'1', 4.0, '2026-05-12 12:00:00', @store_id),
('음식이 정말 만족스러웠어요.', b'1', 5.0, '2026-05-19 12:00:00', @store_id),
('일부 메뉴는 아직 조금 짰습니다.', b'1', 5.0, '2026-05-26 12:00:00', @store_id);
SET @after_review_start = LAST_INSERT_ID();

INSERT INTO review_analysis (
    aspect, confidence, review_id, sentiment
) VALUES
('food', 0.94, @after_review_start, 'positive'),
('food', 0.93, @after_review_start + 1, 'positive'),
('food', 0.96, @after_review_start + 2, 'positive'),
('food', 0.91, @after_review_start + 3, 'negative');

SELECT
    @owner_id AS owner_user_id,
    @store_id AS store_id,
    @owner_email AS login_email,
    'bp20test' AS login_password;
