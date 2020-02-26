package dev.esophose.rosestacker.manager;

import dev.esophose.rosestacker.RoseStacker;
import dev.esophose.rosestacker.hologram.HologramHandler;
import dev.esophose.rosestacker.hologram.HologramsHologramHandler;
import dev.esophose.rosestacker.hologram.HolographicDisplaysHologramHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class HologramManager extends Manager {

    private HologramHandler hologramHandler;

    public HologramManager(RoseStacker roseStacker) {
        super(roseStacker);
    }

    @Override
    public void reload() {
        if (this.hologramHandler != null)
            this.hologramHandler.deleteAllHolograms();

        if (Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            this.roseStacker.getLogger().info("HolographicDisplays is being used as the Hologram Handler.");
            this.hologramHandler = new HolographicDisplaysHologramHandler();
        } else if (Bukkit.getPluginManager().isPluginEnabled("Holograms")) {
            this.roseStacker.getLogger().info("Holograms is being used as the Hologram Handler.");
            this.hologramHandler = new HologramsHologramHandler();
        } else {
            this.roseStacker.getLogger().warning("No Hologram Handler plugin was detected. " +
                    "If you want Stack tags to be displayed above stacked spawners or blocks, " +
                    "please install one of the following plugins: [HolographicDisplays, Holograms]");
            this.hologramHandler = null;
        }
    }

    @Override
    public void disable() {
        if (this.hologramHandler != null)
            this.hologramHandler.deleteAllHolograms();
    }

    /**
     * Creates or updates a hologram at the given location
     *
     * @param location The location of the hologram
     * @param text The text for the hologram
     */
    public void createOrUpdateHologram(Location location, String text) {
        if (this.hologramHandler != null)
            this.hologramHandler.createOrUpdateHologram(location, text);
    }

    /**
     * Deletes a hologram at a given location if one exists
     *
     * @param location The location of the hologram
     */
    public void deleteHologram(Location location) {
        if (this.hologramHandler != null)
            this.hologramHandler.deleteHologram(location);
    }

    /**
     * Deletes all holograms
     */
    public void deleteAllHolograms() {
        if (this.hologramHandler != null)
            this.hologramHandler.deleteAllHolograms();
    }

    /**
     * Checks if the given Entity is part of a hologram
     *
     * @param entity The Entity to check
     * @return true if the Entity is a hologram, otherwise false
     */
    public boolean isHologram(Entity entity) {
        if (this.hologramHandler != null)
            return this.hologramHandler.isHologram(entity);
        return false;
    }

}
