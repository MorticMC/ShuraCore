package dev.shura.core.id;

import java.time.LocalDate;
import java.util.UUID;

public class PlayerID {

    private final UUID playerUuid;
    private final String idNumber;
    private final String irlName;
    private final int age;
    private final String gender;
    private final String country;
    private final String discordUsername;
    private final LocalDate registrationDate;
    private final LocalDate expiryDate;
    private boolean activated;
    private long activationStartTime;

    public PlayerID(UUID playerUuid, String idNumber, String irlName, int age, String gender, 
                    String country, String discordUsername, LocalDate registrationDate, 
                    LocalDate expiryDate, boolean activated, long activationStartTime) {
        this.playerUuid = playerUuid;
        this.idNumber = idNumber;
        this.irlName = irlName;
        this.age = age;
        this.gender = gender;
        this.country = country;
        this.discordUsername = discordUsername;
        this.registrationDate = registrationDate;
        this.expiryDate = expiryDate;
        this.activated = activated;
        this.activationStartTime = activationStartTime;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getIdNumber() { return idNumber; }
    public String getIrlName() { return irlName; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getCountry() { return country; }
    public String getDiscordUsername() { return discordUsername; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }
    public long getActivationStartTime() { return activationStartTime; }
    public void setActivationStartTime(long time) { this.activationStartTime = time; }
}
