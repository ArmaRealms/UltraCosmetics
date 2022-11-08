package be.isach.ultracosmetics.cosmetics.type;

import be.isach.ultracosmetics.config.MessageManager;
import be.isach.ultracosmetics.cosmetics.suits.ArmorSlot;
import be.isach.ultracosmetics.cosmetics.suits.Suit;
import be.isach.ultracosmetics.util.MathUtils;

import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import com.cryptomorin.xseries.XMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Suit types.
 *
 * @author iSach
 * @since 12-20-2015
 */
public class SuitType extends CosmeticType<Suit> {

    private static final List<SuitType> ENABLED = new ArrayList<>();
    private static final List<SuitType> VALUES = new ArrayList<>();

    public static List<SuitType> enabled() {
        return ENABLED;
    }

    public static List<SuitType> values() {
        return VALUES;
    }

    public static SuitType getSuitPart(String name, ArmorSlot slot) {
        for (SuitType suitType : VALUES) {
            if (suitType.getConfigName().equalsIgnoreCase(name) && suitType.getSlot() == slot) return suitType;
        }
        return null;
    }

    public static void checkEnabled() {
        ENABLED.addAll(values().stream().filter(CosmeticType::isEnabled).collect(Collectors.toList()));
    }

    private final ArmorSlot slot;
    private final SuitCategory category;

    /**
     * @param material The suit part material
     * @param slot     The slot this suit part should occupy
     * @param category The Suit category this part belongs to
     */
    protected SuitType(XMaterial material, ArmorSlot slot, SuitCategory category) {
        super(slot.getSuitsCategory(), category.getConfigName(), material, category.getSuitClass(), false);
        this.slot = slot;
        this.category = category;
        // delay permission registration until we've loaded slot and category fields
        registerPermission();
        VALUES.add(this);
    }

    @Override
    public String getName() {
        return MessageManager.getMessage("Suits." + getConfigName() + "." + slot.toString().toLowerCase() + "-name");
    }

    @Override
    protected String getPermissionSuffix() {
        return category.getPermissionSuffix() + "." + slot.toString().toLowerCase();
    }

    public ArmorSlot getSlot() {
        return slot;
    }

    public SuitCategory getSuitCategory() {
        return category;
    }

    @Override
    public ItemStack getItemStack() {
        ItemStack is = super.getItemStack();
        Color color = null;
        if (category == SuitCategory.RAVE) {
            int r = MathUtils.random(255);
            int g = MathUtils.random(255);
            int b = MathUtils.random(255);

            color = Color.fromRGB(r, g, b);
        } else if (category == SuitCategory.SANTA) {
            color = Color.RED;
        } else if (category == SuitCategory.FROZEN && slot != ArmorSlot.HELMET) {
            color = Color.AQUA;
        }
        if (color != null) {
            LeatherArmorMeta meta = (LeatherArmorMeta) is.getItemMeta();
            meta.setColor(color);
            is.setItemMeta(meta);
        }
        return is;
    }
}
