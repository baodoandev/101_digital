package com.coffee.platform.order;

import com.coffee.platform.queue.QueueEntryRepository;
import com.coffee.platform.shop.ShopQueue;
import com.coffee.platform.shop.ShopQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueSelectorTest {

    @Mock
    private ShopQueueRepository queueRepo;

    @Mock
    private QueueEntryRepository entryRepo;

    private QueueSelector selector;

    @BeforeEach
    void setUp() {
        selector = new QueueSelector(queueRepo, entryRepo);
    }

    private ShopQueue makeQueue(Long id, Long shopId, int maxSize, boolean active) {
        try {
            var ctor = ShopQueue.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ShopQueue q = ctor.newInstance();
            setField(q, "id", id);
            q.setShopId(shopId);
            q.setLabel("Q-" + id);
            q.setMaxSize(maxSize);
            q.setActive(active);
            return q;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("Single queue with space selects it")
    void singleQueueWithSpace_selectsIt() {
        ShopQueue q = makeQueue(1L, 10L, 5, true);
        when(queueRepo.findByShopId(10L)).thenReturn(List.of(q));
        when(entryRepo.countWaiting(1L)).thenReturn(2);

        Optional<ShopQueue> result = selector.selectQueue(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Two queues picks the one with fewer entries")
    void twoQueues_picksLeastLoaded() {
        ShopQueue q1 = makeQueue(1L, 10L, 10, true);
        ShopQueue q2 = makeQueue(2L, 10L, 10, true);
        when(queueRepo.findByShopId(10L)).thenReturn(List.of(q1, q2));
        when(entryRepo.countWaiting(1L)).thenReturn(5);
        when(entryRepo.countWaiting(2L)).thenReturn(2);

        Optional<ShopQueue> result = selector.selectQueue(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("All queues full returns empty")
    void allQueuesFull_returnsEmpty() {
        ShopQueue q = makeQueue(1L, 10L, 3, true);
        when(queueRepo.findByShopId(10L)).thenReturn(List.of(q));
        when(entryRepo.countWaiting(1L)).thenReturn(3);

        Optional<ShopQueue> result = selector.selectQueue(10L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("No queues returns empty")
    void noQueues_returnsEmpty() {
        when(queueRepo.findByShopId(10L)).thenReturn(List.of());

        Optional<ShopQueue> result = selector.selectQueue(10L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Inactive queue is skipped")
    void inactiveQueue_isSkipped() {
        ShopQueue inactive = makeQueue(1L, 10L, 10, false);
        ShopQueue active = makeQueue(2L, 10L, 10, true);
        when(queueRepo.findByShopId(10L)).thenReturn(List.of(inactive, active));
        when(entryRepo.countWaiting(2L)).thenReturn(1);

        Optional<ShopQueue> result = selector.selectQueue(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(2L);
    }
}
