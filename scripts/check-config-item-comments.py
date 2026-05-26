#!/usr/bin/env python3
"""检查并可批量补齐配置文件配置项中文注释。"""

import argparse
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover - 依赖缺失时降级为跳过 YAML 语法检查。
    yaml = None

ROOT = Path(__file__).resolve().parents[1]

CONFIG_FILE_PATTERN = re.compile(
    r"(^|/)(Dockerfile|docker-compose.*\.ya?ml|pom\.xml|package(-lock)?\.json|vite\.config\.js|"
    r".*\.config\.(js|mjs|cjs)|.*\.ya?ml|.*\.properties|.*\.xml|.*\.toml|.*\.env|"
    r".*\.ini|.*\.conf|.*\.cfg|.*\.json5?)$"
)
EXCLUDED_PATTERN = re.compile(r"(^|/)(node_modules|dist|dist-portable|target|\.codex/state|\.codex/tmp)/")
GENERATED_MARKERS = ("配置项", "配置段", "配置列表项")
PLACEHOLDER_COMMENT_TEXT = "说明该配置的用途、默认值或运行影响"

YAML_PURPOSE_EFFECTS = {
    "services": ("声明当前 compose 文件管理的服务集合", "决定本地基础设施会创建哪些服务容器"),
    "image": ("指定容器启动使用的镜像版本", "镜像变化会改变组件版本、启动脚本和兼容性"),
    "container_name": ("固定容器名称，便于脚本和人工排查定位", "名称冲突会导致容器无法创建"),
    "restart": ("设置容器异常退出后的重启策略", "影响本地依赖服务的自动恢复行为"),
    "environment": ("向容器或服务进程注入运行环境变量", "变量变化会改变账号、监听地址、安全协议或组件行为"),
    "ports": ("声明宿主机到容器的端口映射", "端口变化会影响本机访问地址以及端口冲突风险"),
    "command": ("覆盖或追加服务启动命令", "命令变化会直接改变组件启动参数和运行特性"),
    "volumes": ("声明持久化卷或目录挂载", "挂载变化会影响数据保留、恢复和本地文件写入位置"),
    "healthcheck": ("定义容器健康探针", "探针失败会让 compose 标记服务不健康"),
    "test": ("定义健康检查实际执行的命令", "命令返回非零会触发健康检查失败"),
    "interval": ("设置健康检查执行间隔", "间隔越短发现故障越快但检查开销越高"),
    "timeout": ("设置单次健康检查超时时间", "超时过短会造成慢启动服务被误判失败"),
    "retries": ("设置健康检查失败重试次数", "次数越多越能容忍启动抖动但故障发现更慢"),
    "start_period": ("设置容器启动宽限时间", "宽限期内失败不会立即计入重试次数"),
    "profiles": ("声明 compose profile 分组", "未启用对应 profile 时服务不会默认启动"),
    "spring": ("承载 Spring 应用基础配置", "影响应用名称、激活环境、数据源和自动装配"),
    "application": ("定义 Spring 应用元信息", "影响服务注册名、日志标识和运行上下文"),
    "name": ("定义应用或组件名称", "影响日志、监控和服务识别"),
    "autoconfigure": ("配置 Spring Boot 自动装配策略", "排除项变化会影响测试上下文是否初始化外部依赖"),
    "exclude": ("列出需要排除的自动装配类", "排除后测试上下文不会启动对应数据库或 MyBatis 自动配置"),
    "profiles": ("定义 Spring 或 compose profile 配置", "影响加载哪个环境配置或启动哪些可选服务"),
    "active": ("选择默认激活的 Spring profile", "影响 dev、test、prod 配置覆盖顺序"),
    "datasource": ("配置服务使用的数据库连接", "影响持久化、查询和任务状态读写"),
    "driver-class-name": ("指定 JDBC 驱动类", "驱动不匹配会导致数据库连接初始化失败"),
    "url": ("指定数据库或外部服务连接地址", "地址错误会导致运行时连接失败"),
    "jdbc-url": ("指定 JDBC 连接地址", "地址错误会导致计划分析或数据库访问失败"),
    "username": ("指定连接使用的账号", "账号错误会导致认证失败"),
    "password": ("指定连接使用的密码或令牌", "为空或错误会导致认证失败，生产必须由环境变量注入"),
    "redis": ("配置 Redis 连接或功能开关", "影响缓存、队列或临时状态能力是否可用"),
    "host": ("指定网络主机名", "主机不可达会导致依赖连接失败"),
    "port": ("指定监听或连接端口", "端口变化会影响访问地址和端口冲突风险"),
    "cluster": ("配置集群模式连接信息", "节点配置错误会导致集群客户端不可用"),
    "nodes": ("列出集群节点地址", "节点缺失或错误会影响高可用连接"),
    "server": ("配置服务 HTTP 容器", "影响接口监听端口和容器行为"),
    "tomcat": ("配置内嵌 Tomcat 参数", "影响请求体处理和连接容错"),
    "management": ("配置 Actuator 观测端点", "影响 health、metrics、prometheus 等运维入口暴露"),
    "endpoints": ("配置管理端点集合", "影响可通过 HTTP 访问的运维能力范围"),
    "web": ("配置 Web 方式暴露管理端点", "影响管理端点的 HTTP 暴露"),
    "exposure": ("配置端点暴露清单", "暴露范围过大会增加运维接口风险"),
    "include": ("列出允许暴露的端点或配置项", "决定对应能力是否可被调用或加载"),
    "endpoint": ("配置单个管理端点行为", "影响该端点返回内容和可见性"),
    "health": ("配置健康检查端点", "影响服务健康信息展示深度"),
    "show-details": ("控制健康详情展示策略", "展示过多会增加环境信息暴露面"),
    "logging": ("配置日志输出与级别", "影响排障信息量、日志体积和生产可观测性"),
    "level": ("配置包或根日志级别", "级别越低日志越详细且开销越高"),
    "root": ("配置根日志级别", "影响未单独指定包名的日志输出"),
    "config": ("指定外部日志配置文件", "路径错误会导致日志配置回退或启动失败"),
    "mybatis": ("配置 MyBatis 运行参数", "影响 mapper XML 加载和字段映射"),
    "config-location": ("指定 MyBatis 全局配置文件位置", "路径错误会导致 MyBatis 初始化失败"),
    "mapper-locations": ("指定 MyBatis mapper XML 扫描路径", "路径错误会导致 SQL mapper 不可用"),
    "auth": ("配置请求鉴权开关和可信来源", "影响受保护接口是否校验身份上下文"),
    "trusted-auth-sources": ("列出允许透传身份的来源", "来源不匹配会导致请求被拒绝或绕过测试假设"),
    "enabled": ("控制上级功能是否启用", "关闭后上级功能不会参与运行路径"),
    "mode": ("选择上级模块的运行模式", "模式变化会切换实现路径和外部依赖"),
    "sqlforge": ("承载 SQLForge 公共配置", "影响安全、对象存储和治理公共能力"),
    "security": ("承载安全相关配置", "影响敏感字段加密和认证安全边界"),
    "crypto": ("配置敏感字段加密组件", "配置错误会导致密文无法解密或写入失败"),
    "algorithm": ("指定加密算法标识", "算法不匹配会导致加解密不兼容"),
    "key-id": ("指定当前密钥标识", "影响密文元数据和密钥轮换追溯"),
    "base64-key": ("提供 Base64 编码的加密密钥", "为空会使加密组件无法进行真实加解密"),
    "governance": ("配置治理服务或治理客户端能力", "影响租户校验、审计写入和跨服务治理调用"),
    "query-execution": ("配置查询执行服务或客户端能力", "影响查询路由、Hetu 接入和加速命中"),
    "sql-optimization": ("配置 SQL 优化服务或客户端能力", "影响解析、推荐、改写和计划分析"),
    "benchmark-engine": ("配置压测引擎服务或客户端能力", "影响压测任务、报告和证据产物处理"),
    "base-url": ("指定内部服务调用基地址", "地址错误会导致跨服务调用失败"),
    "connect-timeout-ms": ("设置建立连接的超时时间", "过短会误判网络抖动，过长会拖慢失败返回"),
    "read-timeout-ms": ("设置读取响应的超时时间", "过短会中断慢请求，过长会占用调用线程"),
    "access-control": ("配置治理访问控制矩阵", "影响租户和数据源授权判定"),
    "deny-by-default": ("控制未命中授权时是否默认拒绝", "关闭会扩大未显式授权的访问面"),
    "audit-decisions": ("控制授权判定是否写入审计", "关闭会降低访问决策可追溯性"),
    "datasource-scopes": ("定义租户到数据源动作的授权范围", "配置缺失会导致对应租户无法访问目标数据源"),
    "state": ("标记授权或资源状态", "非 ACTIVE 会使对应能力不参与可用路径"),
    "actions": ("列出允许执行的动作集合", "动作缺失会导致对应操作被拒绝"),
    "route-order": ("定义 Hetu 多模式尝试顺序", "顺序越靠前越优先尝试该接入模式"),
    "allowed-modes": ("列出允许参与执行的 Hetu 模式", "不在列表中的模式不会进入路由"),
    "preferred-engines": ("定义查询路由优先引擎顺序", "顺序变化会影响默认选用的执行引擎"),
    "source": ("指定 Hetu client 查询来源标识", "会进入引擎会话来源，便于排查 SQLForge 请求"),
    "max-pages": ("限制 Hetu client 拉取结果页数", "数值越大可读取更多结果但内存和响应时间更高"),
    "type": ("指定上级配置项类型", "类型变化会影响表单校验、缓存后端或参数解析"),
    "retries": ("设置重试次数", "次数过低会更快失败，次数过高会延迟故障暴露"),
    "on": ("定义 GitHub Actions workflow 的触发入口", "触发条件变化会改变 CI 或门禁执行时机"),
    "push": ("定义代码推送触发条件", "分支或 tag 匹配变化会影响推送后是否运行 workflow"),
    "pull_request": ("定义拉取请求触发条件", "配置存在时 PR 会触发对应 CI 检查"),
    "workflow_dispatch": ("启用人工手动触发入口", "缺失时无法在 GitHub 页面手动运行该 workflow"),
    "release": ("定义 GitHub release 事件触发条件", "事件类型变化会影响发布后门禁是否执行"),
    "branches": ("列出允许触发 workflow 的分支", "列表变化会改变哪些分支推送会运行 CI"),
    "tags": ("列出允许触发 workflow 的 tag 模式", "模式变化会改变哪些 tag 推送会运行发布门禁"),
    "types": ("列出允许触发 workflow 的事件类型", "事件类型变化会改变 release 等事件的触发范围"),
    "jobs": ("定义 workflow 内的作业集合", "作业集合变化会改变 CI 或门禁实际执行内容"),
    "runs-on": ("指定作业运行的 GitHub runner 镜像", "runner 变化会影响系统依赖、工具版本和执行兼容性"),
    "env": ("定义作业或步骤环境变量", "变量变化会影响脚本读取的外部配置和密钥注入"),
    "steps": ("定义作业内按顺序执行的步骤", "步骤顺序或内容变化会影响门禁覆盖范围"),
    "uses": ("引用复用的 GitHub Action", "版本变化会影响检出、环境安装或产物上传行为"),
    "with": ("向当前 Action 传入参数", "参数变化会影响该 Action 的执行方式"),
    "distribution": ("指定 JDK 发行版", "发行版变化会影响 Java 工具链获取来源"),
    "java-version": ("指定 CI 使用的 JDK 8u112 版本", "版本变化会破坏仓库固定 JDK 8u112 基线"),
    "node-version": ("指定 CI 使用的 Node.js 版本", "版本变化会影响前端依赖安装和构建一致性"),
    "run": ("定义当前步骤执行的 shell 命令", "命令失败会导致对应 CI 步骤失败"),
    "if": ("定义步骤执行条件表达式", "条件变化会影响步骤是否被跳过"),
    "path": ("指定产物上传或文件匹配路径", "路径错误会导致产物缺失或上传失败"),
    "if-no-files-found": ("指定上传路径为空时的处理策略", "设为 error 会让缺少产物直接失败"),
    "inputs": ("定义手动触发 workflow 时的人机输入项", "输入项变化会影响门禁参数选择"),
    "description": ("定义输入项在人机界面的说明", "说明不准确会导致操作者选择错误参数"),
    "required": ("控制输入项是否必填", "必填项缺失会阻止 workflow 启动"),
    "default": ("定义输入项默认值", "默认值变化会影响人工未改参数时的门禁路径"),
    "type": ("定义输入项类型", "类型变化会影响 GitHub 表单校验和可选值展示"),
    "options": ("列出 choice 输入项的可选值", "列表变化会改变操作者可选门禁范围"),
    "permissions": ("声明 workflow token 权限", "权限过大会扩大 CI 写边界，过小会导致步骤失败"),
    "contents": ("配置仓库内容读取或写入权限", "权限变化会影响 checkout、上传或 release 元数据访问"),
    "concurrency": ("配置 workflow 并发分组", "分组变化会影响重复发布门禁的排队和并发控制"),
    "group": ("定义并发控制分组键", "同组 workflow 会按并发策略互相约束"),
    "cancel-in-progress": ("控制同组新运行是否取消旧运行", "开启后可能终止仍在执行的发布门禁"),
    "gate": ("定义阶段门禁范围输入", "默认或选项变化会影响执行 entry、delivery、compliance 或 full 门禁"),
    "coverage_phase": ("定义覆盖率门禁阶段输入", "阶段变化会影响使用 phase0 还是 phase1plus 覆盖率阈值"),
    "require_sonar": ("控制是否强制执行 Sonar fallback", "开启后 Sonar 配置缺失会阻断门禁"),
    "run_real_kafka_gate": ("控制是否执行真实 Kafka runtime gate", "开启后需要可用 Kafka 环境才能通过对应门禁"),
}


def tracked_files():
    output = subprocess.check_output(["git", "ls-files"], cwd=str(ROOT), text=True)
    return [line.strip() for line in output.splitlines() if line.strip()]


def config_files():
    return [
        path for path in tracked_files()
        if CONFIG_FILE_PATTERN.search(path) and not EXCLUDED_PATTERN.search(path)
    ]


def file_kind(path):
    if path.endswith((".yml", ".yaml")):
        return "yaml"
    if path.endswith(".toml"):
        return "toml"
    if path.endswith((".properties", ".env", ".ini", ".conf", ".cfg")):
        return "kv"
    if path.endswith((".js", ".mjs", ".cjs")):
        return "js"
    if path.endswith(".json"):
        return "json"
    if path.endswith(".xml") or path.endswith("/pom.xml") or path == "pom.xml":
        return "xml"
    return "other"


def has_previous_chinese_comment(output_lines, comment_prefix, allow_placeholder=True):
    for previous in reversed(output_lines):
        stripped = previous.strip()
        if not stripped:
            continue
        has_generated_comment = stripped.startswith(comment_prefix) and any(marker in stripped for marker in GENERATED_MARKERS)
        if has_generated_comment and not allow_placeholder and PLACEHOLDER_COMMENT_TEXT in stripped:
            return False
        return has_generated_comment
    return False


def has_inline_chinese_comment(line, allow_placeholder=True):
    has_generated_comment = any(marker in line for marker in GENERATED_MARKERS)
    if has_generated_comment and not allow_placeholder and PLACEHOLDER_COMMENT_TEXT in line:
        return False
    return has_generated_comment


def strip_placeholder_inline_comment(line):
    return re.sub(
        r"\s+#\s*(?:配置项|配置段|配置列表项)\s+[^：]+：说明该配置的用途、默认值或运行影响。",
        "",
        line,
    )


def yaml_value_default(value_tail):
    if not value_tail:
        return "见其子项"
    env_match = re.fullmatch(r"\$\{([A-Za-z0-9_]+):(.*)\}", value_tail)
    if env_match:
        fallback = env_match.group(2) or "空值"
        return f"环境变量 {env_match.group(1)}，未设置时为 {fallback}"
    return value_tail


def yaml_comment(indent, label, name, line):
    if label == "配置列表项":
        value = line.strip()[1:].strip()
        return (
            f"{indent}# {label} {name}：用途：向上级列表加入 {value}；"
            f"默认值：当前列表项为 {value}；运行影响：列表顺序或成员变化会影响上级配置的匹配和执行顺序。"
        )
    value_tail = strip_placeholder_inline_comment(line).split(":", 1)[1].strip()
    purpose, effect = YAML_PURPOSE_EFFECTS.get(
        name,
        (
            f"定义上级配置下的 {name} 条目",
            f"{name} 的名称或取值变化会改变上级配置的匹配、加载或执行结果",
        ),
    )
    return (
        f"{indent}# {label} {name}：用途：{purpose}；"
        f"默认值：{yaml_value_default(value_tail)}；运行影响：{effect}。"
    )


def generic_comment_text(label, name, line, kind):
    stripped = line.strip()
    default_value = "见节点内容或属性" if kind == "xml" else "见当前配置值"
    if kind in {"toml", "kv", "js"} and any(separator in stripped for separator in (":", "=")):
        separator = "=" if "=" in stripped else ":"
        default_value = stripped.split(separator, 1)[1].strip().rstrip(",") or default_value
    return (
        f"{label} {name}：用途：定义 {name} 配置项；"
        f"默认值：{default_value}；运行影响：{name} 的取值变化会影响对应工具、运行时或构建流程的配置加载结果。"
    )


def yaml_candidates(lines):
    block_indent = None
    for index, line in enumerate(lines):
        stripped = line.strip()
        indent = len(line) - len(line.lstrip(" "))
        if block_indent is not None:
            if stripped and indent <= block_indent:
                block_indent = None
            else:
                continue
        if not stripped or stripped.startswith("#") or stripped in {"---", "..."}:
            continue
        key_match = re.match(r"^(\s*)(?:-\s*)?([A-Za-z0-9_.-]+):(?:\s|$)", line)
        list_match = re.match(r"^(\s*)-\s+(.+)$", line)
        if key_match:
            value_tail = line.split(":", 1)[1].strip()
            if value_tail in {"|", ">"} or value_tail.startswith("|") or value_tail.startswith(">"):
                block_indent = indent
            yield index, key_match.group(1), "配置项", key_match.group(2)
            continue
        if list_match and not list_match.group(2).lstrip().startswith("#"):
            yield index, list_match.group(1), "配置列表项", "列表值"


def annotate_yaml(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in yaml_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("#") and PLACEHOLDER_COMMENT_TEXT in stripped and any(marker in stripped for marker in GENERATED_MARKERS):
            continue
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line, allow_placeholder=False) and not has_previous_chinese_comment(output, "#", allow_placeholder=False):
            indent, label, name = candidate
            comment = yaml_comment(indent, label, name, line)
            if comment is not None:
                output.append(comment)
        output.append(strip_placeholder_inline_comment(line) if candidate else line)
    return output


def toml_candidates(lines):
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        table_match = re.match(r"^\s*\[+([A-Za-z0-9_.-]+)\]+\s*$", line)
        key_match = re.match(r"^(\s*)([A-Za-z0-9_.-]+)\s*=", line)
        if table_match:
            yield index, "", "配置段", table_match.group(1)
        elif key_match:
            yield index, key_match.group(1), "配置项", key_match.group(2)


def annotate_toml(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in toml_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "#"):
            indent, label, name = candidate
            output.append(f"{indent}# {generic_comment_text(label, name, line, 'toml')}")
        output.append(line)
    return output


def kv_candidates(lines):
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!", ";")):
            continue
        match = re.match(r"^(\s*)([A-Za-z0-9_.-]+)\s*[:=]", line)
        if match:
            yield index, match.group(1), "配置项", match.group(2)


def annotate_kv(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in kv_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "#"):
            indent, label, name = candidate
            output.append(f"{indent}# {generic_comment_text(label, name, line, 'kv')}")
        output.append(line)
    return output


def xml_candidates(lines):
    for index, line in enumerate(lines):
        stripped = line.strip()
        if (
            not stripped
            or stripped.startswith(("<!--", "<?", "</", "<!", "<!--"))
            or stripped.startswith("-->")
        ):
            continue
        match = re.match(r"^(\s*)<([A-Za-z_][A-Za-z0-9_.:-]*)\b", line)
        if match:
            descriptor = match.group(2)
            id_match = re.search(r'\b(id|name)="([^"]+)"', line)
            if id_match:
                descriptor += f" {id_match.group(1)}={id_match.group(2)}"
            yield index, match.group(1), "配置项", descriptor


def annotate_xml(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in xml_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "<!--"):
            indent, label, name = candidate
            output.append(f"{indent}<!-- {generic_comment_text(label, name, line, 'xml')} -->")
        output.append(line)
    return output


def js_candidates(lines):
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith(("//", "/*", "*")):
            continue
        match = re.match(r"^(\s*)([A-Za-z_$][A-Za-z0-9_$-]*|'[^']+'|\"[^\"]+\")\s*:", line)
        if match:
            yield index, match.group(1), "配置项", match.group(2).strip("'\"")


def annotate_js(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in js_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "//"):
            indent, label, name = candidate
            output.append(f"{indent}// {generic_comment_text(label, name, line, 'js')}")
        output.append(line)
    return output


ANNOTATORS = {
    "yaml": annotate_yaml,
    "toml": annotate_toml,
    "kv": annotate_kv,
    "xml": annotate_xml,
    "js": annotate_js,
}

CANDIDATES = {
    "yaml": yaml_candidates,
    "toml": toml_candidates,
    "kv": kv_candidates,
    "xml": xml_candidates,
    "js": js_candidates,
}


def missing_comments(kind, lines):
    missing = []
    candidate_indexes = {index for index, _, _, _ in CANDIDATES[kind](lines)}
    for index, line in enumerate(lines):
        if index not in candidate_indexes:
            continue
        comment_prefix = "<!--" if kind == "xml" else "//" if kind == "js" else "#"
        previous_lines = lines[:index]
        if has_inline_chinese_comment(line, allow_placeholder=kind != "yaml"):
            continue
        if not has_previous_chinese_comment(previous_lines, comment_prefix, allow_placeholder=kind != "yaml"):
            missing.append(index + 1)
    return missing


def main():
    parser = argparse.ArgumentParser(description="检查或补齐配置项中文注释。")
    parser.add_argument("--write", action="store_true", help="直接写回可注释配置文件。")
    parser.add_argument("--list", action="store_true", help="仅打印配置文件分类清单。")
    args = parser.parse_args()

    unsupported = []
    changed = []
    failures = []
    parse_failures = []
    categorized = {"yaml": [], "toml": [], "kv": [], "xml": [], "js": [], "json": [], "other": []}

    for relative_path in config_files():
        kind = file_kind(relative_path)
        categorized.setdefault(kind, []).append(relative_path)
        path = ROOT / relative_path
        if kind == "json":
            unsupported.append(relative_path)
            try:
                json.loads(path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as exc:
                parse_failures.append((relative_path, str(exc)))
            continue
        annotator = ANNOTATORS.get(kind)
        if annotator is None:
            continue
        original_text = path.read_text(encoding="utf-8")
        original_lines = original_text.splitlines()
        updated_lines = annotator(original_lines)
        updated_text = "\n".join(updated_lines) + ("\n" if original_text.endswith("\n") else "")
        if args.write and updated_text != original_text:
            path.write_text(updated_text, encoding="utf-8")
            changed.append(relative_path)
            check_lines = updated_lines
        else:
            check_lines = original_lines
        missing = missing_comments(kind, check_lines)
        if missing:
            failures.append((relative_path, missing[:10]))
        if kind == "yaml" and yaml is not None:
            try:
                list(yaml.safe_load_all("\n".join(check_lines)))
            except yaml.YAMLError as exc:
                parse_failures.append((relative_path, str(exc)))
        if kind == "xml":
            try:
                ET.fromstring("\n".join(check_lines))
            except ET.ParseError as exc:
                parse_failures.append((relative_path, str(exc)))

    if args.list:
        for kind in sorted(categorized):
            print(f"{kind}: {len(categorized[kind])}")
            for path in categorized[kind]:
                print(f"  {path}")

    if changed:
        print("已补充中文注释的配置文件:")
        for path in changed:
            print(f"- {path}")
    print(f"可原位注释配置文件数量: {sum(len(categorized[kind]) for kind in ANNOTATORS)}")
    print(f"标准 JSON 不支持原位注释，已保持语法不变数量: {len(unsupported)}")
    if failures:
        print("仍缺少中文注释的配置项:")
        for path, lines in failures:
            print(f"- {path}: 行 {', '.join(str(line) for line in lines)}")
        raise SystemExit(1)
    if parse_failures:
        print("配置文件语法解析失败:")
        for path, error in parse_failures:
            print(f"- {path}: {error}")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
