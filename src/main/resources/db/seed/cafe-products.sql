-- BP20 개발용 카페 상품 더미데이터
-- 현재 존재하는 모든 매장에 동일 상품명이 없을 때 한 번만 삽입한다.
-- 상품은 공통 원장에만 등록하며 온라인 판매 상태는 NOT_REGISTERED로 시작한다.

INSERT INTO products (
    store_id,
    name,
    description,
    price,
    stock_quantity,
    image_url,
    status,
    online_sales_status,
    created_at,
    updated_at
)
SELECT
    s.store_id,
    seed.name,
    seed.description,
    seed.price,
    seed.stock_quantity,
    seed.image_url,
    'ACTIVE',
    'NOT_REGISTERED',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM stores s
CROSS JOIN (
    SELECT
        '아이스 아메리카노' AS name,
        '고소한 원두로 내린 시원한 아메리카노입니다.' AS description,
        4000 AS price,
        100 AS stock_quantity,
        'https://cdn.bp20.com/products/americano-ice.jpg' AS image_url
    UNION ALL SELECT
        '아메리카노 HOT', '고소한 원두의 풍미를 살린 따뜻한 아메리카노입니다.',
        4000, 100, 'https://cdn.bp20.com/products/americano-hot.jpg'
    UNION ALL SELECT
        '카페라떼 ICE', '에스프레소와 우유가 부드럽게 어우러진 아이스 라떼입니다.',
        4500, 80, 'https://cdn.bp20.com/products/cafe-latte-ice.jpg'
    UNION ALL SELECT
        '카페라떼 HOT', '따뜻한 우유와 에스프레소를 조화롭게 담은 라떼입니다.',
        4500, 80, 'https://cdn.bp20.com/products/cafe-latte-hot.jpg'
    UNION ALL SELECT
        '솔티드 크림 라떼', '짭짤한 크림과 부드러운 라떼가 어우러진 시그니처 음료입니다.',
        6000, 40, 'https://cdn.bp20.com/products/salted-cream-latte.jpg'
    UNION ALL SELECT
        '돌체라떼 ICE', '달콤한 연유와 에스프레소를 넣은 아이스 돌체라떼입니다.',
        5000, 50, 'https://cdn.bp20.com/products/dolce-latte-ice.jpg'
    UNION ALL SELECT
        '바닐라라떼 ICE', '바닐라 향과 에스프레소가 어우러진 달콤한 아이스 라떼입니다.',
        5000, 50, 'https://cdn.bp20.com/products/vanilla-latte-ice.jpg'
    UNION ALL SELECT
        '에티오피아 내추럴', '화사한 과일 향과 은은한 단맛을 느낄 수 있는 핸드드립 커피입니다.',
        6000, 30, 'https://cdn.bp20.com/products/ethiopia-natural.jpg'
    UNION ALL SELECT
        '콜드브루 케냐', '케냐 원두를 차갑게 장시간 추출한 깔끔한 콜드브루입니다.',
        5000, 40, 'https://cdn.bp20.com/products/cold-brew-kenya.jpg'
    UNION ALL SELECT
        '클럽 샌드위치', '신선한 채소와 닭가슴살을 넣은 든든한 클럽 샌드위치입니다.',
        6000, 30, 'https://cdn.bp20.com/products/club-sandwich.jpg'
    UNION ALL SELECT
        '햄치즈 샌드위치', '햄과 치즈, 신선한 채소를 넣은 기본 샌드위치입니다.',
        5500, 30, 'https://cdn.bp20.com/products/ham-cheese-sandwich.jpg'
    UNION ALL SELECT
        '에그마요 샌드위치', '고소한 에그마요를 가득 넣은 부드러운 샌드위치입니다.',
        5500, 30, 'https://cdn.bp20.com/products/egg-mayo-sandwich.jpg'
    UNION ALL SELECT
        '버터 크루아상', '고소한 버터 풍미와 바삭한 결이 살아 있는 크루아상입니다.',
        3800, 40, 'https://cdn.bp20.com/products/butter-croissant.jpg'
    UNION ALL SELECT
        '소금빵', '짭짤한 소금과 버터의 풍미가 조화로운 소금빵입니다.',
        3200, 50, 'https://cdn.bp20.com/products/salt-bread.jpg'
    UNION ALL SELECT
        '초콜릿 스콘', '진한 초콜릿과 담백한 반죽을 함께 구운 스콘입니다.',
        4000, 35, 'https://cdn.bp20.com/products/chocolate-scone.jpg'
    UNION ALL SELECT
        '플레인 베이글', '쫄깃한 식감과 담백한 맛을 살린 플레인 베이글입니다.',
        3500, 40, 'https://cdn.bp20.com/products/plain-bagel.jpg'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM products p
    WHERE p.store_id = s.store_id
      AND p.name = seed.name
);
