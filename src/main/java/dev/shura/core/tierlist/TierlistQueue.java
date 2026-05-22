package dev.shura.core.tierlist;

import dev.shura.core.queue.QueueEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TierlistQueue {

    private final String tierlistId;
    private final ConcurrentLinkedQueue<QueueEntry> queue = new ConcurrentLinkedQueue<>();

    public TierlistQueue(String tierlistId) {
        this.tierlistId = tierlistId;
    }

    public void add(QueueEntry entry) { queue.add(entry); }
    public void remove(QueueEntry entry) { queue.remove(entry); }
    public int size() { return queue.size(); }
    public List<QueueEntry> snapshot() { return new ArrayList<>(queue); }
    public String getTierlistId() { return tierlistId; }
}
