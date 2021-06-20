/*
 * Copyright 2020 NAFU_at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package page.nafuchoco.teammessageboard;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import page.nafuchoco.soloservercore.SoloServerApi;
import page.nafuchoco.soloservercore.data.PlayersTeam;
import page.nafuchoco.soloservercore.database.DatabaseConnector;
import page.nafuchoco.teammessageboard.database.MessagesTable;
import page.nafuchoco.teammessageboard.database.TeamMessage;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public final class TeamMessageBoard extends JavaPlugin implements Listener {
    private static TeamMessageBoard instance;
    private static TeamMessageBoardConfig config;

    private DatabaseConnector connector;
    private MessagesTable messagesTable;

    private SoloServerApi soloServerApi;

    public static TeamMessageBoard getInstance() {
        if (instance == null)
            instance = (TeamMessageBoard) Bukkit.getPluginManager().getPlugin("TeamMessageBoard");
        return instance;
    }

    public static TeamMessageBoardConfig getPluginConfig() {
        if (config == null)
            config = new TeamMessageBoardConfig();
        return config;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        getPluginConfig().reloadConfig();

        connector = new DatabaseConnector(getPluginConfig().getInitConfig().getDatabaseType(),
                getPluginConfig().getInitConfig().getAddress() + ":" + getPluginConfig().getInitConfig().getPort(),
                getPluginConfig().getInitConfig().getDatabase(),
                getPluginConfig().getInitConfig().getUsername(),
                getPluginConfig().getInitConfig().getPassword());
        messagesTable = new MessagesTable(connector);
        try {
            messagesTable.createTable();
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "An error occurred while initializing the database table.", e);
        }

        soloServerApi = SoloServerApi.getInstance();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (connector != null)
            connector.close();
    }

    private final Map<Player, TeamMessage.TeamMessageBuilder> makingMessage = new HashMap<>();

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        PlayersTeam joinedTeam = soloServerApi.getPlayersTeam(event.getPlayer());
        if (joinedTeam != null) {
            try {
                List<TeamMessage> messages = messagesTable.getNewMessage(joinedTeam, new Date(event.getPlayer().getLastPlayed()));
                if (!messages.isEmpty())
                    sendMessageList(event.getPlayer(), messages);
            } catch (SQLException e) {
                getLogger().log(Level.WARNING, "An error occurred while fetching the message.", e);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length == 0) {

            } else if (!sender.hasPermission("messageboard." + args[0])) {
                sender.sendMessage(ChatColor.RED + "You can't run this command because you don't have permission.");
            } else switch (args[0]) {
                case "create": {
                    PlayersTeam joinedTeam = soloServerApi.getPlayersTeam(player);
                    if (joinedTeam != null) {
                        TeamMessage.TeamMessageBuilder builder = new TeamMessage.TeamMessageBuilder(player.getUniqueId());
                        builder.setTargetTeam(joinedTeam);
                        makingMessage.put(player, builder);
                        player.sendMessage(ChatColor.GREEN + "[TMB] メッセージの作成を開始しました。");
                        sendMessageEditor(player);
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "[TMB] メッセージを作成するにはチームに所属している必要があります。");
                    }
                }
                break;

                case "subject":
                    if (args.length >= 2) {
                        TeamMessage.TeamMessageBuilder builder = makingMessage.get(player);
                        if (builder != null) {
                            builder.setSubject(args[1]);
                            sendMessageEditor(player);
                        } else {
                            player.sendMessage(ChatColor.YELLOW + "[TMB] 先にメッセージの作成を開始して下さい！");
                        }
                    }
                    break;

                case "message":
                    if (args.length >= 3) {
                        switch (args[1]) {
                            case "add": {
                                TeamMessage.TeamMessageBuilder builder = makingMessage.get(player);
                                if (builder != null) {
                                    builder.addMessageLine(args[2]);
                                    sendMessageEditor(player);
                                } else {
                                    player.sendMessage(ChatColor.YELLOW + "[TMB] 先にメッセージの作成を開始して下さい！");
                                }
                            }
                            break;

                            case "remove": {
                                TeamMessage.TeamMessageBuilder builder = makingMessage.get(player);
                                if (builder != null) {
                                    try {
                                        builder.removeMessageLine(Integer.parseInt(args[2]));
                                        sendMessageEditor(player);
                                    } catch (NumberFormatException e) {
                                        player.sendMessage(ChatColor.RED + "[TMB] 行数を数字で指定して下さい！");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.YELLOW + "[TMB] 先にメッセージの作成を開始して下さい！");
                                }
                            }
                            break;
                        }
                    } else if (args[1].equals("add")) {
                        TeamMessage.TeamMessageBuilder builder = makingMessage.get(player);
                        if (builder != null) {
                            builder.addMessageLine("");
                            sendMessageEditor(player);
                        } else {
                            player.sendMessage(ChatColor.YELLOW + "[TMB] 先にメッセージの作成を開始して下さい！");
                        }
                    }
                    break;

                case "send":
                    TeamMessage.TeamMessageBuilder builder = makingMessage.get(player);
                    if (builder != null) {
                        TeamMessage teamMessage = builder.build();
                        try {
                            messagesTable.registerMessage(teamMessage);
                            makingMessage.remove(player);
                            player.sendMessage(ChatColor.GREEN + "[TMB] メッセージを送信しました！");
                        } catch (SQLException e) {
                            player.sendMessage(ChatColor.RED + "[TMB] メッセージの送信に失敗しました。後でもう一度お試し下さい。");
                        }
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "[TMB] 先にメッセージの作成を開始して下さい！");
                    }
                    break;

                case "check": {
                    PlayersTeam joinedTeam = soloServerApi.getPlayersTeam(player);
                    if (joinedTeam != null) {
                        try {
                            List<TeamMessage> messages = messagesTable.getAllMessage(joinedTeam);
                            sendMessageList(player, messages);
                        } catch (SQLException e) {
                            sender.sendMessage(ChatColor.RED + "[TMB] メッセージの取得に失敗しました。");
                            getLogger().log(Level.WARNING, "An error occurred while fetching the message.", e);
                        }
                    }
                }
                break;

                case "read":
                    try {
                        TeamMessage message = messagesTable.getMessage(UUID.fromString(args[1]));
                        if (message != null)
                            sendMessageViewer(player, message);
                        else
                            sender.sendMessage(ChatColor.YELLOW + "[TMB] 該当するメッセージが見つかりませんでした。");
                    } catch (SQLException e) {
                        sender.sendMessage(ChatColor.RED + "[TMB] メッセージの取得に失敗しました。");
                        getLogger().log(Level.WARNING, "An error occurred while fetching the message.", e);
                    }
                    break;

                case "delete":
                    try {
                        TeamMessage message = messagesTable.getMessage(UUID.fromString(args[1]));
                        if (message != null) {
                            if (message.getSenderPlayer().equals(player.getUniqueId())) {
                                messagesTable.deleteMessage(UUID.fromString(args[1]));
                                sender.sendMessage(ChatColor.GREEN + "[TMB] メッセージを削除しました。");
                            } else {
                                sender.sendMessage(ChatColor.RED + "[TMB] メッセージは作者のみ削除することができます。");
                            }
                        }
                    } catch (SQLException e) {
                        sender.sendMessage(ChatColor.RED + "[TMB] メッセージの削除に失敗しました。");
                        getLogger().log(Level.WARNING, "An error occurred while deleting the message.", e);
                    }
                    break;

            }
        } else {
            sender.sendMessage(ChatColor.RED + "This command must be executed in-game.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "subject", "message", "send", "check", "read", "delete");
        } else if (args.length >= 2 && (args[0].equals("check") || args[0].equals("delete"))) {
            List ids = new LinkedList();
            if (sender instanceof Player) {
                Player player = (Player) sender;
                PlayersTeam joinedTeam = soloServerApi.getPlayersTeam(player);
                if (joinedTeam != null) {
                    try {
                        List<TeamMessage> messages = messagesTable.getAllMessage(joinedTeam);
                        messages.forEach(message -> ids.add(message.getId()));
                        return ids;
                    } catch (SQLException e) {
                        getLogger().log(Level.WARNING, "An error occurred while fetching the messageIds.", e);
                    }
                }
            }
        }
        return null;
    }

    public void sendMessageEditor(Player player) {
        TeamMessage.TeamMessageBuilder builder = makingMessage.get(player);
        player.sendMessage(ChatColor.AQUA + "====== Message Edit ======");
        player.sendMessage("from: " + player.getDisplayName() + ", To: Your Team.");
        player.sendMessage("------");
        player.sendMessage("Subject: " + builder.getSubject());
        player.sendMessage("------");
        player.sendMessage("Message:");
        builder.getMessage().forEach(message -> player.sendMessage(message));
    }

    public void sendMessageViewer(Player player, TeamMessage message) {
        player.sendMessage(ChatColor.AQUA + "====== " + message.getId() + " ======");
        player.sendMessage("from: " + Bukkit.getOfflinePlayer(message.getSenderPlayer()).getName() + ", To: Your Team.");
        player.sendMessage("------");
        player.sendMessage("Subject: " + message.getSubject());
        player.sendMessage("------");
        player.sendMessage("Message:");
        message.getMessage().forEach(text -> player.sendMessage(text));
    }

    public void sendMessageList(Player player, List<TeamMessage> messages) {
        PlayersTeam joinedTeam = soloServerApi.getPlayersTeam(player);
        if (joinedTeam != null) {
            player.sendMessage(ChatColor.AQUA + "====== Team Message! ======");
            messages.forEach(message -> {
                TextComponent component = new TextComponent();
                component.setText("[" + message.getId().toString().split("-")[0] + "] ");
                component.setBold(true);
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/messageboard read " + message.getId()));
                component.addExtra(message.getSubject());
                player.spigot().sendMessage(component);
            });
        }
    }
}
