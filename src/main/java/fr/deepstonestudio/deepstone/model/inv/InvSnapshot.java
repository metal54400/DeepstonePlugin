package fr.deepstonestudio.deepstone.model.inv;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.Objects;

public final class InvSnapshot {

    // ✅ Champs (noms explicites, pas de conflit avec les booleans)
    public ItemStack[] storageContents;   // 36
    public ItemStack[] armorContents;     // 4
    public ItemStack offhandItem;         // 1
    public ItemStack[] enderContents;     // 27

    public int totalExp;
    public int level;
    public float exp;

    public double health;
    public int food;
    public float saturation;

    // -------- capture/apply --------

    public static InvSnapshot capture(Player p,
                                      boolean syncArmor,
                                      boolean syncOffhand,
                                      boolean syncEnder,
                                      boolean syncXp,
                                      boolean syncHealthFood) {

        InvSnapshot s = new InvSnapshot();

        s.storageContents = p.getInventory().getStorageContents(); // 0-35

        if (syncArmor) s.armorContents = p.getInventory().getArmorContents();
        if (syncOffhand) s.offhandItem = p.getInventory().getItemInOffHand();
        if (syncEnder) s.enderContents = p.getEnderChest().getContents();

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

    public void apply(Player p,
                      boolean syncArmor,
                      boolean syncOffhand,
                      boolean syncEnder,
                      boolean syncXp,
                      boolean syncHealthFood) {

        p.getInventory().setStorageContents(nullToEmpty(storageContents, 36));

        if (syncArmor) {
            p.getInventory().setArmorContents(nullToEmpty(armorContents, 4));
        }

        if (syncOffhand) {
            p.getInventory().setItemInOffHand(offhandItem); // ✅ ItemStack, plus boolean
        }

        if (syncEnder) {
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

    private static ItemStack[] nullToEmpty(ItemStack[] arr, int size) {
        if (arr == null) return new ItemStack[size];
        if (arr.length == size) return arr;

        ItemStack[] fixed = new ItemStack[size];
        System.arraycopy(arr, 0, fixed, 0, Math.min(arr.length, size));
        return fixed;
    }

    // -------- YAML serialize (Base64 via Java Object streams) --------

    public void save(ConfigurationSection sec,
                     boolean syncArmor,
                     boolean syncOffhand,
                     boolean syncEnder,
                     boolean syncXp,
                     boolean syncHealthFood) {

        sec.set("storage", ItemStackBase64.toBase64(storageContents));

        if (syncArmor) sec.set("armor", ItemStackBase64.toBase64(armorContents));
        if (syncOffhand) sec.set("offhand", ItemStackBase64.toBase64(offhandItem));
        if (syncEnder) sec.set("ender", ItemStackBase64.toBase64(enderContents));

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

    public static InvSnapshot load(ConfigurationSection sec,
                                   boolean syncArmor,
                                   boolean syncOffhand,
                                   boolean syncEnder,
                                   boolean syncXp,
                                   boolean syncHealthFood) {

        InvSnapshot s = new InvSnapshot();

        s.storageContents = ItemStackBase64.fromBase64Array(sec.getString("storage"));

        if (syncArmor) s.armorContents = ItemStackBase64.fromBase64Array(sec.getString("armor"));
        if (syncOffhand) s.offhandItem = ItemStackBase64.fromBase64Item(sec.getString("offhand"));
        if (syncEnder) s.enderContents = ItemStackBase64.fromBase64Array(sec.getString("ender"));

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

        return s;
    }

    // ---------- helper serializer ----------
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
                oos.flush();
                return Base64.getEncoder().encodeToString(baos.toByteArray());

            } catch (Exception e) {
                return null;
            }
        }

        public static ItemStack fromBase64Item(String b64) {
            ItemStack[] arr = fromBase64Array(b64);
            return (arr != null && arr.length > 0) ? arr[0] : null;
        }

        public static ItemStack[] fromBase64Array(String b64) {
            if (b64 == null || b64.isEmpty()) return null;
            byte[] data = Base64.getDecoder().decode(b64);

            try (java.io.ObjectInputStream ois =
                         new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(data))) {

                int len = ois.readInt();
                ItemStack[] items = new ItemStack[len];
                for (int i = 0; i < len; i++) items[i] = (ItemStack) ois.readObject();
                return items;

            } catch (Exception e) {
                return null;
            }
        }
    }
}