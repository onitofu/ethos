package ru.nyansus.mc.domya_fate.title;

public record Title(int id, String nameRu, String nameEn, TitleColor color,
                    String descriptionRu, String descriptionEn,
                    UnlockCondition unlockCondition) {
}
