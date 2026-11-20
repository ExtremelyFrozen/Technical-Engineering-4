# 发版手册

从准备版本到 GitHub Release、Modrinth、CurseForge 的操作与已知坑。
只记事实与操作；待办在 `TODO.md`，更新日志写法在 `CHANGELOG_STYLE.md`，代码结构在 `ARCHITECTURE.md`。

## 一、发版流程

1. 按 `CHANGELOG_STYLE.md` 整理 `CHANGELOG_UNRELEASED.md` 的 `## [未发布]` 段，并跑完第五节自检。
2. 把该段搬到 `CHANGELOG.md` 的 `<!-- CHANGELOG_INSERT -->` 之后（所有已发布版本段之上），
   标题改成 `## [<版本>] - <YYYY-MM-DD>`；把 `CHANGELOG_UNRELEASED.md` 清回骨架。
3. 把 `gradle.properties` 的 `mod_version` 改成同一个版本号，提交并推送到 `1.21` 分支。
4. 在 GitHub 创建 Release：
   - tag 形如 `v4.1.0-1.21`，**必须包含 `1.21`**，否则 `publish-21` 不会运行；
   - 正文粘贴 `CHANGELOG.md` 的版本段；可以去掉 `## [...]` 标题，在 Release 标题里另写版本；
   - 不要在正文里依赖 GitHub 自动生成的 notes，自动 notes 的基线会被仓库历史带偏。
5. 点击 Publish release。`publish-on-release.yml` 触发，后续由 CI 完成构建与三平台发布。
6. 发布后按第五节核对。

## 二、CI 链路

| 工作流 / Job | 触发 | 做什么 |
|---|---|---|
| `publish-on-release.yml` | GitHub Release `released` | 生成 PR 标签 changelog，按 tag 是否含 `1.21` 调用 `publish.yml` |
| `meta` | 同上 | checkout tag，按 `.github/json/config.json` 生成分类 changelog（兜底） |
| `publish-21` | tag 含 `1.21` | 调用 `publish.yml`，传入 tag、分支线 `1.21`、Release 正文与兜底 changelog |
| `build` | `publish.yml` | `./gradlew assemble`，上传 `build/libs/*` 为 3 天 artifact |
| `upload-release-artifacts` | 同上 | 用 `softprops/action-gh-release` 把 jar 挂到 Release |
| `publish-modrinth` | `simulate == false` | `Kir-Antipov/mc-publish@v3.3.0`，项目 id `PS4p8SFk` |
| `publish-cf` | `simulate == false` | `Kira-NT/mc-publish@v3.3.1`（传 `environment` 而非 `curseforge-environment`），CurseForge 项目 id `1545758` |
| `bump-version-and-changelog` | `build` 与附件上传后 | 升 `mod_version`，按需补录 changelog，推分支并开 PR |

`simulate` 的判定在 `publish-on-release.yml`：

- Release 名称以 `simulate` 开头，或仓库 owner 不是 `ExtremelyFrozen`，都视为演练。
- 演练只跳过 Maven 发布、Modrinth、CurseForge；**GitHub Release 附件上传与版本 bump PR 仍会执行**。
  在真实 tag 上演练会留下 PR，开跑前确认。

## 三、仓库变量与密钥

| 类型 | 名称 | 用途 |
|---|---|---|
| secrets | `MODRINTH_TOKEN` | Modrinth 上传 |
| secrets | `CURSEFORGE_TOKEN` | CurseForge 上传 |
| secrets | `MAVEN_USER` / `MAVEN_PASSWORD` | `./gradlew publish` 读取；当前 `gradle/scripts/publishing.gradle` 未启用仓库，可不配置 |

`gradle/scripts/publishing.gradle` 里目前没有启用的 Maven 仓库或 publication，
`./gradlew publish` 实际是空跑。若以后接入 Maven 分发，再补仓库与凭据。

## 四、更新日志与版本号

- **发布正文应从 `CHANGELOG.md` 的版本段粘贴到 GitHub Release**；
  `publish.yml` 把 GitHub Release 正文送到 Modrinth 与 CurseForge，CI 不直接读取 `CHANGELOG.md` 作为发布正文。
- `publish.yml` 的 `Prepend to CHANGELOG` 只作为兜底：
  - 如果 `CHANGELOG.md` 已有 `## [<版本>]`，跳过，不重复补录；
  - 如果没有，把 PR 标签生成的说明插到 `<!-- CHANGELOG_INSERT -->` 标记之后。
  - 兜底说明会归一化为 `## [<版本>] - <日期>` 与「新增 / 修复 / 改进」小节；
    如果改了 `.github/json/config.json`，保持这个输出格式。
- 版本号取 `./gradlew -q printVersion`，即 `gradle.properties` 的 `mod_version`。
- bump job 把版本号最后一段加一，例如 `4.1.0 → 4.1.1`，然后开一个以 `1.21` 为 base 的 PR。
- bump job 的 `sed` 依赖 `gradle.properties` 里 `mod_version = <版本>` 的精确写法；
  改格式前先确认这一步还能匹配。

## 五、发布后核对

- Release 正文非空，且包含 `### 新增` 一类小节名；正文为空时 Modrinth 与 CurseForge 的 changelog 也会为空。
- Release 附件里有主 jar 与 sources jar；`fail_on_unmatched_files: true` 保证路径写错会硬失败。
- Modrinth 与 CurseForge 的版本号、游戏版本、加载器、Java 版本正确；
  workflow 里的 Modrinth 项目 id 是 `PS4p8SFk`，CurseForge 是 `1545758`。公开 API 对未发布/私有项目会 404，
  发布前用后台或带 token 的 API 核对；另有旧的 Technical Engineering 4 项目 id `tUaaXwLH`，不要混用。
- `CHANGELOG.md` 有且只有一段对应版本的归档；没有重复的自动生成段。
- 版本 bump PR 已创建，base 为 `1.21`，标签含 `ignore changelog`。
- 如果发布中断：Modrinth 已存在同版本时重跑会失败，CurseForge 可能重复上传。
  优先改版本号重发，不要在同一个版本上反复重跑。

## 六、已知坑

- **tag 必须含 `1.21`**：`publish-21` 的条件是 `contains(github.ref_name, '1.21')`。
- **不要删 `<!-- CHANGELOG_INSERT -->`**：兜底补录靠这个标记定位。删了之后自动补录会直接失败，
  而不是静默跳过。
- **`simulate` 不是完全只读**：见第二节，附件与 bump PR 仍会执行。
- **`meta` 是发布的前置，且用 tag 里的配置**：它用 PR 标签生成兜底 changelog；`publish-21` 以 `meta` 为前置，
  `meta` 失败时整条发布链不会启动。`.github/json/config.json` 的 `tag_resolver.filter` 目前排除了 `v*-1.21` 形式的 tag；
  发布前用一次 dry run 确认。修好后改版本号重新打 tag 发版；同一 Release Re-run 仍会用旧 tag 的 `config.json`。
- **版本 bump 的 `sed`**：没有 `g`，只替换每行的第一处匹配；若其他键出现同值，仍可能误伤。
  改文件前先看当前内容。
- **Actions 版本**：workflow 里仍是 `actions/checkout@v4`、`setup-java@v4`、`gradle/actions/setup-gradle@v4`。
  升级时先确认 Node 运行时与缓存参数没有行为变化，再一起改。
