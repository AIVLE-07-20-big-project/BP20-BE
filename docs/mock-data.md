# Effect verification mock data

This dataset exists only for local development of the effect-verification flow.
It does not represent production tables or real customer data.

## Run

Start MySQL and run Spring Boot with both the `local` and `mock` profiles.

```bat
docker compose up -d mysql
gradlew.bat bootRun --args="--spring.profiles.active=local,mock"
```

The mock schema and seed data are recreated every time the application starts
with the `mock` profile. Never enable this profile in a shared or production
database.

Preview the automatically aggregated metrics through Swagger or HTTP.

```text
GET /api/mock/effect-verification/metrics
```

Sales example parameters:

```text
store_id=1
type=SALES
from=2026-06-01T00:00:00
to=2026-06-15T00:00:00
start_hour=14
end_hour=17
```

Review example parameters:

```text
store_id=1
type=REVIEW
from=2026-06-01T00:00:00
to=2026-06-15T00:00:00
target_aspect=waiting_time
```

Use the aspect code returned by the ABSA API. The final list of aspect codes
still needs to be confirmed with the owning team.

The live integration test on 2026-07-21 classified `대기시간이 너무 길어요`
as the `convenience` aspect, so the waiting-time mock recommendation currently
uses that code.

## Scenarios

- Store 1: clearly effective sales/review scenario after 2026-06-15.
- Store 2: inconclusive sales/review scenario.
- Store 3: ineffective scenario.

All recommendations use an internal numeric `RecommendationID` and also carry a
UUID-shaped external `DecisionID`. This lets the effect-verification MVP keep its
current numeric key while the team decides the final customer-response contract.

## Tables

- `MockStore`
- `MockCustomer`
- `MockVisit`
- `MockOrders`
- `MockCouponIssue`
- `MockReview`
- `MockReviewAspectSentiment`
- `MockRecommendation`

Public review data may later replace `MockReview`. Sales, customer visits,
coupon use, and repeat visits cannot normally be recovered from public review
pages and therefore remain synthetic until a real data source is available.

## Review sentiment integration

Configure the teammate's ABSA server URL at runtime. Localtunnel addresses are
temporary, so they are never hard-coded in source control.

```bat
set REVIEW_SENTIMENT_AI_URL=https://modern-apples-divide.loca.lt
```

With the `local,mock` profiles active, analyze and persist one seeded review:

```text
POST /api/mock/reviews/{reviewId}/analyze
```

The original confidence (`0..100`) and normalized sentiment score (`-1..1`) are
both stored. Positive results become a positive score, neutral results become
zero, and negative results become a negative score.

Analyze every review without a stored ABSA result in a period:

```text
POST /api/mock/reviews/analyze-pending
```

Register a recommendation without manually supplying `before` metrics:

```text
POST /api/mock/effect-verifications/executions/{recommendationId}/register-auto
```

The automatic registration reads the mock recommendation, calculates the
14-day baseline window, analyzes pending reviews for REVIEW recommendations,
aggregates the matching metrics, and then creates a `COLLECTING` execution.
