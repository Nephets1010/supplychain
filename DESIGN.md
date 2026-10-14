# SupplyChain 供应链协同系统 — 设计文档

> 客户需求：商家（货源方）、店家（采购方）、平台管理方三角色协同的供应链系统。
> 后端 Java（Spring Boot 3.5 + Java 21），前端 Vue 3 + Vite，H2 内存库（预留 MySQL/PostgreSQL profile）。

---

## 1. 角色与核心流程

| 角色 | 标识 | 职责 |
|------|------|------|
| 商家 Merchant | `MERCHANT` | 维护供应商/货源信息；上架商品（规格/售价/成本/库存）；查看店家对自己的采购 |
| 店家 Shopkeeper | `SHOPKEEPER` | 浏览商家与商品；下单采购（数量/单价）；下单时商家库存实时扣减；查看自己的采购记录 |
| 平台管理方 Admin | `ADMIN` | 每日 10:00 自动双方对账；审核店家订单（可修改数量）；查看操作审计日志 |

**关键业务规则**
1. 店家下单购买某商品时，商家对应商品库存**实时扣减**（乐观锁防超卖）。
2. 订单生成后状态为 `PENDING`，需管理方审核；审核通过 `APPROVED` 触发分账结算，驳回 `REJECTED`。
3. 管理方审核时可**修改订单项数量**，改后重新计算金额与分账。
4. 每日 10:00（`cron = "0 0 10 * * *"`）系统对前一日所有已结算订单做双方对账，生成 `Reconciliation` 记录。
5. 平台费率默认 **3%**，可在 `application.yml` 通过 `supplychain.split.platform-rate` 配置。
6. 所有写操作（下单/审核/对账/库存变更）记录到 `AuditLog`。

---

## 2. 分账公式（Settlement / Split）

以单笔订单为例，订单含若干 `OrderItem`：

```
itemSubtotal   = itemQty * itemUnitPrice           // 单项成交额
orderAmount    = Σ itemSubtotal                     // 订单总成交额
merchantCost   = Σ (itemQty * product.costPrice)    // 商家本金（商家供货成本）
platformFee    = round(orderAmount * platformRate)  // 平台佣金（默认 3%）
shopkeeperGain = orderAmount - merchantCost - platformFee  // 店家利润
```

**校验（守恒）**：`orderAmount == merchantCost + platformFee + shopkeeperGain`

示例（需求文档）：商家本金 100，店家买 120，平台挣 120×3% = 3.6，店家挣 120 − 100 − 3.6 = 16.4。

- `Product.costPrice` = 商家成本（本金）
- `Product.price` = 店家采购单价
- `platformRate` = 0.03

金额使用 `BigDecimal`，平台费四舍五入保留 2 位（HALF_UP）。

---

## 3. 并发与一致性

- **抗 300 人在线**：无状态 JWT（不依赖 HttpSession），水平可扩展。
- **库存扣减**：`Product` 带 `@Version` 乐观锁；下单走 `@Transactional`，扣减前校验 `stockQty >= qty`，版本冲突抛 `OptimisticLockException` 由调用方重试/提示。
- **下单幂等**：订单号 `orderNo` 唯一；同一请求重复提交由前端防重 + 后端唯一约束兜底。
- **对账幂等**：按 `reconcileDate + merchantId + shopkeeperId` 唯一，重复执行不产生重复记录。

---

## 4. 数据模型（JPA 实体）

基础包：`io.github.nephets.supplychain`

### AppUser（账户）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long PK | |
| username | String @Column(unique) | 登录名 |
| passwordHash | String | BCrypt |
| role | String | MERCHANT / SHOPKEEPER / ADMIN |
| merchantId | Long null | 角色为 MERCHANT 时关联 Merchant.id |
| shopkeeperId | Long null | 角色为 SHOPKEEPER 时关联 Shopkeeper.id |
| createdAt | Instant | |

### Merchant（商家）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long PK | |
| name | String | 商家名称 |
| contact | String | 联系人 |
| supplierName | String | 供应商名称（商家字段：添加供应商） |
| supplierInfo | String | 供应链信息（规格/价格/数量等备注） |
| createdAt | Instant | |

### Shopkeeper（店家）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long PK | |
| name | String | 店家名称 |
| contact | String | 联系人 |
| createdAt | Instant | |

### Product（商品 / 货源）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long PK | |
| merchantId | Long | 所属商家 |
| name | String | 商品名 |
| spec | String | 规格 |
| costPrice | BigDecimal | 商家成本（本金） |
| price | BigDecimal | 店家采购单价 |
| stockQty | int | 库存 |
| version | @Version Long | 乐观锁 |
| createdAt | Instant | |

### PurchaseOrder（采购订单）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long PK | |
| orderNo | String unique | 订单号 |
| shopkeeperId | Long | 店家 |
| merchantId | Long | 商家 |
| status | String | PENDING/APPROVED/REJECTED |
| totalAmount | BigDecimal | 订单成交额 |
| platformFee | BigDecimal | 平台费 |
| merchantCost | BigDecimal | 商家本金 |
| shopkeeperGain | BigDecimal | 店家利润 |
| createdAt | Instant | |
| reviewedAt | Instant null | |
| reviewerId | Long null | 审核人（AppUser.id） |
| reviewNote | String null | |

### OrderItem（订单项）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long PK | |
| orderId | Long | 关联 PurchaseOrder |
| productId | Long | |
| productName | String | 快照 |
| qty | int | 数量 |
| unitPrice | BigDecimal | 单价快照 |
| costPrice | BigDecimal | 成本快照 |
| subtotal | BigDecimal | qty*unitPrice |

### Reconciliation（对账记录）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long PK | |
| reconcileDate | LocalDate | 对账日（订单产生日） |
| merchantId | Long | |
| shopkeeperId | Long | |
| orderCount | int | 当日订单数 |
| totalAmount | BigDecimal | 成交额合计 |
| platformFee | BigDecimal | 平台费合计 |
| merchantCost | BigDecimal | 商家本金合计 |
| shopkeeperGain | BigDecimal | 店家利润合计 |
| generatedAt | Instant | |
| status | String | RECONCILED |

### AuditLog（操作审计）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long PK | |
| actorId | Long | 操作人 AppUser.id |
| actorName | String | |
| action | String | CREATE_ORDER / APPROVE_ORDER / REJECT_ORDER / MODIFY_ORDER / RECONCILE / LOGIN ... |
| targetType | String | ORDER / PRODUCT / USER ... |
| targetId | String | |
| detail | String | JSON 摘要 |
| createdAt | Instant | |

---

## 5. REST API 契约（base path `/api`）

通用：错误返回 `{ "error": "...", "message": "..." }`，HTTP 状态码语义化。
鉴权：`POST /api/auth/login` 公开；其余接口需 `Authorization: Bearer <jwt>`，按角色授权。

### 认证
| Method | Path | 说明 | 角色 |
|--------|------|------|------|
| POST | `/api/auth/login` | `{username,password}` → `{token,role,username}` | 公开 |
| GET | `/api/auth/me` | 当前用户 | 全部 |

### 商家（MERCHANT）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/merchant/products` | 我的商品列表 |
| POST | `/api/merchant/products` | 上架商品 `{name,spec,costPrice,price,stockQty,supplierName?,supplierInfo?}` |
| PUT | `/api/merchant/products/{id}` | 改商品 |
| GET | `/api/merchant/orders` | 来自店家对我的采购订单 |

### 店家（SHOPKEEPER）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/shop/merchants` | 商家列表（名称） |
| GET | `/api/shop/products?merchantId=` | 某商家商品 |
| POST | `/api/shop/orders` | 下单 `{items:[{productId,qty}]}` → 扣库存+生成订单 |
| GET | `/api/shop/orders` | 我的采购订单（实时数量记录） |
| GET | `/api/shop/orders/{id}` | 订单明细 |

### 管理方（ADMIN）
| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/admin/orders?status=` | 全部订单 |
| GET | `/api/admin/orders/{id}` | 订单明细 |
| POST | `/api/admin/orders/{id}/review` | 审核 `{status:APPROVED|REJECTED, items?:[{id,qty}], note?}`（可改数量） |
| GET | `/api/admin/reconciliations?date=` | 对账记录 |
| POST | `/api/admin/reconciliations/run?date=` | 手动触发对账（测试/补算） |
| GET | `/api/admin/audit` | 操作日志 |
| GET | `/api/admin/stats` | 概览统计 |

### 分账查询
| Method | Path | 说明 |
|--------|------|------|
| POST | `/api/settle/preview` | `{items:[{qty,unitPrice,costPrice}]}` → 分账预览（公开/调试用） |

---

## 6. 安全与 JWT

- Spring Security + 手写 JWT 过滤器（`jjwt` 0.12.x，HS256）。
- `application.yml`：`supplychain.jwt.secret`（>=32字节 Base64）、`supplychain.jwt.expire-minutes`（默认 480）。
- 密码 BCrypt；`/api/auth/login` 免认证，其余需 Bearer。
- 角色授权：`MERCHANT` 仅访问 `/api/merchant/**`，`SHOPKEEPER` 仅 `/api/shop/**`，`ADMIN` 仅 `/api/admin/**`；`/api/settle/preview` 与 `/api/auth/**` 公开。

---

## 7. 工程结构

```
supplychain/
├── DESIGN.md
├── backend/                      # Spring Boot 单模块
│   ├── pom.xml
│   └── src/main/java/io/github/nephets/supplychain/
│       ├── SupplychainApplication.java
│       ├── config/              # SecurityConfig, JwtFilter, OpenApiConfig
│       ├── security/            # JwtUtil, AuthService, LoginRequest/Response
│       ├── domain/              # AppUser, Merchant, Shopkeeper, Product, PurchaseOrder, OrderItem, Reconciliation, AuditLog
│       ├── repository/          # *Repository
│       ├── service/             # AuthService, ProductService, OrderService, SettlementService, ReconciliationService, AuditService, SeedService
│       ├── web/                 # *Controller, *Request/Response DTO
│       └── job/                 # DailyReconcileJob
│   └── src/main/resources/application.yml
│   └── src/test/java/...        # 分账/并发/对账测试
└── frontend/                     # Vue 3 + Vite + Element Plus + Pinia + Router + Axios
    ├── package.json
    ├── vite.config.ts           # proxy /api -> http://localhost:8080
    └── src/
        ├── main.ts, App.vue, router/, stores/, api/, views/{Login, Merchant, Shopkeeper, Admin}.vue
```

---

## 8. 构建与运行

```bash
# 后端
cd backend && mvn -B verify        # 含 spotless + jacoco(>=0.60)
mvn spring-boot:run                # http://localhost:8080

# 前端
cd frontend && npm install && npm run dev   # http://5173 (代理 /api -> 8080)

# 生产构建
cd frontend && npm run build        # 产出 dist/，可由 nginx 托管
```

种子账号（见 `SeedService`，`@Profile` 默认激活）：
- `admin / admin123`  (ADMIN)
- `merchant1 / merch123` (MERCHANT)
- `shop1 / shop123` (SHOPKEEPER)

切换数据库：默认 H2（`spring.datasource.url=jdbc:h2:mem:supplychain`）。MySQL/PostgreSQL 通过 `application-mysql.yml` / `application-pg.yml` + `-Dspring.profiles.active=mysql` 启用（本 MVP 仅预留 yml 占位与注释，不强制依赖外部库）。
