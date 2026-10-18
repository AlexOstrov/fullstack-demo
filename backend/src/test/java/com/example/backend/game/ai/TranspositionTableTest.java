package com.example.backend.game.ai;

import com.example.backend.game.Board;
import com.example.backend.game.Cell;
import com.example.backend.game.Color;
import com.example.backend.game.Move;
import com.example.backend.game.MoveType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranspositionTableTest {

    private final Zobrist zobrist = Zobrist.INSTANCE;

    private static Move sampleMove() {
        return new Move(List.of(new Cell(3, 2), new Cell(2, 1)), MoveType.NORMAL);
    }

    @Test
    void storeThenProbeReturnsEntry() {
        TranspositionTable table = new TranspositionTable(64);
        long hash = zobrist.hash(Board.initialPosition(), Color.WHITE);

        table.store(hash, 4, 123, TranspositionTable.Flag.EXACT, sampleMove());

        TranspositionTable.Entry entry = table.probe(hash);
        assertNotNull(entry);
        assertEquals(4, entry.depth());
        assertEquals(123, entry.score());
        assertEquals(TranspositionTable.Flag.EXACT, entry.flag());
        assertEquals(sampleMove(), entry.move());
    }

    @Test
    void probeMissesOnForeignHashInSameSlot() {
        TranspositionTable table = new TranspositionTable(64);
        long hash = zobrist.hash(Board.initialPosition(), Color.WHITE);

        table.store(hash, 4, 10, TranspositionTable.Flag.EXACT, sampleMove());

        assertNull(table.probe(hash + table.capacity()),
                "слот занят чужим хэшем — запись отбрасывается");
    }

    @Test
    void capacityRoundsUpToPowerOfTwo() {
        assertEquals(64, new TranspositionTable(64).capacity());
        assertEquals(128, new TranspositionTable(100).capacity());
        assertEquals(1 << 16, new TranspositionTable(AiSettings.DEFAULT_TT_CAPACITY).capacity());
    }

    @Test
    void sizeStaysLimitedAndOverflowOverwrites() {
        TranspositionTable table = new TranspositionTable(64);
        int capacity = table.capacity();

        for (int i = 0; i < capacity + 16; i++) {
            table.store(31L * i + 7, 1, i, TranspositionTable.Flag.UPPER, sampleMove());
        }

        assertEquals(capacity, table.capacity(), "размер TT ограничен вместимостью");
        assertNotNull(table.probe(31L * (capacity + 15) + 7), "последняя запись перезаполнила слот");
    }

    @Test
    void deeperEntryIsNotReplacedByShallowOne() {
        TranspositionTable table = new TranspositionTable(64);
        long hash = zobrist.hash(Board.initialPosition(), Color.WHITE);

        table.store(hash, 4, 50, TranspositionTable.Flag.LOWER, sampleMove());
        table.store(hash, 2, 10, TranspositionTable.Flag.UPPER, sampleMove());

        TranspositionTable.Entry entry = table.probe(hash);
        assertEquals(4, entry.depth(), "глубокая запись не затирается более слабой");
        assertEquals(50, entry.score());

        table.store(hash, 6, 70, TranspositionTable.Flag.EXACT, sampleMove());
        assertEquals(6, table.probe(hash).depth(), "более глубокая запись заменяет старую");
    }

    @Test
    void capacityBelowMinimumThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(32));
    }
}
