# SQLForge Frontend Design System

## Summary

本文件将用户提供的 Supabase 风格要求完整固化为 SQLForge 前端的长期设计参考原则。目标不是复制 Supabase 官网，而是把其中可复用的视觉语义、排版纪律、组件边界与验收标准转译为 SQLForge 可执行、可复用、可验收的桌面端仪表板设计基线。

当前版本只定义桌面端 Web 规范。源需求中涉及移动端/断点的内容全部保留为“延后参考”，不作为本阶段强制实现项。

## 1. Visual Theme & Atmosphere

### 1.1 Core Positioning

- SQLForge 默认采用 dark-mode-native 视觉基线。
- 页面气质必须接近“高级代码编辑器上的治理工作台”，而不是传统 BI 大屏、营销官网或浅色企业后台。
- 主背景必须使用 near-black 深灰，不得使用纯黑，也不得继续沿用浅蓝玻璃感初始化主题。

### 1.2 Atmosphere Rules

- 主背景基线：
  - 页面主背景：`#171717`
  - 深层背景/主按钮背景：`#0f0f0f`
- 品牌强调色：
  - 品牌绿：`#3ecf8e`
  - 交互绿：`#00c573`
- 视觉深度主要来自：
  - 深浅表面差异
  - 透明叠层
  - 边框层级
  - 少量功能性 focus 阴影
- 视觉深度不得依赖明显 box-shadow。

### 1.3 Personality

- 设计要保留 developer-console 的效率感。
- 大标题应具备低行高、强压缩、少废话的“终端命令感”。
- monospace 标签只用于技术语义标记，如 `governance cockpit`、`runtime health`、`audit stream`。
- 绿色必须作为“SQLForge 品牌识别”使用，而不是大面积装饰色。

## 2. Color System & Token Roles

### 2.1 Brand & Accent

| Token | Value | Use |
|---|---:|---|
| `--sqlforge-color-brand` | `#3ecf8e` | 品牌标识、小面积高亮、状态强调 |
| `--sqlforge-color-link` | `#00c573` | 链接、可点击强调文本 |
| `--sqlforge-color-brand-border` | `rgba(62, 207, 142, 0.3)` | 强调边框、激活态轮廓 |

### 2.2 Neutral Dark Scale

| Token | Value | Use |
|---|---:|---|
| `--sqlforge-bg-page` | `#171717` | 页面主背景 |
| `--sqlforge-bg-page-deep` | `#0f0f0f` | 深层背景、主按钮背景 |
| `--sqlforge-surface-1` | `#171717` | 常规表面 |
| `--sqlforge-surface-2` | `#1d1d1d` | 稍抬升表面 |
| `--sqlforge-surface-3` | `rgba(41, 41, 41, 0.84)` | 透明深色表面 |
| `--sqlforge-border-subtle` | `#242424` | 分区线、极弱边框 |
| `--sqlforge-border-default` | `#2e2e2e` | 标准卡片/导航边框 |
| `--sqlforge-border-strong` | `#363636` | 强调边框 |
| `--sqlforge-border-stronger` | `#393939` | 更强对比边框 |
| `--sqlforge-text-primary` | `#fafafa` | 主文本 |
| `--sqlforge-text-secondary` | `#b4b4b4` | 次文本 |
| `--sqlforge-text-muted` | `#898989` | 弱文本/说明文本 |

### 2.3 Auxiliary Semantic Colors

- 可保留受 Radix 风格启发的辅助语义色，用于警告、错误、冷色提示、信息层叠。
- 辅助色只能用于状态语义，不得抢占主视觉。
- 推荐语义映射：
  - 成功/运行正常：品牌绿体系
  - 警告：yellow/orange alpha
  - 异常：tomato/crimson alpha
  - 冷色提示：indigo/slate alpha

### 2.4 HSL / Alpha Principle

- 颜色 token 应优先支持 alpha / hsla 形式，服务于透明叠层、边框和状态提示。
- 页面禁止大量纯实色平铺；应优先使用深色表面 + 半透明描边 + 低饱和叠层组合。
- 透明 token 的目标是制造“层”而不是“发光”。

## 3. Typography Rules

### 3.1 Font Families

- 主字体策略：采用几何感接近 Circular 的开源替代字体栈。
- 推荐 sans 栈：
  - `Manrope`
  - `Plus Jakarta Sans`
  - `IBM Plex Sans`
  - `Noto Sans SC`
  - `PingFang SC`
  - `sans-serif`
- 推荐 monospace 栈：
  - `Source Code Pro`
  - `IBM Plex Mono`
  - `Menlo`
  - `monospace`

### 3.2 Hierarchy

| Role | Size | Weight | Line Height | Letter Spacing | Rule |
|---|---:|---:|---:|---:|---|
| Hero Display | `64px-72px` | `400` | `1.00` | `normal` | 首页总览主标题 |
| Section Heading | `32px-36px` | `400` | `1.15-1.25` | `normal` | 大区块标题 |
| Card Title | `22px-24px` | `400` | `1.25-1.33` | `-0.16px` | 模块卡片标题 |
| Sub-heading | `18px` | `400` | `1.45-1.56` | `normal` | 组标题 |
| Body | `16px` | `400` | `1.50` | `normal` | 正文 |
| Nav / Button | `14px` | `500` | `1.14-1.43` | `normal` | 导航和交互 |
| Caption | `12px-14px` | `400-500` | `1.33-1.43` | `normal` | 元数据 |
| Code Label | `12px` | `400` | `1.33` | `1.2px` | 大写技术标签 |

### 3.3 Typographic Constraints

- 默认文字权重必须是 `400`。
- `500` 只允许用于按钮、导航、局部状态强调。
- 不使用 `700` 作为主层级手段。
- 视觉层级优先靠字号、行高、间距建立，而不是粗体堆叠。
- Hero 标题必须保持低行高密排版，禁止稀疏 leading。
- Card Title 可以使用轻微负字距，制造更紧的控制台质感。

## 4. Component Styling Rules

### 4.1 Buttons

#### Primary Pill

- 背景：`#0f0f0f`
- 文本：`#fafafa`
- 边框：`1px solid #fafafa`
- 圆角：`9999px`
- padding：`8px 32px`
- 用途：主 CTA
- 禁止：
  - 16px 中间态圆角
  - 大面积纯绿底按钮
  - 夸张阴影

#### Secondary Pill

- 背景：`#0f0f0f`
- 文本：`#fafafa`
- 边框：`1px solid #2e2e2e`
- 圆角：`9999px`
- opacity 可轻微降低
- 用途：次 CTA

#### Ghost Button

- 背景：`transparent`
- 文本：`#fafafa`
- 边框：`1px solid transparent`
- 圆角：`6px`
- 用途：轻操作、辅助操作

### 4.2 Cards & Containers

- 主背景：深色表面或透明深色表面
- 边框：`#2e2e2e` 或 `#363636`
- 圆角：`8px-16px`
- 阴影：默认无
- 内边距：`16px-24px`
- 不允许卡片套卡片形成厚重层叠；优先用分区和内部分栏组织信息

### 4.3 Tabs / Pills

- 轮廓：`1px solid #2e2e2e`
- 形状：`9999px`
- 激活态：
  - 深层背景提升
  - 文本增强
  - 可辅以品牌绿边框
- 不允许用高饱和整块色填充

### 4.4 Links

- 交互绿：`#00c573`
- 标准亮文本链接：`#fafafa`
- 次级链接：`#b4b4b4`
- 弱链接：`#898989`

### 4.5 Navigation

- 侧栏与页面背景保持同体系深色，不做亮底反差。
- 导航项字体使用 `14px / 500`。
- 激活态优先通过：
  - 表面提升
  - 边框增强
  - 品牌绿小面积点缀
- 禁止使用默认亮色 Element Plus 菜单风格。

### 4.6 Dashboard-Specific Components

#### Top Status Header

- 不是普通卡片，属于壳层顶部状态栏。
- 需承载：
  - 当前工作区
  - 页面标题
  - 页面简要说明
  - 主题/语言切换

#### KPI Metric Card

- 内容优先级：
  - 数值
  - 指标名
  - 趋势/状态
  - 技术标签
- 以摘要可读性优先，不允许出现原始 JSON。

#### Quick Entry Card

- 必须包含：
  - 功能名
  - 一句话说明
  - 当前状态摘要
  - 进入动作

#### Risk / Alert Card

- 以结论和建议为主，而非只列状态码。
- 可以使用语义色边框或轻微背景洗色，但深色基底不变。

#### Activity Stream Item

- 必须包含：
  - 事件类型
  - 对象标识
  - 时间
  - 状态结果
- 不允许过度装饰；应保持日志/时间线质感。

## 5. Layout Principles

### 5.1 Page Structure

- 页面必须遵循项目规则 `R-023`：
  - 上下文
  - 当前任务
  - 关键输入
  - 运行状态
  - 结果
  - 历史
  - 下一步
- Dashboard 首页固定映射为：
  - 顶部状态头
  - Hero 总览区
  - KPI 指标区
  - 五大功能快捷入口区
  - 平台健康与风险区
  - 最近活动区
  - 下一步建议区

### 5.2 Spacing

- 基础单位：`8px`
- 核心刻度：`4 / 6 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64 / 96 / 128`
- 区块之间允许使用更大间距制造“场景切换感”。
- 区块内部应保持较紧凑的信息密度。

### 5.3 Container Strategy

- 页面内容应充分利用桌面端完整视口。
- 不使用居中窄栏博客式布局。
- 使用分区面板和多列网格承载信息，但必须保持阅读优先级清晰。
- Dashboard 不是图表海报，不追求填满所有区域。

## 6. Depth & Elevation Rules

| Level | Treatment | Use |
|---|---|---|
| Level 0 | 无阴影，`#2e2e2e` 描边 | 默认平面 |
| Level 1 | 更强描边，表面略提亮 | hover / 当前模块 |
| Level 2 | 极轻 focus shadow | 键盘 focus / 交互确认 |
| Level 3 | 品牌绿边框或弱洗色 | 品牌强调 / 重要推荐 |

### Mandatory Constraints

- 绝大多数表面无阴影。
- 深度主要靠边框层级：`#242424 -> #2e2e2e -> #363636 -> #393939`
- 品牌绿边框是“提升态”，不是“成功态唯一表达”。

## 7. Do / Don’t

### Do

- 使用 `#171717` / `#0f0f0f` 作为深色背景基线
- 克制使用 `#3ecf8e` / `#00c573`
- 让 `400` 成为默认字重
- 保持 Hero `1.00` 左右的低行高
- 用边框和透明层建立深度
- 让主 CTA、Tabs、状态标签保持 pill 形态
- 用 monospace 技术标签制造控制台感
- 让页面先给出摘要、结论和建议，再允许下钻

### Don’t

- 不要把绿色铺成大面积背景
- 不要依赖 box-shadow 制造层级
- 不要用 `700` 粗体建立主要层级
- 不要把多个核心流程堆回一个长页面
- 不要让首页退化成静态介绍页或开发文档复制品
- 不要让卡片套卡片形成厚重管理后台感
- 不要让默认 Element Plus 亮色风格泄漏到核心页面

## 8. Desktop-Only Scope

### Current Scope

- 本文件当前唯一强制覆盖范围是桌面端 Web。
- 本阶段不定义移动端版式、断点、折叠菜单或小屏交互。
- 所有验收以桌面端显示与操作为准。

### Deferred Source Reference

用户原始要求中包含以下响应式参考信息，现完整保留为后续阶段输入，但不作为本轮强制实现项：

- 原始建议断点：
  - Mobile：`<600px`
  - Desktop：`>600px`
- 原始建议变化：
  - Hero 缩小
  - Grid 由多列转单列
  - 导航收敛为 hamburger
  - Section spacing 缩小
  - 按钮纵向堆叠

如未来进入移动端阶段，应以本节为来源重新制定专门的 mobile spec，而不是直接套用桌面布局。

## 9. Dashboard Information Architecture Mapping

### 9.1 Product Role

- `/dashboard` 是 SQLForge 的运营总览首页。
- 首页不替代五大主功能页面：
  - SQL 查询
  - 解析记录
  - 压测报告
  - 加速配置
  - 系统管理
- 首页职责是：
  - 汇总状态
  - 提供结论
  - 提供建议
  - 分发进入各主功能页

### 9.2 Required Sections

#### Top Status Header

- 工作区
- 页面标题
- 页面摘要
- 全局操作

#### Hero Overview

- 高密度标题
- 技术标签
- 平台摘要
- 一个主 CTA
- 一个次 CTA

#### KPI Metrics

- 总查询量
- 解析成功率
- 压测通过率
- 加速命中率
- 审计/异常事件数

#### Quick Entries

- SQL 查询
- 解析记录
- 压测报告
- 加速配置
- 系统管理

#### Health & Risk

- 服务健康
- 规则一致性
- 运行异常
- 待处理风险

#### Recent Activity

- 最近查询
- 最近解析
- 最近压测
- 最近加速动作
- 最近系统变更/审计事件

#### Recommended Next Steps

- 提示需要立即处理的事项
- 给出跳转动作

## 10. Acceptance Checklist

### 10.1 Visual

- 页面主背景已切换到 `#171717` / `#0f0f0f` 深色体系
- 绿色只用于品牌和强调，不用于大面积背景
- 页面层次主要通过边框和表面色差建立
- Hero 标题具备控制台式高密度排版
- 主 CTA、状态标签、Tabs 使用 pill 形态

### 10.2 Information Architecture

- `/dashboard` 不再是占位页
- 五大主功能入口完整且清晰可达
- 首页同时包含摘要、状态、建议、最近活动
- 首页不承载五大主流程的完整操作表单

### 10.3 Engineering

- 主题 token 已在样式文件中落地
- 页面、组件、文档使用同一组命名和语义
- `npm run build` 通过
- `npm run lint` 通过
- 设计文档已纳入 `docs/README.md`

## 11. Reference Preservation

以下源要求已完整保留并项目化映射：

- Dark-mode-native 近黑背景
- 绿色品牌强调
- Circular 风格几何 sans 气质
- Source Code Pro 风格 monospace 仪式感
- HSL / alpha 分层系统
- 主按钮 pill + 次级 6px/8px/16px 圆角层次
- 极低阴影、边框主导深度
- Hero `1.00` line-height
- 以字号而不是粗体建立层级
- 以“开发者工具进化为精致工作台”为气质目标

后续新增页面默认继承本设计系统，除非有新的 ADR 或人类明确批准覆盖。

## 12. Management Console Page Engineering Patterns

本节约束 SQLForge 的 CRUD、配置、历史、任务、审计、数据源、权限、报表接口等管理后台页面。Dashboard、首页总览和 KPI 摘要仍按前文 Dashboard 信息架构执行；管理后台主路径默认不以卡片堆叠承载主要功能。

### 12.1 Default Page Skeleton

- 顶部：搜索 / 筛选区，默认使用 `SearchForm.vue` 或同等共享组件承载。
- 主体：操作按钮区 + `<el-table>` 或列表区，新增、编辑、删除、导入、导出、批量操作必须与表格状态清晰关联。
- 底部：分页、批量处理状态、查询结果数量、最近刷新时间或其他状态栏。
- 弹窗 / 抽屉：只承载新增、编辑、详情、确认、错误处理和辅助配置；不得把完整主流程藏入弹窗。

### 12.2 SearchForm.vue

- 搜索项必须优先通过字段定义数组数据驱动渲染，而不是在每个页面重复手写一组表单模板。
- 页面使用 `searchForm` 保存筛选值，并把筛选状态与分页、排序状态分离。
- 点击搜索时必须先重置 `pageInfo.currentPage`，再刷新表格数据。
- 点击重置时必须清理筛选值、重置分页和排序，并重新获取数据或回到页面定义的空筛选状态。
- 搜索项候选值必须来自后端接口、已验证静态枚举或共享字典模块；不得用前端临时枚举替代后端契约。

### 12.3 Table Columns

- 管理列表统一优先使用 `<el-table>`，列定义通过数组驱动 `v-for` 渲染。
- 列定义应包含 `prop`、`labelKey`、宽度 / 最小宽度、对齐、formatter、sortable、slotName 和可见性等必要元数据。
- 表格排序使用 Element Plus `sortable` / `@sort-change`，并与 `pageInfo` / sort state 绑定后重新获取数据。
- 复杂展示必须通过自定义插槽承载，例如状态标签、进度条、SQL 摘要、风险等级、批量操作状态和行操作按钮。
- 行操作按钮只做体验层展示或禁用；最终权限、状态机和业务校验以服务端结果为准。

### 12.4 Pagination And State Bar

- 分页统一绑定 `pageInfo` 响应式对象，至少包含 `currentPage`、`pageSize`、`total`。
- `current-change` 和 `size-change` 必须触发重新获取数据；`size-change` 默认重置到第一页。
- 表格 loading、空状态、错误状态和刷新状态必须独立表达，不得只依靠表格空数组隐式说明。
- 批量操作、导出和上传任务应在分页附近或状态栏展示进度与结果摘要。

### 12.5 EditDialog.vue

- 新增 / 编辑弹窗默认使用 `EditDialog.vue` 或同等共享组件，通过 `v-model` 控制 `el-dialog` 显隐。
- 弹窗表单数据必须通过明确的 model / payload 传递，避免直接编辑列表行对象造成取消后状态污染。
- 弹窗内部使用 `<el-form>` 与 Element Plus 表单验证，提交前保持字段格式与后端契约一致。
- 保存成功后由页面刷新列表或局部更新数据；失败时展示后端错误，不得在前端伪造成功状态。

### 12.6 useDict

- 字典翻译使用独立 `useDict` 模块或同等 composable，负责 API 获取、全局缓存、过期刷新和 `(dictType, code) -> label` 翻译。
- 字典 label 只用于展示；提交值必须继续使用后端契约规定的 code / enum 值。
- 字典加载失败时页面应保留 code 可见，并提供错误或降级提示，不得展示误导性默认 label。

### 12.7 Progress Feedback

- 上传、导出、批量处理、异步刷新和长耗时操作必须使用 `el-progress`、`nprogress` 或共享进度组件。
- 进度反馈应区分进行中、成功、失败、部分成功和取消状态。
- 进度状态必须来自接口、轮询结果或可验证的本地任务状态，不得用固定动画冒充完成。

### 12.8 Tabs And Split Layouts

- 信息量大时优先使用 `<el-tabs>` 分组，避免把多维统计、详情、证据、日志和配置堆在单个长页面中。
- 需要同时查看列表和详情时，优先使用左右分区、上下分区或抽屉详情。
- Tab 内容必须共享同一任务上下文、筛选条件或详情对象；无关主流程应拆到独立页面。

### 12.9 API And State Layering

- API 放在 services、api module 或 composable；页面只编排查询、提交、刷新和状态切换。
- 页面状态必须拆分为搜索表单、分页排序、表格数据、弹窗表单、loading / error / empty / success。
- 权限能力、候选值、字典、跨页面缓存和当前租户上下文必须来自后端或共享状态模块，不得在页面中各自复制一份事实源。
- 国际化 key 必须覆盖页面标题、操作按钮、表格列、表单 label、提示、错误兜底和空状态文案。

## 13. Full Page Refactor Baseline

`HARN-106` 将全量页面扫描结论固化为后续重构基线。该基线不代表页面已经重构完成，只说明后续任务的拆分顺序、非实现边界和验收约束。

### 13.1 Priority Surfaces

- 第一优先级拆分超长页面：`src/views/parse-record/ParseRecordView.vue`、`src/views/parse-batch/ParseBatchCenterView.vue`、`src/views/optimization/AccelerationView.vue`。
- 第二优先级收敛大型业务页：`SystemView`、`SqlHistoryView`、`DashboardView`、`AuditForensicsView`、`AuditTroubleshootingView`、`RepairEvidenceView`。
- `App.vue`、`src/router/index.js` 与 `src/config/routePaths.mjs` 是壳层与路由元数据优先治理点；重构必须保持 path、legacy redirect、菜单可达性和生产隐藏 `delivery-progress` 语义不变。

### 13.2 Required Shared Shapes

- 页面 hero、section header、evidence panel、metric card、toolbar/filter shell 优先抽到共享组件或同等本地模式，避免每个页面重复复制样式和状态拼装。
- 业务页面状态必须拆分为筛选、分页排序、表格/列表数据、详情/弹层数据、loading/error/empty/success，不得继续把主流程状态全部堆在单个 SFC 顶层。
- 重复 trace lookup、timeline、queue、retry、datasource/config 表格与详情模式应收敛为 composable 或共享组件；抽象必须服务真实重复复杂度，不能引入跨业务的杂项组件。

### 13.3 Non-Implementation Boundaries

- 不更换 Vue SFC + JavaScript + Element Plus + 自研组件栈。
- 不改变后端 API、SQL 执行 payload、parser payload、历史查询、审计追溯、权限或持久化语义。
- SQL 输入输出继续以 `SqlEditorField` 与 `SqlCodeBlock` 为契约；raw SQL 展示面必须保留不自动格式化语义。
- 页面中文/英文文案应逐步迁入现有 i18n 文件；不得新增平行国际化系统或继续扩大 `isChinese ? ...` 本地三元文案债务。
