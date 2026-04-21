# Frontend Backend Separation Baseline

本文件记录当前 SQLForge 仓库按“前后端分离”要求完成的专项检测基线和本轮治理结果。

## 当前结构

- 前端工程位于仓库根目录，入口为 `package.json`、`vite.config.js`、`src/`。
- 后端工程位于 Maven 多模块目录，当前包括 `sqlforge-shared/`、`governance/` 与 `query-execution/`。
- 前端通过 Vite 代理访问 `http://localhost:8080`，未直接依赖后端实现类。

## 检测范围

- 目录边界：前端源码与后端源码是否混放。
- 构建边界：前端和后端是否可独立构建。
- 运行边界：前端是否仅通过 HTTP API 访问后端。
- 逻辑边界：前端是否承载后端权威逻辑或数据访问实现。
- 部署边界：后端是否内嵌前端业务源码作为运行前置。

## 本轮结果

- 已新增 `scripts/check-frontend-backend-separation.js` 作为专项检测脚本。
- 已将前后端分离检查接入 `Makefile lint` 和 CI。
- 当前基线未发现以下结构性违规：
  - 前端目录下混入 Java、Mapper、Repository、SQL 访问实现
  - 后端模块下混入 `package.json`、`vite.config.js`、Vue 源码
  - 后端资源目录内托管前端运行必需源码
- 当前仍需持续关注的点：
  - 仓库为 monorepo 形态，前后端虽同仓但必须继续保持独立构建和独立部署边界
  - 前端仅允许保留编排、输入、展示和体验型预校验，领域权威逻辑仍需留在后端

## 执行方式

- 本地执行：`node scripts/check-frontend-backend-separation.js`
- 集成执行：`make lint`
- 持续集成：GitHub Actions `CI`
