package tlm.dev.basalt.plugin.world;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.edit.SectionWrite;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.world.Departure;
import tlm.dev.basalt.core.world.HostArea;
import tlm.dev.basalt.core.world.HostRule;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.core.world.WorldChunks;
import tlm.dev.basalt.core.world.WorldListener;
import tlm.dev.basalt.core.world.WorldSettings;
import tlm.dev.basalt.core.world.WorldTools;
import tlm.dev.basalt.core.world.WorldViewers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldSessionsTest {

    private static final HostArea AREA = new HostArea(new UUID(0, 1), new ChunkRange(68, 64, 69, 65), -64, 384);

    @Test
    void countsTheHeldEntitiesOnlyOnceEveryWorldIsClosed() {
        VirtualWorlds<String> worlds = new VirtualWorlds<>(new WorldTools<>(new NamedChunks(), (world, chunk) -> Optional.empty(),
                new SilentViewers(), new WorldListener() {
                }), new WorldSettings(4, 2), UUID::randomUUID);
        worlds.create(AREA, HostRule.AIR_ONLY, "Owner");
        List<String> lines = new ArrayList<>();
        WorldSessions sessions = new WorldSessions(worlds, () -> worlds.open().size(), recording(lines));

        sessions.onDisable();

        assertEquals(List.of("Closed 1 virtual world", "Virtual entities still held: 0"), lines);
    }

    private static Logger recording(List<String> lines) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                lines.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }

    private static final class NamedChunks implements WorldChunks<String> {

        @Override
        public String empty(ChunkKey chunk, HostArea host) {
            return "empty " + chunk;
        }

        @Override
        public String fill(String chunk, BlockBox box, int state) {
            return chunk;
        }

        @Override
        public String written(String chunk, List<SectionWrite> sections) {
            return chunk;
        }

        @Override
        public int stateAt(String chunk, BlockPosition position) {
            return 0;
        }

        @Override
        public int air() {
            return 0;
        }
    }

    private static final class SilentViewers implements WorldViewers {

        @Override
        public boolean enter(UUID player, UUID world, ChunkRange area) {
            return true;
        }

        @Override
        public void exit(UUID player, Departure departure) {
        }

        @Override
        public void blockChanged(Set<UUID> viewers, BlockPosition position, int state) {
        }

        @Override
        public void chunksChanged(Set<UUID> viewers, List<ChunkKey> chunks) {
        }
    }
}
