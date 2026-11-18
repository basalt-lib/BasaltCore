package tlm.dev.basalt.core.overlay;

import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class OverlaySnapshot<L> {

    private final UUID id;
    private final String owner;
    private final UUID world;
    private final ChunkRange area;
    private final OverlayBlockIndex blocks;
    private final List<OverlayEntity<L>> entities;
    private final int[] entityIds;

    OverlaySnapshot(UUID id, String owner, UUID world, ChunkRange area, OverlayBlockIndex blocks, List<OverlayEntity<L>> entities) {
        this.id = id;
        this.owner = owner;
        this.world = world;
        this.area = area;
        this.blocks = blocks;
        List<OverlayEntity<L>> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.comparingInt(OverlayEntity::id));
        this.entities = List.copyOf(sorted);
        this.entityIds = this.entities.stream().mapToInt(OverlayEntity::id).toArray();
    }

    public static <L> OverlaySnapshot<L> none() {
        return new OverlaySnapshot<>(null, "", null, null, OverlayBlockIndex.empty(), List.of());
    }

    public boolean isNone() {
        return id == null;
    }

    public UUID id() {
        return id;
    }

    public String owner() {
        return owner;
    }

    public UUID world() {
        return world;
    }

    public ChunkRange area() {
        return area;
    }

    public OverlayBlockIndex blocks() {
        return blocks;
    }

    public List<OverlayEntity<L>> entities() {
        return entities;
    }

    public boolean covers(int chunkX, int chunkZ) {
        return area != null && chunkX >= area.minX() && chunkX <= area.maxX() && chunkZ >= area.minZ() && chunkZ <= area.maxZ();
    }

    public boolean touches(int chunkX, int chunkZ) {
        if (!covers(chunkX, chunkZ)) {
            return false;
        }
        if (blocks.holdsChunk(chunkX, chunkZ)) {
            return true;
        }
        for (OverlayEntity<L> entity : entities) {
            if (entity.chunkX() == chunkX && entity.chunkZ() == chunkZ) {
                return true;
            }
        }
        return false;
    }

    public List<OverlayEntity<L>> entitiesIn(int chunkX, int chunkZ) {
        List<OverlayEntity<L>> found = new ArrayList<>();
        for (OverlayEntity<L> entity : entities) {
            if (entity.chunkX() == chunkX && entity.chunkZ() == chunkZ) {
                found.add(entity);
            }
        }
        return found;
    }

    public int stateAt(int x, int y, int z) {
        return blocks.stateAt(x, y, z);
    }

    public boolean holdsEntity(int entity) {
        return Arrays.binarySearch(entityIds, entity) >= 0;
    }

    public Optional<OverlayEntity<L>> entity(int entity) {
        int found = Arrays.binarySearch(entityIds, entity);
        return found < 0 ? Optional.empty() : Optional.of(entities.get(found));
    }

    public boolean claimsBlock(BlockPosition clicked) {
        return stateAt(clicked.x(), clicked.y(), clicked.z()) != OverlayBlockIndex.NONE;
    }

    public boolean claimsUse(BlockPosition clicked, BlockFace face) {
        return claimsBlock(clicked)
                || stateAt(clicked.x() + face.stepX(), clicked.y() + face.stepY(), clicked.z() + face.stepZ()) != OverlayBlockIndex.NONE;
    }
}
