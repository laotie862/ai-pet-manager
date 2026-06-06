const fs=require("fs"),encode=require("plantuml-encoder").encode;
const http=require("https"),url=require("url");

const DIRS={
  srs:"C:\\Users\\YL\\Desktop\\宠物平台\\diagrams\\srs",
  odd:"C:\\Users\\YL\\Desktop\\宠物平台\\phases\\phase2-概要设计\\diagrams",
  dd:"C:\\Users\\YL\\Desktop\\宠物平台\\phases\\phase3-详细设计\\diagrams"
};
Object.values(DIRS).forEach(d=>{if(!fs.existsSync(d))fs.mkdirSync(d,{recursive:true});});

// Fetch PNG from PlantUML server
function fetchPlantUML(puml, outPath){
  return new Promise((resolve,reject)=>{
    let enc=encode(puml);
    let u=`https://www.plantuml.com/plantuml/png/${enc}`;
    let f=fs.createWriteStream(outPath);
    http.get(u,res=>{
      if(res.statusCode===200){res.pipe(f);f.on("finish",()=>resolve(outPath));}
      else{let d="";res.on("data",c=>d+=c);res.on("end",()=>reject(`HTTP ${res.statusCode}: ${d.substring(0,200)}`));}
    }).on("error",reject);
  });
}

// =============== SRS USE CASE DIAGRAMS (14) ===============
let srsDir=DIRS.srs;
let srsDiagrams=[
  ["D00_overview",`@startuml
skinparam rectangleBackgroundColor #F5F7FA
skinparam actorBackgroundColor #E3F2FD
skinparam usecaseBackgroundColor #E8F0FE
left to right direction
actor 游客 as g
actor "用户 " as u
actor VIP as v
actor 管理员 as a
rectangle "宠物管家" {
  (浏览功能) as uc1
  (AI监控) as uc2
  (健康管理) as uc3
  (趋势分析) as uc4
  (专家咨询) as uc5
  (后台管理) as uc6
}
g --> uc1
u --|> g
u --> uc2
u --> uc3
v --|> u
v --> uc4
v --> uc5
a --> uc6
@enduml`],
  ["D01_guest",`@startuml
left to right direction
actor 游客
rectangle "前台" {
  usecase "浏览功能介绍" as UC1
  usecase "查看示例报告" as UC2
  usecase "注册账号" as UC3
}
游客 --> UC1
游客 --> UC2
游客 --> UC3
@enduml`],
  ["D02_user",`@startuml
left to right direction
actor 用户
rectangle "用户管理" {
  usecase "登录系统" as UC1
  usecase "找回密码" as UC2
  usecase "修改资料" as UC3
  usecase "修改头像" as UC4
  usecase "升级VIP" as UC5
  usecase "绑定企业微信" as UC6
}
用户 --> UC1
用户 --> UC2
用户 --> UC3
用户 --> UC4
用户 --> UC5
用户 --> UC6
@enduml`],
  ["D03_pet",`@startuml
left to right direction
actor 用户
rectangle "宠物档案" {
  usecase "新增宠物" as UC1
  usecase "编辑信息" as UC2
  usecase "删除档案" as UC3
  usecase "查看详情" as UC4
}
用户 --> UC1
用户 --> UC2
用户 --> UC3
用户 --> UC4
@enduml`],
  ["D04_camera",`@startuml
left to right direction
actor 用户
rectangle "摄像头管理" {
  usecase "添加摄像头" as UC1
  usecase "配置监控区域" as UC2
  usecase "查看实时画面" as UC3
  usecase "移除摄像头" as UC4
}
用户 --> UC1
用户 --> UC2
用户 --> UC3
用户 --> UC4
@enduml`],
  ["D05_behavior",`@startuml
left to right direction
actor 用户
rectangle "AI行为分析" {
  usecase "实时行为状态" as UC1
  usecase "行为事件时间线" as UC2
  usecase "当日行为摘要" as UC3
  usecase "异常行为告警" as UC4
}
用户 --> UC1
用户 --> UC2
用户 --> UC3
用户 --> UC4
@enduml`],
  ["D06_health",`@startuml
left to right direction
actor 用户
rectangle "健康管理" {
  usecase "添加疫苗" as UC1
  usecase "添加驱虫" as UC2
  usecase "添加体检" as UC3
  usecase "添加用药" as UC4
  usecase "查看记录" as UC5
  usecase "编辑删除" as UC6
}
用户 --> UC1
用户 --> UC2
用户 --> UC3
用户 --> UC4
用户 --> UC5
用户 --> UC6
@enduml`],
  ["D07_reminder",`@startuml
left to right direction
actor 用户
actor "系统" as sys
rectangle "智能提醒" {
  usecase "每日报告推送" as UC1
  usecase "异常告警推送" as UC2
  usecase "健康到期提醒" as UC3
  usecase "生日提醒" as UC4
  usecase "管理提醒设置" as UC5
}
用户 --> UC1
用户 --> UC5
sys --> UC2
sys --> UC3
sys --> UC4
@enduml`],
  ["D08_feeding",`@startuml
left to right direction
actor 用户
rectangle "AI饲养建议" {
  usecase "通用饲养指南" as UC1
  usecase "每日AI饲养建议" as UC2
}
用户 --> UC1
用户 --> UC2
@enduml`],
  ["D09_news",`@startuml
left to right direction
actor 用户
rectangle "养宠资讯" {
  usecase "浏览推荐流" as UC1
  usecase "分类浏览" as UC2
  usecase "收藏资讯" as UC3
  usecase "搜索资讯" as UC4
  usecase "品种百科" as UC5
}
用户 --> UC1
用户 --> UC2
用户 --> UC3
用户 --> UC4
用户 --> UC5
@enduml`],
  ["D10_vip",`@startuml
left to right direction
actor VIP
rectangle "VIP增值服务" {
  usecase "趋势分析" as UC1
  usecase "AI健康预警" as UC2
  usecase "定制饲养计划" as UC3
  usecase "导出报告" as UC4
  usecase "视频云存储" as UC5
  usecase "多宠对比" as UC6
  usecase "自定义阈值" as UC7
  usecase "专家咨询" as UC8
  usecase "不限宠物" as UC9
  usecase "不限摄像头" as UC10
  usecase "续费管理" as UC11
}
VIP --> UC1
VIP --> UC2
VIP --> UC3
VIP --> UC4
VIP --> UC5
VIP --> UC6
VIP --> UC7
VIP --> UC8
@enduml`],
  ["D11_admin_user",`@startuml
left to right direction
actor 管理员
rectangle "后台-用户管理" {
  usecase "查询用户" as UC1
  usecase "封禁解封" as UC2
  usecase "调整权限" as UC3
  usecase "查看详情" as UC4
}
管理员 --> UC1
管理员 --> UC2
管理员 --> UC3
管理员 --> UC4
@enduml`],
  ["D12_admin_expert",`@startuml
left to right direction
actor 管理员
rectangle "后台-专家管理" {
  usecase "审核入驻" as UC1
  usecase "管理排班" as UC2
  usecase "查看咨询记录" as UC3
}
管理员 --> UC1
管理员 --> UC2
管理员 --> UC3
@enduml`],
  ["D13_admin_sys",`@startuml
left to right direction
actor 管理员
rectangle "后台-系统统计" {
  usecase "AI模型配置" as UC1
  usecase "告警阈值管理" as UC2
  usecase "系统参数" as UC3
  usecase "运营数据" as UC4
  usecase "导出报表" as UC5
  usecase "操作日志" as UC6
}
管理员 --> UC1
管理员 --> UC2
管理员 --> UC3
管理员 --> UC4
管理员 --> UC5
管理员 --> UC6
@enduml`],
];

// =============== ODD DIAGRAMS (6) ===============
let oddDir=DIRS.odd;
let oddDiagrams=[
  ["D01_arch",`@startuml
!define RECTANGLE class
skinparam rectangleBackgroundColor #E3F2FD
skinparam databaseBackgroundColor #E8F5E9
skinparam nodeBackgroundColor #FFF3E0
title 系统架构图
node "接入层" {
  rectangle "Flutter App" as app
  rectangle "Vue3 Admin" as admin
  rectangle "企业微信" as wc
  rectangle "H5 WebView" as h5
}
node "网关层" {
  rectangle "Nginx HTTPS/WSS" as nginx
}
node "服务层" {
  rectangle "Spring Boot API" as sb
  rectangle "Python CV" as cv
  rectangle "XXL-JOB" as job
}
node "数据层" {
  database "PostgreSQL" as pg
  database "Redis" as redis
  database "MinIO" as minio
  database "Kafka" as kafka
}
cloud "外部服务" {
  rectangle "FCM/APNs" as push
  rectangle "企业微信API" as wecom
  rectangle "LLM API" as llm
  rectangle "支付网关" as pay
}
app -down-> nginx
admin -down-> nginx
wc -down-> nginx
h5 -down-> nginx
nginx -down-> sb
sb -right-> cv: "HTTP"
sb -down-> pg
sb -down-> redis
sb -down-> minio
sb -down-> kafka
sb .right.> push
sb .right.> wecom
sb .right.> llm
sb .right.> pay
@enduml`],
  ["D02_er",`@startuml
entity t_user {
  * id <<PK>>
  phone
  password
  nickname
  vip_status
  wecom_user_id
  push_token
}
entity t_pet {
  * id <<PK>>
  * user_id <<FK>>
  name
  breed
  gender
  weight_kg
}
entity t_device {
  * id <<PK>>
  * pet_id <<FK>>
  name
  rtsp_url
  status
}
entity t_behavior_event {
  * id <<PK>>
  * device_id <<FK>>
  * pet_id <<FK>>
  behavior
  confidence
  start_time
  video_url
}
entity t_behavior_summary {
  * id <<PK>>
  * pet_id <<FK>>
  date
  eat_count
  drink_count
  exercise_min
  sleep_min
  defecate_count
}
entity t_alert {
  * id <<PK>>
  * pet_id <<FK>>
  alert_type
  push_status
}
entity t_health_record {
  * id <<PK>>
  * pet_id <<FK>>
  record_type
  content <<JSONB>>
}
entity t_vip_order {
  * id <<PK>>
  * user_id <<FK>>
  plan_type
  amount
  pay_status
}
entity t_expert {
  * id <<PK>>
  name
  title
  status
  rating
}
entity t_consultation {
  * id <<PK>>
  * user_id <<FK>>
  * expert_id <<FK>>
  status
  fee
}
t_user ||--o{ t_pet
t_user ||--o{ t_vip_order
t_user ||--o{ t_consultation
t_pet ||--o{ t_device
t_pet ||--o{ t_behavior_event
t_pet ||--o{ t_behavior_summary
t_pet ||--o{ t_health_record
t_pet ||--o{ t_alert
t_device ||--o{ t_behavior_event
t_expert ||--o{ t_consultation
@enduml`],
  ["D03_module",`@startuml
skinparam componentStyle rectangle
[Spring Boot API] as sb
package "核心服务" {
  [auth-service] as auth
  [user-service] as user
  [pet-service] as pet
  [device-service] as dev
  [behavior-service] as beh
  [health-service] as health
  [reminder-service] as rem
  [feeding-service] as feed
  [trend-service] as trend
  [expert-service] as exp
  [news-service] as news
  [vip-service] as vip
  [notification-service] as notif
  [admin-service] as adm
}
sb .down.> auth
sb .down.> user
sb .down.> pet
sb .down.> dev
sb .down.> beh
sb .down.> health
sb .down.> rem
sb .down.> feed
sb .down.> trend
sb .down.> exp
sb .down.> news
sb .down.> vip
sb .down.> notif
sb .down.> adm
beh -[hidden]right-> health
health -[hidden]right-> rem
@enduml`],
  ["D04_deploy",`@startuml
title 部署拓扑图
node "应用服务器×2" {
  [Nginx :443] as nginx1
  [Spring Boot :8080] as sb1
  [Spring Boot :8081] as sb2
}
node "数据服务器" {
  database "PostgreSQL :5432" as pg
  database "Redis :6379" as redis
  database "MinIO :9000" as minio
  [Kafka] as kafka
}
node "GPU推理机" {
  [Python CV :9001] as cv
  [FFmpeg进程池] as ffmpeg
}
cloud "外部服务" {
  [FCM/APNs] as push
  [企业微信API] as wecom
  [LLM API] as llm
}
nginx1 -down-> sb1
nginx1 -down-> sb2
sb1 -down-> pg
sb1 -down-> redis
sb1 -down-> minio
sb1 -down-> kafka
sb1 -up-> cv: "HTTP"
sb1 .right.> push
sb1 .right.> wecom
@enduml`],
  ["D05_flow",`@startuml
title 核心处理流程
|摄像头|
start
:RTSP推流;
|FFmpeg|
:每5秒抽帧;
|CV服务|
:YOLO检测宠物;
:行为分类;
|Kafka|
:发布行为事件;
|BehaviorService|
:消费事件;
:滑动窗口判异;
if (异常?) then (是)
  :生成告警;
  |NotificationService|
  :App推送;
  if (5分未读?) then (是)
    :企微追加;
    if (15分未读VIP?) then (是)
      :短信兜底;
    endif
  endif
else (否)
  :事件聚合;
  :写入事件表;
  :更新每日摘要;
endif
|XXL-JOB|
:22:00触发;
:调LLM生成报告;
|NotificationService|
:推送日报;
stop
@enduml`],
  ["D06_interface",`@startuml
title 系统接口关系
[Flutter App] as app
[Vue3 Admin] as admin
[企业微信] as wc
[Spring Boot] as sb
[Python CV :9001] as cv
[PostgreSQL] as pg
[Redis] as redis
[MinIO] as minio
[Kafka] as kafka
[LLM API] as llm
[FCM/APNs] as push
[企业微信API] as wecom
app -down-> sb: "REST + WS"
admin -down-> sb: "REST"
wc -down-> sb: "HTTP"
sb -up-> cv: "HTTP JSON"
sb -down-> pg: "JDBC"
sb -down-> redis: "Lettuce"
sb -down-> minio: "S3 SDK"
sb -down-> kafka: "Stream"
sb .right.> push: "HTTP"
sb .right.> wecom: "HTTP"
sb .right.> llm: "HTTP"
@enduml`],
];

// =============== SDDD DIAGRAMS (7) ===============
let ddDir=DIRS.dd;
let ddDiagrams=[
  ["D01_seq_login",`@startuml
title 用户登录 时序图
actor App
participant "Spring Boot" as SB
database PostgreSQL as DB
database Redis
App -> SB: POST /api/auth/login\\n{phone,password}
SB -> DB: SELECT user WHERE phone=?
DB --> SB: user record
SB -> SB: BCrypt校验
SB -> SB: 生成JWT
SB -> Redis: SET token
Redis --> SB: OK
SB --> App: 200 {accessToken,refreshToken}
note right: 备选：密码错误→401\\n用户冻结→403
@enduml`],
  ["D02_seq_alert",`@startuml
title 摄像头→告警推送 时序图
actor 摄像头
participant FFmpeg
participant "CV服务" as CV
participant Kafka
participant "BehaviorService" as BS
participant "NotificationService" as NS
actor 用户App
摄像头 -> FFmpeg: RTSP流
FFmpeg -> CV: Base64 JPEG
CV -> Kafka: 行为事件JSON
Kafka -> BS: 消费事件
BS -> BS: 滑动窗口判异
BS -> NS: 30min无进食→告警
NS -> 用户App: FCM推送
NS -> NS: 5分未读→企微追发
note right: VIP 15分→短信
BS -> BS: 写事件表+更新摘要
@enduml`],
  ["D03_seq_report",`@startuml
title 每日报告生成 时序图
actor "XXL-JOB" as Job
participant "BehaviorService" as BS
participant "VipService" as VS
participant "LLM API" as LLM
participant "NotificationService" as NS
actor 用户
Job -> BS: 22:00触发
BS -> BS: 查当日摘要+昨日对比
BS -> VS: isVip(userId)?
VS --> BS: true/false
BS -> LLM: 组装Prompt\\n(VIP增强版/普通版)
LLM --> BS: {summary,highlights,suggestion}
BS -> BS: 写t_daily_report
BS -> NS: 推送内容
NS -> 用户: 企微+App
@enduml`],
  ["D04_seq_pay",`@startuml
title VIP支付 时序图
actor App
participant "VipService" as VS
participant "微信支付" as Pay
participant "UserService" as US
participant NotificationService as NS
App -> VS: POST /vip/orders
VS -> Pay: 统一下单
Pay --> VS: 预付单参数
VS --> App: 支付参数
App -> App: 调起支付
Pay -> VS: POST /callback\\n支付结果
VS -> VS: 验签+幂等
VS -> US: PUT vip_status=active
US --> VS: OK
VS -> NS: 推送升级通知
NS --> App: 恭喜升级
@enduml`],
  ["D05_seq_expert",`@startuml
title 专家咨询 时序图
actor 用户App
participant "ExpertService" as ES
participant WebSocketHub as WS
actor 专家端
actor 数据库 as DB
用户App -> ES: POST /consultations
ES -> WS: 通知专家
WS -> 专家端: 推送接单
专家端 -> WS: 确认接单
WS -> ES: 已接单
== WebSocket消息通道 ==
用户App -> WS: 文字/图片消息
WS -> 专家端: 转发
专家端 -> WS: 回复
WS -> 用户App: 转发
用户App -> WS: WebRTC Offer
WS -> 专家端: 转发
专家端 -> WS: Answer
用户App -> ES: 结束咨询
ES -> ES: 计算费用
ES -> DB: 写状态+费用
ES --> 用户App: 费用+评价入口
@enduml`],
  ["D06_state_behavior",`@startuml
title 行为分析状态机
[*] --> 等待帧
等待帧 --> CV推理: 新帧到达
CV推理 --> 事件聚合: 行为JSON
事件聚合 --> 异常判定: 事件结束
异常判定 --> 推送告警: 触发告警
异常判定 --> 写事件表: 正常
推送告警 --> 写告警表
写事件表 --> 更新摘要
写告警表 --> 等待帧: 下一帧
更新摘要 --> 等待帧: 下一帧
@enduml`],
  ["D07_class",`@startuml
title 核心Service类图
class AuthService {
  + login(LoginDTO): TokenVO
  + register(RegisterDTO): TokenVO
  - generateJWT(User): String
}
class DeviceService {
  + addDevice(DeviceDTO): DeviceVO
  + removeDevice(Long): void
  - startFFmpeg(Device): Process
}
class BehaviorService {
  + getCurrent(Long): BehaviorDTO
  + getTodaySummary(Long): SummaryDTO
  + getTimeline(Long,Date): List<Event>
  + getTrend(Long,Period): TrendDTO
  - checkAlerts(Window): void
}
class NotificationService {
  + pushAlert(Alert): void
  + sendReport(Report): void
  - pushFCM(String,Message)
  - pushWeCom(String,Message)
}
class FeedingService {
  + getDailyAdvice(Long): AdviceDTO
  + createPlan(PlanDTO): PlanVO
  - buildPrompt(Pet,Summary): String
}
class VipService {
  + createOrder(OrderDTO): PayParams
  + paymentCallback(Map): void
  - verifySign(Map): boolean
}
BehaviorService --> NotificationService
FeedingService --> BehaviorService
FeedingService --> VipService
@enduml`],
];

// =============== GENERATE ALL ===============
async function genAll(diagrams, dir, label){
  console.log(`\n=== ${label} (${diagrams.length} diagrams) ===`);
  for(let [fn,puml] of diagrams){
    let pngPath=dir+"\\"+fn+".png";
    try{
      console.log(`  ${fn}...`);
      await fetchPlantUML(puml, pngPath);
      console.log(`    OK (${(fs.statSync(pngPath).size/1024).toFixed(1)}KB)`);
    }catch(e){
      console.error(`    FAIL: ${e.message}`);
    }
  }
}

async function main(){
  console.log("Generating all PlantUML diagrams...\n");
  await genAll(srsDiagrams, srsDir, "SRS - 用例图");
  await genAll(oddDiagrams, oddDir, "ODD - 概要设计图");
  await genAll(ddDiagrams, ddDir, "SDDD - 详细设计图");
  console.log("\nAll done!");
}
main().catch(e=>{console.error("Fatal:",e);process.exit(1);});
