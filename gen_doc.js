const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, LevelFormat,
  TableOfContents, HeadingLevel, BorderStyle, WidthType, ShadingType,
  PageBreak, PageNumber, ImageRun
} = require("docx");

const OUT_DIR = "C:\\Users\\YL\\Desktop\\宠物平台";
const SRS_DIR = OUT_DIR + "\\diagrams\\srs";

// ============================================================
//  DIAGRAM REFERENCES (PlantUML PNGs)
// ============================================================
const DIAGRAMS = [
  {file:"D00_overview",  title:"系统角色与权限递进关系图"},
  {file:"D01_guest",     title:"游客用例图"},
  {file:"D02_user",      title:"普通用户 — 用户管理用例图"},
  {file:"D03_pet",       title:"普通用户 — 宠物档案管理用例图"},
  {file:"D04_camera",    title:"普通用户 — 摄像头管理用例图"},
  {file:"D05_behavior",  title:"普通用户 — AI行为分析用例图"},
  {file:"D06_health",    title:"普通用户 — 健康管理用例图"},
  {file:"D07_reminder",  title:"普通用户 — 智能提醒用例图"},
  {file:"D08_feeding",   title:"普通用户 — AI饲养建议用例图"},
  {file:"D09_news",      title:"普通用户 — 养宠资讯浏览用例图"},
  {file:"D10_vip",       title:"VIP用户增值服务用例图"},
  {file:"D11_admin_user",title:"管理员 — 用户管理用例图"},
  {file:"D12_admin_expert",title:"管理员 — 专家管理用例图"},
  {file:"D13_admin_sys", title:"管理员 — 系统配置与数据统计用例图"},
];

// ============================================================
//  DOCX HELPERS
// ============================================================
const PW=11906,PH=16838,MG=1440;
const HB="1F4E79",AB="2E75B6",AR="F2F7FB";
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
  return new Table({width:{size:tw,type:WidthType.DXA},columnWidths:cw,
    rows:[new TableRow({tableHeader:true,children:headers.map((h,i)=>MC(h,{w:cw[i],b:true,z:20,s:HB}))}),
      ...rows.map((row,ri)=>new TableRow({children:row.map((c,ci)=>MC(c,{w:cw[ci],s:ri%2===0?AR:undefined,z:20}))}))]});
}
function UCT(rows){return MT(["编号","用例名称","简要描述"],rows,[1600,2400,5026]);}
function DT(rows){return MT(["项目","说明"],rows,[2000,7026]);}
function IMG(pngData,title){
  return new Paragraph({alignment:AlignmentType.CENTER,spacing:{before:240,after:80},children:[new ImageRun({type:"png",data:pngData,transformation:{width:480,height:300},altText:{title,description:title,name:title}})]});
}
function ICAP(text){return new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:280},children:[new TextRun({text,size:20,italics:true,color:"666666"})]});}

function insertDiagram(fileId){
  let d=DIAGRAMS.find(d=>d.file===fileId);
  if(!d)return[];
  let png=fs.readFileSync(SRS_DIR+"\\"+d.file+".png");
  return [IMG(png,d.title),ICAP("图 "+d.title)];
}

// ============================================================
//  BUILD DOCUMENT
// ============================================================
function buildDoc(){
  const HF={
    headers:{default:new Header({children:[new Paragraph({alignment:AlignmentType.RIGHT,border:{bottom:{style:BorderStyle.SINGLE,size:4,color:AB,space:4}},children:[new TextRun({text:"AI宠物管家 \u2014 软件需求规格说明书",size:18,color:"888888",italics:true})]})]})},
    footers:{default:new Footer({children:[new Paragraph({alignment:AlignmentType.CENTER,border:{top:{style:BorderStyle.SINGLE,size:2,color:"CCCCCC",space:4}},children:[new TextRun({text:"第 ",size:18,color:"888888"}),new TextRun({children:[PageNumber.CURRENT],size:18,color:"888888"}),new TextRun({text:" 页",size:18,color:"888888"})]})]})}};

  let cover={properties:sp,children:[
    new Paragraph({spacing:{before:2400}}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:600},children:[new TextRun({text:"AI宠物管家平台",size:52,bold:true,color:HB})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:200},children:[new TextRun({text:"软件需求规格说明书",size:40,bold:true,color:AB})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:200},children:[new TextRun({text:"Software Requirements Specification (SRS)",size:22,color:"666666",italics:true})]}),
    new Paragraph({spacing:{before:1200}}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:80},border:{top:{style:BorderStyle.SINGLE,size:6,color:AB,space:12}},children:[new TextRun({text:"版本号：V1.0",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:80},children:[new TextRun({text:"文档编号：PRJ-PETAI-SRS-001",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:80},children:[new TextRun({text:"编制日期：2025年6月",size:24})]}),
    new Paragraph({alignment:AlignmentType.CENTER,spacing:{after:80},children:[new TextRun({text:"密级：内部",size:24})]})]};

  let rev=[H1("修订记录"),
    MT(["版本","日期","修订人","修订说明"],[["V0.1","2025-05-28","产品组","初始草稿，完成AI宠物管家功能框架与用例梳理"],["V0.2","2025-06-01","产品组","增加VIP专家咨询与后台专家管理模块"],["V1.0","2025-06-02","产品组","整合审阅，形成正式V1.0发布版本"]],[1200,1800,1800,4226]),
    PB(),H1("目  录"),new TableOfContents("目录",{hyperlink:true,headingStyleRange:"1-3"}),PB()];

  let ch1=[H1("1  引言"),H2("1.1  编写目的"),
    P("本文档旨在对AI宠物管家平台进行全面的需求分析，明确系统的功能边界、用户角色、业务流程与非功能约束，作为后续系统设计、开发、测试及验收的共同依据。"),
    P("预期读者包括：产品经理与业务方、开发团队、测试团队、项目管理者。"),
    H2("1.2  项目背景"),
    P("随着宠物经济的快速发展，养宠人群对宠物健康的关注度日益提升。然而，大多数宠主白天需上班，无法实时了解宠物的进食、运动、睡眠等行为状态。目前市场上摄像头产品仅提供画面查看，缺乏AI行为分析与主动告警能力；而专业兽医咨询服务门槛高、不便捷。"),
    P("AI宠物管家旨在打造一款以AI摄像头监控+行为分析为核心，融合健康管理、智能提醒、饲养建议与专家咨询于一体的智能宠物看护平台，帮助宠主\"看得见\"宠物的一举一动，\"看得懂\"数据背后的含义。"),
    H2("1.3  范围"),
    P("a) 摄像头接入与实时画面查看——支持RTSP/ONVIF协议的网络摄像头。"),
    P("b) AI行为分析——进食、饮水、运动、睡眠、排泄行为识别与异常告警。"),
    P("c) 宠物档案——宠物基础信息与健康记录管理。"),
    P("d) AI饲养建议——基于品种、年龄、体重及行为数据的通用/定制饲养方案。"),
    P("e) 智能提醒——每日报告、异常告警、健康到期、生日祝福等。"),
    P("f) VIP增值——趋势分析、AI健康预警、定制饲养计划、专家在线咨询、视频云存储。"),
    P("g) 后台管理——用户管理、专家审核与排班、AI模型配置、运营数据统计。"),
    H2("1.4  术语定义"),MT(["术语","说明"],[["SRS","软件需求规格说明书"],["RTSP","Real Time Streaming Protocol，网络摄像头实时流协议"],["CV","Computer Vision，计算机视觉"],["FCM","Firebase Cloud Messaging，移动端推送服务"],["VIP","Very Important Person，平台付费会员"]],[2000,7026]),
    H2("1.5  参考文献"),P("a) GB/T 9385-2008；b) GB/T 8567-2006。"),PB()];

  let ch2=[H1("2  综合描述"),H2("2.1  产品描述"),
    P("AI宠物管家是一个基于AI视觉分析的智能宠物看护平台，采用B/S + App + 微信机器人三端架构。系统以摄像头接入为入口，通过计算机视觉（CV）技术对宠物行为进行实时识别与分析，结合大语言模型（LLM）生成每日行为报告与个性化饲养建议。消息推送通过双通道触达用户——App推送（FCM/APNs）负责实时告警，微信机器人以好友聊天方式推送每日报告和温馨提醒。平台还提供精选养宠资讯供用户浏览，提升日常打开频次。VIP用户额外享受深度趋势分析、AI健康预警、定制饲养计划和线上专家咨询服务。"),
    H2("2.2  产品功能总览"),...insertDiagram("D00_overview"),
    P("a) 游客——浏览功能介绍与示例报告，可注册为普通用户。"),
    P("b) 普通用户——接入摄像头，享受完整AI行为监控、每日报告、健康管理、智能提醒，浏览精选养宠资讯，绑定微信机器人接收消息。"),
    P("c) VIP用户——在普通用户基础上，增加趋势分析、AI健康预警、定制饲养计划、专家在线咨询、视频云存储、资讯全文阅读等。"),
    P("d) 管理员——后台管理用户、审核专家、配置AI模型与系统参数、查看运营数据。"),
    H2("2.3  用户特征"),MT(["用户角色","技术背景","使用频率","关键诉求"],[["游客","无需技术背景","低频","了解平台，快速注册"],["普通用户","基本App操作能力","中频（日均1-3次）","实时监控宠物状态，接收异常告警"],["VIP用户","基本App操作能力","高频（日均3-5次）","深度分析数据趋势，获取定制建议与专家咨询"],["管理员","具备后台管理经验","高频（工作日）","高效审核、配置管理与数据分析"]],[1500,2000,2000,3526]),
    H2("2.4  约束"),P("a) 主流浏览器及iOS/Android最新两个版本；b) 满足《个人信息保护法》要求；c) 视频处理延迟不超过5秒（端到端）；d) AI分析置信度不低于85%；e) 推送延迟不超过10秒。"),
    H2("2.5  假设与依赖"),P("a) 用户家中有WiFi网络且带宽不低于5Mbps上行；b) 摄像头支持RTSP或ONVIF协议；c) CV模型与LLM服务稳定可用；d) 第三方推送渠道可达。"),PB()];

  // Ch3
  let ch3=[H1("3  功能性需求")];

  ch3.push(H2("3.1  游客用例"),...insertDiagram("D01_guest"),
    UCT([["UC-T-01","浏览功能介绍","了解平台AI摄像头监控、行为分析、每日报告等特色功能。"],["UC-T-02","查看示例报告","浏览匿名化的宠物行为分析示例日报，直观感受AI分析能力。"],["UC-T-03","注册账号","填写手机/邮箱、密码、昵称，注册成为普通用户。"]]),PB());

  ch3.push(H2("3.2  普通用户用例"),P("普通用户自动继承游客权限，并额外拥有以下模块。"));

  ch3.push(H3("3.2.1  用户管理"),...insertDiagram("D02_user"),
    UCT([["UC-U-01","登录系统","账号密码或验证码登录。"],["UC-U-02","找回密码","注册手机或邮箱重置密码。"],["UC-U-03","修改个人资料","编辑昵称、简介、联系方式。"],["UC-U-04","修改头像","上传并裁剪头像。"],["UC-U-05","升级VIP","选择套餐并完成支付。"],["UC-U-06","绑定微信机器人","扫码添加平台微信机器人为好友，开启微信渠道推送。"]]));

  ch3.push(H3("3.2.2  宠物档案管理"),...insertDiagram("D03_pet"),
    UCT([["UC-P-01","新增宠物档案","录入名称、品种、性别、生日、体重并上传照片（免费版限1只）。"],["UC-P-02","编辑宠物信息","修改已创建宠物的各项档案。"],["UC-P-03","删除宠物档案","删除宠物及全部关联数据（需二次确认）。"],["UC-P-04","查看宠物档案详情","查看完整档案及健康记录汇总。"]]));

  ch3.push(H3("3.2.3  摄像头管理"),...insertDiagram("D04_camera"),
    UCT([["UC-CA-01","添加摄像头","输入RTSP地址或扫码配对，绑定摄像头。"],["UC-CA-02","配置监控区域","在画面中划定活动区域以提升分析准确度。"],["UC-CA-03","查看实时画面","App内查看摄像头实时画面。"],["UC-CA-04","移除摄像头","解除摄像头绑定。"]]));
  ch3.push(PB());

  ch3.push(H3("3.2.4  AI行为分析"),...insertDiagram("D05_behavior"),
    UCT([["UC-A-01","查看实时行为状态","当前行为标签（进食中/饮水中/运动中/睡眠中/排泄中）。"],["UC-A-02","查看行为事件时间线","当日所有行为事件的时间轴及对应短视频片段。"],["UC-A-03","查看当日行为摘要","进食次数、饮水次数、运动时长、睡眠时长、排泄次数的每日汇总。"],["UC-A-04","接收异常行为告警","长时间未进食/未饮水/长时间不动等异常自动告警。"]]));

  ch3.push(H3("3.2.5  健康管理"),...insertDiagram("D06_health"),
    UCT([["UC-H-01","添加疫苗记录","疫苗名称、接种日期、有效期、备注。"],["UC-H-02","添加驱虫记录","驱虫类型（体内/体外）、用药名称、日期、下次驱虫时间。"],["UC-H-03","添加体检记录","体检日期、项目、结果、附件。"],["UC-H-04","添加医疗/用药记录","就诊原因、诊断结果、用药名称、剂量、起止时间。"],["UC-H-05","查看健康记录列表","按时间或类型筛选查看。"],["UC-H-06","删除/编辑健康记录","修改或删除某条记录。"]]));
  ch3.push(PB());

  ch3.push(H3("3.2.6  智能提醒"),...insertDiagram("D07_reminder"),
    P("消息通过双通道触达用户：App推送（FCM/APNs）用于实时告警，微信机器人以好友聊天方式推送每日报告和温馨提醒。用户可任选或同时开启。"),
    UCT([["UC-R-01","接收每日报告推送","每晚定时通过App推送+微信机器人推送当日行为总结与明日建议。"],["UC-R-02","接收异常告警推送","通过App实时推送不吃、不喝、长时间不动等异常告警；若用户5分钟内未查看，微信机器人追加提醒。"],["UC-R-03","接收健康到期提醒","疫苗到期、驱虫到期通过App推送提醒。"],["UC-R-04","接收宠物生日提醒","生日当天App推送+微信机器人祝福。"],["UC-R-05","管理提醒设置","开启/关闭各渠道提醒、设置免打扰时段、选择提醒方式（App/微信/双通道）。"]]));

  ch3.push(H3("3.2.7  AI饲养建议"),...insertDiagram("D08_feeding"),
    UCT([["UC-F-01","查看通用饲养指南","基于品种+年龄+体重，获取标准喂食量、运动量、护理要点。"],["UC-F-02","查看每日AI饲养建议","结合当日行为数据（如运动偏少），给出明日饮食和运动调整建议。"]]));

  ch3.push(H3("3.2.8  养宠资讯浏览"),...insertDiagram("D09_news"),
    P("资讯模块提供平台精选的养宠内容，支持信息流浏览，提升用户日常打开频次。所有内容由平台编辑和认证专家撰写，不开放UGC发布与评论。"),
    UCT([["UC-N-01","浏览推荐资讯流","首页信息流展示AI个性化推荐的文章，基于用户宠物品种、年龄、季节等因素。"],["UC-N-02","按分类浏览资讯","按\"品种养护\"\"营养食谱\"\"疾病预防\"\"行业动态\"\"训练技巧\"等分类浏览。"],["UC-N-03","收藏资讯","收藏感兴趣的文章，方便在个人中心回顾。"],["UC-N-04","搜索资讯","按关键词搜索文章标题与内容。"],["UC-N-05","查看品种百科","按宠物品种分类浏览系统化的百科知识库（性格、饲养要点、常见疾病等）。"]]),
    P("VIP用户额外享受：资讯全文阅读（免费版仅摘要）、AI关联推荐（\"你家金毛最近运动偏少→推荐金毛运动量科普\"）。"));
  ch3.push(PB());

  ch3.push(H2("3.3  VIP用户用例"),P("VIP拥有普通用户全部用例，并额外拥有以下功能："),...insertDiagram("D10_vip"),
    UCT([["UC-V-01","查看趋势分析图表","周/月/季行为趋势曲线、体重变化曲线可视化。"],["UC-V-02","接收AI健康预警","趋势异常→疾病风险预判（如食量连续3天下降→消化系统预警）。"],["UC-V-03","获取AI定制饲养计划","综合品种、年龄、体重、行为趋势，动态调整每日喂食克数、运动时长建议。"],["UC-V-04","导出分析报告","周报/月报一键导出PDF，可分享给兽医。"],["UC-V-05","云端视频存储与回放","行为事件录像云端保存30天，可回放历史片段。"],["UC-V-06","多宠对比看板","多只宠物数据并列对比。"],["UC-V-07","自定义提醒阈值","自定义告警触发条件（如运动<20分钟提醒）。"],["UC-V-08","宠物专家线上咨询","文字/图片/视频通话方式向平台认证执业兽医或训犬师咨询。"],["UC-V-09","不限宠物数量","可添加多只宠物。"],["UC-V-10","不限摄像头数量","多摄像头多角度覆盖。"],["UC-V-11","VIP续费管理","管理自动续费协议，续费或到期取消。"]]));
  ch3.push(PB());

  ch3.push(H2("3.4  管理员用例"),P("管理员通过后台管理系统执行以下用例："));
  ch3.push(H3("3.4.1  用户管理"),...insertDiagram("D11_admin_user"),
    UCT([["UC-M-01","查询用户","按用户名、时间、状态搜索。"],["UC-M-02","封禁/解封用户","违规用户封禁或解除。"],["UC-M-03","调整用户权限","手动赋予或收回VIP。"],["UC-M-04","查看用户详情","资料、宠物数、使用记录。"]]));
  ch3.push(H3("3.4.2  专家管理"),...insertDiagram("D12_admin_expert"),
    UCT([["UC-M-05","审核/入驻专家","审核执业兽医/训犬师资质，通过后上线。"],["UC-M-06","管理专家排班","设置在线时段。"],["UC-M-07","查看咨询记录","查阅咨询内容与评价。"]]));
  ch3.push(H3("3.4.3  系统配置与数据统计"),...insertDiagram("D13_admin_sys"),
    UCT([["UC-M-08","AI模型配置","调整行为识别模型参数、切换版本。"],["UC-M-09","告警阈值管理","全局默认异常告警阈值配置。"],["UC-M-10","系统参数配置","推送频率、分析间隔等。"],["UC-M-11","查看运营数据","用户增长、日活、VIP转化率、设备接入量。"],["UC-M-12","导出统计报表","Excel/PDF导出。"],["UC-M-13","操作日志查看","管理员敏感操作审计。"]]));
  ch3.push(PB());

  ch3.push(H2("3.5  核心用例详细规约"));
  ch3.push(H3("3.5.1  UC-CA-01  添加摄像头"),
    DT([["用例编号","UC-CA-01"],["用例名称","添加摄像头"],["参与者","普通用户/VIP用户"],["前置条件","用户已登录，拥有WiFi环境，摄像头已通电并连接同局域网。"],["基本事件流","1. 用户进入\"设备管理\"页面，点击\"添加摄像头\"。\n2. 系统展示两种添加方式：扫码配对 / 手动输入RTSP地址。\n3a. 扫码：系统调用相机扫描摄像头底部的二维码→自动解析RTSP地址。\n3b. 手动：用户输入RTSP地址（如rtsp://192.168.1.x:554/stream）、用户名、密码。\n4. 系统尝试连接摄像头，显示连接进度。\n5. 连接成功后，展示实时预览画面，用户确认设备命名（如\"客厅摄像头\"）。\n6. 用户为当前设备选择监控的宠物。\n7. 系统保存设备配置，开始拉流分析。"],["备选事件流","4a. 连接超时——提示\"无法连接摄像头，请检查网络与地址\"，允许重试。\n4b. 免费版已达1个上限——提示\"升级VIP可添加更多摄像头\"。\n5a. 画面模糊或过暗——提示\"当前画面质量较低，可能影响分析准确度\"。"],["后置条件","摄像头写入设备表，系统启动该设备的视频拉流与分析任务。"]]));

  ch3.push(H3("3.5.2  UC-A-04  接收异常行为告警"),
    DT([["用例编号","UC-A-04"],["用例名称","接收异常行为告警"],["参与者","普通用户/VIP用户"],["前置条件","用户已登录，摄像头在线，AI行为分析服务正常运行。"],["基本事件流","1. CV模型从视频流中持续检测宠物行为。\n2. 当满足告警触发条件时（如30分钟内未检测到进食行为，且当前处于常规进食时段），系统生成一条告警事件。\n3. 系统通过FCM/APNs向用户移动端推送告警通知，标题为\"[宠物名]进食异常\"，内容为\"已30分钟未检测到进食\"。\n4. 用户点击通知，打开App并跳转至实时画面+行为事件时间线页面。\n5. 系统高亮标注异常时间段，展示对应的短视频片段。"],["备选事件流","2a. 摄像头被遮挡——系统推送\"摄像头视线受阻\"告警，而非行为告警。\n2b. 宠物不在画面中——暂停行为分析，推送\"未检测到宠物\"提示。\n3a. 用户处于免打扰时段——延迟到免打扰结束后推送汇总。"],["后置条件","告警事件写入数据库，关联短视频片段。用户可在告警历史中查看。"]]));

  ch3.push(H3("3.5.3  UC-V-03  获取AI定制饲养计划"),
    DT([["用例编号","UC-V-03"],["用例名称","获取AI定制饲养计划"],["参与者","VIP用户"],["前置条件","用户为VIP，宠物档案完整，摄像头已接入并累积至少7天行为数据。"],["基本事件流","1. VIP用户进入某宠物详情页，点击\"AI饲养计划\"。\n2. 系统展示最近7天的行为趋势摘要（进食、运动、睡眠变化）。\n3. 用户点击\"生成最新计划\"，系统将以下数据发送至LLM：品种、年龄、体重、7天行为趋势、当前季节。\n4. LLM返回结构化建议：每日推荐喂食量（克数/次数）、推荐运动时长、饮水量建议、注意事项。\n5. 用户可对每个建议项做\"采纳\"/\"微调\"操作，微调后手动修改目标值。\n6. 采纳的计划写入数据库，次日起作为推送日报中的参考建议。"],["备选事件流","3a. 行为数据不足7天——提示\"数据累积不足，目前基于品种标准值生成建议\"。\n4a. LLM返回的喂食量超出安全范围——系统按品种安全区间自动修正并标注\"已自动校正\"。\n5a. 用户长期不采纳——系统在3天后询问\"是否需要调整建议方向\"。"],["后置条件","定制计划保存，系统每日对比实际行为与计划目标的偏差，在日报中提示。"]]));

  ch3.push(H3("3.5.4  UC-V-08  宠物专家线上咨询"),
    DT([["用例编号","UC-V-08"],["用例名称","宠物专家线上咨询"],["参与者","VIP用户、平台认证专家"],["前置条件","用户为VIP，已登录，处于专家在线时段。"],["基本事件流","1. VIP用户进入\"专家咨询\"模块，系统展示当前在线专家列表（头像、科室、评分、收费标准）。\n2. 用户选择一位专家，进入咨询发起页，可选择沟通方式：文字咨询 / 图片+文字 / 视频通话。\n3. 用户简要描述问题，可选附带宠物的近期行为数据摘要和健康档案供专家参考。\n4. 用户点击\"开始咨询\"，系统建立咨询会话。\n5. 专家端收到请求，确认接单后双向沟通开始。\n6. 咨询过程中支持发送文字、图片和实时视频画面。\n7. 咨询结束后，用户对专家进行评分和评价。"],["备选事件流","2a. 当前无在线专家——提示\"专家已休息，可留言预约\"，支持预约指定时段。\n4a. 专家5分钟内未接单——系统提示\"专家暂未响应，是否等待或转接其他专家\"。\n5a. 视频通话质量差——自动降级为语音通话。\n6a. 聊天内容涉及紧急医疗——系统提示\"如有紧急情况请立即前往宠物医院\"。"],["后置条件","咨询记录写入数据库，评价计入专家评分，咨询费用按实际时长结算。"]]));

  ch3.push(H3("3.5.5  UC-R-02  接收异常告警与推送链路"),
    DT([["用例编号","UC-R-02"],["用例名称","异常告警推送完整链路（App+微信双通道）"],["参与者","系统（自动触发）、普通用户/VIP用户"],["前置条件","AI行为分析引擎持续运行，App推送服务和微信机器人均已配置。"],["基本事件流","1. CV模型持续从摄像头视频流中提取逐帧画面，进行行为分类。\n2. 当行为进入异常判定窗口（如\"无进食\"窗口累计达30分钟，且当前处于该宠物常规进食时段±2小时），行为分析引擎生成告警事件写入事件队列（Kafka/Redis Stream）。\n3. 告警消费服务读取事件，获取用户推送设备Token及微信绑定状态。\n4. 优先通过FCM/APNs向用户App推送告警通知（标题+内容+跳转DeepLink）。\n5. 若用户5分钟内未查看告警，系统通过微信机器人向用户微信发送补充提醒：\"豆豆好像还没吃饭，快看看怎么回事？\"\n6. 用户点击App通知→打开告警详情页；或在微信中回复关键词（如\"看看\"）→机器人回复实时截图或短视频片段。\n7. VIP用户若15分钟内仍未查看，系统追加一条短信提醒。"],["备选事件流","2a. 连续10分钟无法检测到宠物——App推送+微信均提示\"未检测到宠物\"。\n4a. App推送不可达——直接走微信机器人渠道。\n4b. 微信机器人未绑定——仅走App推送。\n4c. 用户处于免打扰时段——告警缓存，结束后双通道批量推送汇总。"],["后置条件","告警事件入库，标记推送状态（App已送达/微信已发/已读/超时），纳入运营统计。"]]));
  ch3.push(PB());

  let ch4=[H1("4  非功能性需求"),
    H2("4.1  性能需求"),P("a) 视频端到端延迟≤5秒（摄像头到App画面）。b) AI行为识别延迟≤3秒（行为发生到标签更新）。c) 推送延迟≤10秒（告警产生到手机收到）。d) 支持1000路并发视频流同时分析。e) 每日报告生成时间≤30秒。"),
    H2("4.2  安全性需求"),P("a) 摄像头RTSP密码加密存储。b) 视频流传输使用加密通道。c) 个人信息遵循《个人信息保护法》。d) RBAC权限控制。e) 管理员操作全量审计日志。"),
    H2("4.3  可用性需求"),P("a) 核心服务可用性≥99.9%。b) 摄像头离线时保存最后状态并推送离线通知。c) CV模型服务不可用时自动降级为预设规则告警。"),
    H2("4.4  可维护性需求"),P("a) AI模型支持热切换，无需停服。b) 告警阈值后台可动态配置。c) 统一日志格式，支持集中采集。"),
    H2("4.5  兼容性需求"),P("a) iOS 14+、Android 10+。b) 支持H.264/H.265编码的RTSP摄像头。c) 网络带宽建议上行≥2Mbps/路。"),PB()];

  let ch5=[H1("5  数据需求"),H2("5.1  核心数据实体"),
    MT(["实体","关键属性","说明"],[["用户(User)","ID、昵称、手机号、密码哈希、VIP状态、VIP到期时间、推送Token","平台用户核心信息"],["宠物(Pet)","ID、用户ID、名称、品种、性别、生日、体重、照片","宠物档案"],["摄像头(Device)","ID、用户ID、RTSP地址、设备名、状态、关联宠物ID","摄像头设备信息"],["行为事件(BehaviorEvent)","ID、设备ID、宠物ID、行为类型、开始/结束时间、置信度、视频片段URL","CV识别到的行为事件"],["行为摘要(BehaviorSummary)","ID、宠物ID、日期、进食次数、饮水次数、运动时长、睡眠时长、排泄次数","每日汇总"],["告警事件(AlertEvent)","ID、宠物ID、告警类型、触发时间、推送状态","异常告警记录"],["健康记录(HealthRecord)","ID、宠物ID、类型、详细数据JSON","疫苗/驱虫/体检/用药"],["专家咨询(Consultation)","ID、用户ID、专家ID、开始/结束时间、类型、评分","咨询会话"],["操作日志(AuditLog)","ID、管理员ID、操作类型、详情JSON","管理员审计"]],[1500,3500,4026]),
    H2("5.2  数据保留"),P("a) 行为事件保留90天，90天后仅保留每日摘要。b) 告警事件保留180天。c) 视频片段：免费版保留24小时，VIP云端保留30天。d) 操作日志保留不少于180天。e) 每日自动备份，备份周期不超过24小时。"),
    H2("5.3  视频存储估算"),P("每路摄像头按每天约20个行为事件（每个30秒片段），免费版24小时内覆盖，VIP云端30天约600个片段，每段约10MB，总计约6GB/路/月。"),PB()];

  let ch6=[H1("6  附录"),H2("6.1  用例索引"),
    MT(["角色","模块","用例编号范围","数量"],[["游客","浏览与注册","UC-T-01 ~ UC-T-03","3"],["普通用户","用户管理","UC-U-01 ~ UC-U-06","6"],["普通用户","宠物档案","UC-P-01 ~ UC-P-04","4"],["普通用户","摄像头管理","UC-CA-01 ~ UC-CA-04","4"],["普通用户","AI行为分析","UC-A-01 ~ UC-A-04","4"],["普通用户","健康管理","UC-H-01 ~ UC-H-06","6"],["普通用户","智能提醒","UC-R-01 ~ UC-R-05","5"],["普通用户","AI饲养建议","UC-F-01 ~ UC-F-02","2"],["普通用户","养宠资讯","UC-N-01 ~ UC-N-05","5"],["VIP用户","增值服务","UC-V-01 ~ UC-V-11","11"],["管理员","用户管理","UC-M-01 ~ UC-M-04","4"],["管理员","专家管理","UC-M-05 ~ UC-M-07","3"],["管理员","系统与统计","UC-M-08 ~ UC-M-13","6"],[],[["总计","","","63个用例，覆盖4种角色、13个功能模块"]],],[1500,2000,2500,3026]),
    H2("6.2  待确认事项"),P("a) CV行为识别模型的选型（YOLO/MediaPipe/自研）及合作方待确认。b) VIP套餐定价与咨询费用分成方案待业务侧确认。c) 推送服务选型（极光/个推/FCM）待评估。d) 摄像头兼容性测试品牌列表待补充。e) 专家入驻资质审核标准与合作协议待法务侧确认。")];

  let doc=new Document({styles:{default:{document:{run:{font:"Arial",size:22}}},
    paragraphStyles:[
      {id:"Heading1",name:"Heading 1",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:32,bold:true,font:"Arial",color:HB},paragraph:{spacing:{before:360,after:200},outlineLevel:0}},
      {id:"Heading2",name:"Heading 2",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:28,bold:true,font:"Arial",color:AB},paragraph:{spacing:{before:280,after:160},outlineLevel:1}},
      {id:"Heading3",name:"Heading 3",basedOn:"Normal",next:"Normal",quickFormat:true,run:{size:26,bold:true,font:"Arial"},paragraph:{spacing:{before:200,after:120},outlineLevel:2}}]},
    sections:[cover,{properties:{...sp,page:{...sp.page,pageNumbers:{start:1}}},...HF,children:rev},{properties:sp,...HF,children:[...ch1,...ch2,...ch3,...ch4,...ch5,...ch6]}]});
  return doc;
}

async function main(){
  console.log("Building document...");
  let doc=buildDoc();
  let op=OUT_DIR+"\\AI宠物管家_GB需求规格说明书_V1.1.docx";
  let buf=await Packer.toBuffer(doc);
  fs.writeFileSync(op,buf);
  console.log("Saved: "+op);
}
main().catch(e=>{console.error(e);process.exit(1);});
