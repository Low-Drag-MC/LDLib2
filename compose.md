# LDLib2 Compose — 声明式 UI

数据结构就是 UI 树。改状态，树自己跟着变 —— 不用 `clearAllChildren()`，不用 reload，不用脏标记。

```kotlin
@Composable
fun DynamicButtonList() {
    var items by remember { mutableStateOf(listOf("A", "B", "C")) }

    ScrollView {
        items.forEach { item ->
            key(item) {
                Button(onClick = { println(item) }, text = Component.literal(item))
            }
        }
        Button(
            onClick = { items = items + "Button ${items.size + 1}" },
            text = Component.literal("添加按钮"),
        )
    }
}
```

游戏里跑一下：`/ldlib2_screen_test compose`（还有 `compose_todo`、`compose_widgets`、`compose_lazy`）。

---

## 这是什么

**运行时就是真的 Jetpack Compose**（`androidx.compose.runtime`），不是仿写。`@Composable`、重组、快照、`remember`、`key`、effects 的语义和 Android 上完全一致。

LDLib2 提供的是它下面那半截：一个把 Compose 的树编辑应用到 `UIElement` 树上的 `Applier`，和一个挂在渲染循环上的帧时钟。

**上面那半截（`compose.ui`）没有引入**，所以布局是 Taffy 的 flexbox/grid、样式是 LSS 级联、事件是 DOM 式冒泡 —— 都是 LDLib2 自己的。差异见[已知边界](#已知边界)。

---

## 接入

**这一层不在 LDLib2 主 jar 里。** 它是一个独立的可选模组 `ldlib2-compose`（modid `ldlib2_compose`）：不装它，LDLib2 和以前完全一样；装了才多出这一层。核心里只多了四个不提 compose 的通用钩子（`UIElement#reorderChildren`、`UIElement#getContentHost`、`ModularUI#addFrameCallback`、`ScrollerView#getContentHost`）。

做成**模组**而不是普通的库 jar，是因为它要带 Compose runtime，而 NeoForge 的 Jar-in-Jar **只解一层**：下游把 `ldlib2-compose` JiJ 进自己的 jar，只会拿到这个 jar，拿不到它里面嵌套的 runtime，运行时直接 `NoClassDefFoundError`。作为顶层模组，它自己的 JiJ 就在第一层，能正常加载。

### 本仓库

`module.compose` 子项目。核心的 `build.gradle` 里没有任何 compose 痕迹 —— 编译器插件、runtime、JiJ 声明全在 `module.compose/build.gradle`。`gradlew runClient` 会把两个模组一起装进去，`gradlew test` 会同时跑两边的单测。

### 下游模组

**要写 `@Composable` 就必须应用同一个编译器插件**，否则编译不过：

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.1.20'
    id 'org.jetbrains.kotlin.plugin.compose' version '2.1.20'  // 版本必须和 kotlin.jvm 一致
}

repositories {
    maven {
        // androidx.annotation / androidx.collection 是 compose runtime 的传递依赖，不在 Maven Central
        url = "https://maven.google.com"
        content { includeGroupByRegex "androidx\\..*" }
    }
}

dependencies {
    implementation "com.lowdragmc.ldlib2:ldlib2-compose-1.21.1:${ldlib2_version}"
}
```

`compose.runtime`、coroutines、androidx.collection 都是 `ldlib2-compose` 的 api 依赖，编译期跟着上面这一行进来，运行期由它 Jar-in-Jar 提供，不用再声明。

最后在 `neoforge.mods.toml` 里声明模组依赖，否则玩家少装一个 jar 就是启动崩溃：

```toml
[[dependencies.yourmod]]
modId = "ldlib2_compose"
type = "required"
versionRange = "[2.2.37,)"
```

---

## 建一个界面

```kotlin
@LDLRegisterClient(name = "my_screen", registry = "ldlib2:screen_test")
class MyScreen : IScreenTest {
    override fun createUI(player: Player?): ModularUI =
        composeModularUI(player, StylesheetManager.MODERN) { MyContent() }
}
```

想把声明式的一块塞进已有的传统树里：

```kotlin
parent.addChild(ComposeElement { MyContent() })
```

反过来，把已经建好的元素塞进声明式树里用 `Mount(element)`。

---

## 状态

和 Compose 一样。`remember` 记住跨重组的值，`mutableStateOf` 让读它的地方在它变化时重组。

```kotlin
var count by remember { mutableStateOf(0) }
Button(onClick = { count++ }, text = Component.literal("$count"))
```

**状态提到哪一层，决定了它什么时候消失。** 组件离开组合（`if` 变 false、列表项被删、行滚出 `LazyColumn`），它 `remember` 的东西就跟着没了。要活得更久就往上提：

```kotlin
// 选中态放在列表外面，行被回收也不影响
var selected by remember { mutableStateOf(-1) }
LazyColumn(items, itemHeight = 14f) { index, item ->
    Text(item, style = { backgroundTexture(if (index == selected) hi else none) })
}
```

### key

列表项要 `key`。有 key，重排就是移动元素（保住焦点、动画、内部状态）；没 key，只按位置匹配 —— 结果是行还在，但第 0 行的状态留给了新的第 0 行。

```kotlin
items.forEach { item -> key(item.id) { Row(item) } }
```

---

## 属性

外观和行为不是一条 Modifier 链，而是一组具名参数 —— 而且用的就是 LDLib2 自己那套 DSL，**一个新名字都不引入**：

```kotlin
Button(
    onClick = { … },
    text = Component.literal("确定"),
    layout = { width(60.px); padding { all(4.px) } },       // 接收者 = TaffyLayoutStyleDsl，和 layoutDsl { } 同一个
    style  = { backgroundTexture(Sprites.RECT_RD) },        // 接收者 = BasicStyle，和 styleDsl { } 同一个
    buttonStyle = { hoverTexture(Sprites.RECT_RD_LIGHT) },  // 控件自己那个 facade
    classes = "primary",
)
```

> 早先这里是 `UIModifier.width(60.px).background(…)` 的链式写法，**已经去掉**。Compose UI 的 Modifier 是有序包装链（`padding().background()` 和反过来视觉不同），那是它存在的理由；这里写的是同一个 `StyleBag` 里的属性，顺序无关 —— 保留链式只是留了个形，代价是 261 行手写别名和第二套要记的名字。

### 有哪些

| 参数 | |
|---|---|
| `layout` | 尺寸、间距、flex/grid、定位。接收者 `TaffyLayoutStyleDsl` |
| `style` | 背景、遮罩、透明度、z-index、tooltip、变换。接收者 `BasicStyle` |
| `xxxStyle` | 控件自己的样式 facade：`buttonStyle` `toggleStyle` `sliderStyle` `textStyle` `slotStyle` `scrollerStyle` …… |
| `classes` | 空格分隔的样式表类名 |
| `id` | 给 `#id` 选择器用 |
| `enabled` / `visible` / `focusable` | 元素开关。`enabled = false` 会加上 `__disabled__` 类 |
| `events` | 事件，见下 |

单位还是旧 DSL 那一套：`40.px`、`50.pct`、`auto`。

### 三种参数，三种脾气

| | 值变了 | 不写了 |
|---|---|---|
| `layout` / `style` / `xxxStyle` | 写进去 | **留在那，不会撤销** |
| `classes` | diff | 移除 |
| `enabled` / `visible` / 事件 | 写进去 | 不存在（总是传值） |

**样式块是写穿的** —— `element.styleDsl { }` 一直就是这个语义，这里没有发明新规则。所以条件性外观要注意：

```kotlin
textStyle = { if (hot) fontSize(18f) }          // ✗ hot 变 false 之后字还是大的
textStyle = { fontSize(if (hot) 18f else 9f) }  // ✓ 两个分支都给值
classes = if (hot) "label hot" else "label"     // ✓✓ 更好：交给样式表，class 是 diff 的
```

重复写同一个值几乎不要钱：`StyleBag.replaceOrPutCandidate` 发现值没变会直接 return，不脏化、不重算布局。

### 块里读状态：能用，而且比重组更省

```kotlin
val alpha by animateFloatAsState(target)
Box(style = { opacity(alpha) })   // ✓ 直接读，会跟着动
```

块是在 **apply 阶段**跑的，Compose 只在组合阶段追踪读取，所以这本来是不成立的 —— 早期版本这里确实是个陷阱。现在每个节点的块跑在一个 `SnapshotStateObserver` 里（和 Compose UI 给 `Modifier.drawBehind` / `graphicsLayer` 用的是同一招）：读取归属到**这个节点**，值变了就**只重跑这一个块**。

所以它比"把值提到函数体里"**更省** —— 那样会重组整个 composable，这样只是一次 `bag.set`。

两点要知道：

- **只有块能这么用。** `classes`、`enabled` 这些是普通参数，在函数体里求值，走的还是正常的重组依赖。
- **决定"组合什么"的值必须在函数体里读。** 块只能改属性，不能决定挂不挂载：

```kotlin
val o = alpha.coerceIn(0f, 1f)        // 函数体里读，因为下面这行要用它决定挂不挂
if (visible || o > 0f) { Box(style = { opacity(o) }) }
```

`AnimatedVisibility` 就是这个形状。

> 实现细节：陈旧的读取依赖靠快照 id 判定，所以一个"不再读某个状态"的块会**多被重跑一次**才把它丢掉。方向永远是多跑，不会漏跑，而多跑一次写的是相同的值，`StyleBag` 直接丢弃。

### 事件

```kotlin
Box(events = {
    onClick { count++ }
    onMouseEnter { hovered = true }
})
```

handler 每次重组都是新 lambda，所以它们被放在一个 `remember` 的持有器后面：注册一次，重组只换指向。直接反复 `addEventListener` 会每帧增删监听器，而 LDLib2 的监听器是前插的，顺序还会被搅乱。

按 `(类型, capture)` 去重，同一个事件写两次是覆盖不是叠加。没有具名助手的走 `on("someType") { }`。

> `onClick` 是 `mouseClick`（按下再松开）。`Button(onClick = …)` 是控件自带的，在 **mouse-down** 触发并播放点击音 —— 两者行为不同，都保留了原语义。

`onMouseEnter` / `onMouseLeave` 注册在**捕获阶段**，没得选：`ModularUI` 派发这两个事件时 `hasBubblePhase = false`，冒泡阶段的监听器永远不会触发；而且 target 是指针底下最内层的那个元素，所以容器要靠捕获才听得见自己孩子被划过。

### hover / pressed

**只是想换个外观的话不需要写代码。** 核心已经在悬停元素和它所有祖先上挂了 `__hovered__` 类：

```lss
.card.__hovered__ { background: ...; }
```

样式表做不到的是**改变组合内容** —— 换文字、多挂一个子元素、改动画目标。这种情况用：

```kotlin
val interaction = rememberInteraction()
Box(events = { track(interaction) }) {
    Text(Component.literal(if (interaction.hovered) "松开以删除" else "删除"))
}
```

`pressed` 没有对应的类，只有自己实现了三态贴图的控件才知道按下与否，所以那一半无论如何是新的。

### 控件自己的 *Style 为什么不需要特殊处理

`Style` 的构造函数拿的是**宿主元素的那一个 `StyleBag`**：

```java
public Style(UIElement holder) { this.styleBag = holder.getStyleBag(); }
```

所以那二十个 `*Style` 只是属性的分组命名，不是另一套系统。需要联动的控件也把监听器注册在 **Property** 上（见 `Toggle.ToggleStyle.init`），谁写的都一样触发。这里直接把那个 facade 当接收者交给你，中间没有任何一层。

一处要知道：**封装替你选的默认值写在 `DEFAULT` origin**（`Row` 的 flex 方向、`Text` 的自适应高度），你的 `layout` / `style` 在 `INLINE`。所以你永远压得过封装，样式表也压得过封装。

---

## 受控控件

有值的控件都是受控的：值由组合持有，控件把编辑回报上来。

```kotlin
var text by remember { mutableStateOf("") }
TextField(value = text, onValueChange = { text = it })
```

不接 `onValueChange` 的话，控件看起来就是只读的 —— 因为每次重组都会把组合里的旧值写回去。这是设计如此，和 Compose 一致。

`Switch` / `Toggle` / `Slider` / `Selector` / `ItemSlot` / `FluidSlot` / `ColorSelector` / `TagField` / `TextArea` / `CodeEditor` / `StructuredTagEditor` 都是这个形状。

---

## 容器

`Box` `Column` `Row` `ScrollView` `SplitViewHorizontal` `SplitViewVertical` `TabView` `Dialog` `LazyColumn` `LazyRow` `LazyVerticalGrid` `ToggleGroup` `Spacer`

### 长列表

```kotlin
LazyColumn(items = recipes, itemHeight = 18f, key = { _, r -> r.id }) { index, recipe ->
    RecipeRow(recipe)
}

LazyVerticalGrid(items = stacks, columns = 9, cellWidth = 18f, cellHeight = 18f) { _, stack ->
    ItemSlot(item = stack)
}
```

只组合看得见的那些 —— 一万行实测挂载不到 40 个元素。上下用空 spacer 撑住总高度，所以滚动条尺寸和位置仍是整个列表的。

两个前提：

- **行高/格子尺寸必须固定。** 要测量就得先造出来，而那正是要避免的事。
- **滚出视野的行会离开组合**，它 `remember` 的状态跟着没。选中态、展开态、编辑中的内容提到列表外面按 item 存。

### 带命名槽位的控件

`SplitView` 有两个面板，`TabView` 每页一个内容区 —— 一个 Applier 只认识一个落点，所以这些用**子组合**：

```kotlin
Subcompose(someContainer) { Whatever() }
```

它是真正的子组合（`rememberCompositionContext`），不是独立的：`CompositionLocal` 能穿过去，同帧重组，跟着父组合一起销毁。自己封装带槽位的控件时就用它。

---

## 动画

```kotlin
val alpha by animateFloatAsState(if (expanded) 1f else 0f, TweenSpec(200, easing = Eases.QUAD_OUT))
Box(style = { opacity(alpha) }) { ... }   // 块里直接读，不重组，只重跑这个块

// 弹性：跨目标切换保留速度，中途改目标是继续走而不是重新起步
val offset by animateFloatAsState(target, spring(dampingRatio = 0.6f, stiffness = 300f))

// 命令式：拖拽、序列、可中断
val x = rememberAnimatable(0f)
LaunchedEffect(Unit) {
    x.animateTo(100f, spring())
    x.animateTo(0f, TweenSpec(400))
}

AnimatedVisibility(visible = shown, animationSpec = TweenSpec(300)) {
    Text(Component.literal("淡入淡出"))
}
```

缓动用的是 LDLib2 自己的 `Eases`，和样式表 transition、`StyleAnimation` 同一套。

`AnimatedVisibility` 存在的意义在**淡出**：内容必须在树里多留一段时间，`if (visible)` 当帧就删了，没东西可动。目前只做透明度。

> 这一层驱动的是**组合状态**，所以读它的部分每帧重组一次。LDLib2 原有的 `StyleAnimation` 是直接改样式包、完全不碰组合 —— 两套机制都在，按需要选。

---

## 服务端数据

同步槽是按注册顺序编号且不复用的，所以 **binding 要在组合外面建好**，只把值桥进来：

```kotlin
// 组合外面，槽位顺序固定
private val progress = TrackData(0f)

// 组合里面
val value by progress.collectAsState()
```

三个桥：`IDataProvider.collectAsState()`、`TrackData.asMutableState()`、`IBinding.asMutableState()`。

轮询型的数据源（`ITickable`，比如 `SupplierDataSource`）会挂在宿主元素的 20Hz tick 上，跟着 tick 更新而不是跟着帧。

---

## 用没有封装的控件

**不需要给每个控件写封装。** 任何 `UIElement` 子类直接就能用：

```kotlin
UIElementNode(
    factory = { TreeList<TreeNode<String, String>>().setFlattenRoot(true) },
    layout = { size(240.px, 60.px) },
    update = { set(depth) { setRoot(root) } },
)
```

- `factory` 只跑一次 —— 一次性构造配置放这里
- `update` 每次重组跑，里面用 `set(值) { 应用 }` 做变化才执行

别的模组的控件同样如此。封装是语法糖（命名参数、修掉默认值、把易错的地方收口），不是前提。

> **坑：`set(值)` 是按 `equals` 判断要不要执行的。**
> 上面刻意写的是 `set(depth)` 而不是 `set(root)` —— `TreeNode.equals` 只比 key 和 content，不看子节点，两棵内容不同但根同名的树是"相等"的，`set(root)` 会静默永不触发。
> **按真正会变的东西做 key。** 实在要每次都跑，用 `reconcile { }`。

---

## 性能

500 行真实元素树（含样式引擎注册和布局节点），`-PldTest=compose_perf` 可复现：

| 操作 | 耗时 |
|---|---|
| 空闲帧 | ~0 |
| 更新 1 行 / 500 | 1.2ms |
| 更新 1 行 / 500（内容 hoist 后） | **0.2ms** |
| 追加 1 行 | 1.3ms |
| 反转 500 行 | 9ms |
| *clear+rebuild 500 行（旧写法）* | *2.6ms* |

> 这台机器上 `clear+rebuild` 那一行在 2.4–3.5ms 之间飘，所以别把比值当精确数字看。

三点值得知道：

**只有真的给了块才会被观察。** `layout` / `style` / `xxxStyle` / `events` 全为 null 时（`Text(t, classes = "row")` 就是），节点完全不进 `SnapshotStateObserver` —— 观察一个不可能读状态的东西只是白花钱，实测约 1.5µs 一个节点。

**外层 lambda 仍是 O(N)。** 列表变了，那个 `forEach` 会走完每一项 —— 只是每项都命中编译器插入的跳过，不重组。所以"改一行"不是 O(1)，而是"N 次很便宜的检查 + 1 次真更新"。真正的收益是**保住状态、焦点、动画**，不是纯速度。

**常数项在你手里。** 上表里 hoist 前后差 4 倍，区别只是把 `Component.literal(...)` 从循环里提出去：

```kotlin
// 每行每帧一次分配，而且 Component 不是 @Stable，这一行拦住了编译器的跳过
items.forEach { key(it.id) { Text(Component.literal(it.name), classes = "row") } }

// 提出去之后：参数全是稳定值，整个 Text 调用可以被跳过
items.forEach { key(it.id) { Text(it.nameComponent, classes = "row") } }
```

> 顺带一个和参数形状有关的事实：**传 lambda 会让控件调用不可跳过**（每次重组都是新实例）。所以热路径上的行，能用 `classes` 表达的就别写 `style = { }`。

---

## 测试

协调逻辑可以在 `module.compose/src/test` 里纯 JVM 测（那个 source set 不启动 Minecraft，而 Compose runtime 是纯 JVM 的）。`TestComposeHarness.kt` 提供逐帧步进的 harness —— 连动画都能精确测，不用 sleep：

```kotlin
val h = TestHarness()
h.setContent { ... }
h.frame()          // 一帧，默认推进 16ms
h.advance(200)     // 200ms 的帧
h.applier.inserts  // 断言这次改动确实是最小的
```

真实控件走 `uitest` 场景（见 `uitest.md`）。三个坑：

- **`click(selector)` 在 release 时会重新解析选择器**，而 LDLib2 按钮在 mouse-**down** 触发。所以点了会删掉自己、或者会被弹窗盖住的按钮，`click()` 必然报 no element matched —— 要一次解析、复用坐标。
- **点按钮会抢走焦点**（正常行为）。要测「重排是否保留焦点」得绕开真实点击直接派发 `MOUSE_DOWN`。
- **`ModularUI.select()` 返回的是注册顺序，不是文档顺序**，重排之后两者会分叉。顺序敏感的断言要走 `getChildren()` 逐层走树。

---

## 从 IDE 运行时的一个坑

如果第一次打开 compose 界面就崩，报错长这样：

```
IllegalAccessError: class kotlin.coroutines.jvm.internal.DebugProbesKt (in module kotlin.stdlib)
cannot access class kotlinx.coroutines.debug.internal.DebugProbesImpl (in module kotlinx.coroutines.core)
```

**这不是 mod 的问题，也不会发生在正式环境。** kotlinx-coroutines 的 jar 里带了一个 java agent
（manifest 的 `Premain-Class`），它会把 kotlin-stdlib 的 `DebugProbesKt` 改写成调用 coroutines 自己的
`DebugProbesImpl`。**IntelliJ 只要在 classpath 上看到 coroutines 就会自动挂这个 agent。** 改写后的类
仍属于 `kotlin.stdlib` 模块，而它不 read `kotlinx.coroutines.core` —— 于是**创建任何协程都会炸**，而
重组器的协程正好是 Compose 创建的第一个。

**解决**：在 IDE 设置里搜 "coroutine agent" 关掉它，或者直接用 `gradlew runClient` 跑。

这一侧没有能修的地方：`--add-reads` 到不了 ModLauncher 自建的 module layer（试过），agent 自身没有
开关，模块外部也加不了 read 边。所以 `ComposeHost` 只是把这个错误重新包装成一句人话。

## 已知边界

| | |
|---|---|
| **样式块无序、写穿不撤销** | 架构性的。底下是 CSS 级联 + Taffy，不是 Compose 的有序包装链。条件性外观走 `classes` |
| **块只能改属性** | 块里读状态会跟着变，但决定"挂不挂载"的值必须在函数体里读 |
| **手势** | 有 DOM 式事件冒泡和拖放（`DragHandler`），没有 `pointerInput` 那套 slop / 惯性 / 手势识别 |
| **`rememberSaveable`** | 没有。关闭界面重开状态会丢（最小化编辑器换宿主的场景是能存活的） |
| **无障碍** | 组合出来的元素不向 MC 旁白贡献信息 |
| **Lazy 变高行** | 只支持固定尺寸 |
| **TabView 重排** | 加删支持，已有页签换顺序不生效 |
| **`AnimatedVisibility`** | 只有淡入淡出，没有 slide / expand |
| **服务端 UI** | 组合只在客户端跑。服务端数据走上面的 binding 桥 |

`Menu` / `SearchComponent` / `Scene` / `GraphView` 没有类型化封装 —— 用 `UIElementNode` 直接使。
