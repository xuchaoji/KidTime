# 📱 童心护航 (KidTime) - Android 开发与代码生成指南

本文档旨在为 AI 代码辅助工具（如 OpenCode, Cursor, GitHub Copilot 等）提供完整的系统架构说明及分阶段开发指令（Prompts）。请按照以下阶段依次将提示词输入给 AI，以确保生成的代码具备良好的上下文连贯性和架构一致性。

---

## 🏗️ 第一部分：系统架构与技术选型介绍

**产品定位**：KidTime 是一款“跨设备时间账本”与“计时仪表盘”应用。它不具备直接锁机功能，而是通过主动打卡计时、任务悬赏与透支惩罚机制，帮助儿童管理其他电子设备（如电视、平板）的使用时间。

**核心技术栈**：
*   **开发语言**：Java
*   **架构模式**：MVVM (Model-View-ViewModel)，遵循 Android 官方推荐的架构指南。
*   **本地数据持久化**：
    *   `Room Database`：用于管理复杂的结构化数据，如时间流水账单 (`TimeTransaction`) 和任务记录 (`TaskRecord`)。
    *   `SharedPreferences`：用于管理高频读取的全局状态，如家长 PIN 码、每日基础额度、当前余额、透支额度、最后一次结算日期。
*   **核心业务组件**：
    *   `Foreground Service`：前台服务，确保应用在后台或息屏状态下，计时器依然能够稳定运行并触发跨应用级别的警报。
    *   `BroadcastReceiver` / `LiveData`：用于 Service 与 UI 层之间的状态解耦与实时同步。
*   **UI 表现层**：
    *   AndroidX & Material Design 3。
    *   自定义 View (`Canvas` 绘制)，实现符合儿童认知习惯的环形色彩预警倒计时器。

---

## 🚀 第二部分：分阶段代码生成提示词 (Prompts)

请在开启新的 AI 对话会话后，**依次**复制以下 Prompt 发送给代码生成工具。

### 阶段 0：全局初始化声明 (Global Context)
建立 AI 的开发上下文和技术栈认知。

**【复制以下内容】**
> 你是一个资深的 Android 开发工程师。我们要从零开发一款名为 "KidTime" 的儿童设备时间管理 App。
> 核心机制是“时间银行打卡”：软件不锁机，仅作为计时仪表盘和跨设备时间账本。
> 技术栈要求：
> 1. 语言：Java
> 2. 架构：严格遵循 MVVM (Model-View-ViewModel)
> 3. 本地存储：Room Database (用于复杂流水) + SharedPreferences (用于简单状态)
> 4. 异步处理：ExecutorService / Handler / LiveData (尽量避免引入复杂的第三方 RxJava，保持原生和轻量)
> 5. UI组件：AndroidX, Material Components
>
> 请确认你已理解技术栈和项目目标，回复“收到，已准备好”，暂不需要生成任何代码。接下来我会分阶段给你发送具体模块的开发指令。

---

### 阶段 1：数据持久化层开发 (Data Layer)
构建基础的状态存储和数据库结构。

**【复制以下内容】**
> 请完成第一阶段【数据层】的开发，输出完整的 Java 代码：
>
> 1. 编写 `PrefsManager.java` (单例模式)，使用 SharedPreferences 存储以下状态，并提供 getter/setter：
     >    - PARENT_PIN (String, 默认 "1234")
>    - DAILY_ALLOWANCE (int, 基础额度分钟，默认 30)
>    - CURRENT_BALANCE (int, 当前可用分钟，默认 30)
>    - OVERDRAFT_DEBT (int, 透支欠款分钟，默认 0)
>    - LAST_RESET_DATE (String, 格式 YYYY-MM-DD，上次结算日期)
>
> 2. 编写 Room 数据库实体类 (Entities)：
     >    - `TimeTransaction`: 字段包括 id (主键自增), amount (int, 正负数), type (int枚举: 1-每日发放, 2-任务奖励, 3-使用消耗, 4-违规惩罚), timestamp (long), description (String)。
>    - `TaskRecord`: 字段包括 id (主键自增), title (String), rewardMinutes (int), status (int枚举: 0-待接取, 1-待审核, 2-已完成)。
>
> 3. 编写对应的 DAOs (`TransactionDao`, `TaskDao`)，提供基础的 CRUD 和查询 List<LiveData> 的方法。
>
> 4. 编写 `AppDatabase.java` 继承 RoomDatabase，实现单例模式。

---

### 阶段 2：核心业务逻辑与状态机 (Domain Logic)
处理时间扣除、透支结算、每日跨天重置的核心逻辑。

**【复制以下内容】**
> 请完成第二阶段【核心业务逻辑】的开发。编写一个名为 `TimeBankManager.java` 的业务类（需依赖前一步的 PrefsManager 和 Room Database），实现以下核心方法：
>
> 1. `void checkAndApplyDailyReset()`: 每日结算状态机。
     >    - 获取当前系统日期 (YYYY-MM-DD)。
>    - 对比 PrefsManager 中的 LAST_RESET_DATE。若跨天，则进行结算：
>    - 读取 DAILY_ALLOWANCE 和 OVERDRAFT_DEBT。
>    - 计算 actual_grant = DAILY_ALLOWANCE - OVERDRAFT_DEBT。
>    - 若 actual_grant > 0：CURRENT_BALANCE = actual_grant, OVERDRAFT_DEBT = 0。
>    - 若 actual_grant <= 0：CURRENT_BALANCE = 0, OVERDRAFT_DEBT = Math.abs(actual_grant)。
>    - 更新 LAST_RESET_DATE 为当前日期，并使用 ExecutorService 异步向数据库插入一条 type=1 (每日发放) 的 TimeTransaction。
>
> 2. `void applyPenalty(int penaltyMinutes, String reason)`: 惩罚/透支机制。
     >    - 计算 newBalance = CURRENT_BALANCE - penaltyMinutes。
>    - 若 newBalance >= 0，CURRENT_BALANCE = newBalance。
>    - 若 newBalance < 0，OVERDRAFT_DEBT += Math.abs(newBalance)，CURRENT_BALANCE = 0。
>    - 异步插入一条 type=4 (违规惩罚) 的 TimeTransaction，描述为入参 reason。
>    - 保存所有状态至 PrefsManager。
>
> 3. `void rewardTime(int rewardMinutes, String reason)`: 任务奖励机制。
     >    - 增加 CURRENT_BALANCE，保存至 PrefsManager。
>    - 异步插入一条 type=2 (任务奖励) 的流水记录。

---

### 阶段 3：后台计时服务 (Foreground Service)
实现脱离 UI 依然能稳定运行的倒计时引擎。

**【复制以下内容】**
> 请完成第三阶段【计时服务】的开发。我们需要一个前台服务来保证计时器在后台不被系统杀死。
>
> 请编写 `TimerForegroundService.java` (继承 Service)：
> 1. 在 `onCreate` 和 `onStartCommand` 中配置 Foreground Service。需创建一个 NotificationChannel 并显示一个持续更新的 Notification (标题为"KidTime正在计时中")。
> 2. 内部维护一个使用 `ScheduledExecutorService` 的定时器，每 60 秒触发一次执行逻辑：
     >    - 从 PrefsManager 读取 CURRENT_BALANCE。
>    - 如果 CURRENT_BALANCE > 0：将其减 1，并保存回 PrefsManager。通过 LocalBroadcastManager 发送包含剩余时间的广播给 UI 层更新进度。
>    - 如果 CURRENT_BALANCE == 0：
       >      a) 停止内部定时器。
       >      b) 发送时间耗尽的广播。
       >      c) 拉起一个全屏的告警 Activity (AlertActivity)，必须设置 `FLAG_ACTIVITY_NEW_TASK` 和 `FLAG_ACTIVITY_CLEAR_TOP` 以确保能覆盖当前屏幕。
> 3. 提供 public 的绑定方法或接收特定 Intent 的逻辑来停止定时器，并在 `onDestroy` 中清理资源，停止前台服务。

---

### 阶段 4：自定义可视化组件 (Custom View)
构建儿童友好的进度展示 UI。

**【复制以下内容】**
> 请完成第四阶段【UI组件】的开发。编写一个自定义的 View：`CircleTimerView.java`。
>
> 要求如下：
> 1. 继承 View。暴露两个对外的方法：`setMaxTime(int minutes)` 和 `setCurrentTime(int minutes)`，调用后需触发 `invalidate()`。
> 2. 使用 Canvas 和 Paint 绘制一个闭合的环形进度条 (类似能量环)。底色为浅灰色，进度色根据剩余时间动态变化。
> 3. 颜色预警逻辑（在 `onDraw` 中实现）：
     >    - 计算百分比 ratio = currentTime / (float)maxTime。
>    - ratio >= 0.5：进度条为绿色 (例如 #4CAF50)。
>    - 0.2 < ratio < 0.5：进度条为黄色 (例如 #FFEB3B)。
>    - ratio <= 0.2：进度条为红色 (例如 #F44336)。
> 4. 环形正中心使用 `drawText` 绘制 currentTime 的数字（要求字体大、粗体），并在数字正下方绘制“分钟”字样。
> 5. 请重写 `onMeasure` 保证其在布局中始终呈现为正方形。

---

### 阶段 5：ViewModel 与核心界面层 (UI Layer)
将业务逻辑、服务与 UI 进行绑定。

**【复制以下内容】**
> 请完成第五阶段【UI与视图模型】的开发，将前面的模块组合起来。
>
> 1. 编写 `ChildDashboardViewModel.java` (继承 ViewModel)：
     >    - 提供 LiveData 观察 CURRENT_BALANCE 和 OVERDRAFT_DEBT 的变化。
>    - 封装调用启动/停止 TimerForegroundService 的意图逻辑。
>
> 2. 编写 `ChildDashboardFragment.java` (儿童主界面)：
     >    - 布局中包含上一步实现的 `CircleTimerView`。
>    - 包含一个大型的 Toggle Button："开始使用" / "结束使用"。
>    - 包含一个顶部的警告 CardView：默认隐藏，当观察到 ViewModel 中的 OVERDRAFT_DEBT > 0 时显示，文本格式为："⚠️ 注意：明天将扣除 {债务} 分钟"。
>    - 注册 BroadcastReceiver 监听 TimerForegroundService 发出的每分钟滴答广播，实时更新 `CircleTimerView` 的当前时间。
>
> 3. 编写 `AlertActivity.java` (超时告警界面)：
     >    - 界面要求全屏，红色背景，中间显示巨大的警告图标和文字：“时间到！请立即关闭设备！”
>    - 在 `onCreate` 中使用 `MediaPlayer` 循环播放系统默认的 TYPE_ALARM 铃声。
>    - 提供一个“我已关闭电视/平板”的按钮，点击后停止 MediaPlayer 播放，并调用 `finish()` 退出该 Activity。