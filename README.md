# TikTok Gaming 客户端训练营 — Android 作业项目

本项目基于 **Android + Java** 开发，完成客户端训练营 **作业一 & 作业二**。

---

## 项目概览

| 作业 | 功能说明 | 使用技术 |
|------|------|------|
| 作业一 Feed 视频瀑布流展示 + 播放 | 双列视频瀑布流、点击进入播放页 | Retrofit2、RecyclerView、Glide、ExoPlayer |
| 作业二 多模态口播脏话 + 情绪检测 | 本地直播视频播放、文本&声音结合情绪分析 | ExoPlayer、Visualizer、Spannable、高亮敏感词 |

---

## 功能展示

| 页面 | 描述 |
|------|------|
| Feed 页面 | 显示视频卡片、封面图、观看数、LIVE 标签 |
| 播放页面 | 使用 ExoPlayer 点击进入播放 |
| 情绪检测页面（作业二） | 字幕脏话识别 + 声音幅度情绪推断 + 实时状态变化 |

> ⚠ 声音检测需真机测试（模拟器无麦克风输入）

---

## 项目结构
com.leonardo.myapplication
├── MainActivity.java                  # 启动页面
├── AndroidManifest.xml                # 权限、Activity 声明
├── data
│   ├── api
│   │   └── ApiService.java            # Retrofit 接口
│   ├── model
│   │   └── VideoItem.java             # 视频数据模型
│   └── profanity
│       ├── ProfanityDetector.java     # 脏话检测核心逻辑
│       ├── ProfanityHighlighter.java  # 敏感词高亮显示
│       └── ProfanityResult.java       # 检测结果封装
├── ui
│   ├── feed
│   │   ├── FeedActivity.java          # 作业一推荐流页面
│   │   ├── FeedAdapter.java           # 卡片 Adapter
│   │   └── VideoViewHolder.java       # 视频卡片布局绑定
│   ├── player
│   │   └── PlayerActivity.java        # 视频播放页面 (ExoPlayer)
│   └── livecheck
│       └── LiveCheckActivity.java     # 作业二多模态分析页面
└── res
    ├── layout                         # XML UI 布局文件
    │   ├── activity_feed.xml
    │   ├── item_video.xml
    │   ├── activity_player.xml
    │   └── activity_live_check.xml
    ├── drawable                       # 图标、背景资源
    ├── mipmap                         # 应用图标
    └── raw
        └── live_clip.mp4              # 本地测试视频(中英口播)
