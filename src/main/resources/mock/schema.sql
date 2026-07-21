CREATE TABLE IF NOT EXISTS MockStore (
    StoreID BIGINT PRIMARY KEY,
    StoreName VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS MockCustomer (
    CustomerID BIGINT PRIMARY KEY,
    StoreID BIGINT NOT NULL,
    JoinedAt DATETIME NOT NULL,
    CONSTRAINT FK_MockCustomer_Store
        FOREIGN KEY (StoreID) REFERENCES MockStore(StoreID)
);

CREATE TABLE IF NOT EXISTS MockVisit (
    VisitID BIGINT PRIMARY KEY,
    StoreID BIGINT NOT NULL,
    CustomerID BIGINT NOT NULL,
    VisitedAt DATETIME NOT NULL,
    CONSTRAINT FK_MockVisit_Store
        FOREIGN KEY (StoreID) REFERENCES MockStore(StoreID),
    CONSTRAINT FK_MockVisit_Customer
        FOREIGN KEY (CustomerID) REFERENCES MockCustomer(CustomerID),
    INDEX IX_MockVisit_StoreDate (StoreID, VisitedAt)
);

CREATE TABLE IF NOT EXISTS MockOrders (
    OrderID BIGINT PRIMARY KEY,
    VisitID BIGINT NOT NULL,
    StoreID BIGINT NOT NULL,
    CustomerID BIGINT NOT NULL,
    OrderedAt DATETIME NOT NULL,
    TotalAmount DECIMAL(12, 2) NOT NULL,
    TargetCustomer BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT FK_MockOrders_Visit
        FOREIGN KEY (VisitID) REFERENCES MockVisit(VisitID),
    CONSTRAINT FK_MockOrders_Store
        FOREIGN KEY (StoreID) REFERENCES MockStore(StoreID),
    CONSTRAINT FK_MockOrders_Customer
        FOREIGN KEY (CustomerID) REFERENCES MockCustomer(CustomerID),
    INDEX IX_MockOrders_StoreDate (StoreID, OrderedAt)
);

CREATE TABLE IF NOT EXISTS MockCouponIssue (
    CouponIssueID BIGINT PRIMARY KEY,
    StoreID BIGINT NOT NULL,
    CustomerID BIGINT NOT NULL,
    IssuedAt DATETIME NOT NULL,
    UsedAt DATETIME NULL,
    OrderID BIGINT NULL,
    TargetCustomer BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT FK_MockCouponIssue_Store
        FOREIGN KEY (StoreID) REFERENCES MockStore(StoreID),
    CONSTRAINT FK_MockCouponIssue_Customer
        FOREIGN KEY (CustomerID) REFERENCES MockCustomer(CustomerID),
    CONSTRAINT FK_MockCouponIssue_Order
        FOREIGN KEY (OrderID) REFERENCES MockOrders(OrderID),
    INDEX IX_MockCouponIssue_StoreDate (StoreID, IssuedAt)
);

CREATE TABLE IF NOT EXISTS MockReview (
    ReviewID BIGINT PRIMARY KEY,
    StoreID BIGINT NOT NULL,
    CustomerID BIGINT NOT NULL,
    CreatedAt DATETIME NOT NULL,
    Rating DECIMAL(2, 1) NOT NULL,
    Content VARCHAR(500) NOT NULL,
    Negative BOOLEAN NOT NULL,
    TargetAspect VARCHAR(50) NULL,
    AspectSentimentScore DECIMAL(4, 3) NULL,
    CONSTRAINT FK_MockReview_Store
        FOREIGN KEY (StoreID) REFERENCES MockStore(StoreID),
    CONSTRAINT FK_MockReview_Customer
        FOREIGN KEY (CustomerID) REFERENCES MockCustomer(CustomerID),
    INDEX IX_MockReview_StoreDate (StoreID, CreatedAt),
    INDEX IX_MockReview_Aspect (StoreID, TargetAspect, CreatedAt)
);

CREATE TABLE IF NOT EXISTS MockRecommendation (
    RecommendationID BIGINT PRIMARY KEY,
    DecisionID VARCHAR(36) NOT NULL UNIQUE,
    StoreID BIGINT NOT NULL,
    RecommendationType VARCHAR(20) NOT NULL,
    ActionID VARCHAR(50) NOT NULL,
    TargetStartHour INT NULL,
    TargetEndHour INT NULL,
    TargetCustomerGroup VARCHAR(50) NULL,
    TargetAspect VARCHAR(50) NULL,
    Executed BOOLEAN NOT NULL DEFAULT FALSE,
    ExecutedAt DATETIME NULL,
    CONSTRAINT FK_MockRecommendation_Store
        FOREIGN KEY (StoreID) REFERENCES MockStore(StoreID)
);

