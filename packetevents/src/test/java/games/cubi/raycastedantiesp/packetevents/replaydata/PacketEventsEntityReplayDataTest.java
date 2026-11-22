package games.cubi.raycastedantiesp.packetevents.replaydata;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketEventsEntityReplayDataTest {
    @Test
    void repeatedPartialUpdatesKeepTheLatestValueForEachIndex() {
        PacketEventsEntityReplayData replayData = PacketEventsEntityReplayData.create();
        replayData.mergeMetadata(List.of(new EntityData<>(0, null, (byte) 0x42), new EntityData<>(5, null, true)));

        for (int value = 0; value < 1_000; value++) {
            replayData.mergeMetadata(List.of(new EntityData<>(9, null, value)));
        }
        replayData.mergeMetadata(List.of(new EntityData<>(0, null, (byte) 0)));

        Map<Integer, Object> values = replayData.metadataForReplay().stream()
                .collect(Collectors.toMap(EntityData::getIndex, EntityData::getValue));
        assertEquals(Map.of(0, (byte) 0, 5, true, 9, 999), values);
    }

    @Test
    void replayBorrowsTheListAndReusesCachedEntries() {
        PacketEventsEntityReplayData replayData = PacketEventsEntityReplayData.create();
        EntityDataType<Integer> intType = new EntityDataType<>(null, PacketWrapper::readInt, PacketWrapper::writeInt);
        EntityDataType<Byte> byteType = new EntityDataType<>(null, PacketWrapper::readByte, PacketWrapper::writeByte);
        EntityData<Integer> update = new EntityData<>(5, intType, 10);
        replayData.mergeMetadata(List.of(update));
        List<EntityData<?>> metadata = replayData.metadataForReplay();
        EntityData<?> cached = metadata.getFirst();

        update.setIndex(9);
        update.setValue(20);
        assertEquals(5, cached.getIndex());
        assertEquals(10, cached.getValue());

        replayData.mergeMetadata(List.of(new EntityData<>(5, intType, 30)));
        assertSame(metadata, replayData.metadataForReplay());
        assertSame(cached, metadata.getFirst());
        assertEquals(30, cached.getValue());

        replayData.mergeMetadata(List.of(new EntityData<>(5, byteType, (byte) 0)));
        assertEquals(1, metadata.size());
        assertSame(byteType, metadata.getFirst().getType());
        assertEquals((byte) 0, metadata.getFirst().getValue());
    }

    @Test
    void preallocatedMetadataReplaysPopulatedIndicesInOrder() {
        PacketEventsEntityReplayData replayData = PacketEventsEntityReplayData.create(20);
        List<EntityData<?>> metadata = replayData.metadataForReplay();
        assertTrue(metadata.isEmpty());

        replayData.mergeMetadata(List.of(new EntityData<>(10, null, true), new EntityData<>(2, null, 5)));

        assertSame(metadata, replayData.metadataForReplay());
        assertEquals(List.of(2, 10), metadata.stream().map(EntityData::getIndex).toList());
        assertEquals(2, metadata.getFirst().getIndex());
        assertEquals(10, metadata.getLast().getIndex());
    }

    @Test
    void supportsEveryWireIndexAndClearsFields() {
        PacketEventsEntityReplayData replayData = PacketEventsEntityReplayData.create();
        List<EntityData<?>> updates = new ArrayList<>();
        for (int index = 0; index < 255; index++) {
            updates.add(new EntityData<>(index, null, index));
        }
        replayData.mergeMetadata(updates);
        replayData.mergeMetadata(List.of(new EntityData<>(254, null, 0)));

        List<EntityData<?>> metadata = replayData.metadataForReplay();
        assertEquals(255, metadata.size());
        assertEquals(254, metadata.getLast().getIndex());
        assertEquals(0, metadata.getLast().getValue());

        replayData.clear();
        assertTrue(replayData.metadataForReplay().isEmpty());

        replayData.mergeMetadata(List.of(new EntityData<>(254, null, 1)));
        assertEquals(1, replayData.metadataForReplay().size());
        assertEquals(1, replayData.metadataForReplay().getFirst().getValue());
    }
}
