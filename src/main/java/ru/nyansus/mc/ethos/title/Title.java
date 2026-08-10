package ru.nyansus.mc.ethos.title;

import org.bukkit.entity.Player;
import ru.nyansus.mc.ethos.Messages;

public record Title(int id, String nameRu, String nameEn, TitleColor color,
                    String descriptionRu, String descriptionEn,
                    UnlockCondition unlockCondition) {

    public String localizedName(Player player, Messages messages) {
        return messages.isRussian(player) ? nameRu : nameEn;
    }

    public String localizedDescription(Player player, Messages messages) {
        return messages.isRussian(player) ? descriptionRu : descriptionEn;
    }
}
