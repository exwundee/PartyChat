package com.exwundee.partychat;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;

public final class PartyChat extends JavaPlugin implements Listener {

    HashMap<Player, Player> currentParty = new HashMap<Player, Player>();
    HashMap<Player, ArrayList<Player>> inviteList = new HashMap<Player, ArrayList<Player>>();

    HashMap<Player, ArrayList<Player>> memberList = new HashMap<Player, ArrayList<Player>>();

    HashMap<Player, Boolean> isPartyChatEnabled = new HashMap<Player, Boolean>();
    HashMap<Player, Boolean> isSpying = new HashMap<Player, Boolean>();

    FileConfiguration config = getConfig();


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (isPartyChatEnabled.get(event.getPlayer()) != null && (currentParty.get(event.getPlayer()) != null && isPartyChatEnabled.get(event.getPlayer()))) {
            event.setCancelled(true);
            for (Player member : memberList.get(currentParty.get(event.getPlayer()))) {
                member.sendMessage(getConfigMessage("party-chat-message")
                        .replaceAll("%player%", event.getPlayer().getName())
                        .replaceAll("%message%", event.signedMessage().message()));
            }
            for (Player spy : Bukkit.getOnlinePlayers()) {
                if (isSpying.get(spy) != null && isSpying.get(spy)) {
                    spy.sendMessage(getConfigMessage("admin-party-chat-message")
                            .replaceAll("%player%", event.getPlayer().getName())
                            .replaceAll("%message%", event.signedMessage().message()));
                }
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (currentParty.get(player) != null && currentParty.get(player).equals(player)) {
            disbandParty(player, false);
        }
    }

    public String getConfigMessage(String value) {
        return ChatColor.translateAlternateColorCodes('&', config.getString(value));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("party")) {
            Player player = null;
            if (!(sender instanceof Player)) {
                sender.sendMessage(getConfigMessage("console-message"));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "PARTY HELP MENU");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party create" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Creates a party");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party invite <player>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Invites player to party");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party accept <player>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Accepts and joins party request");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party deny <player>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Denies party request");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party leave" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Leaves current party");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party list" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Shows current party info");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party kick <player>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Kicks player from party");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party disband" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Disbands the party");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party transfer <player>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Transfers ownership of the party");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party toggle" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Toggle party chat");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party <message>" + ChatColor.GRAY + " - " + ChatColor.YELLOW + "Send a chat to party");
                return true;
            }
            player = (Player) sender;
            if (args[0].equalsIgnoreCase("create")) {
                if (currentParty.get(player) != null) {
                    sender.sendMessage(getConfigMessage("already-in-party-message"));
                    return true;
                } else {
                    sender.sendMessage(getConfigMessage("party-creation-message"));
                    currentParty.put(player, player);
                    ArrayList<Player> newMemberList = new ArrayList<>();
                    newMemberList.add(player);
                    memberList.put(player, newMemberList);
                }
            } else if (args[0].equalsIgnoreCase("list")) {
                if (currentParty.get(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                    return true;
                }
                sender.sendMessage("");
                sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "PARTY LIST");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GOLD + "Host: " + currentParty.get(player).getName());
                ArrayList<String> memberNames = new ArrayList<>();
                for (Player player2 : memberList.get(currentParty.get(player))) {
                    memberNames.add(player2.getName());
                }
                memberNames.remove(currentParty.get(player).getName());
                sender.sendMessage(ChatColor.YELLOW + "Member: " + ChatColor.WHITE + memberNames.toString().replaceAll("\\[", "").replaceAll("]", ""));
            } else if (args[0].equalsIgnoreCase("leave")) {
                if (currentParty.get(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                    return true;
                } else {
                    if (currentParty.get(player) == player) {
                        disbandParty(player, false);
                    } else {
                        ArrayList<Player> newMemberList = memberList.get(currentParty.get(player));
                        for (Player player2 : memberList.get(currentParty.get(player))) {
                            player2.sendMessage(getConfigMessage("party-leave-message")
                                    .replaceAll("%player%", player.getName()));
                        }
                        newMemberList.remove(player);
                        memberList.put(currentParty.get(player), newMemberList);
                        currentParty.put(player, null);
                    }
                }
            } else if (args[0].equalsIgnoreCase("disband")) {
                if (currentParty.get(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                } else if (!currentParty.get(player).equals(player)) {
                    sender.sendMessage(getConfigMessage("not-party-leader-message"));
                } else {
                    disbandParty(player, false);
                }
            } else if (args[0].equalsIgnoreCase("transfer")) {
                if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("transfer-invalid-arguments-message"));
                } else if (currentParty.get(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                } else if (!currentParty.get(player).equals(player)) {
                    sender.sendMessage(getConfigMessage("not-party-leader-message"));
                } else if (!memberList.get(player).contains(Bukkit.getOfflinePlayer(args[1]))) {
                    sender.sendMessage(getConfigMessage("player-not-in-party-message"));
                } else if (args[1].equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(getConfigMessage("transer-self-message"));
                } else {
                    Player newOwner = Bukkit.getPlayer(args[1]);
                    memberList.put(newOwner, memberList.get(player));
                    for (Player newPlayer : memberList.get(player)) {
                        currentParty.put(newPlayer, newOwner);
                        newPlayer.sendMessage(getConfigMessage("ownership-transferred-message")
                                .replaceAll("%player%", newOwner.getName()));
                    }
                    memberList.put(player, null);
                    inviteList.put(player, null);
                }
            } else if (args[0].equalsIgnoreCase("invite")) {
                if (currentParty.get(player) != player) {
                    sender.sendMessage(getConfigMessage("not-party-leader-message"));
                    return true;
                } else if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("invite-invalid-arguments-message"));
                    return true;
                } else if (currentParty.get(Bukkit.getPlayer(args[1])) != null) {
                    sender.sendMessage(getConfigMessage("player-already-in-party-message"));
                    return true;
                } else if (inviteList.get(player) != null && inviteList.get(player).contains(Bukkit.getPlayer(args[1]))) {
                    sender.sendMessage(getConfigMessage("already-invited-message"));
                    return true;
                } else if (args[1].equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(getConfigMessage("invite-self-message"));
                    return true;
                } else if (!Bukkit.getServer().getOfflinePlayer(args[1]).isOnline()) {
                    sender.sendMessage(getConfigMessage("recipient-not-online-message"));
                } else {
                    sender.sendMessage(getConfigMessage("sender-invite-message")
                            .replaceAll("%player%", Bukkit.getOfflinePlayer(args[1]).getName()));
                    ArrayList<Player> newInviteList = new ArrayList<>();
                    newInviteList.add(Bukkit.getPlayer(args[1]));
                    inviteList.put(player, newInviteList);
                    Bukkit.getPlayer(args[1]).sendMessage(getConfigMessage("recipient-invite-message")
                            .replaceAll("%player%", sender.getName()));
                    Player finalPlayer = player;
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (inviteList.get(finalPlayer).contains(Bukkit.getPlayer(args[1]))) {
                            // args[1
                            sender.sendMessage(getConfigMessage("sender-invite-expired-message")
                                    .replaceAll("%player%", Bukkit.getOfflinePlayer(args[1]).getName()));
                            Bukkit.getPlayer(args[1]).sendMessage(getConfigMessage("recipient-invite-expired-message")
                                    .replaceAll("%player%", sender.getName()));
                            ArrayList<Player> newerInviteList = new ArrayList<>();
                            newerInviteList.remove(Bukkit.getPlayer(args[1]));
                            inviteList.put(finalPlayer, newerInviteList);
                        }
                    }, 20 * 120);
                }
            } else if (args[0].equalsIgnoreCase("kick")) {
                // TODO Prevent player from kicking themselves.
                if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("kick-invalid-arguments-message"));
                } else if (currentParty.get(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                } else if (!currentParty.get(player).getName().equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(getConfigMessage("not-party-leader-message"));
                } else if (!memberList.get(currentParty.get(player)).contains(Bukkit.getPlayer(args[1]))) {
                    sender.sendMessage(getConfigMessage("player-not-in-party-message"));
                } else if (player.getName().equalsIgnoreCase(args[1])) {
                    sender.sendMessage(getConfigMessage("kick-self-message"));
                } else {
                    Player kickedPlayer = Bukkit.getPlayer(args[1]);
                    ArrayList<Player> newMemberList = memberList.get(currentParty.get(kickedPlayer));
                    sender.sendMessage(getConfigMessage("sender-kick-message")
                            .replaceAll("%player%", kickedPlayer.getName()));
                    for (Player player2 : memberList.get(currentParty.get(kickedPlayer))) {
                        player2.sendMessage(getConfigMessage("party-leave-message")
                                .replaceAll("%player%", kickedPlayer.getName()));
                    }
                    newMemberList.remove(kickedPlayer);
                    memberList.put(currentParty.get(kickedPlayer), newMemberList);
                    currentParty.put(kickedPlayer, null);
                    kickedPlayer.sendMessage(getConfigMessage("recipient-kick-message")
                            .replaceAll("%player%", player.getName()));
                }
            } else if (args[0].equalsIgnoreCase("deny")) {
                if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("deny-invalid-arguments-message"));
                } else if (inviteList.get(Bukkit.getPlayer(args[1])) == null || !(inviteList.get(Bukkit.getPlayer(args[1])).contains(player))) {
                    sender.sendMessage(getConfigMessage("not-invited-message"));
                } else {
                    inviteList.get(Bukkit.getPlayer(args[1])).remove(player);
                    sender.sendMessage(getConfigMessage("deny-success-message")
                            .replaceAll("%player%", Bukkit.getPlayer(args[1]).getName()));
                }
            } else if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("accept")) {
                if (args.length != 2) {
                     sender.sendMessage(getConfigMessage("join-invalid-arguments-message"));
                } else if (inviteList.get(Bukkit.getPlayer(args[1])) == null || !(inviteList.get(Bukkit.getPlayer(args[1])).contains(player))) {
                    sender.sendMessage(getConfigMessage("not-invited-message"));
                } else if (currentParty.get(player) != null) {
                    sender.sendMessage(getConfigMessage("already-in-party-message"));
                } else {
                    inviteList.get(Bukkit.getPlayer(args[1])).remove(player);
                    memberList.get(Bukkit.getPlayer(args[1])).add(player);
                    currentParty.put(player, Bukkit.getPlayer(args[1]));
                    for (Player player2 : memberList.get(currentParty.get(player))) {
                        player2.sendMessage(getConfigMessage("party-join-message")
                                .replaceAll("%player%", player.getName()));
                    }
                }
            } else if (args[0].equalsIgnoreCase("toggle")) {
                if (currentParty.get(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                    return true;
                }
                if (isPartyChatEnabled.get(player) == null || !isPartyChatEnabled.get(player)) {
                    isPartyChatEnabled.put(player, true);
                    sender.sendMessage(getConfigMessage("party-chat-enabled-message"));
                } else {
                    isPartyChatEnabled.put(player, false);
                    sender.sendMessage(getConfigMessage("party-chat-disabled-message"));
                }
            } else {
                if (currentParty.get(player) != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < args.length; i++) {
                        stringBuilder.append(args[i] + " ");
                    }
                    for (Player member : memberList.get(currentParty.get(player))) {
                        member.sendMessage(getConfigMessage("party-chat-message")
                                .replaceAll("%player%", player.getName())
                                .replaceAll("%message%", stringBuilder.toString()));
                    }
                    for (Player spy : Bukkit.getOnlinePlayers()) {
                        if (isSpying.get(spy) != null && isSpying.get(spy)) {
                            spy.sendMessage(getConfigMessage("admin-party-chat-message")
                                    .replaceAll("%player%", player.getName())
                                    .replaceAll("%message%", stringBuilder.toString()));
                        }
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("adminparty")) {
            Player player = (Player) sender;
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "/adminparty <spy/show> [<player>]");
                return true;
            }
            if (args[0].equalsIgnoreCase("spy")) {
                if (isSpying.get(player) == null || !isSpying.get(player)) {
                    isSpying.put(player, true);
                    sender.sendMessage(getConfigMessage("spy-enabled-message"));
                } else {
                    isSpying.put(player, false);
                    sender.sendMessage(getConfigMessage("spy-disabled-message"));
                }
            } else if (args[0].equalsIgnoreCase("show")) {
                if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("show-invalid-arguments-message"));
                } else {
                    OfflinePlayer checkedPlayer = Bukkit.getOfflinePlayer(args[1]);
                    if (!checkedPlayer.isOnline()) {
                        sender.sendMessage(getConfigMessage("recipient-not-online-message"));
                    } else if (currentParty.get(checkedPlayer) == null) {
                        sender.sendMessage(getConfigMessage("recipient-not-in-party-message"));
                    } else {
                        sender.sendMessage(getConfigMessage("show-message")
                                .replaceAll("%player%", checkedPlayer.getName())
                                .replaceAll("%group%", currentParty.get(checkedPlayer).getName()));
                    }
                }
            }
        }
        return true;
    }

    public void disbandParty(Player player, boolean hideMessages) {
        for (Player player2 : memberList.get(player)) {
            currentParty.put(player2, null);
            if (!hideMessages) {
                if (player2 != player) {
                    player2.sendMessage(getConfigMessage("party-removed-message"));
                } else {
                    player2.sendMessage(getConfigMessage("party-disband-message"));
                }
            }
        }
        if (inviteList.get(player) != null) {
            inviteList.get(player).clear();
        }
        memberList.get(player).clear();
    }

}
