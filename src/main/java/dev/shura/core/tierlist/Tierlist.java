package dev.shura.core.tierlist;

public class Tierlist {

    private final String id;
    private String name;
    private String kitId;
    private boolean enabled;
    private boolean queueEnabled;

    public Tierlist(String id, String name, String kitId) {
        this.id = id;
        this.name = name;
        this.kitId = kitId;
        this.enabled = true;
        this.queueEnabled = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKitId() { return kitId; }
    public void setKitId(String kitId) { this.kitId = kitId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isQueueEnabled() { return queueEnabled; }
    public void setQueueEnabled(boolean queueEnabled) { this.queueEnabled = queueEnabled; }
}
