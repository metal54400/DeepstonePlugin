package fr.deepstonestudio.deepstone.model.inv;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.Objects;
import java.util.logging.Level;

public final class InvSnapshot {

    public ItemStack[] storageContents;
    public ItemStack[] armorContents;
    public ItemStack offhandItem;
    public ItemStack[] enderContents;

    public int totalExp;
    public int level;
    public float exp;

    public double health;
    public int food;
    public float saturation;

    // ========================= CAPTURE =========================

    public static InvSnapshot capture(Player p,
                                      boolean syncArmor,
                                      boolean syncOffhand,
                                      boolean syncEnder,
                                      boolean syncXp,
                                      boolean syncHealthFood) {

        InvSnapshot s = new InvSnapshot();

        s.storageContents = cloneArray(p.getInventory().getStorageContents());

        if (syncArmor)
            s.armorContents = cloneArray(p.getInventory().getArmorContents());

        if (syncOffhand)
            s.offhandItem = p.getInventory().getItemInOffHand();

        if (syncEnder)
            s.enderContents = cloneArray(p.getEnderChest().getContents());

        if (syncXp) {
            s.totalExp = p.getTotalExperience();
            s.level = p.getLevel();
            s.exp = p.getExp();
        }

        if (syncHealthFood) {
            double max = Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH)).getValue();
            s.health = Math.min(p.getHealth(), max);
            s.food = p.getFoodLevel();
            s.saturation = p.getSaturation();
        }

        return s;
    }

    // ========================= APPLY SAFE =========================

    public void apply(Player p,
                      boolean syncArmor,
                      boolean syncOffhand,
                      boolean syncEnder,
                      boolean syncXp,
                      boolean syncHealthFood) {

        if (storageContents != null) {
            p.getInventory().setStorageContents(nullToEmpty(storageContents, 36));
        }

        if (syncArmor && armorContents != null) {
            p.getInventory().setArmorContents(nullToEmpty(armorContents, 4));
        }

        if (syncOffhand && offhandItem != null) {
            p.getInventory().setItemInOffHand(offhandItem);
        }

        if (syncEnder && enderContents != null) {
            p.getEnderChest().setContents(nullToEmpty(enderContents, 27));
        }

        if (syncXp) {
            p.setTotalExperience(0);
            p.setExp(0);
            p.setLevel(0);

            p.setLevel(level);
            p.setExp(exp);
            p.setTotalExperience(totalExp);
        }

        if (syncHealthFood) {
            double max = Objects.requireNonNull(p.getAttribute(Attribute.MAX_HEALTH)).getValue();
            p.setHealth(Math.max(0.0, Math.min(health, max)));
            p.setFoodLevel(Math.max(0, Math.min(food, 20)));
            p.setSaturation(Math.max(0f, Math.min(saturation, 20f)));
        }

        p.updateInventory();
    }

    private static ItemStack[] cloneArray(ItemStack[] original) {
        return original == null ? null : original.clone();
    }

    private static ItemStack[] nullToEmpty(ItemStack[] arr, int size) {
        if (arr == null) return null;

        if (arr.length == size) return arr;

        ItemStack[] fixed = new ItemStack[size];
        System.arraycopy(arr, 0, fixed, 0, Math.min(arr.length, size));
        return fixed;
    }

    // ========================= SAVE =========================

    public void save(ConfigurationSection sec,
                     boolean syncArmor,
                     boolean syncOffhand,
                     boolean syncEnder,
                     boolean syncXp,
                     boolean syncHealthFood) {

        if (storageContents != null)
            sec.set("storage", ItemStackBase64.toBase64(storageContents));

        if (syncArmor && armorContents != null)
            sec.set("armor", ItemStackBase64.toBase64(armorContents));

        if (syncOffhand && offhandItem != null)
            sec.set("offhand", ItemStackBase64.toBase64(offhandItem));

        if (syncEnder && enderContents != null)
            sec.set("ender", ItemStackBase64.toBase64(enderContents));

        if (syncXp) {
            sec.set("xp.total", totalExp);
            sec.set("xp.level", level);
            sec.set("xp.exp", exp);
        }

        if (syncHealthFood) {
            sec.set("hf.health", health);
            sec.set("hf.food", food);
            sec.set("hf.saturation", saturation);
        }
    }

    // ========================= LOAD SAFE =========================

    public static InvSnapshot load(ConfigurationSection sec,
                                   boolean syncArmor,
                                   boolean syncOffhand,
                                   boolean syncEnder,
                                   boolean syncXp,
                                   boolean syncHealthFood) {

        InvSnapshot s = new InvSnapshot();

        try {
            String storage = sec.getString("storage");
            if (storage != null)
                s.storageContents = ItemStackBase64.fromBase64Array(storage);

            if (syncArmor) {
                String armor = sec.getString("armor");
                if (armor != null)
                    s.armorContents = ItemStackBase64.fromBase64Array(armor);
            }

            if (syncOffhand) {
                String offhand = sec.getString("offhand");
                if (offhand != null)
                    s.offhandItem = ItemStackBase64.fromBase64Item(offhand);
            }

            if (syncEnder) {
                String ender = sec.getString("ender");
                if (ender != null)
                    s.enderContents = ItemStackBase64.fromBase64Array(ender);
            }

            if (syncXp) {
                s.totalExp = sec.getInt("xp.total", 0);
                s.level = sec.getInt("xp.level", 0);
                s.exp = (float) sec.getDouble("xp.exp", 0.0);
            }

            if (syncHealthFood) {
                s.health = sec.getDouble("hf.health", 20.0);
                s.food = sec.getInt("hf.food", 20);
                s.saturation = (float) sec.getDouble("hf.saturation", 5.0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return s;
    }

    // ========================= BASE64 SERIALIZER =========================

    static final class ItemStackBase64 {

        public static String toBase64(ItemStack item) {
            if (item == null) return null;
            return toBase64(new ItemStack[]{item});
        }

        public static String toBase64(ItemStack[] items) {
            if (items == null) return null;

            try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                 java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {

                oos.writeInt(items.length);
                for (ItemStack it : items) oos.writeObject(it);

                return Base64.getEncoder().encodeToString(baos.toByteArray());

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static ItemStack fromBase64Item(String b64) {
            ItemStack[] arr = fromBase64Array(b64);
            return (arr != null && arr.length > 0) ? arr[0] : null;
        }

        public static ItemStack[] fromBase64Array(String b64) {
            if (b64 == null || b64.isEmpty()) return null;

            try {
                byte[] data = Base64.getDecoder().decode(b64);

                try (java.io.ObjectInputStream ois =
                             new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(data))) {

                    int len = ois.readInt();
                    ItemStack[] items = new ItemStack[len];

                    for (int i = 0; i < len; i++)
                        items[i] = (ItemStack) ois.readObject();

                    return items;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}