# SupplyChain 供应链协同系统

三角色（**商家 / 店家 / 平台管理方**）协同的供应链系统：商家维护货源与商品，店家下单采购并实时扣减商家库存，平台每日 10:00 自动双方对账、审核订单、记录操作审计，并内置分账结算。

> 后端 Spring Boot 3.5 + Java 21；前端 Vue 3 + Vite + Element Plus；默认 H2 内存库（预留 MySQL / PostgreSQL profile）。

## 角色与能力

| 角色 | 能力 |
|------|------|
| 商家 MERCHANT | 维护供应商字段与供应链信息；上架 / 编辑商品（规格 / 成本 / 售价 / 库存）；查看店家对自己的采购订单 |
| 店家 SHOPKEEPER | 浏览商家与商品；下单采购（下单时商家库存实时扣减）；查看自己的采购订单与分账明细 |
| 平台管理方 ADMIN | 每日 10:00 自动双方对账；审核店家订单（可改数量）；查看对账记录、操作审计与概览统计 |

## 分账公式

```
orderAmount    = Σ(qty × price)              // 订单成交额
merchantCost   = Σ(qty × costPrice)          // 商家本金（供货成本）
platformFee    = round(orderAmount × 3%)      // 平台佣金（可配置）
shopkeeperGain = orderAmount − merchantCost − platformFee
```

示例：商家本金 100，店家买 120 → 平台挣 3.6，店家挣 16.4。

## 快速开始

```bash
# 后端（端口 8080）
cd backend && mvn -B verify
mvn spring-boot:run

# 前端（端口 5173，/api 代理到 8080）
cd frontend && npm install && npm run dev
```

种子账号：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 平台管理方 |
| merchant1 | merch123 | 商家 |
| shop1 | shop123 | 店家 |

## 工程结构

```
supplychain/
├── DESIGN.md          # 设计文档（数据模型 / API / 分账 / 对账 / 并发）
├── backend/           # Spring Boot 单模块（Java 21）
└── frontend/          # Vue 3 + Vite + Element Plus
```

## 抗并发与一致性

- 无状态 JWT（HS256），水平可扩展，目标抗 300 在线。
- `Product` 采用 `@Version` 乐观锁，下单扣库存防超卖。
- 订单号唯一、对账按 `(日期,商家,店家)` 幂等，重复执行不产生重复记录。

## 数据库切换

默认 H2。切换 MySQL / PostgreSQL 见 `backend/src/main/resources/application.yml` 末尾注释，通过
`java -jar backend.jar --spring.profiles.active=mysql` 激活（需自行添加对应 driver 依赖）。
