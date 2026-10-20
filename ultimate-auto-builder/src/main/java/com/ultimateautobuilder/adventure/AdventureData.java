package com.ultimateautobuilder.adventure;

import com.ultimateautobuilder.UltimateAutoBuilderMod;
import com.ultimateautobuilder.core.adventure.LootTier;
import com.ultimateautobuilder.core.adventure.StructureType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Données persistantes de l'aventure, enregistrées avec le monde (Overworld) : sites de
 * structures, état de chaque joueur (kit, maison de départ, récompenses, objets en attente,
 * découvertes), trésors des cartes et Wardens déjà récompensés. Lecture tolérante : une entrée
 * invalide est ignorée sans faire perdre le reste.
 */
public final class AdventureData extends SavedData {
    private static final String NAME = UltimateAutoBuilderMod.MOD_ID + "_adventure";
    private static final int MAX_REWARDED = 4096;

    public enum SiteState { PENDING, PLACING, PLACED, SKIPPED }

    public static final class Site {
        public final String key;
        public StructureType type;
        public int x, y, z, rotation;
        public long seed;
        public SiteState state = SiteState.PENDING;
        /** Boîte englobante monde une fois posée {minX, minY, minZ, maxX, maxY, maxZ}. */
        public int[] box;

        Site(String key) {
            this.key = key;
        }

        public boolean contains(double px, double py, double pz) {
            return box != null && px >= box[0] && px <= box[3] + 1 && py >= box[1] - 2 && py <= box[4] + 2 && pz >= box[2] && pz <= box[5] + 1;
        }
    }

    public static final class PlayerState {
        public boolean kitGiven;
        public boolean starterHouseDone;
        public int starterAttempts;
        public final Set<String> armorClaimed = new LinkedHashSet<>();
        public final Set<String> armorNotified = new LinkedHashSet<>();
        public final List<ItemStack> pending = new ArrayList<>();
        public final Set<String> discovered = new LinkedHashSet<>();
        public int wardens;
        public int bosses;
        public boolean knowsSanctuary;
    }

    public enum TreasureState { PENDING, PLACED, FOUND }

    public static final class Treasure {
        public final String id;
        public int x, y, z;
        public LootTier tier = LootTier.COMMON;
        public TreasureState state = TreasureState.PENDING;

        Treasure(String id) {
            this.id = id;
        }
    }

    private final Map<String, Site> sites = new LinkedHashMap<>();
    private final Map<UUID, PlayerState> players = new HashMap<>();
    private final Map<String, Treasure> treasures = new LinkedHashMap<>();
    private final LinkedHashSet<UUID> rewarded = new LinkedHashSet<>();
    /** Index spatial des sites posés (cases de 256 blocs) pour la détection des découvertes. */
    private final Map<Long, List<Site>> spatial = new HashMap<>();

    public static AdventureData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AdventureData::new, AdventureData::load, null), NAME);
    }

    // ------------------------------------------------------------------ sites

    public Site site(String key) {
        return sites.get(key);
    }

    public Site addSite(String key) {
        Site s = sites.computeIfAbsent(key, Site::new);
        setDirty();
        return s;
    }

    public Collection<Site> sites() {
        return sites.values();
    }

    public void markPlaced(Site s, int[] box) {
        s.state = SiteState.PLACED;
        s.box = box;
        index(s);
        setDirty();
    }

    private static long cell(int x, int z) {
        return ((long) (x >> 8) << 32) | ((z >> 8) & 0xFFFFFFFFL);
    }

    private void index(Site s) {
        if (s.box == null) {
            return;
        }
        for (int cx = s.box[0] >> 8; cx <= s.box[3] >> 8; cx++) {
            for (int cz = s.box[2] >> 8; cz <= s.box[5] >> 8; cz++) {
                spatial.computeIfAbsent(((long) cx << 32) | (cz & 0xFFFFFFFFL), k -> new ArrayList<>()).add(s);
            }
        }
    }

    /** Sites posés dont la case spatiale contient la position. */
    public List<Site> placedNear(int x, int z) {
        return spatial.getOrDefault(cell(x, z), List.of());
    }

    /** Vrai si un site (posé ou prévu) est à moins de {@code distance} blocs horizontaux. */
    public boolean siteWithin(int x, int z, int distance) {
        long d2 = (long) distance * distance;
        for (Site s : sites.values()) {
            if (s.state == SiteState.SKIPPED) {
                continue;
            }
            long dx = s.x - x, dz = s.z - z;
            if (dx * dx + dz * dz < d2) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ joueurs

    public PlayerState player(UUID id) {
        return players.computeIfAbsent(id, k -> new PlayerState());
    }

    // ------------------------------------------------------------------ trésors

    public Treasure treasure(String id) {
        return treasures.get(id);
    }

    public Treasure addTreasure(String id) {
        Treasure t = treasures.computeIfAbsent(id, Treasure::new);
        setDirty();
        return t;
    }

    public Collection<Treasure> treasures() {
        return treasures.values();
    }

    // ------------------------------------------------------------------ Wardens

    /** Enregistre une victoire ; renvoie faux si cette entité a déjà été récompensée. */
    public boolean markRewarded(UUID entity) {
        if (!rewarded.add(entity)) {
            return false;
        }
        while (rewarded.size() > MAX_REWARDED) {
            rewarded.remove(rewarded.iterator().next());
        }
        setDirty();
        return true;
    }

    // ------------------------------------------------------------------ sauvegarde

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Format", 1);
        ListTag siteList = new ListTag();
        for (Site s : sites.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("Key", s.key);
            t.putString("Type", s.type == null ? "" : s.type.key);
            t.putIntArray("Pos", new int[]{s.x, s.y, s.z, s.rotation});
            t.putLong("Seed", s.seed);
            t.putString("State", s.state.name());
            if (s.box != null) {
                t.putIntArray("Box", s.box);
            }
            siteList.add(t);
        }
        tag.put("Sites", siteList);
        CompoundTag playerTag = new CompoundTag();
        for (Map.Entry<UUID, PlayerState> e : players.entrySet()) {
            PlayerState p = e.getValue();
            CompoundTag t = new CompoundTag();
            t.putBoolean("Kit", p.kitGiven);
            t.putBoolean("StarterHouse", p.starterHouseDone);
            t.putInt("StarterAttempts", p.starterAttempts);
            t.put("ArmorClaimed", strings(p.armorClaimed));
            t.put("ArmorNotified", strings(p.armorNotified));
            t.put("Discovered", strings(p.discovered));
            ListTag pend = new ListTag();
            for (ItemStack st : p.pending) {
                if (!st.isEmpty()) {
                    pend.add(st.save(registries));
                }
            }
            t.put("Pending", pend);
            t.putInt("Wardens", p.wardens);
            t.putInt("Bosses", p.bosses);
            t.putBoolean("KnowsSanctuary", p.knowsSanctuary);
            playerTag.put(e.getKey().toString(), t);
        }
        tag.put("Players", playerTag);
        ListTag tr = new ListTag();
        for (Treasure t : treasures.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", t.id);
            c.putIntArray("Pos", new int[]{t.x, t.y, t.z});
            c.putString("Tier", t.tier.key);
            c.putString("State", t.state.name());
            tr.add(c);
        }
        tag.put("Treasures", tr);
        ListTag rw = new ListTag();
        for (UUID id : rewarded) {
            rw.add(StringTag.valueOf(id.toString()));
        }
        tag.put("Rewarded", rw);
        return tag;
    }

    private static ListTag strings(Collection<String> values) {
        ListTag l = new ListTag();
        for (String v : values) {
            l.add(StringTag.valueOf(v));
        }
        return l;
    }

    private static void readStrings(ListTag l, Collection<String> out) {
        for (int i = 0; i < l.size(); i++) {
            String s = l.getString(i);
            if (!s.isEmpty() && s.length() <= 128) {
                out.add(s);
            }
        }
    }

    static AdventureData load(CompoundTag tag, HolderLookup.Provider registries) {
        AdventureData d = new AdventureData();
        ListTag siteList = tag.getList("Sites", Tag.TAG_COMPOUND);
        for (int i = 0; i < siteList.size(); i++) {
            CompoundTag t = siteList.getCompound(i);
            String key = t.getString("Key");
            StructureType type = StructureType.byKey(t.getString("Type"));
            int[] pos = t.getIntArray("Pos");
            if (key.isEmpty() || pos.length < 4) {
                UltimateAutoBuilderMod.LOGGER.warn("Site d'aventure illisible ignoré : {}", t);
                continue;
            }
            Site s = new Site(key);
            s.type = type;
            s.x = pos[0];
            s.y = pos[1];
            s.z = pos[2];
            s.rotation = pos[3];
            s.seed = t.getLong("Seed");
            try {
                s.state = SiteState.valueOf(t.getString("State"));
            } catch (IllegalArgumentException e) {
                s.state = SiteState.PENDING;
            }
            // Pose interrompue par un arrêt : elle reprend depuis le début (pose idempotente).
            if (s.state == SiteState.PLACING) {
                s.state = SiteState.PENDING;
            }
            if (type == null && s.state != SiteState.SKIPPED && s.state != SiteState.PENDING) {
                s.state = SiteState.SKIPPED;
            }
            int[] box = t.getIntArray("Box");
            if (box.length == 6) {
                s.box = box;
            }
            d.sites.put(key, s);
            if (s.state == SiteState.PLACED) {
                d.index(s);
            }
        }
        CompoundTag playerTag = tag.getCompound("Players");
        for (String k : playerTag.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(k);
            } catch (IllegalArgumentException e) {
                continue;
            }
            CompoundTag t = playerTag.getCompound(k);
            PlayerState p = new PlayerState();
            p.kitGiven = t.getBoolean("Kit");
            p.starterHouseDone = t.getBoolean("StarterHouse");
            p.starterAttempts = Math.max(0, t.getInt("StarterAttempts"));
            readStrings(t.getList("ArmorClaimed", Tag.TAG_STRING), p.armorClaimed);
            readStrings(t.getList("ArmorNotified", Tag.TAG_STRING), p.armorNotified);
            readStrings(t.getList("Discovered", Tag.TAG_STRING), p.discovered);
            ListTag pend = t.getList("Pending", Tag.TAG_COMPOUND);
            for (int i = 0; i < pend.size(); i++) {
                ItemStack.parse(registries, pend.getCompound(i)).ifPresent(st -> {
                    if (!st.isEmpty()) {
                        p.pending.add(st);
                    }
                });
            }
            p.wardens = Math.max(0, t.getInt("Wardens"));
            p.bosses = Math.max(0, t.getInt("Bosses"));
            p.knowsSanctuary = t.getBoolean("KnowsSanctuary");
            d.players.put(id, p);
        }
        ListTag tr = tag.getList("Treasures", Tag.TAG_COMPOUND);
        for (int i = 0; i < tr.size(); i++) {
            CompoundTag c = tr.getCompound(i);
            int[] pos = c.getIntArray("Pos");
            String id = c.getString("Id");
            if (id.isEmpty() || pos.length < 3) {
                continue;
            }
            Treasure t = new Treasure(id);
            t.x = pos[0];
            t.y = pos[1];
            t.z = pos[2];
            LootTier tier = LootTier.byKey(c.getString("Tier"));
            t.tier = tier == null ? LootTier.COMMON : tier;
            try {
                t.state = TreasureState.valueOf(c.getString("State"));
            } catch (IllegalArgumentException e) {
                t.state = TreasureState.PENDING;
            }
            d.treasures.put(id, t);
        }
        ListTag rw = tag.getList("Rewarded", Tag.TAG_STRING);
        for (int i = 0; i < rw.size(); i++) {
            try {
                d.rewarded.add(UUID.fromString(rw.getString(i)));
            } catch (IllegalArgumentException e) {
                // entrée invalide ignorée
            }
        }
        return d;
    }
}
