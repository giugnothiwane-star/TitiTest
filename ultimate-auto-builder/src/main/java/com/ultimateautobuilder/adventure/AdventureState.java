package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.core.adventure.ArmorSetReward;
import com.ultimateautobuilder.network.AdventureStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Construction et envoi de l'état d'aventure d'un joueur vers son client. */
public final class AdventureState {
    private AdventureState() {
    }

    public static void send(ServerPlayer player) {
        AdventureData data = AdventureData.get(player.server);
        AdventureData.PlayerState ps = data.player(player.getUUID());
        int[] armor = new int[ArmorSetReward.values().length];
        for (ArmorSetReward s : ArmorSetReward.values()) {
            armor[s.ordinal()] = ArmorRewards.state(player, ps, s).ordinal();
        }
        List<ItemStack> pending = new ArrayList<>();
        for (ItemStack st : ps.pending) {
            pending.add(st.copy());
        }
        List<AdventureStatePayload.Discovery> disc = new ArrayList<>();
        for (String key : ps.discovered) {
            AdventureData.Site s = data.site(key);
            if (s != null && s.type != null) {
                disc.add(new AdventureStatePayload.Discovery(s.type.key, s.x, s.y, s.z));
            }
        }
        int[] sanctuary = new int[0];
        AdventureData.Site sanct = data.site(Sanctuary.SITE_KEY);
        if (ps.knowsSanctuary && sanct != null) {
            sanctuary = new int[]{sanct.x, sanct.z, sanct.state.ordinal()};
        }
        int placed = 0;
        for (AdventureData.Site s : data.sites()) {
            if (s.state == AdventureData.SiteState.PLACED) {
                placed++;
            }
        }
        com.ultimateautobuilder.network.Net.send(player, new AdventureStatePayload(armor, pending, disc, ps.wardens, ps.bosses, sanctuary, placed));
    }
}
