package net.kenji.ai_talking_villagers.api.context.villager_info;

import net.kenji.ai_talking_villagers.api.interfaces.IContext;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public enum ProfessionContext implements IContext {
    UNEMPLOYED,
    ARMORER,
    BUTCHER,
    CARTOGRAPHER,
    CLERIC,
    FARMER,
    FISHERMAN,
    FLETCHER,
    LEATHERWORKER,
    LIBRARIAN,
    MASON,
    NITWIT,
    SHEPHERD,
    TOOLSMITH,
    WEAPONSMITH,
    SPECIAL;

    ProfessionContext(){}

    public static ProfessionContext getContext(Villager villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();

        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) return UNEMPLOYED;

        try {
            return valueOf(profession.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SPECIAL;
        }
    }

    @Override
    public String getContextType() {
        return "Profession";
    }

    @Override
    public String getContextName() {
        String text = this.toString().toLowerCase().replace("_", " ");

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }
}
