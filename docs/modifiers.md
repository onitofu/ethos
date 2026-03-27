# Модификаторы кармы (баффы и дебаффы)

Каждый модификатор может быть привязан к порогу кармы в `config.yml`. Несколько модификаторов можно комбинировать на одном пороге.

---

## Боевые

| Модификатор | Тип значения | Описание |
|---|---|---|
| `MOB_DAMAGE_BONUS` | double (множитель) | Бонус урона по враждебным мобам. 0.10 = +10% |
| `PASSIVE_MOB_DAMAGE_BONUS` | double (множитель) | Бонус урона по мирным мобам. Отрицательное = штраф |
| `PVP_DAMAGE_BONUS` | double (множитель) | Бонус урона по игрокам. 0.10 = +10% |
| `PVP_DAMAGE_PENALTY` | double (множитель) | Штраф урона по игрокам. 0.10 = -10% |
| `RESISTANCE` | double (множитель) | Сопротивление входящему урону. 0.10 = -10% получаемого урона |
| `FALL_DAMAGE_REDUCTION` | double (множитель) | Снижение урона от падения. 0.50 = -50% |
| `FIRE_RESISTANCE` | double (множитель) | Снижение урона от огня и лавы. 0.30 = -30% |

## Характеристики

| Модификатор | Тип значения | Описание |
|---|---|---|
| `HEALTH_BONUS` | double (хп) | Модификатор максимального здоровья. 4.0 = +2 сердца |
| `SPEED_BONUS` | double (множитель) | Бонус скорости передвижения. Реализуется через зельевой эффект Speed |
| `REGENERATION_BONUS` | int (amplifier) | Уровень регенерации. 0 = Regen I, 1 = Regen II |
| `HUNGER_RATE` | double (множитель) | Множитель скорости голодания. 1.5 = на 50% быстрее, 0.5 = на 50% медленнее |
| `NIGHT_VISION` | boolean | Постоянное ночное зрение |

## Опыт

| Модификатор | Тип значения | Описание |
|---|---|---|
| `XP_BONUS` | double (множитель) | Бонус к получаемому опыту. 0.15 = +15% |
| `XP_PENALTY` | double (множитель) | Штраф к получаемому опыту. 0.10 = -10% |
| `XP_DEATH_PENALTY` | double (множитель) | Множитель потери XP при смерти. 0.5 = теряешь на 50% больше |

## Добыча

| Модификатор | Тип значения | Описание |
|---|---|---|
| `LOOT_BONUS` | double (множитель) | Бонус к дропу с мобов. 0.20 = +20% шанс дополнительного дропа |
| `MINING_SPEED` | int (amplifier) | Бонус к скорости копания. 0 = Haste I, 1 = Haste II. Отрицательное = Mining Fatigue |
| `DOUBLE_CROP_CHANCE` | double (шанс) | Шанс двойного урожая при сборе. 0.15 = 15% шанс |

## Торговля

| Модификатор | Тип значения | Описание |
|---|---|---|
| `TRADE_PRICE_INCREASE` | int (предметы) | Наценка у жителей. 2 = +2 предмета к цене |
| `TRADE_PRICE_DECREASE` | int (предметы) | Скидка у жителей. 2 = -2 предмета от цены |
| `BLOCK_TRADING` | boolean | Полная блокировка торговли с жителями |

## Взаимодействие с мобами

| Модификатор | Тип значения | Описание |
|---|---|---|
| `HOSTILE_MOB_NEUTRAL` | boolean | Враждебные мобы не агрятся на игрока |
| `HOSTILE_MOB_REDUCED_RANGE` | double (блоки) | Уменьшенная дальность обнаружения враждебными мобами |
| `PASSIVE_MOB_FLEE` | boolean | Мирные мобы разбегаются от игрока |
| `GOLEM_AGGRO` | double (блоки) | Железные големы агрятся на игрока в указанном радиусе |

## Взаимодействие с животными

| Модификатор | Тип значения | Описание |
|---|---|---|
| `BLOCK_TAMING` | boolean | Невозможность приручать животных |
| `BLOCK_RIDING` | boolean | Невозможность кататься на животных |

## Смерть

| Модификатор | Тип значения | Описание |
|---|---|---|
| `KEEP_INVENTORY_CHANCE` | double (шанс) | Шанс сохранить инвентарь при смерти. 0.25 = 25% |

## Видимость

| Модификатор | Тип значения | Описание |
|---|---|---|
| `GLOWING` | boolean | Эффект свечения — игрок виден через стены другим игрокам. "В розыске" |

## Зельевые эффекты

| Модификатор | Параметры | Описание |
|---|---|---|
| `POTION_EFFECT` | potion (тип), amplifier (уровень), ambient (частицы) | Любой ванильный зельевой эффект Minecraft |

---

## Пример конфигурации

```yaml
buffs:
  effect-duration: 1400
  tiers:
    # === Отрицательная карма ===
    -2000:
      effects:
        - { type: MOB_DAMAGE_BONUS, value: 0.05 }
        - { type: TRADE_PRICE_INCREASE, value: 1 }
    -4000:
      effects:
        - { type: MOB_DAMAGE_BONUS, value: 0.10 }
        - { type: SPEED_BONUS, value: 0.05 }
        - { type: TRADE_PRICE_INCREASE, value: 2 }
        - { type: NIGHT_VISION, value: true }
        - { type: LOOT_BONUS, value: 0.10 }
    -6000:
      effects:
        - { type: MOB_DAMAGE_BONUS, value: 0.15 }
        - { type: SPEED_BONUS, value: 0.10 }
        - { type: BLOCK_TRADING, value: true }
        - { type: HOSTILE_MOB_REDUCED_RANGE, value: 8.0 }
        - { type: NIGHT_VISION, value: true }
        - { type: LOOT_BONUS, value: 0.15 }
        - { type: FIRE_RESISTANCE, value: 0.20 }
        - { type: PASSIVE_MOB_FLEE, value: true }
        - { type: GLOWING, value: true }
    -8000:
      effects:
        - { type: MOB_DAMAGE_BONUS, value: 0.20 }
        - { type: SPEED_BONUS, value: 0.15 }
        - { type: BLOCK_TRADING, value: true }
        - { type: GOLEM_AGGRO, value: 16.0 }
        - { type: HOSTILE_MOB_NEUTRAL, value: true }
        - { type: PASSIVE_MOB_FLEE, value: true }
        - { type: BLOCK_TAMING, value: true }
        - { type: BLOCK_RIDING, value: true }
        - { type: NIGHT_VISION, value: true }
        - { type: LOOT_BONUS, value: 0.20 }
        - { type: FIRE_RESISTANCE, value: 0.30 }
        - { type: FALL_DAMAGE_REDUCTION, value: 0.30 }
        - { type: GLOWING, value: true }

    # === Положительная карма ===
    2000:
      effects:
        - { type: XP_BONUS, value: 0.05 }
        - { type: DOUBLE_CROP_CHANCE, value: 0.05 }
    4000:
      effects:
        - { type: XP_BONUS, value: 0.10 }
        - { type: DOUBLE_CROP_CHANCE, value: 0.10 }
        - { type: REGENERATION_BONUS, value: 0 }
    6000:
      effects:
        - { type: XP_BONUS, value: 0.15 }
        - { type: POTION_EFFECT, potion: HERO_OF_THE_VILLAGE, amplifier: 0 }
        - { type: PVP_DAMAGE_PENALTY, value: 0.05 }
        - { type: DOUBLE_CROP_CHANCE, value: 0.15 }
        - { type: RESISTANCE, value: 0.05 }
        - { type: KEEP_INVENTORY_CHANCE, value: 0.10 }
    8000:
      effects:
        - { type: XP_BONUS, value: 0.20 }
        - { type: POTION_EFFECT, potion: HERO_OF_THE_VILLAGE, amplifier: 1 }
        - { type: POTION_EFFECT, potion: LUCK, amplifier: 0 }
        - { type: PVP_DAMAGE_PENALTY, value: 0.10 }
        - { type: HEALTH_BONUS, value: 4.0 }
        - { type: DOUBLE_CROP_CHANCE, value: 0.20 }
        - { type: RESISTANCE, value: 0.10 }
        - { type: KEEP_INVENTORY_CHANCE, value: 0.25 }
```
