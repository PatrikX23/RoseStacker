package dev.rosewood.rosestacker.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosestacker.stack.StackingThread;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

public class EntityCacheManager extends Manager {

    private final Map<ChunkLocation, Collection<Entity>> entityCache;
    private BukkitTask refreshTask;

    public EntityCacheManager(RosePlugin rosePlugin) {
        super(rosePlugin);
        this.entityCache = new ConcurrentHashMap<>();
    }

    @Override
    public void reload() {
        this.refreshTask = Bukkit.getScheduler().runTaskTimer(this.rosePlugin, this::refresh, 5L, 60L);
    }

    @Override
    public void disable() {
        this.entityCache.clear();

        if (this.refreshTask != null) {
            this.refreshTask.cancel();
            this.refreshTask = null;
        }
    }

    /**
     * Gets nearby entities from cache
     *
     * @param center The center of the area to check
     * @param radius The radius to check around
     * @param predicate Conditions to be met
     * @return A Set of nearby entities
     */
    public Collection<Entity> getNearbyEntities(Location center, double radius, Predicate<Entity> predicate) {
        List<Entity> nearbyEntities = new ArrayList<>();
        World world = center.getWorld();
        if (world == null)
            return nearbyEntities;

        BoundingBox boundingBox = new BoundingBox(
                center.getX() - radius,
                center.getY() - radius,
                center.getZ() - radius,
                center.getX() + radius,
                center.getY() + radius,
                center.getZ() + radius
        );

        int minX = (int) boundingBox.getMinX() >> 4;
        int maxX = (int) boundingBox.getMaxX() >> 4;
        int minZ = (int) boundingBox.getMinZ() >> 4;
        int maxZ = (int) boundingBox.getMaxZ() >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Collection<Entity> entities = this.entityCache.get(new ChunkLocation(world, x, z));
                if (entities != null)
                    nearbyEntities.addAll(entities);
            }
        }

        return nearbyEntities.stream()
                .filter(Entity::isValid)
                .filter(x -> boundingBox.contains(x.getLocation().toVector()))
                .filter(predicate)
                .collect(Collectors.toSet());
    }

    /**
     * Gets entities in the Chunk of a Location
     *
     * @param location The Location of the Chunk
     * @param predicate Conditions to be met
     * @return A Set of entities in the chunk
     */
    public Collection<Entity> getEntitiesInChunk(Location location, Predicate<Entity> predicate) {
        World world = location.getWorld();
        if (world == null)
            return new ArrayList<>();

        Collection<Entity> entities = this.entityCache.get(new ChunkLocation(world, location.getBlockX() >> 4, location.getBlockZ() >> 4));
        if (entities == null)
            return new ArrayList<>();

        return entities.stream().filter(predicate).collect(Collectors.toSet());
    }

    /**
     * Forces an entry into the cache, used for newly spawned entities
     *
     * @param entity The entity to cache
     */
    public void preCacheEntity(Entity entity) {
        Location location = entity.getLocation();
        Collection<Entity> entities = this.entityCache.get(new ChunkLocation(entity.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4));
        if (entities != null)
            entities.add(entity);
    }

    private void refresh() {
        synchronized (this.entityCache) {
            this.entityCache.clear();
            for (StackingThread stackingThread : this.rosePlugin.getManager(StackManager.class).getStackingThreads().values()) {
                World world = stackingThread.getTargetWorld();
                for (Entity entity : world.getEntities()) {
                    ChunkLocation chunkLocation = new ChunkLocation(world, entity.getLocation().getBlockX() >> 4, entity.getLocation().getBlockZ() >> 4);
                    Collection<Entity> entities = this.entityCache.get(chunkLocation);
                    if (entities == null) {
                        entities = new LinkedBlockingDeque<>();
                        this.entityCache.put(chunkLocation, entities);
                    }
                    entities.add(entity);
                }
            }
        }
    }

    private static class ChunkLocation {

        private final World world;
        private final int x, z;

        public ChunkLocation(World world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        public World getWorld() {
            return this.world;
        }

        public int getX() {
            return this.x;
        }

        public int getZ() {
            return this.z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof ChunkLocation))
                return false;

            ChunkLocation other = (ChunkLocation) o;
            return this.x == other.x && this.z == other.z && this.world.equals(other.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.world, this.x, this.z);
        }

    }

}
