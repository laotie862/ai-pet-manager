const fs=require("fs");
const {Document,Packer,Paragraph,TextRun,Table,TableRow,TableCell,Header,Footer,AlignmentType,LevelFormat,TableOfContents,HeadingLevel,BorderStyle,WidthType,ShadingType,PageBreak,PageNumber,ImageRun}=require("docx");
const OUT_DIR="C:\\Users\\YL\\Desktop\\宠物平台\\phases\\phase2-概要设计";
const DIA_DIR=OUT_DIR+"\\diagrams";
if(!fs.existsSync(DIA_DIR))fs.mkdirSync(DIA_DIR);

const DIAGRAMS = [
  {file:"D01_arch", path:DIA_DIR+"\\D01_arch.png", title:"系统架构图"},
  {file:"D02_er", path:DIA_DIR+"\\D02_er.png", title:"核心数据模型（ER图）"},
  {file:"D03_module", path:DIA_DIR+"\\D03_module.png", title:"系统模块结构图"},
  {file:"D04_deploy", path:DIA_DIR+"\\D04_deploy.png", title:"部署拓扑图"},
  {file:"D05_flow", path:DIA_DIR+"\\D05_flow.png", title:"核心处理流程图"},
  {file:"D06_interface", path:DIA_DIR+"\\D06_interface.png", title:"系统接口关系图"},
];

// ================== DOCX Build ==================
const PW=11906,PH=16838,MG=1440,HB="1F4E79",AB="2E75B6",AR="F2F7FB";
const bd={style:BorderStyle.SINGLE,size:1,color:"999999"};
const bds={top:bd,bottom:bd,left:bd,right:bd};
const cm={top:60,bottom:60,left:100,right:100};
const sp={page:{size:{width:PW,height:PH},margin:{top:MG,right:MG,bottom:MG,left:MG}}};

function H1(t){return new Paragraph({heading:HeadingLevel.HEADING_1,spacing:{before:360,after:200},children:[new TextRun({text:t})]});}
function H2(t){return new Paragraph({heading:HeadingLevel.HEADING_2,spacing:{before:280,after:160},children:[new TextRun({text:t})]});}
function H3(t){return new Paragraph({heading:HeadingLevel.HEADING_3,spacing:{before:200,after:120},children:[new TextRun({text:t,bold:true,size:26})]});}
function P(t){return new Paragraph({spacing:{after:120,line:360},children:[new TextRun({text:t,size:22})]});}
function PB(){return new Paragraph({children:[new PageBreak()]});}
function MC(t,opts={}){return new TableCell({borders:bds,margins:cm,width:opts.w?{size:opts.w,type:WidthType.DXA}:undefined,shading:opts.s?{fill:opts.s,type:ShadingType.CLEAR}:undefined,verticalAlign:"center",children:[new Paragraph({alignment:opts.a||AlignmentType.LEFT,spacing:{after:40,before:40},children:[new TextRun({text:String(t),size:opts.z||20,bold:opts.b||false})]})]});}
function MT(headers,rows,cw){
  let tw=cw.reduce((a,b)=>a+b,0);
  return new Table({width:{size:tw,type:WidthType.DXA},columnWidths:cw,rows:[new TableRow({tableHeader:true,children:headers.map((h,i)=>MC(h,{w:cw[i],b:true,z:20,s:HB}))}),...rows.map((row,ri)=>new TableRow({children:row.map((c,ci)=>MC(c,{w:cw[ci],s:ri%2===0?AR:undefined,z:20}))}))]});
}
function IMG(data,title){
  return new Paragraph({alignment:AlignmentType.CENTER,spacing:{before:240,after:80},children:[new ImageRun({type:"png",data,transformation:{width:480,height:300},altText:{title,description:title,name:title}})]});
}
function ICAP(text){return new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:280},children:[new TextRun({text,size:20,italics:true,color:"666666"})]});}
function insertDiag(fn){
  let f=DIAGRAMS.filter(x=>x.file===fn);if(f.length===0)return[];
  let png=fs.readFileSync(f[0].path);
  return [IMG(png,f[0].title),ICAP("图 "+f[0].title)];
}

async function buildDoc(){
  const HF={headers:{default:new Header({children:[new Paragraph({alignment:AlignmentType.RIGHT,border:{bottom:{style:BorderStyle.SINGLE,size:4,color:AB,space:4}},children:[new TextRun({text:"AI宠物管家 — 软件概要设计说明书",size:18,color:"888888",italics:true})]})]})},footers:{default:new Footer({children:[new Paragraph({alignment:AlignmentType.CENTER,border:{top:{style:BorderStyle.SINGLE,size:2,color:"CCCCCC",space:4}},children:[new TextRun({text:"第 ",size:18,color:"888888"}),new TextRun({children:[PageNumber.CURRENT],size:18,color:"888888"}),new TextRun({text:" 页",size:18,color:"888888"})]})]})}};

  let cover={properties:sp,children:[
    new Paragraph({spacing:{before:2400}}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:600},children:[new TextRun({text:"AI宠物管家平台",size:52,bold:true,color:HB})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:200},children:[new TextRun({text:"软件概要设计说明书",size:40,bold:true,color:AB})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:200},children:[new TextRun({text:"Software Outline Design Description",size:22,color:"666666",italics:true})]}),
    new Paragraph({spacing:{before:1200}}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},border:{top:{style:BorderStyle.SINGLE,size:6,color:AB,space:12}},children:[new TextRun({text:"文档编号：PRJ-PETAI-ODD-001",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},children:[new TextRun({text:"版本号：V1.0",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},children:[new TextRun({text:"编制日期：2025年6月",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},children:[new TextRun({text:"密级：内部",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},children:[new TextRun({text:"参照标准：GB/T 8567-2006",size:20,color:"888888"})]}),
  ]};

  let rev=[H1("修订记录"),
    MT(["版本","日期","修订人","修订说明"],[["V1.0","2025-06-04","架构组","依据GB/T 8567-2006编制，完成概要设计全稿"]],[1200,1800,1800,4226]),
    PB(),H1("目  录"),new TableOfContents("目录",{hyperlink:true,headingStyleRange:"1-3"}),PB()];

  // Chapter 1
  let c1=[H1("1  引言"),
    H2("1.1  编写目的"),P("本文档为AI宠物管家平台的软件概要设计说明书，依据GB/T 8567-2006《计算机软件文档编制规范》编制。旨在明确系统的总体架构、模块划分、接口关系、数据结构和运行机制，作为后续详细设计和编码实现的纲领性依据。"),
    H2("1.2  背景"),P("AI宠物管家是一个基于计算机视觉和人工智能技术的智能宠物看护平台。通过接入网络摄像头，实时分析宠物的进食、饮水、运动、睡眠、排泄等行为，结合大语言模型生成每日行为报告和个性化饲养建议，通过App推送和企业微信双通道触达用户。"),P("需求依据：《AI宠物管家_GB需求规格说明书_V1.1》（含63个用例）。"),
    H2("1.3  术语定义"),
    MT(["术语","说明"],[["SRS","软件需求规格说明书"],["SODD","Software Outline Design Description，软件概要设计说明书"],["RTSP","Real Time Streaming Protocol，实时流传输协议"],["CV","Computer Vision，计算机视觉"],["LLM","Large Language Model，大语言模型"],["FCM","Firebase Cloud Messaging，消息推送服务"]],[2000,7026]),
    H2("1.4  参考资料"),P("a) GB/T 8567-2006 计算机软件文档编制规范；b) 《AI宠物管家_GB需求规格说明书_V1.1》；c) 《Phase 2 — 概要设计 AGENTS.md》。"),PB()];

  // Chapter 2 - Overall Design
  let c2=[H1("2  总体设计"),
    H2("2.1  需求规定"),
    P("本系统需满足SRS V1.1中规定的63个功能用例和全部非功能性需求。主要功能模块包括：摄像头管理与AI行为分析、宠物档案与健康管理、智能提醒与消息推送、AI饲养建议、VIP趋势分析与专家咨询、养宠资讯浏览、后台管理与数据统计。"),
    P("非功能需求核心指标：视频端到端延迟≤5秒、告警推送延迟≤10秒、CV行为识别准确率≥85%、App首屏加载≤3秒、接口P95响应≤500ms、系统可用性≥99.9%。"),
    H2("2.2  运行环境"),
    MT(["环境","配置要求"],[["应用服务器","Linux (CentOS 7.9+ / Ubuntu 22.04+)，4C8G×2台，Docker 24.x"],["数据服务器","8C16G，1TB SSD，PostgreSQL 17 + Redis 7 + Kafka + MinIO"],["GPU推理机","RTX 4090 24GB，CUDA 12.x，Python 3.11，FFmpeg 6.x"],["客户端","iOS 14+ / Android 10+，企业微信，Chrome/Firefox/Safari/Edge 最新版"]],[2800,6226]),
    H2("2.3  基本设计概念和处理流程"),P("系统采用分层架构：接入层（Flutter App / Vue3 Admin / 企业微信 / H5 WebView）→ 网关层（Nginx HTTPS/WSS）→ 服务层（Spring Boot + Python CV）→ 数据层（PostgreSQL + Redis + MinIO + Kafka）。"),
    H3("2.3.1  AI行为分析逻辑"),P("行为分析是系统的核心中枢，完整链路分五步："),
    P("第一步——拉流抽帧：FFmpeg从RTSP摄像头每秒抽5帧JPEG，发给Python CV服务。"),
    P("第二步——AI推理：YOLOv8（预训练，开箱即用）检测画面中宠物位置，行为分类器（ResNet50，需自行标注训练）判断当前行为标签。基础标签5种：eating(进食)、drinking(饮水)、exercising(运动)、sleeping(睡眠)、defecating(排泄)。后续可扩展：playing(玩耍)、grooming(舔毛)、vomiting(呕吐)等。"),
    P("第三步——事件聚合：相同行为连续命中→合并为1个事件（如12:00:05到12:03:25标记为一次eating），行为标签切换→关闭上一个事件开启新事件，小于5秒的事件丢弃滤抖。"),
    P("第四步——异常告警：滑动窗口实时判异——30分钟无eating→进食异常、2小时无drinking→饮水异常、4小时无exercising→运动异常、10分钟未检测到宠物→不在画面中。告警产生→写队列入库→查用户推送Token→App推送→5分钟未读→企微追加→15分钟未读→短信兜底(VIP)。"),
    P("第五步——每日摘要：t_behavior_summary实时统计当日进食次数、饮水次数、运动总时长、睡眠总时长、排泄次数。每晚22:00 XXL-JOB触发→调LLM生成自然语言报告→app+企微推送。"),
    H3("2.3.2  模块联动关系"),P("系统核心模块联动可概括为六条故事线："),
    P("故事1——摄像头拍到宠物在吃饭：画面→FFmpeg抽帧→CV推理→behavior-service写事件表+更新摘要→App实时展示。"),
    P("故事2——狗子一直没吃东西：滑动窗口发现30分钟无进食→告警事件写库→notification-service查user推送Token→FCM/APNs推送通知→5分钟未读→企微再发→15分钟VIP兜底短信。"),
    P("故事3——每晚自动写日报：XXL-JOB 22:00触发→behavior-service查当日摘要+pet-service查品种/年龄/体重→feeding-service组装提示词→调LLM生成报告→写库→notification-service推App+企微。VIP生成前加一步：查vip-service判断身份→VIP用增强Prompt(含7天趋势对比)→普通用标准Prompt。"),
    P("故事4——付钱升级VIP：vip-service创建订单→微信/支付宝支付→回调验签→user-service更新vip_status→notification-service推送\"恭喜升级\"→次日日报自动切VIP模板。"),
    P("故事5——专家接单：用户发起咨询→expert-service查在线专家→推送通知→专家接单→WebSocket双向通道建立→文字/图片/视频通话→结束自动算费结算。"),
    P("故事6——该驱虫了：reminder-service每早扫描health-service→发现上次驱虫距今30天→查user推送Token→App+企微推送\"[豆豆]该驱虫了\"。"),
    ...insertDiag("D05_flow"),
    H2("2.4  结构"),
    H3("2.4.1  系统架构"),
    ...insertDiag("D01_arch"),
    H3("2.4.2  模块划分"),
    ...insertDiag("D03_module"),
    P("系统共划分为16个服务模块："),
    MT(["序号","模块","功能简述","依赖模块"],[["1","auth-service","注册、登录、JWT令牌管理、Token刷新","common, user"],["2","user-service","用户资料管理、VIP状态查询、微信绑定","auth, vip"],["3","pet-service","宠物档案CRUD、照片管理","storage, device"],["4","device-service","摄像头RTSP接入、拉流调度、监控区域配置","pet, behavior"],["5","behavior-service","行为事件聚合、视频片段管理、摘要生成","device, storage, alarm"],["6","health-service","疫苗/驱虫/体检/用药记录CRUD","pet"],["7","reminder-service","提醒规则配置、定时推送触发","notification"],["8","feeding-service","通用饲养指南、AI每日饲养建议、VIP定制计划","pet, behavior, trend"],["9","trend-service","周/月/季行为趋势计算、健康风险预警","pet, behavior"],["10","expert-service","专家管理、在线咨询、视频通话信令","user, notification"],["11","news-service","资讯流推荐、分类浏览、收藏、品种百科","user"],["12","vip-service","VIP套餐选购、支付对接、自动续费、订单管理","user, notification"],["13","storage-service","文件上传下载、MinIO/OSS管理","—"],["14","notification-service","FCM/APNs推送、微信消息、短信","user"],["15","admin-service","用户封禁/解封、专家审核、运营统计","auth, user, expert"],["16","common","统一返回体、异常处理、工具类、常量","—"]],[600,1800,3200,3426]),
    H2("2.5  功能需求与程序的关系"),P("各模块与服务间通过RESTful HTTP JSON通信，内部调用走Spring Bean依赖注入。一个用例可能跨越多个模块，如UC-R-02（异常告警推送）涉及behavior-service（判异）、device-service（视频）、notification-service（推送）、user-service（查询Token）。"),
    H2("2.6  人工处理过程"),P("a) 管理员审核专家入驻资质（人工审核执业兽医资格证照片）；b) 管理员处理用户申诉；c) 内容编辑撰写和发布养宠资讯文章；d) 运维人员处理服务告警和手动备份。"),
    H2("2.7  尚未解决的问题"),P("a) CV模型在低光照、遮挡、多宠场景下的准确率需要实际验证；b) 企业微信API消息推送频率限制（单应用日发送量上限待确认）；c) 视频通话WebRTC在复杂网络环境下的穿透成功率；d) 不同品牌摄像头的RTSP兼容性差异。"),PB()];

  // Chapter 3 - Interface Design
  let c3=[H1("3  接口设计"),
    H2("3.1  用户接口"),P("前端共4个用户界面："),
    MT(["序号","界面","技术","关键页面"],[["1","Flutter 移动App","Flutter 3.x + Dio + ijkplayer","首页行为卡片、实时画面、行为时间线、健康记录、提醒设置、专家咨询页、VIP套餐页、资讯流"],["2","Vue3 管理后台","Vue 3 + Element Plus + ECharts","用户管理列表、用户详情、专家审核页、运营面板（DAU/设备数/告警量）、内容发布"],["3","企业微信","Spring Boot 直接调企微API","扫码添加企微员工号、每日报告推送、回复\"看看\"获取实时快照"],["4","H5 WebView","Vue 3（嵌入App）","资讯流浏览、品种百科、文章详情"]],[1500,2000,2000,3526]),
    H2("3.2  外部接口"),
    ...insertDiag("D06_interface"),
    H3("3.2.1  与Python CV推理服务接口"),
    P("通信方式：HTTP JSON。Spring Boot定时（每5秒）发送当前帧（Base64 JPEG）至Python FastAPI服务，CV服务返回行为识别结果。"),
    MT(["项目","说明"],[["接口地址","POST http://cv-service:9001/cv/detect"],["请求格式","{ \"image_base64\": \"...\", \"device_id\": 1, \"roi_polygon\": [...] }"],["响应格式","{ \"found\": true, \"behavior\": \"eating\", \"confidence\": 0.92, \"bbox\": {...} }"],["异常处理","超时5秒→重试1次→丢弃当前帧"]],[2500,6526]),
    H3("3.2.2  与LLM接口"),
    P("通信方式：HTTP JSON。用于日报告生成、AI饲养建议、健康预警文本。支持OpenAI/DeepSeek/通义千问等多供应商切换。"),
    MT(["项目","说明"],[["接口地址","POST https://api.openai.com/v1/chat/completions (可配置)"],["请求格式","Chat Completions格式，system prompt + user prompt"],["响应格式","{ \"choices\": [{ \"message\": { \"content\": \"...\" } }] }"],["容错策略","超时15秒→降级为预设模板报告，提示\"AI生成中\"后异步补发"]],[2500,6526]),
    H3("3.2.3  推送服务接口"),P("FCM/APNs：Spring Boot通过Firebase Admin SDK和APNs HTTP/2推送；企业微信：Spring Boot通过企微官方API（POST /cgi-bin/message/send）直接推送消息至用户微信，无需独立进程。"),
    H3("3.2.4  支付网关接口"),P("微信支付（JSAPI/APP支付）+ 支付宝（APP支付）。统一下单→用户支付→异步回调→验签→更新订单状态。回调接口POST /api/vip/callback须做幂等（按transaction_id去重）。"),
    H2("3.3  内部接口"),
    P("内部模块间通过Spring Bean直接调用（同步）或Kafka消息（异步）通信。关键内部接口："),
    MT(["调用方","被调用方","方式","说明"],[["behavior-service","device-service","同步","获取设备信息（RTSP地址、ROI区域）"],["behavior-service","存储→Kafka","异步","行为事件写入Kafka Topic"],["notification-service","user-service","同步","获取用户推送Token和微信绑定状态"],["reminder-service","behavior-service","同步","读取当日行为摘要生成提醒内容"],["feeding-service","behavior-service","同步","获取7天行为趋势数据"],["trend-service","behavior-service","同步","聚合历史行为数据计算趋势"],["All","common","同步","统一异常处理、统一返回体"]],[1800,1800,1200,4226]),PB()];

  // Chapter 4 - Runtime Design
  let c4=[H1("4  运行设计"),
    H2("4.1  运行模块组合"),P("系统运行时分为以下进程组合："),
    MT(["进程","数量","职责","启动方式"],[["Spring Boot API","2实例（负载均衡）","业务逻辑处理，REST API，WebSocket信令","Docker容器，docker-compose scale"],["Python CV Service","1实例（GPU独占）","视频帧推理，行为识别","Docker容器（GPU直通）"],["企业微信推送","无需独立进程","Spring Boot直接调用企微API","HTTP同步调用"],["Nginx","2实例","HTTPS终结，WebSocket反向代理","Docker容器"],["FFmpeg进程池","每路摄像头1进程","RTSP拉流 + 抽帧","Spring Boot ProcessBuilder管理"],["XXL-JOB","1实例","定时任务调度，每日报告触发","Docker容器"]],[1800,1800,3000,2426]),
    H2("4.2  运行控制"),P("a) 服务启动顺序：PostgreSQL→Redis→MinIO→Kafka→Spring Boot→Python CV→Nginx。通过docker-compose depends_on和healthcheck控制。"),P("b) CV推理按每路摄像头独立FFmpeg进程，GPU推理排队处理(针对GPU并发数可控)。"),P("c) 优雅关闭：收到SIGTERM→拒绝新请求→等待进行中请求完成（≤30秒）→关闭数据库连接池→退出。"),
    H2("4.3  运行时间"),P("a) 各接口预估响应时间：登录<200ms、宠物CRUD<100ms、行为时间线查询<200ms、每日报告生成<30秒、PDF报告导出<15秒。"),P("b) 定时任务时间：每日报告22:00生成、趋势数据每周一01:00预计算、VIP续费检查每日10:00执行、健康到期提醒每日09:00扫描。"),P("c) 系统可用性：核心服务99.9%（年停机≤8.76小时），每日02:00-03:00为低流量维护窗口。"),PB()];

  // Chapter 5 - Data Structure Design
  let c5=[H1("5  系统数据结构设计"),
    H2("5.1  逻辑结构设计要点"),
    ...insertDiag("D02_er"),
    P("核心实体及关系简要说明："),
    MT(["实体","主键","核心属性","关键关系"],[["t_user","id","phone, password, nickname, vip_status, wechat_user_id, push_token","1:N → t_pet, t_vip_order, t_alert, t_consultation"],["t_pet","id","user_id(FK), name, breed, gender, birthday, weight_kg","1:N → t_device, t_behavior_event, t_behavior_summary, t_health_record, t_feeding_plan, t_behavior_trend"],["t_device","id","pet_id(FK), name, rtsp_url, status, roi","1:N → t_behavior_event"],["t_behavior_event","id","device_id(FK), pet_id(FK), behavior, confidence, start_time, end_time, video_url","聚合→ t_behavior_summary"],["t_behavior_summary","id","pet_id(FK), date(唯一约束), eat_count, drink_count, exercise_min, sleep_min, defecate_count","—"],["t_alert","id","pet_id(FK), alert_type, message, push_status, push_channel, triggered_at","—"],["t_health_record","id","pet_id(FK), record_type, content(JSONB), record_date","—"],["t_vip_order","id","user_id(FK), plan_type, amount, pay_channel, pay_status, auto_renew, transaction_id","—"],["t_expert","id","name, title, specialty, license, status, rating","1:N → t_consultation"],["t_consultation","id","user_id(FK), expert_id(FK), consult_type, status, duration_min, fee, rating","1:N → t_consult_msg"],["t_behavior_trend","id","pet_id(FK), period_type, period_start(唯一组合), metrics(JSONB)","—"],["t_feeding_plan","id","pet_id(FK), version, daily_feed_g, feed_times, exercise_min, status","—"],["t_admin","id","username, password, role","1:N → t_audit_log"],["t_audit_log","id","admin_id(FK), action, target_type, target_id, detail(JSONB), ip_address","—"]],[1800,1000,3400,2826]),
    H2("5.2  物理结构设计要点"),P("a) 数据库：PostgreSQL 17，字符集UTF-8，排序规则zh_CN.UTF-8。"),P("b) 索引策略：t_behavior_event按(device_id, start_time DESC)；t_behavior_summary按(pet_id, date)唯一索引；t_alert按(pet_id, triggered_at DESC)；t_health_record按(pet_id, record_type)。"),P("c) 分区策略：t_behavior_event按月范围分区（日增量约50万行/100路），t_audit_log按月分区。"),P("d) Redis键设计：token:blacklist:{jti}→过期时间、sms:rate:{phone}→令牌桶计数器、device:status:{id}→在线状态缓存。"),
    H2("5.3  数据结构与程序的关系"),P("各Service模块对应操作约定的实体表：pet-service→t_pet、behavior-service→t_behavior_event/t_behavior_summary、vip-service→t_vip_order等。JSONB字段（t_health_record.content, t_behavior_trend.metrics, t_audit_log.detail）由各自Service负责序列化/反序列化，数据库不感知结构。"),PB()];

  // Chapter 6 - Error Handling
  let c6=[H1("6  系统出错处理设计"),
    H2("6.1  出错信息"),
    MT(["错误类型","错误码","错误信息","处理措施"],[["认证失败","401","未登录或Token已过期","前端跳转登录页，刷新Token"],["权限不足","403","无此操作权限","展示无权限提示"],["资源不存在","404","请求的资源不存在","展示404页面"],["参数校验","400","参数校验失败：{具体字段}","Toast提示具体原因"],["业务异常","500","{业务错误描述}","统一异常处理返回错误信息"],["服务不可用","503","服务暂时不可用，请稍后重试","前端展示降级页面，3秒后自动刷新"],["CV推理超时","504","行为分析超时，正在重试","丢弃当前帧，记录日志，不影响流程"],["支付回调失败","520","支付回调处理异常","写入死信队列，人工介入"]],[1500,1000,2200,4326]),
    H2("6.2  补救措施"),
    P("a) 数据库故障：PostgreSQL主从切换（<30秒），应用自动重连；每日自动全量备份至MinIO。"),
    P("b) Redis故障：哨兵自动故障转移，应用启动时检测Redis可用性；降级策略：Token黑名单走数据库查询，验证码限流失效时放宽频率。"),
    P("c) CV服务故障：Spring Boot检测CV Health连续3次失败→标记CV不可用→行为分析暂停→推送\"AI分析暂时离线\"通知→每30秒重试连接。"),
    P("d) 推送服务故障：FCM/APNs不可达→自动切换微信通道；若双通道均不可达→告警写入\"待补发\"队列→恢复后批量补发。"),
    P("e) 支付回调丢失：定时任务每日凌晨对比微信/支付宝对账单与本地订单，发现已支付未回调订单→主动查询支付状态→补更新。"),
    P("f) FFmpeg进程异常退出：Spring Boot ProcessBuilder监听进程退出事件→自动重启（最多3次/5分钟）→超过重启上限则标记设备离线并推送用户。"),
    H2("6.3  系统维护设计"),
    P("a) 系统监控：Prometheus + Grafana监控核心指标（QPS、错误率、延迟、GPU使用率），钉钉/企微告警通知。"),
    P("b) 日志管理：ELK Stack（Elasticsearch + Logstash + Kibana），统一JSON格式，按服务名和TraceId关联。"),
    P("c) 数据库维护：每周VACUUM ANALYZE、每月重建部分索引、按分区策略自动清理过期数据（行为事件保留90天，告警保留180天）。"),
    P("d) CV模型更新：上传新模型文件至MinIO→管理后台触发模型切换→CV服务热加载新模型→A/B测试验证→切换默认版本。"),
    P("e) 代码发布：GitHub Actions流水线→构建Docker镜像→推送Registry→docker-compose滚动更新（先启新实例→健康检查→停旧实例）。"),PB()];

  // Appendices
  let app=[H1("附录"),
    H2("A  系统架构图"),...insertDiag("D01_arch"),
    H2("B  核心数据模型（ER图）"),...insertDiag("D02_er"),
    H2("C  系统模块结构图"),...insertDiag("D03_module"),
    H2("D  部署拓扑图"),...insertDiag("D04_deploy"),
    H2("E  核心处理流程图"),...insertDiag("D05_flow"),
    H2("F  系统接口关系图"),...insertDiag("D06_interface")];

  let doc=new Document({
    styles:{default:{document:{run:{font:"Arial",size:22}}},
      paragraphStyles:[
        {id:"Heading1",name:"Heading 1",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:32,bold:true,font:"Arial",color:HB},paragraph:{spacing:{before:360,after:200},outlineLevel:0}},
        {id:"Heading2",name:"Heading 2",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:28,bold:true,font:"Arial",color:AB},paragraph:{spacing:{before:280,after:160},outlineLevel:1}},
        {id:"Heading3",name:"Heading 3",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:26,bold:true,font:"Arial"},paragraph:{spacing:{before:200,after:120},outlineLevel:2}}]},
    sections:[cover,{properties:{...sp,page:{...sp.page,pageNumbers:{start:1}}},...HF,children:rev},{properties:sp,...HF,children:[...c1,...c2,...c3,...c4,...c5,...c6,...app]}]
  });
  return doc;
}

async function main(){
  console.log("Building GB/T 8567-2006 ODD document...");
  let doc=await buildDoc();
  let op=OUT_DIR+"\\AI宠物管家_概要设计说明书_GB8567_V1.1.docx";
  let buf=await Packer.toBuffer(doc);
  fs.writeFileSync(op,buf);
  console.log("Saved: "+op);
}
main().catch(e=>{console.error(e);process.exit(1);});
