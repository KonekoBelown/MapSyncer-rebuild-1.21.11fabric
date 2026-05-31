# MapSyncer for Xaero World Map

## Xaero default map and migration

MapSyncer writes normal Xaero World Map sync data to `mw$default` instead of creating a second `mw$map` map from the server `worldId`. Players should keep using Xaero's default map for the whole server.

If an older client already has `mw$map` or another legacy `mw$*` folder, MapSyncer starts a background migration before the next map write. It copies only missing `.zip` region files into the matching dimension under `mw$default`, keeps existing default files, preserves `caves/<layer>`, and ignores `.part`, `.temp`, and non-zip files.

The migration runs on a client background thread and then uses the existing batched Xaero reload queue, so players do not need to relog after the merge. It never reads, writes, deletes, or overwrites `waypoints.txt`; private waypoints remain untouched. Old `mw$map` folders are kept for safety, and players can manually delete them under `xaero/world-map/.../` after confirming the merged default map looks correct.

把服务端已探索区域生成成 Xaero's World Map 可读取的缓存，并让客户端按需同步。

## 版本

- Minecraft / Fabric：`1.21.11`
- Fabric Loader：`0.19.2`
- Fabric API：`0.141.4+1.21.11`
- Java：`21`
- Mod：`1.0.1-fabric-1.21.11`

## 来源

本项目基于 [RuoChennn/MapSyncer-for-XaeroWorldmap](https://github.com/RuoChennn/MapSyncer-for-XaeroWorldmap) 改写。

- 原作者：RuoChennn
- 当前 `1.21.11` 适配版本：ShanHe_YF / ChatGPT 迁移调整

## 安装

服务端安装：

- Fabric Loader
- Fabric API
- MapSyncer

客户端安装：

- Fabric Loader
- Fabric API
- MapSyncer
- Xaero's World Map
- Voxy（可选，仅用于 Voxy 同步按钮）

服务端不需要安装 Xaero's World Map。

## 使用

图形界面：

```text
/mapsyncer gui                             # 打开 MapSyncer 图形操作面板
/mapsyncergui                             # 客户端兜底入口；如果 /mapsyncer 被服务端命令抢占，就用这个
```

玩家同步命令：

```text
/mapsyncer sync                            # 同步当前所在维度
/mapsyncer sync radius 1000                # 同步当前维度内指定半径附近的地图
/mapsyncer sync all                        # 同步所有已生成缓存的维度
/mapsyncer sync <dimension>                # 同步指定维度，例如 overworld 或 minecraft:the_nether
```

GUI 的“同步”页也提供半径同步按钮，适合大型服务器只下载出生点或当前活动区域附近的地图。

管理员命令：

```text
/mapsyncer generate                        # 生成所有维度的服务端地图缓存
/mapsyncer generate <dimension>            # 增量生成指定维度的地图缓存
/mapsyncer generate <dimension> force      # 强制重新生成指定维度缓存
/mapsyncer generate <dimension> <x> <z>    # 只生成指定 region 坐标的地图缓存
/mapsyncer incremental run                 # 立即执行一次增量刷新
/mapsyncer status                          # 查看生成任务、增量更新状态和缓存统计
/mapsyncer incremental off                 # 关闭自动增量更新
/mapsyncer incremental tick [interval]     # 按 Tick 间隔执行自动增量更新
/mapsyncer incremental scheduled [hour] [minute] # 按每天固定时间执行自动增量更新
```

## Voxy 同步

GUI 会自动检测客户端是否安装并启用 Voxy。只有客户端有 Voxy、服务端开启 `enableVoxySync`、且当前没有其他同步任务时，“同步 Voxy 当前维度”按钮才可点击。

**重要安全警告：** `enableVoxySync` 默认是 `false`。开启后，服务端会把当前维度的完整 MCA region 文件发给客户端，不会清洗 NBT；这可能暴露箱子内容、方块实体、实体、矿物和隐藏结构。只建议在信任玩家的建筑服或生电服开启。

Voxy 同步只同步玩家当前所在维度，并读取服务端最近一次已经落盘的存档；刚放置的方块可能需要等服务器自动保存后才会出现在 Voxy 中。

客户端 Voxy 增量缓存保存在：

```text
mapsyncer/voxy-sync-cache.json
```

## 配置

服务端配置：

```text
config/mapsyncer.json
```

客户端配置：

```text
config/mapsyncer-client.json
```

客户端 GUI 可修改：

- `autoSyncOnJoin`：进服自动同步，默认 `false`
- `showSyncHud`：显示同步 HUD，默认 `true`
- `syncProgressChatIntervalPercent`：聊天栏进度提示间隔，默认 `0`
- `autoSyncDelaySeconds`：自动同步延迟秒数，默认 `3`

服务端关键配置：

- `enableVoxySync`：是否允许 Voxy MCA 同步，默认 `false`
- `maxSyncPacketSize`：同步分包大小
- `syncSpeedLimitKBps`：每个玩家同步的固定最高速率；`0` 表示不设固定上限
- `enableAdaptiveSyncThrottle`：是否根据玩家 Ping 自动调节同步速率，默认 `true`
- `adaptivePingThresholdMs` / `adaptivePingRecoverMs`：触发降速和恢复的 Ping 阈值，默认 `200ms` / `150ms`
- `adaptiveThrottleAdjustCooldownMs`：自适应调速冷却时间，默认 `2000ms`
- `enableDirtyRegionTracking`：是否启用 DirtyRegion 精准增量，默认 `true`
- `enableRadiusSync`：是否允许半径同步，默认 `true`
- `maxRadiusSyncBlocks`：玩家可请求的最大半径，默认 `3000`
- `radiusSyncCenterMode`：半径同步中心，可为 `PLAYER_POSITION`、`WORLD_SPAWN`、`FIXED`
- `radiusSyncFixedDimension` / `radiusSyncFixedX/Y/Z`：固定中心模式使用的维度与坐标

同步性能：

- 服务端按 `maxSyncPacketSize` 分包，并按每玩家有效速率发送；固定上限来自 `syncSpeedLimitKBps`
- 自适应限速开启时，服务端会读取原版 KeepAlive 统计的玩家 Ping。Ping 超过阈值会降速，恢复稳定后再慢慢上调；同一玩家每次调速至少间隔 `adaptiveThrottleAdjustCooldownMs`，避免一次高 Ping 周期内连续降到最低
- 客户端会在后台写入地图文件、计算哈希和保存同步缓存；当前视距内的 Xaero 地图刷新会按 tick 节流，避免一次性刷新大量 region

服务端缓存目录：

```text
server_map_cache/
```

## 公共路标

公共路标由服务端配置文件维护：

```text
config/mapsyncer-public-waypoints.json
```

默认会生成示例配置但不启用。启用后，服务端会在玩家进服或开始地图同步时下发公共路标，也可以在 GUI 的“同步”页手动同步。客户端会尝试写入 Xaero 的 `waypoints.txt` 文本文件，写入格式为：

```text
waypoint:name:initial:x:y:z:color:disabled:type:set:dimension
```

MapSyncer 只管理配置中的公共组（默认 `ServerPublic`）：写入前会读取现有 `waypoints.txt`，删除该组旧行，再追加服务端下发的新行。玩家私人路标和其他组不会被修改。

公共路标同步会在客户端后台执行，并使用 hash 缓存跳过重复写入。合并 `waypoints.txt` 时采用逐行流式读写到临时文件，再原子替换，避免大文件一次性读入内存。如果 Xaero 正在写入路标导致文件被占用，MapSyncer 会放弃本次覆盖并在 GUI 中显示“文件被占用”，玩家稍后可手动重试。

公共路标依赖客户端存在可识别的 Xaero 路标目录，推荐同时安装/启用 Xaero Minimap 或 Xaero Waypoints。若客户端没有对应路标目录，MapSyncer 会跳过公共路标同步，普通地图同步不受影响。
