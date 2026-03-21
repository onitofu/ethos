# domya-karma — Техническая спецификация

## 1. Обзор

Standalone Paper-плагин для системы кармы, титулов и баффов. Интегрируется с domya-chat через PlaceholderAPI.

- **Группа**: `ru.nyansus.mc`
- **Артефакт**: `domya-karma`
- **API**: Paper 1.21
- **Сборка**: Gradle, checkstyle, jacoco (аналогично domya-chat)
- **Soft-dependency**: PlaceholderAPI

---

## 2. Структура пакетов

```
ru.nyansus.mc.domya_karma/
  DomyaKarma.java              -- Главный класс плагина
  Messages.java                -- Локализация (паттерн из domya-chat)

  karma/
    KarmaManager.java          -- Логика кармы: get/set/add, clamp [-1000,1000], decay
    KarmaStorage.java          -- Интерфейс хранилища
    YamlKarmaStorage.java      -- YAML-реализация
    KarmaDecayTask.java        -- Ленивый decay при доступе к данным

  buff/
    BuffManager.java           -- Применение/снятие эффектов по порогам кармы
    BuffDefinition.java        -- Record: порог, тип эффекта, уровень

  title/
    Title.java                 -- Record: id, имена (ru/en), цвет, тип (KARMA/VANILLA)
    TitleRegistry.java         -- Загрузка определений из titles.yml
    TitleManager.java          -- Открытые титулы, активный титул
    TitleStorage.java          -- Интерфейс хранилища
    YamlTitleStorage.java      -- YAML-реализация

  unlock/
    UnlockChecker.java         -- Оркестратор: проверка условий разблокировки
    UnlockCondition.java       -- Интерфейс: boolean isMet(Player, PlayerStats)
    StatCondition.java         -- Условие через Minecraft Statistics API
    KarmaCondition.java        -- Условие по текущей карме
    CompositeCondition.java    -- AND/OR из нескольких условий
    SpecialCondition.java      -- Для нестандартных условий (редкие титулы)

  stats/
    StatsTracker.java          -- Кастомная статистика (то, что не трекает Minecraft)
    StatsStorage.java          -- Интерфейс
    YamlStatsStorage.java      -- YAML-реализация

  listener/
    MobKillListener.java       -- EntityDeathEvent: карма + счётчик убийств
    PlayerKillListener.java    -- PlayerDeathEvent (PvP): логика кармы
    TradeListener.java         -- InventoryClickEvent (MerchantInventory)
    AnimalListener.java        -- EntityBreedEvent, PlayerInteractEntityEvent, EntityTameEvent
    FarmingListener.java       -- BlockPlaceEvent (семена), PlayerHarvestBlockEvent
    MiningListener.java        -- BlockBreakEvent: руды для титулов
    FishingListener.java       -- PlayerFishEvent
    DeathListener.java         -- PlayerDeathEvent: смерти по причинам, стрик выживания
    BuildListener.java         -- BlockPlaceEvent: счётчик блоков
    CraftListener.java         -- CraftItemEvent, EnchantItemEvent, BrewEvent, наковальня
    TravelListener.java        -- Периодический таск (не PlayerMoveEvent!)
    VillagerCureListener.java  -- EntityTransformEvent (CURED)
    RaidListener.java          -- RaidFinishEvent
    SpecialActionListener.java -- Редкие/секретные условия

  command/
    KarmaCommand.java          -- /karma [player]
    TitleCommand.java          -- /dt list|select|info
    DomyaKarmaCommand.java     -- /domyakarma reload|setkarma|givetitle

  integration/
    PlaceholderExpansion.java   -- %domya_karma%, %domya_title%, %domya_title_colored%
```

---

## 3. Хранилище

YAML-файлы с `synchronized`-методами (паттерн из domya-chat).

### karma.yml

```yaml
<uuid>:
  karma: 450
  last-update: 1679500000000   # timestamp для lazy decay
```

### titles.yml (данные игроков)

```yaml
<uuid>:
  active: 800
  unlocked:
    - 5
    - 800
    - 901
```

### stats.yml (кастомная статистика)

Только то, что Minecraft НЕ трекает:

```yaml
<uuid>:
  villager-trades: 145
  piglin-barters: 23
  zombie-villager-cures: 3
  animals-bred: 87
  bees-bred: 12
  seeds-planted: 560
  crops-harvested: 1200
  potions-brewed: 45
  anvil-uses: 30
  treasures-fished: 5
  raids-won: 2
  beds-used: 150
  survival-streak-start: 1679400000000
  pvp-deaths: 3
  biomes-visited:
    - plains
    - desert
```

### Стратегия сохранения

- Редкие события (убийства, торговля): сохранение сразу
- Частые события (блоки, перемещение): dirty-flag + flush каждые 60 сек + при `onDisable`

### Почему YAML, а не SQLite

Консистентность с domya-chat. Для SMP масштаба достаточно. Интерфейсы хранилища позволяют заменить на SQLite позже.

---

## 4. Система кармы

### Действия

| Действие | Карма | Детекция |
|---|---|---|
| Убить враждебного моба | +1 | EntityDeathEvent, killer instanceof Player |
| Убить босса (Визер/Дракон) | +50 | EntityDeathEvent, entity type check |
| Торговля с жителем | +2 | InventoryClickEvent, MerchantInventory |
| Вылечить зомби-жителя | +30 | EntityTransformEvent, CURED |
| Покормить животное | +1 | PlayerInteractEntityEvent |
| Убить мирное животное | -3 | EntityDeathEvent, passive mob check |
| Убить приручённого | -20 | EntityDeathEvent, Tameable.isTamed() |
| Убить жителя | -25 | EntityDeathEvent, entity type VILLAGER |
| Убить игрока | -15 | PlayerDeathEvent, killer check |
| Убить игрока с позитивной кармой | -30 | Доп. проверка кармы жертвы |
| Убить игрока с негативной кармой | +10 | Доп. проверка кармы жертвы |

### Анти-фарм

Защита от абьюза через повторные убийства одного и того же игрока или сговор.

**PvP кулдаун по жертве:**
- После убийства игрока X, повторные убийства X не дают карму и не засчитываются в счётчик титулов в течение **30 минут**
- Хранение: in-memory `Map<UUID, Map<UUID, Long>>` (killer → victim → timestamp)
- Очищается при выходе killer'а с сервера

**Взаимные убийства:**
- Если игрок A убил B, а B убил A в течение **5 минут** — второе убийство не засчитывается
- Защита от «обмена убийствами» для фарма титулов

**Спавнер-фарм мобов:**
- Мобы из спавнеров (`entity.fromMobSpawner()`) дают **половину** кармы (округление вниз)
- Не засчитываются в счётчики титулов

**Конфигурируемость:**
```yaml
anti-farm:
  pvp-cooldown-minutes: 30
  mutual-kill-window-minutes: 5
  spawner-karma-multiplier: 0.5
  spawner-counts-for-titles: false
```



Не итерировать всех игроков каждый час. Вместо этого при **чтении** кармы:

```
hoursSinceUpdate = (now - lastUpdate) / 3600000
effectiveKarma = storedKarma * pow(0.99, hoursSinceUpdate)
```

Пересчёт и сохранение при входе игрока или запросе кармы. Обрабатывает даунтайм сервера автоматически.

Порог: карма в диапазоне [-99, 99] не затухает (delta < 1, округляется в 0).

---

## 5. Система баффов

`BuffManager` — периодический таск каждые 60 секунд (1200 тиков) для всех онлайн-игроков.

### Отрицательная карма

| Диапазон | Баффы | Дебаффы | Реализация |
|---|---|---|---|
| -200...-400 | Strength I (слабый, +5%) | Цены жителей +1 уровень | PotionEffect + VillagerReputation |
| -400...-600 | Strength I (+10%) | Цены +2 уровня | PotionEffect + VillagerReputation |
| -600...-800 | Strength II (слабый, +15%) | Блокировка торговли | PotionEffect + TradeListener cancel |
| -800...-1000 | Strength II (+20%), Speed I | Блокировка торговли, големы агрятся с 16 блоков, замедленная регенерация | PotionEffect + IronGolemTarget + слабый Wither |

### Положительная карма

| Диапазон | Баффы | Дебаффы | Реализация |
|---|---|---|---|
| 200...400 | Слабая регенерация | — | PotionEffect(REGENERATION, 0, ambient) |
| 400...600 | Regen I, +10% опыт | -5% урон по игрокам | PotionEffect + PlayerExpChangeEvent + EntityDamageByEntityEvent |
| 600...800 | Regen I, Hero of the Village I | -10% урон по игрокам | PotionEffect + EntityDamageByEntityEvent |
| 800...1000 | Regen II, Hero of the Village II, Luck | -15% урон по игрокам, нет first-strike (5 сек PvP cooldown) | PotionEffect + EntityDamageByEntityEvent + PvP cooldown |

### Технические детали

- Длительность эффектов: 1400 тиков (70 сек) — чуть больше интервала проверки (60 сек), чтобы не мерцали
- Блокировка торговли: `TradeListener` проверяет карму, при < -600 → отмена события + сообщение
- Модификация урона по игрокам: `EntityDamageByEntityEvent`, множитель в зависимости от кармы атакующего
- PvP cooldown для Легенды: in-memory `Map<UUID, Long>` — timestamp последнего полученного удара, атака разрешена только после 5 сек
- Агрессия големов: `EntityTargetEvent`, если цель — игрок с кармой < -800, увеличить дальность через `target.setTarget()`
- Все пороги, эффекты и множители конфигурируемые

### Конфиг

```yaml
buffs:
  negative:
    -200:
      effects: [{type: INCREASE_DAMAGE, amplifier: 0}]
      trade-price-increase: 1
    -400:
      effects: [{type: INCREASE_DAMAGE, amplifier: 0}]
      trade-price-increase: 2
    -600:
      effects: [{type: INCREASE_DAMAGE, amplifier: 1}]
      block-trading: true
    -800:
      effects: [{type: INCREASE_DAMAGE, amplifier: 1}, {type: SPEED, amplifier: 0}]
      block-trading: true
      golem-aggro: true
      slow-regen: true
  positive:
    200:
      effects: [{type: REGENERATION, amplifier: 0, ambient: true}]
    400:
      effects: [{type: REGENERATION, amplifier: 0}]
      xp-bonus: 0.10
      pvp-damage-penalty: 0.05
    600:
      effects: [{type: REGENERATION, amplifier: 0}, {type: HERO_OF_THE_VILLAGE, amplifier: 0}]
      pvp-damage-penalty: 0.10
    800:
      effects: [{type: REGENERATION, amplifier: 1}, {type: HERO_OF_THE_VILLAGE, amplifier: 1}, {type: LUCK, amplifier: 0}]
      pvp-damage-penalty: 0.15
      no-first-strike: true
      first-strike-cooldown: 5
```

---

## 6. Система титулов

### Система ID

Каждый титул имеет уникальный числовой ID. ID **никогда не меняется** и **не переиспользуется**.
Группировка по диапазонам (1–99 кармические, 100–199 PvP, 200–299 нежить, ... 1800–1899 юмор).
Запас ~90 слотов в каждом диапазоне для будущих дополнений.

Полный список ID — в `karma-titles-design.md`.

### Определения (titles.yml — конфиг, не данные)

```yaml
titles:
  800:
    name:
      en: "Dragon Slayer"
      ru: "Драконоборец"
    color: "light_purple"
    type: VANILLA
    conditions:
      - type: STAT
        stat: KILL_ENTITY
        entity: ENDER_DRAGON
        value: 1

  901:
    name:
      en: "Prospector"
      ru: "Старатель"
    color: "aqua"
    type: VANILLA
    conditions:
      - type: STAT
        stat: MINE_BLOCK
        material: DIAMOND_ORE
        value: 1000

  1819:
    name:
      en: "Speedrunner"
      ru: "Спидраннер"
    color: "light_purple"
    type: VANILLA
    conditions:
      - type: SPECIAL
        id: kill_dragon_with_bed
```

Ключ — числовой ID титула. Админы могут добавлять/изменять титулы без кода.

### Обратная совместимость

- ID никогда не меняется после добавления
- ID никогда не переиспользуется после удаления
- Данные игроков (`titles.yml`) хранят ссылки по ID — переименование титула не ломает сохранения
- Новые титулы = новые ID в соответствующем диапазоне

### Кармические титулы — особый случай

Не «открываются» навсегда. Автоматически доступны/недоступны в зависимости от текущей кармы. В `TitleManager`: при выборе активного кармического титула проверяем, что карма всё ещё в диапазоне. Если карма ушла — титул автоматически снимается.

### Разблокировка: гибридный подход

1. **По событию** (мгновенно): при убийстве моба, торговле и т.д. проверяем только релевантные титулы. O(1) на событие.
2. **Периодическое сканирование** (каждые 5 минут): для ванильной статистики (дистанция, блоки, крафт), многоусловных титулов, стрика выживания.

---

## 7. Детекция редких/секретных титулов

| Титул | Способ детекции |
|---|---|
| Меломан | EntityDeathEvent: крипер умер, проверить дроп MUSIC_DISC |
| Паркурщик | EntityDamageEvent: FALL, fallDistance >= 100, игрок выжил |
| Изобретательный | EntityDamageByEntityEvent: FallingBlock ANVIL, моб умер |
| Барахольщик | InventoryCloseEvent: эндер-сундук, 27 непустых слотов |
| Дегустатор | EntityPotionEffectEvent: иглобрюх (PUFFER_FISH) |
| Громоотвод | EntityDamageEvent: LIGHTNING, игрок выжил |
| Пиротехник | PlayerDeathEvent: DamageCause FIREWORK, killer == victim |
| Спидраннер | EntityDeathEvent: дракон + проверка lastDamageCause == BED |
| Ванпанчмен | EntityDeathEvent: Визер, killer bare hands (no weapon) |
| Берсерк | 3 убийства за 60 сек — in-memory deque с timestamp'ами |
| Полный незеритовый сет | Периодическая проверка 4 слотов брони |
| 64 стака хлеба | Периодическая проверка инвентаря |
| AFK 1 час | Трекинг последней позиции, сравнение при периодической проверке |
| 24 часа под водой | Периодический трекинг, аккумуляция времени в stats |

Реализация: интерфейс `SpecialCondition` с отдельной имплементацией на каждый редкий титул.

---

## 8. Event listeners — производительность

### Высокочастотные события

| Событие | Подход |
|---|---|
| BlockBreakEvent | Фильтр по Material (только руды, камень). In-memory счётчик, flush каждые 60 сек |
| BlockPlaceEvent | In-memory счётчик, flush каждые 60 сек |
| PlayerMoveEvent | **НЕ слушать.** Периодический таск (5 мин) читает `player.getStatistic(Statistic.WALK_ONE_CM)` |
| Биомы | На том же таске: `player.getLocation().getBlock().getBiome()`, добавить в set если новый |

### Использование Minecraft Statistics API

Ванильная статистика покрывает: убийства мобов по типу, убийства игроков, смерти, добыча руд по типу, размещённые блоки, дистанция, пойманные рыбы, скрафченные предметы, зачарования.

**При проверке титулов читаем ванильную стату напрямую**, а не дублируем трекинг. Кастомный трекинг только для: торговля с жителями, бартер с пиглинами, лечение зомби-жителей, варка зелий, наковальня, биомы, стрик выживания, смерти в PvP.

---

## 9. Команды

### /karma [player]

- Без аргументов: своя карма
- С аргументом: карма другого игрока (permission: `domya.karma.view.others`)
- Показывает: значение, кармический титул (если есть), визуальный бар

### /dt

- Без аргументов: список открытых титулов с ID и цветами
- Пагинированный вывод
- Открытые — цветные с ID, закрытые — серые с подсказкой условия

### /dt \<id\>

- Установить активный титул по числовому ID (напр. `/dt 801`)
- Tab completion по открытым ID

### /dt info \<id\>

- Название, цвет, условие, прогресс, статус (открыт/закрыт)

### /dt reset

- Снять активный титул

### /domyakarma reload

- Permission: `domya.karma.admin`
- Перезагрузка config.yml, titles.yml, messages

### /domyakarma setkarma \<player\> \<value\>

- Permission: `domya.karma.admin`
- Установить карму напрямую

### /domyakarma givetitle \<player\> \<title_id\>

- Permission: `domya.karma.admin`
- Принудительно открыть титул

---

## 10. Конфигурация (config.yml)

```yaml
# Диапазон кармы
karma:
  min: -1000
  max: 1000

# Карма за действия
karma-actions:
  kill-hostile: 1
  kill-boss: 50
  villager-trade: 2
  cure-zombie-villager: 30
  feed-animal: 1
  kill-passive: -3
  kill-tamed: -20
  kill-villager: -25
  kill-player: -15
  kill-good-player: -30
  kill-evil-player: 10

# Затухание
decay:
  enabled: true
  percent-per-hour: 1.0
  min-threshold: 100

# Кармические титулы
karma-titles:
  tyrant:
    range: [-1000, -800]
    color: "dark_red"
    name: { en: "Tyrant", ru: "Тиран" }
  punisher:
    range: [-800, -600]
    color: "red"
    name: { en: "Punisher", ru: "Каратель" }
  thug:
    range: [-600, -400]
    color: "red"
    name: { en: "Thug", ru: "Головорез" }
  bandit:
    range: [-400, -200]
    color: "gold"
    name: { en: "Bandit", ru: "Разбойник" }
  defender:
    range: [200, 400]
    color: "green"
    name: { en: "Defender", ru: "Защитник" }
  guardian:
    range: [400, 600]
    color: "green"
    name: { en: "Guardian", ru: "Страж" }
  hero:
    range: [600, 800]
    color: "aqua"
    name: { en: "Hero", ru: "Герой" }
  legend:
    range: [800, 1000]
    color: "light_purple"
    name: { en: "Legend", ru: "Легенда" }

# Баффы
buffs:
  strength:
    karma-below: -600
    effect: INCREASE_DAMAGE
    amplifier: 0
  no-trade:
    karma-below: -800
  regeneration:
    karma-above: 600
    effect: REGENERATION
    amplifier: 0
  hero:
    karma-above: 800
    effect: HERO_OF_THE_VILLAGE
    amplifier: 0

# Интервалы (тики)
unlock-check-interval: 6000    # 5 минут
buff-check-interval: 1200      # 60 секунд
stats-save-interval: 1200      # 60 секунд
```

Определения ванильных титулов — в отдельном `titles.yml`.

---

## 11. Интеграция с domya-chat

### PlaceholderAPI (основной)

| Placeholder | Значение |
|---|---|
| `%domya_karma%` | Числовое значение кармы |
| `%domya_karma_title%` | Кармический титул (пусто в нейтральной зоне) |
| `%domya_title%` | Активный титул (имя) |
| `%domya_title_colored%` | Активный титул в MiniMessage цвете |
| `%domya_title_raw%` | ID активного титула |

В domya-chat формат: `"%domya_title_colored% <player>"`.

### Прямой API (fallback)

Если PlaceholderAPI не установлен:

```java
public class DomyaKarmaAPI {
    public static int getKarma(UUID uuid);
    public static String getActiveTitle(UUID uuid);
    public static String getActiveTitleColored(UUID uuid);
}
```

domya-chat может soft-depend на domya-karma и вызывать API напрямую.

---

## 12. Фазы реализации

### Фаза 1 — Ядро (MVP)

1. Скаффолд проекта (build.gradle, plugin.yml)
2. `DomyaKarma`, `Messages`
3. `KarmaManager`, `KarmaStorage`, `YamlKarmaStorage`
4. Lazy decay
5. Основные listeners: `MobKillListener`, `PlayerKillListener`, `TradeListener`, `VillagerCureListener`, `AnimalListener`
6. `/karma`
7. `BuffManager`

### Фаза 2 — Титулы

8. `Title`, `TitleRegistry` (загрузка из titles.yml)
9. `TitleManager`, `TitleStorage`, `YamlTitleStorage`
10. `UnlockChecker` + `StatCondition`
11. `StatsTracker` + listeners для кастомной статистики
12. `/dt` (list, select, info)
13. Кармические титулы (автоматические)

### Фаза 3 — Интеграция и полировка

14. PlaceholderAPI expansion
15. GUI-меню титулов
16. Прогресс титулов в `/dt info`
17. Админ-команды (setkarma, givetitle)

### Фаза 4 — Редкие титулы

18. `SpecialActionListener` для каждого редкого титула
19. Тестирование edge-кейсов

---

## 13. Ключевые архитектурные решения

| Решение | Выбор | Обоснование |
|---|---|---|
| Хранилище | YAML | Консистентность с domya-chat, достаточно для SMP |
| Затухание кармы | Lazy при доступе | Нет итерации оффлайн-игроков, обработка даунтайма |
| Трекинг статистики | Vanilla API + кастомное дополнение | Не дублировать то, что Minecraft уже считает |
| Определения титулов | Конфиг-driven (titles.yml) | Админы могут менять без кода |
| Проверка разблокировки | Гибрид: по событию + периодическое сканирование | Баланс отзывчивости и производительности |
| Баффы | Периодическое обновление (60 сек) | Просто, не требует стейт для снятия |
| Интеграция с чатом | PlaceholderAPI + fallback API | Стандарт экосистемы |
| Кармические vs ванильные титулы | Один пул, но кармические авто-grant/revoke | Кармические транзиентны по природе |
| Высокочастотные события | In-memory счётчики + периодический flush | Избежать disk I/O на каждый BlockBreakEvent |
