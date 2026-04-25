需求：塑形并实现新的未实例化 Phase-D 正式任务 D-TASK-030，归属 D-STORY-005，承接 D-TASK-029 的 next step，聚焦 provider-authenticated object-storage operations，以及 governance-side batch retention / recovery orchestration。
输出物：代码 / 测试 / 文档
确认：用户已明确确认按 D-TASK-030 / D-STORY-005 塑形并实现。
硬约束：保持 LOCAL_FILE 为默认主路径；不得把 environment-backed object storage 写成仓库默认事实；不得引入未治理的 provider SDK 或明文凭据落仓；不得绕过现有鉴权、租户隔离、审计链和 task_audit/closeout。
实现方向：优先沿用现有 provider-backed HTTP contract 与 governance artifact operation surfaces，补强 provider-authenticated operations、batch retention execution、batch recovery orchestration、失败/回滚和追溯证据；不要求绑定特定云厂商。
