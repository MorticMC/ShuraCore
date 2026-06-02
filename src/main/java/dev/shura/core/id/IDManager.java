package dev.shura.core.id;

import dev.shura.core.ShuraCore;
import dev.shura.core.extra.MessageService;
import dev.shura.core.util.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IDManager {

    private final ShuraCore plugin;
    private final Map<UUID, PlayerID> playerIDs = new ConcurrentHashMap<>();
    private final Map<UUID, IDCreationSession> creationSessions = new ConcurrentHashMap<>();
    private final Set<String> usedIDNumbers = new HashSet<>();
    private Location idCreationRoom;

    public IDManager(ShuraCore plugin) {
        this.plugin = plugin;
        loadIDsFromDatabase();
    }

    public void startIDCreation(Player player) {
        if (playerIDs.containsKey(player.getUniqueId())) {
            player.sendMessage(MessageService.colorize("&#FF0000You already have an ID!"));
            return;
        }

        if (creationSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(MessageService.colorize("&#FF0000You are already creating an ID!"));
            return;
        }

        if (idCreationRoom != null) {
            player.teleport(idCreationRoom);
        }

        IDCreationSession session = new IDCreationSession(player.getUniqueId());
        creationSessions.put(player.getUniqueId(), session);

        showTitle(player, "&#FFD700ID", "&#FFFF00Type Yes in chat if you want to create an ID");
        player.sendMessage(MessageService.colorize("&#FFD700Type &#FFFF00Yes &#FFD700in chat to confirm ID creation, or &#FF0000No &#FFD700to cancel."));
    }

    public void handleChatInput(Player player, String message) {
        IDCreationSession session = creationSessions.get(player.getUniqueId());
        if (session == null) return;

        switch (session.getCurrentStep()) {
            case CONFIRM_START:
                if (message.equalsIgnoreCase("yes")) {
                    session.setCurrentStep(IDCreationSession.CreationStep.IRL_NAME);
                    player.sendMessage(MessageService.colorize("&#FFD700What is your &#FFFF00IRL Name&#FFD700?"));
                } else if (message.equalsIgnoreCase("no")) {
                    creationSessions.remove(player.getUniqueId());
                    player.sendMessage(MessageService.colorize("&#FF0000ID creation cancelled."));
                }
                break;

            case IRL_NAME:
                session.setIrlName(message);
                session.setCurrentStep(IDCreationSession.CreationStep.AGE);
                player.sendMessage(MessageService.colorize("&#FFD700What is your &#FFFF00Age&#FFD700?"));
                break;

            case AGE:
                try {
                    int age = Integer.parseInt(message);
                    if (age < 1 || age > 120) {
                        player.sendMessage(MessageService.colorize("&#FF0000Please enter a valid age (1-120)."));
                        return;
                    }
                    session.setAge(age);
                    session.setCurrentStep(IDCreationSession.CreationStep.GENDER);
                    player.sendMessage(MessageService.colorize("&#FFD700What is your &#FFFF00Gender &#FFD700(Male/Female/Other)?"));
                } catch (NumberFormatException e) {
                    player.sendMessage(MessageService.colorize("&#FF0000Please enter a valid number."));
                }
                break;

            case GENDER:
                session.setGender(message);
                session.setCurrentStep(IDCreationSession.CreationStep.COUNTRY);
                player.sendMessage(MessageService.colorize("&#FFD700What is your &#FFFF00Country&#FFD700?"));
                break;

            case COUNTRY:
                session.setCountry(message);
                session.setCurrentStep(IDCreationSession.CreationStep.DISCORD_USERNAME);
                player.sendMessage(MessageService.colorize("&#FFD700What is your &#FFFF00Discord Username&#FFD700?"));
                break;

            case DISCORD_USERNAME:
                session.setDiscordUsername(message);
                session.setCurrentStep(IDCreationSession.CreationStep.FINAL_CONFIRMATION);
                player.sendMessage(MessageService.colorize(""));
                player.sendMessage(MessageService.colorize("&#FFD700========== &#FFFF00ID Summary &#FFD700=========="));
                player.sendMessage(MessageService.colorize("&#FFD700IRL Name: &#FFFF00" + session.getIrlName()));
                player.sendMessage(MessageService.colorize("&#FFD700Age: &#FFFF00" + session.getAge()));
                player.sendMessage(MessageService.colorize("&#FFD700Gender: &#FFFF00" + session.getGender()));
                player.sendMessage(MessageService.colorize("&#FFD700Country: &#FFFF00" + session.getCountry()));
                player.sendMessage(MessageService.colorize("&#FFD700Discord: &#FFFF00" + session.getDiscordUsername()));
                player.sendMessage(MessageService.colorize("&#FFD700================================"));
                player.sendMessage(MessageService.colorize("&#FFD700Are you sure you want to order this ID? Type &#FFFF00Yes &#FFD700to confirm or &#FF0000No &#FFD700to cancel."));
                break;

            case FINAL_CONFIRMATION:
                if (message.equalsIgnoreCase("yes")) {
                    createID(player, session);
                } else if (message.equalsIgnoreCase("no")) {
                    creationSessions.remove(player.getUniqueId());
                    player.sendMessage(MessageService.colorize("&#FF0000ID creation cancelled."));
                }
                break;
        }
    }

    private void createID(Player player, IDCreationSession session) {
        String idNumber = generateUniqueIDNumber();
        LocalDate registrationDate = LocalDate.now();
        LocalDate expiryDate = registrationDate.plusYears(2);

        PlayerID playerID = new PlayerID(
            player.getUniqueId(),
            idNumber,
            session.getIrlName(),
            session.getAge(),
            session.getGender(),
            session.getCountry(),
            session.getDiscordUsername(),
            registrationDate,
            expiryDate,
            false,
            0
        );

        playerIDs.put(player.getUniqueId(), playerID);
        creationSessions.remove(player.getUniqueId());
        saveIDToDatabase(playerID);

        player.sendMessage(MessageService.colorize("&#00FF00Your ID has been created successfully!"));
        player.sendMessage(MessageService.colorize("&#FFD700ID Number: &#FFFF00" + idNumber));
        player.sendMessage(MessageService.colorize("&#FFD700Type &#FFFF00/id &#FFD700to activate your ID!"));
        SoundUtil.playSuccess(player);
    }

    private String generateUniqueIDNumber() {
        String idNumber;
        Random random = new Random();
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 13; i++) {
                sb.append(random.nextInt(10));
            }
            idNumber = sb.toString();
        } while (usedIDNumbers.contains(idNumber));
        
        usedIDNumbers.add(idNumber);
        return idNumber;
    }

    public void openIDMenu(Player player) {
        PlayerID playerID = playerIDs.get(player.getUniqueId());
        
        if (playerID == null) {
            plugin.getGuiEditorManager().openGui(player, "id-creation");
        } else if (!playerID.isActivated()) {
            plugin.getGuiEditorManager().openGui(player, "id-newlyjoin");
        } else {
            // Open functional ID player GUI with real data
            new dev.shura.core.gui.IDPlayerGui(plugin, player, playerID).open();
        }
    }

    public void startActivation(Player player) {
        PlayerID playerID = playerIDs.get(player.getUniqueId());
        
        if (playerID == null) {
            player.sendMessage(MessageService.colorize("&#FF0000You don't have an ID!"));
            return;
        }

        if (playerID.isActivated()) {
            player.sendMessage(MessageService.colorize("&#FF0000Your ID is already activated!"));
            return;
        }

        if (playerID.getActivationStartTime() > 0) {
            long elapsed = System.currentTimeMillis() - playerID.getActivationStartTime();
            long remaining = 300000 - elapsed; // 5 minutes in ms
            
            if (remaining > 0) {
                int seconds = (int) (remaining / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                player.sendMessage(MessageService.colorize("&#FFFF00Please wait " + minutes + "m " + seconds + "s before your ID is activated."));
                return;
            }
        }

        playerID.setActivationStartTime(System.currentTimeMillis());
        player.sendMessage(MessageService.colorize("&#FFD700ID activation started! Please wait 5 minutes..."));
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                completeActivation(player, playerID);
            }
        }, 6000L); // 5 minutes = 6000 ticks
    }

    private void completeActivation(Player player, PlayerID playerID) {
        playerID.setActivated(true);
        updateIDInDatabase(playerID);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        showTitle(player, "&#FFD700Congratulations", "&#FFFF00Your ID is successfully activated!");
        player.sendMessage(MessageService.colorize("&#00FF00Your ID has been activated!"));
        player.sendMessage(MessageService.colorize("&#FFD700Type &#FFFF00/id &#FFD700to view your ID!"));
    }

    private void showTitle(Player player, String title, String subtitle) {
        player.showTitle(Title.title(
            MessageService.colorizeComponent(title),
            MessageService.colorizeComponent(subtitle),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
        ));
    }

    public PlayerID getPlayerID(UUID uuid) {
        return playerIDs.get(uuid);
    }

    public boolean hasActiveSession(UUID uuid) {
        return creationSessions.containsKey(uuid);
    }

    public void setIDCreationRoom(Location location) {
        this.idCreationRoom = location;
    }

    private void loadIDsFromDatabase() {
        // TODO: Load from database
    }

    private void saveIDToDatabase(PlayerID playerID) {
        // TODO: Save to database
    }

    private void updateIDInDatabase(PlayerID playerID) {
        // TODO: Update in database
    }
}
