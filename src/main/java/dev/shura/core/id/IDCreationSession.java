package dev.shura.core.id;

import java.util.UUID;

public class IDCreationSession {

    private final UUID playerUuid;
    private String irlName;
    private Integer age;
    private String gender;
    private String country;
    private String discordUsername;
    private CreationStep currentStep;

    public enum CreationStep {
        CONFIRM_START,
        IRL_NAME,
        AGE,
        GENDER,
        COUNTRY,
        DISCORD_USERNAME,
        FINAL_CONFIRMATION
    }

    public IDCreationSession(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.currentStep = CreationStep.CONFIRM_START;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getIrlName() { return irlName; }
    public void setIrlName(String irlName) { this.irlName = irlName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getDiscordUsername() { return discordUsername; }
    public void setDiscordUsername(String discordUsername) { this.discordUsername = discordUsername; }
    public CreationStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(CreationStep step) { this.currentStep = step; }
}
