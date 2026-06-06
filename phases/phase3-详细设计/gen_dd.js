const fs=require("fs");
const {Document,Packer,Paragraph,TextRun,Table,TableRow,TableCell,Header,Footer,AlignmentType,LevelFormat,TableOfContents,HeadingLevel,BorderStyle,WidthType,ShadingType,PageBreak,PageNumber,ImageRun}=require("docx");
const OUT_DIR="C:\\Users\\YL\\Desktop\\宠物平台\\phases\\phase3-详细设计";
const DIA_DIR=OUT_DIR+"\\diagrams";
if(!fs.existsSync(DIA_DIR))fs.mkdirSync(DIA_DIR);
function E(s){return String(s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");}

const DIAGRAMS = [
  {file:"D01_seq_login", path:DIA_DIR+"\\D01_seq_login.png", title:"用户登录时序图"},
  {file:"D02_seq_alert", path:DIA_DIR+"\\D02_seq_alert.png", title:"摄像头→告警推送时序图"},
  {file:"D03_seq_report", path:DIA_DIR+"\\D03_seq_report.png", title:"每日报告生成时序图"},
  {file:"D04_seq_pay", path:DIA_DIR+"\\D04_seq_pay.png", title:"VIP支付时序图"},
  {file:"D05_seq_expert", path:DIA_DIR+"\\D05_seq_expert.png", title:"专家咨询时序图"},
  {file:"D06_state_behavior", path:DIA_DIR+"\\D06_state_behavior.png", title:"行为分析状态机"},
  {file:"D07_class", path:DIA_DIR+"\\D07_class.png", title:"核心Service类图"},
];

// ==================== DOCX ====================
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
function insDiag(fn){
  let f=DIAGRAMS.filter(x=>x.file===fn);if(f.length===0)return[];
  let png=fs.readFileSync(f[0].path);
  return [IMG(png,f[0].title),ICAP("图 "+f[0].title)];
}
function code(t){
  return new Paragraph({spacing:{after:80,before:80},indent:{left:720},children:[new TextRun({text:t,size:18,font:"Courier New",color:"333333"})]});
}

async function buildDoc(){
  const HF={headers:{default:new Header({children:[new Paragraph({alignment:AlignmentType.RIGHT,border:{bottom:{style:BorderStyle.SINGLE,size:4,color:AB,space:4}},children:[new TextRun({text:"AI宠物管家 — 软件详细设计说明书",size:18,color:"888888",italics:true})]})]})},footers:{default:new Footer({children:[new Paragraph({alignment:AlignmentType.CENTER,border:{top:{style:BorderStyle.SINGLE,size:2,color:"CCCCCC",space:4}},children:[new TextRun({text:"第 ",size:18,color:"888888"}),new TextRun({children:[PageNumber.CURRENT],size:18,color:"888888"}),new TextRun({text:" 页",size:18,color:"888888"})]})]})}};

  let cover={properties:sp,children:[
    new Paragraph({spacing:{before:2400}}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:600},children:[new TextRun({text:"AI宠物管家平台",size:52,bold:true,color:HB})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:200},children:[new TextRun({text:"软件详细设计说明书",size:40,bold:true,color:AB})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:200},children:[new TextRun({text:"Software Detailed Design Description (SDDD)",size:22,color:"666666",italics:true})]}),
    new Paragraph({spacing:{before:1200}}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},border:{top:{style:BorderStyle.SINGLE,size:6,color:AB,space:12}},children:[new TextRun({text:"文档编号：PRJ-PETAI-SDDD-001",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},children:[new TextRun({text:"版本号：V1.0",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},children:[new TextRun({text:"编制日期：2025年6月",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},children:[new TextRun({text:"密级：内部",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:60},children:[new TextRun({text:"参照标准：GB/T 8567-2006",size:20,color:"888888"})]}),
  ]};

  // Ch1
  let c1=[H1("1  引言"),
    H2("1.1  编写目的"),P("本文档为AI宠物管家平台的软件详细设计说明书，依据GB/T 8567-2006编制。旨在对每个模块的接口、数据结构、算法逻辑、调用关系和异常处理进行详细描述，作为编码实现的直接依据。"),
    H2("1.2  背景"),P("本系统是基于计算机视觉与LLM的智能宠物看护平台，需求依据《SRS V1.1》（63个用例），总体架构依据《概要设计说明书 V1.1》。本文档对概要设计中定义的16个服务模块展开详细设计。"),
    H2("1.3  术语定义"),MT(["术语","说明"],[["SDDD","Software Detailed Design Description"],["JWT","JSON Web Token"],["AOP","Aspect-Oriented Programming"],["ROI","Region of Interest，监控区域"]],[2000,7026]),
    H2("1.4  参考资料"),P("a) GB/T 8567-2006；b) 《AI宠物管家_GB需求规格说明书_V1.1》；c) 《AI宠物管家_概要设计说明书_GB8567_V1.1》；d) Phase 3 AGENTS.md。"),PB()];

  // Ch2 - Program Structure
  let c2=[H1("2  程序系统的结构"),...insDiag("D07_class"),
    P("系统共16个微服务模块，部署为Spring Boot单体（内部模块通过Spring Bean调用）或将来拆分为独立微服务。核心模块Service类图如上图所示。"),
    P("模块分层："),
    P("接口层（Controller）：接收HTTP请求，参数校验，调用Service。"),
    P("业务层（Service）：业务逻辑编码，事务管理，跨模块调用。"),
    P("持久层（Repository/DAO）：MyBatis-Plus Mapper，数据访问。"),
    P("基础设施层（common）：统一返回体、异常处理、工具类、JWT过滤器。"),
    H2("2.1  启动顺序"),P("PostgreSQL → Redis → MinIO → Kafka → Spring Boot → Python CV → Nginx。所有服务通过docker-compose健康检查保证依赖就绪后才启动下游。"),
    H2("2.2  包结构规范"),
    code("pet-butler-server/\n├── src/main/java/com/petbutler/\n│   ├── config/         安全/CORS/Redis/MinIO配置\n│   ├── common/         统一返回体/异常/常量/工具类\n│   ├── auth/           认证模块(Login/Register/JWT)\n│   ├── user/           用户管理模块\n│   ├── pet/            宠物档案模块\n│   ├── device/         设备管理+FFmpeg调度\n│   ├── behavior/       行为分析核心模块\n│   ├── health/         健康记录模块\n│   ├── reminder/       智能提醒模块\n│   ├── feeding/        AI饲养建议模块\n│   ├── trend/          趋势分析模块(VIP)\n│   ├── expert/         专家咨询模块\n│   ├── news/           资讯管理模块\n│   ├── vip/            VIP支付+续费模块\n│   ├── storage/        MinIO文件服务\n│   ├── notification/   推送服务(FCM/企微/短信)\n│   └── admin/          后台管理模块\n├── cv-inference/       Python CV推理服务\n├── wecom-bridge/       企业微信消息适配层\n└── pom.xml"),PB()];

  // Ch3 - Module detailed design (pick core 8)
  let ch3=[H1("3  核心模块详细设计")];

  // 3.1 auth
  ch3.push(H2("3.1  认证模块（auth-service）"),H3("3.1.1  程序描述"),P("负责用户注册、登录、Token刷新与注销。基于Spring Security + JWT + Redis实现。"),
    H3("3.1.2  接口规格"),
    MT(["接口","方法","请求参数","响应","说明"],[["/api/auth/login","POST","{phone,password}","{accessToken,refreshToken,expiresIn}","密码BCrypt验证，生成双令牌"],["/api/auth/register","POST","{phone,password,nickname}","同login","校验密码强度+手机号唯一性"],["/api/auth/refresh","POST","Header: Bearer {refreshToken}","{accessToken,expiresIn}","刷新accessToken，旧Token入黑名单"],["/api/auth/me","GET","Header: Bearer {accessToken}","UserVO","返回当前用户信息"]],[1400,600,2000,2200,2826]),
    H3("3.1.3  流程逻辑"),...insDiag("D01_seq_login"),
    H3("3.1.4  算法说明"),P("JWT生成：HMAC-SHA256签名，payload={userId,role,iat,exp}，accessToken 2h有效，refreshToken 7d有效。密码校验：BCryptPasswordEncoder.matches(raw, hash)。Token注销：将旧Token加入Redis黑名单，过期时间=Token剩余有效期。"),
    H3("3.1.5  存储分配"),P("Redis key: token:blacklist:{jti} value:1 TTL=剩余有效期。User表字段phone、password_hash为AES-256加密存储。"),
    H3("3.1.6  异常处理"),P("密码错误→ErrorCode.LOGIN_FAILED(401)、账号冻结→ErrorCode.USER_BANNED(403)、Token过期→ErrorCode.TOKEN_EXPIRED(401)。"));

  // 3.2 device
  ch3.push(H2("3.2  设备管理模块（device-service）"),H3("3.2.1  程序描述"),P("管理摄像头RTSP接入、FFmpeg拉流调度、监控区域配置和实时视频转发。"),
    H3("3.2.2  接口规格"),
    MT(["接口","方法","关键逻辑"],[["POST /api/devices","新增","校验RTSP格式→连接测试(5s超时)→免费版限1个→写DB→异步启FFmpeg"],["DELETE /api/devices/{id}","移除","停止FFmpeg进程→删除设备记录→清理缓存"],["GET /api/devices/{id}/status","状态","返回online/offline/analyzing，查询Redis缓存优先"],["PUT /api/devices/{id}/region","ROI","存储多边形顶点坐标JSON，CV推理时仅分析区域内"],["WS /api/devices/{id}/live","直播","后端RTSP→Base64 JPEG→WebSocket推送MJPG流"]],[2400,1200,5426]),
    H3("3.2.3  算法——FFmpeg进程管理"),P("使用Java ProcessBuilder启动FFmpeg子进程，每个设备一个独立进程。命令模板：ffmpeg -rtsp_transport tcp -i {url} -vf fps=1/5 -s 1280x720 -q:v 3 {dir}/dev_{id}_%d.jpg。进程监听：Process.onExit()→自动重启（最多3次/5分钟）→超过上限标记设备offline并推送。"),
    H3("3.2.4  限制条件"),P("免费版最多1个设备，VIP不限。RTSP连接超时5秒。FFmpeg需系统安装。"),PB());

  // 3.3 CV
  ch3.push(H2("3.3  CV推理服务（Python FastAPI）"),H3("3.3.1  程序描述"),P("独立Python服务，接收Base64图像，执行宠物检测与行为分类，返回结构化结果。"),
    H3("3.3.2  接口规格"),
    code("POST /cv/detect\nRequest: { image_base64, device_id, roi_polygon? }\nResponse: { found: true|false, behavior: 'eating'|..., confidence: 0.92, bbox: {x,y,w,h}, timestamp }\n异常: found=false → 未检测到宠物, confidence<0.5 → uncertain"),
    H3("3.3.3  算法说明"),P("步骤一：Base64解码→numpy array→若ROI配置则裁剪。步骤二：YOLOv8nano推理，输出宠物bounding box（COCO类别cat=15,dog=16）。步骤三：裁剪宠物区域→ResNet50行为分类器→输出5类标签。步骤四：置信度<0.5标记uncertain。"),
    H3("3.3.4  模型管理"),P("YOLOv8：使用预训练权重yolov8n.pt，微调可选。行为分类器：需自行标注训练，每类≥500张标注帧，ResNet50微调，训练脚本cv-inference/train_classifier.py。模型切换：管理后台触发→CV服务热加载新模型→A/B验证→切换默认。"),
    H3("3.3.5  性能指标"),P("单帧推理：200ms（GPU）~600ms（CPU）。并发10路：GPU 500ms/帧，CPU不可用。内存占用：~2GB。"),PB());

  // 3.4 behavior
  ch3.push(H2("3.4  行为分析模块（behavior-service）"),H3("3.4.1  程序描述"),P("行为分析是系统核心中枢。消费Kafka行为事件，执行事件聚合、去抖动、异常告警判定和每日摘要维护。"),
    H3("3.4.2  核心算法——事件聚合与去抖动"),
    code("事件聚合伪代码：\n当前帧行为=newBehavior, 置信度=conf\nif newBehavior != lastBehavior:\n    if lastEvent.duration >= 5s: 保存lastEvent到DB\n    创建新事件 start_time=now\nelse:\n    更新当前事件的end_time=now, 裁剪视频片段[-2s,+2s]\n\n去抖动规则：单帧切换忽略，需连续3帧(15s)一致才确认切换"),
    H3("3.4.3  核心算法——滑动窗口异常判定"),
    code("告警规则引擎（可配置）：\n1. 滑动窗口30min→窗口内eating事件数=0→\"进食异常\"\n2. 滑动窗口2h→窗口内drinking事件数=0→\"饮水异常\"\n3. 滑动窗口4h→窗口内exercising事件总时长=0→\"运动异常\"\n4. 连续10min CV返回found=false→\"宠物不在画面中\"\n\n告警去重：同一类型告警1h内只发1次"),
    H3("3.4.4  状态机"),...insDiag("D06_state_behavior"),
    H3("3.4.5  接口规格"),
    MT(["接口","说明","返回"],[["GET /api/behaviors/{petId}/current","当前行为状态","{behavior,confidence,updatedAt}"],["GET /api/behaviors/{petId}/summary?date=","当日行为摘要","{eatCount,drinkCount,exerciseMin,sleepMin,defecateCount}"],["GET /api/behaviors/{petId}/timeline?date=","行为时间轴","[{behavior,startTime,endTime,videoUrl}...]"]],[3200,2000,3826]),
    H3("3.4.6  时序图——告警推送全链路"),...insDiag("D02_seq_alert"),PB());

  // 3.5 notification
  ch3.push(H2("3.5  推送服务模块（notification-service）"),H3("3.5.1  程序描述"),P("统一推送中心，封装FCM/APNs/企业微信/短信四个通道，提供统一接口供其他模块调用。"),
    H3("3.5.2  接口规格"),
    MT(["内部方法","调用方","格式"],[["pushAlert(Alert)","behavior-service","FCM(App实时)+企微(5分未读)+短信(15分)"],["sendReport(Report)","XXL-JOB","企微每日报告+App静默通知"],["sendRemind(Remind)","reminder-service","企微+App双通道"],["pushWeCom(userId,text)","任意","POST 企微API /cgi-bin/message/send"]],[2000,2400,4626]),
    H3("3.5.3  通道实现细节"),
    P("a) FCM/APNs：Firebase Admin SDK / APNs HTTP2，消息格式{title,body,data:{deepLink}}。b) 企业微信：Spring Boot直接调企微官方API POST /cgi-bin/message/send，无需独立进程。c) 短信：阿里云短信SDK，VIP兜底。"),
    H3("3.5.4  免打扰逻辑"),P("查询user推送配置表，若当前处于免打扰时段→告警入缓存队列→免打扰结束→按时间线合并→批量推送摘要。免打扰时段内紧急告警（宠物失踪）仍然强推。"),PB());

  // 3.6 feeding
  ch3.push(H2("3.6  AI饲养建议模块（feeding-service）"),H3("3.6.1  程序描述"),P("通用饲养指南查询与AI每日建议生成。VIP用户额外获得基于7天趋势的定制饲养计划。"),
    H3("3.6.2  核心算法——LLM Prompt构建"),
    P("普通用户Prompt模板：\"你是宠物小助手。宠物信息：{name},{breed},{age}月,{weight}kg。今日行为：进食{eatCount}次...请输出JSON：{summary,highlights,suggestion,mood_score}\""),
    P("VIP用户Prompt增强：\"你是高级宠物管家。除今日数据外，附7天趋势对比：{trendJSON}。请输出增强JSON：{summary,highlights,suggestion,trend_analysis,feeding_plan:{daily_feed_g,feed_times,exercise_min,water_ml},mood_score}\""),
    H3("3.6.3  VIP判断逻辑"),P("report生成前调用vip-service.isVip(userId)→true用增强Prompt→false用标准Prompt。用户升级VIP后从次日生效。"),
    H3("3.6.4  接口规格"),
    MT(["接口","VIP?","说明"],[["GET /api/feeding/guide?breed=X","否","基于品种的标准饲养指南"],["GET /api/feeding/daily-advice/{petId}","否","当日AI建议（普通版）"],["POST /api/feeding/plan/{petId}","是","生成/更新定制饲养计划"]],[2800,800,5426]),
    H3("3.6.5  时序图"),...insDiag("D03_seq_report"),PB());

  // 3.7 vip
  ch3.push(H2("3.7  VIP支付模块（vip-service）"),H3("3.7.1  程序描述"),P("VIP套餐选购、微信/支付宝支付对接、回调处理、自动续费和订单管理。"),
    H3("3.7.2  接口规格"),MT(["接口","说明","关键逻辑"],[["POST /api/vip/orders","创建订单","生成订单号→调支付统一下单→返回支付参数"],["POST /api/vip/callback","支付回调","验签→幂等去重(transaction_id)→更新订单→更新vip_status"],["PUT /api/vip/auto-renew","续费开关","修改auto_renew标志"],["GET /api/vip/orders","订单列表","按时间倒序"]],[2400,1200,5426]),
    H3("3.7.3  安全机制"),P("回调验签：微信RSA-SHA256、支付宝RSA2。金额校验：回调金额与订单金额服务端对比，不一致拒绝。幂等：transaction_id唯一约束，重复回调返回OK不重复处理。对账：每日凌晨对比支付平台对账单。"),
    H3("3.7.4  时序图"),...insDiag("D04_seq_pay"),PB());

  // 3.8 expert
  ch3.push(H2("3.8  专家咨询模块（expert-service）"),H3("3.8.1  程序描述"),P("处理专家列表查询、咨询会话创建、WebSocket信令转发、WebRTC视频通话信令和费用结算。"),
    H3("3.8.2  接口规格"),
    MT(["接口","说明"],[["GET /api/experts","在线专家列表（科室/评分/收费）"],["POST /api/consultations","发起咨询→推送专家→创建会话"],["WS /ws/consult/{sessionId}","WebSocket双向消息通道"],["POST /api/consultations/{id}/end","结束咨询→计算费用→返回账单"]],[3500,5526]),
    H3("3.8.3  WebSocket消息格式"),code("{ \"type\": \"text|image|video_offer|video_answer|ice_candidate|system|end\",\n  \"payload\": { ... },\n  \"senderId\": \"xxx\",\n  \"timestamp\": 1717500000000 }"),
    H3("3.8.4  WebRTC信令"),P("用户端createOffer→WS转发→专家端setRemote+createAnswer→WS转发→ICE Candidates双向WS转发。STUN：Google STUN（开发），TURN：自建coturn（生产）。"),
    H3("3.8.5  时序图"),...insDiag("D05_seq_expert"),PB());

  // 3.9 health
  ch3.push(H2("3.9  健康管理模块（health-service）"),H3("3.9.1  程序描述"),P("疫苗、驱虫、体检、用药四种健康记录的CRUD管理，以及健康到期扫描触发提醒。"),
    H3("3.9.2  接口规格"),MT(["接口","说明"],[["POST /api/health-records","新增健康记录，record_type区分类型"],["GET /api/health-records?petId=&type=&date=","按类型/时间筛选"],["PUT/DELETE /api/health-records/{id}","修改或删除"]],[4000,5026]),
    H3("3.9.3  JSONB字段规范"),P("不同record_type对应的content JSON结构不同。疫苗：{vaccine_name, batch, next_date}。驱虫：{type:internal/external, medicine, next_date}。体检：{hospital, items:[], result}。用药：{diagnosis, medicine, dosage, start_date, end_date}。"),
    H3("3.9.4  到期扫描逻辑"),P("reminder-service定时(每日09:00)调health-service.getExpiringRecords()→扫描所有next_date≤today+7天的记录→生成提醒列表→调notification-service推送。"),PB());

  // 3.10 reminder
  ch3.push(H2("3.10  智能提醒模块（reminder-service）"),H3("3.10.1  程序描述"),P("管理用户提醒配置、定时触发健康到期提醒、宠物生日提醒和每日报告推送调度。"),
    H3("3.10.2  提醒配置"),MT(["配置项","默认值","可调整?"],[["每日报告推送时间","22:00","否"],["健康到期提前提醒","提前3天","否"],["免打扰时段","23:00-07:00","是"],["告警阈值(运动)","4小时","VIP可调"],["告警阈值(进食)","30分钟","VIP可调"]],[2500,2500,4026]),
    H3("3.10.3  接口规格"),MT(["接口","说明"],[["GET /api/reminders/config","获取当前提醒配置"],["PUT /api/reminders/config","更新配置（VIP可调全部，普通用户仅免打扰）"]],[4000,5026]),PB());

  // Ch4 - Interface design
  let c4=[H1("4  接口设计"),
    H2("4.1  统一返回格式"),code("{ \"code\": 200, \"message\": \"success\", \"data\": {} }"),
    H2("4.2  错误码枚举"),
    MT(["错误码","说明","HTTP状态"],[["LOGIN_FAILED(1001)","密码错误","401"],["USER_BANNED(1002)","用户已封禁","403"],["TOKEN_EXPIRED(1003)","Token过期","401"],["VIP_REQUIRED(2001)","需VIP权限","403"],["DEVICE_LIMIT(3001)","设备数量超限","400"],["RTSP_FAILED(3002)","RTSP连接失败","400"],["CV_OFFLINE(4001)","AI分析离线","503"],["PAY_FAILED(5001)","支付失败","400"],["EXPERT_OFFLINE(6001)","专家不在线","404"]],[2000,3500,3526]),
    H2("4.3  分页规范"),P("请求参数：page(从1开始), size(默认20, 最大100)。响应格式：{code:200, data:{records:[], total, page, size}}。"),
    H2("4.4  内部模块间调用规范"),P("同步调用：Spring Bean注入，带@Transactional事务。异步调用：Kafka Topic（behavior-events/alert-events/notification-events）。"),PB()];

  // Ch5 - Data
  let c5=[H1("5  数据结构与数据库设计"),
    H2("5.1  完整DDL"),P("以下为核心模块涉及的完整建表SQL，共14张表。"),
    code("-- 用户表\nCREATE TABLE t_user (id BIGSERIAL PRIMARY KEY, phone VARCHAR(20) UNIQUE NOT NULL, password VARCHAR(256) NOT NULL, nickname VARCHAR(64), email VARCHAR(128), avatar_url VARCHAR(512), bio VARCHAR(256), push_token VARCHAR(256), wecom_user_id VARCHAR(128), vip_status VARCHAR(16) DEFAULT 'free', vip_expire_at TIMESTAMP, ban_status VARCHAR(16) DEFAULT 'active', created_at TIMESTAMP DEFAULT NOW());\n\n-- 宠物表\nCREATE TABLE t_pet (id BIGSERIAL PRIMARY KEY, user_id BIGINT REFERENCES t_user(id), name VARCHAR(64), breed VARCHAR(64), gender SMALLINT, birthday DATE, weight_kg DECIMAL(5,2), photo_url VARCHAR(512), created_at TIMESTAMP DEFAULT NOW());\n\n-- 设备表\nCREATE TABLE t_device (id BIGSERIAL PRIMARY KEY, user_id BIGINT, pet_id BIGINT, name VARCHAR(128), rtsp_url VARCHAR(512), status VARCHAR(16) DEFAULT 'offline', roi_polygon JSONB, created_at TIMESTAMP DEFAULT NOW());"),
    H2("5.2  索引策略"),P("高频查询字段建立索引：t_user(phone UNIQUE)、t_pet(user_id)、t_device(user_id, status)、t_behavior_event(device_id, start_time DESC)、t_behavior_summary(pet_id, date UNIQUE)、t_health_record(pet_id, record_type)、t_alert(pet_id, triggered_at DESC)、t_vip_order(user_id, transaction_id UNIQUE)。"),
    H2("5.3  数据保留策略"),P("t_behavior_event按月分区，90天后归档删除仅保留摘要。t_alert保留180天。视频片段：免费24h VIP 30天（MinIO lifecycle policy）。t_audit_log保留180天。"),PB()];

  // Ch6 - Security
  let c6=[H1("6  安全设计"),
    MT(["层","措施"],[["传输","全站HTTPS + WSS(WebSocket over TLS)"],["认证","JWT双令牌(access 2h + refresh 7d)，Redis黑名单"],["授权","@PreAuthorize + @AuthCheck(role) AOP注解"],["数据","phone/email/rtsp_password AES-256加密存储，password BCrypt哈希"],["支付","回调RSA验签，金额服务端二次校验，transaction_id唯一幂等"],["注入防护","Spring Security XSS过滤，MyBatis-Plus参数预编译防SQL注入"],["限流","Redis+Lua令牌桶，登录接口 5次/分钟/IP"],["文件","上传文件白名单(jpg/png/mp4)，后端校验MIME和魔数"],["审计","管理员所有操作+支付操作+专家审核→全量写t_audit_log"]],[2000,7026]),PB()];

  let app=[H1("附录"),
    H2("A  用户登录 时序图"),...insDiag("D01_seq_login"),
    H2("B  告警推送全链路 时序图"),...insDiag("D02_seq_alert"),
    H2("C  每日报告生成 时序图"),...insDiag("D03_seq_report"),
    H2("D  VIP支付 时序图"),...insDiag("D04_seq_pay"),
    H2("E  专家咨询 时序图"),...insDiag("D05_seq_expert"),
    H2("F  行为分析 状态机"),...insDiag("D06_state_behavior"),
    H2("G  核心Service 类图"),...insDiag("D07_class")];

  let doc=new Document({
    styles:{default:{document:{run:{font:"Arial",size:22}}},
      paragraphStyles:[
        {id:"Heading1",name:"Heading 1",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:32,bold:true,font:"Arial",color:HB},paragraph:{spacing:{before:360,after:200},outlineLevel:0}},
        {id:"Heading2",name:"Heading 2",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:28,bold:true,font:"Arial",color:AB},paragraph:{spacing:{before:280,after:160},outlineLevel:1}},
        {id:"Heading3",name:"Heading 3",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:26,bold:true,font:"Arial"},paragraph:{spacing:{before:200,after:120},outlineLevel:2}}]},
    sections:[cover,{properties:{...sp,page:{...sp.page,pageNumbers:{start:1}}},...HF,children:[H1("修订记录"),MT(["版本","日期","修订人","修订说明"],[["V1.0","2025-06-05","架构组","依据GB/T 8567-2006编制详细设计全稿"]],[1200,1800,1800,4226]),PB(),H1("目  录"),new TableOfContents("目录",{hyperlink:true,headingStyleRange:"1-3"}),PB()]},{properties:sp,...HF,children:[...c1,...c2,...ch3,...c4,...c5,...c6,...app]}]
  });
  return doc;
}

async function main(){
  console.log("Building GB/T 8567-2006 SDDD...");
  let doc=await buildDoc();
  let op=OUT_DIR+"\\AI宠物管家_详细设计说明书_GB8567_V1.0.docx";
  let buf=await Packer.toBuffer(doc);
  fs.writeFileSync(op,buf);
  console.log("Saved: "+op);
}
main().catch(e=>{console.error(e);process.exit(1);});
