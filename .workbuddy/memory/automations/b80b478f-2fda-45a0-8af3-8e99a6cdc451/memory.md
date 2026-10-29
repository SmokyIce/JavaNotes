# 自动化记忆 · 每日编码任务

## 2026-10-28（首次执行）
- 任务口径：先查历史会话/文件找出未完成项并补完，再推进当天计划（`00-plan/today.md` 的时间盒）。
- 本次补完 4 项遗留：W02 空工程目录、W01 Day 1 验收清单未勾、W02 笔记空模板、题库泛型 +3。
- 推进 M1 Day 1：类型擦除 + PECS，产出 `w02-generics-stream/` 工程（2 个 Demo + 1 个故意报错文件 + 实测快照）。
- 同步文件：`W02` 笔记、`today.md`、`MILESTONES.md`（M1 进展表）。
- 下次执行建议起点：`00-plan/today.md` 的「下一个时间盒」= W02 Day 2 异常体系；
  每次开工先扫 `01-notes` 里 `- [ ]` 未勾项与 `02-code` 下空目录（`.gitkeep` 单独存在即为未开工）。
- 环境：JDK 17 在 `C:\Program Files\Java\jdk-17`；Bash 需先 export PATH；PowerShell 不回显 stdout。

## 2026-10-29（第二次执行）
- 补完 3 项遗留：① `ErasureProof` B2 段抛 `NoSuchMethodException`（`getMethod` 只找 public，改 `getDeclaredMethod`），
  C~F 四段此前从未跑过；② `compile-errors/GenericLimits2.java`（通配符 6 个错）；③ `day1/WildcardCapture.java` 捕获正例。
- 推进 M1 Day 2：异常体系，`day2/` 两个 Demo 共 10 段 + `day2-result.txt`；题库 15 → 17。
- 同步：`W02` 笔记（Day 1 遗留 ✅ + 第 8 小节 + Day 2 整节 + 参考要点 Q4/Q5）、`today.md`、`MILESTONES.md`、工程 `README.md`。
- 下次起点：`00-plan/today.md` 的「下一个时间盒」= W02 Day 3 Optional（三反模式 + map/flatMap 链）。
- 新增经验（写进 MEMORY.md）：`getMethod` 只认 public；抓 `capture of ?` 要 `-Xdiags:verbose`；
  `compile-errors/` 下注释必须英文（javac 回显源码行会让中文变 GBK 乱码）。
- 口径提醒：笔记「一句话总结」「核心问题」仍按约定留空给学员，只把答案写进文末「参考要点」。
