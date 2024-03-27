package com.exwundee.partychat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;

public final class PartyChat extends JavaPlugin implements Listener {

    HashMap<String, String> currentParty = new HashMap<String, String>();
    HashMap<String, ArrayList<String>> inviteList = new HashMap<String, ArrayList<String>>();

    HashMap<String, ArrayList<String>> memberList = new HashMap<String, ArrayList<String>>();

    HashMap<String, Boolean> isPartyChatEnabled = new HashMap<String, Boolean>();
    HashMap<String, Boolean> isSpying = new HashMap<String, Boolean>();

    FileConfiguration config = getConfig();

    HashMap<String, String> recentInvite = new HashMap<>();


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
    }

    public Player getCurrentParty(Player player) {
        if (currentParty.get(player.getName()) == null) return null;
        return Bukkit.getPlayer(currentParty.get(player.getName()));
    }

    public void setCurrentParty(Player player, Player newCurrentParty) {
        if (newCurrentParty == null) {
            currentParty.put(player.getName(), null);
            return;
        }
        currentParty.put(player.getName(), newCurrentParty.getName());
    }

    public ArrayList<Player> getInviteList(Player player) {
        if (inviteList.get(player.getName()) == null) return null;
        ArrayList<Player> list = new ArrayList<>();
        for (String invitedString : inviteList.get(getCurrentParty(player).getName())){
            list.add(Bukkit.getPlayer(invitedString));
        }
        return list;
    }

    public void setInviteList(Player player, ArrayList<Player> newInviteList) {
        if (newInviteList == null) {
            inviteList.put(player.getName(), null);
            return;
        }
        ArrayList<String> list = new ArrayList<String>();
        for (Player invitedPlayer : newInviteList) {
            list.add(invitedPlayer.getName());
        }
        inviteList.put(player.getName(), list);
    }

    public ArrayList<Player> getMemberList(Player player) {
        if (memberList.get(player.getName()) == null) return null;
        ArrayList<Player> list = new ArrayList<>();
        for (String invitedString : memberList.get(getCurrentParty(player).getName())){
            list.add(Bukkit.getPlayer(invitedString));
        }
        return list;
    }

    public void setMemberList(Player player, ArrayList<Player> newMemberList) {
        if (newMemberList == null) {
            memberList.put(player.getName(), null);
            return;
        }
        ArrayList<String> list = new ArrayList<String>();
        for (Player member : newMemberList) {
            list.add(member.getName());
        }
        memberList.put(player.getName(), list);
    }

    public Player getMostRecentInvite(Player player) {
        if (recentInvite.get(player.getName()) == null) return null;
        return Bukkit.getPlayer(recentInvite.get(player.getName()));
    }

    public void setMostRecentInvite(Player player, Player newRecentPlayer) {
        if (newRecentPlayer == null) {
            recentInvite.put(player.getName(), null);
            return;
        }
        recentInvite.put(player.getName(), newRecentPlayer.getName());
    }

    public boolean getPartyChatEnabled(Player player) {
        if (isPartyChatEnabled.get(player.getName()) != null)
            return isPartyChatEnabled.get(player.getName());
        else return false;
    }

    public void setPartyChatEnabled(Player player, boolean newValue) {
        isPartyChatEnabled.put(player.getName(), newValue);
    }

    public boolean getSpying(Player player) {
        if (isSpying.get(player.getName()) != null)
            return isSpying.get(player.getName());
        else return false;
    }

    public void setSpying(Player player, boolean newValue) {
        isSpying.put(player.getName(), newValue);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (getPartyChatEnabled(event.getPlayer()) && (getCurrentParty(event.getPlayer()) != null && getPartyChatEnabled(event.getPlayer()))) {
            event.setCancelled(true);
            for (Player member : getMemberList(getCurrentParty(event.getPlayer()))) {
                member.sendMessage(getConfigMessage("party-chat-message")
                        .replace("%player%", event.getPlayer().getName())
                        .replace("%message%", event.getMessage()));
            }
            for (Player spy : Bukkit.getOnlinePlayers()) {
                if (getSpying(spy)) {
                    spy.sendMessage(getConfigMessage("admin-party-chat-message")
                            .replace("%player%", event.getPlayer().getName())
                            .replace("%message%", event.getMessage()));
                }
            }

            // Hopefully makes other plugins not pick up the message.
            event.getRecipients().clear();
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (getCurrentParty(player) != null && getCurrentParty(player).equals(player)) {
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
                if (getCurrentParty(player) != null) {
                    sender.sendMessage(getConfigMessage("already-in-party-message"));
                    return true;
                } else {
                    sender.sendMessage(getConfigMessage("party-creation-message"));
                    setCurrentParty(player, player);
                    ArrayList<Player> newMemberList = new ArrayList<>();
                    newMemberList.add(player);
                    setMemberList(player, newMemberList);
                }
            } else if (args[0].equalsIgnoreCase("list")) {
                if (getCurrentParty(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                    return true;
                }
                sender.sendMessage("");
                sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "PARTY LIST");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GOLD + "Host: " + getCurrentParty(player).getName());
                ArrayList<String> memberNames = new ArrayList<>();
                for (Player player2 : getMemberList(getCurrentParty(player))) {
                    memberNames.add(player2.getName());
                }
                memberNames.remove(getCurrentParty(player).getName());
                sender.sendMessage(ChatColor.YELLOW + "Member: " + ChatColor.WHITE + memberNames.toString().replaceAll("\\[", "").replace("]", ""));
            } else if (args[0].equalsIgnoreCase("leave")) {
                if (getCurrentParty(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                    return true;
                } else {
                    if (getCurrentParty(player) == player) {
                        disbandParty(player, false);
                    } else {
                        ArrayList<Player> newMemberList = getMemberList(getCurrentParty(player));
                        for (Player player2 : getMemberList(getCurrentParty(player))) {
                            player2.sendMessage(getConfigMessage("party-leave-message")
                                    .replace("%player%", player.getName()));
                        }
                        newMemberList.remove(player);
                        setMemberList(getCurrentParty(player), newMemberList);
                        setCurrentParty(player, null);
                    }
                }
            } else if (args[0].equalsIgnoreCase("disband")) {
                if (getCurrentParty(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                } else if (!getCurrentParty(player).equals(player)) {
                    sender.sendMessage(getConfigMessage("not-party-leader-message"));
                } else {
                    disbandParty(player, false);
                }
            } else if (args[0].equalsIgnoreCase("transfer")) {
                if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("transfer-invalid-arguments-message"));
                } else if (getCurrentParty(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                } else if (!getCurrentParty(player).equals(player)) {
                    sender.sendMessage(getConfigMessage("not-party-leader-message"));
                } else if (!getMemberList(player).contains(Bukkit.getOfflinePlayer(args[1]))) {
                    sender.sendMessage(getConfigMessage("player-not-in-party-message"));
                } else if (args[1].equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(getConfigMessage("transfer-self-message"));
                } else {
                    Player newOwner = Bukkit.getPlayer(args[1]);
                    setMemberList(newOwner, getMemberList(player));
                    for (Player newPlayer : getMemberList(player)) {
                        setCurrentParty(newPlayer, newOwner);
                        newPlayer.sendMessage(getConfigMessage("ownership-transferred-message")
                                .replace("%player%", newOwner.getName()));
                    }
                    setMemberList(player, null);
                    setInviteList(player, null);
                }
            } else if (args[0].equalsIgnoreCase("invite")) {
                if (getCurrentParty(player) != player) {
                    sender.sendMessage(getConfigMessage("not-party-leader-message"));
                    return true;
                } else if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("invite-invalid-arguments-message"));
                    return true;
                } else if (getCurrentParty(Bukkit.getPlayer(args[1])) != null) {
                    sender.sendMessage(getConfigMessage("player-already-in-party-message"));
                    return true;
                } else if (getInviteList(player) != null && getInviteList(player).contains(Bukkit.getPlayer(args[1]))) {
                    sender.sendMessage(getConfigMessage("already-invited-message"));
                    return true;
                } else if (args[1].equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(getConfigMessage("invite-self-message"));
                    return true;
                } else if (!Bukkit.getServer().getOfflinePlayer(args[1]).isOnline()) {
                    sender.sendMessage(getConfigMessage("recipient-not-online-message"));
                } else {
                    sender.sendMessage(getConfigMessage("sender-invite-message")
                            .replace("%player%", Bukkit.getOfflinePlayer(args[1]).getName()));
                    ArrayList<Player> newInviteList = new ArrayList<>();
                    if (getInviteList(player) != null) {
                        newInviteList = getInviteList(player);
                    }
                    newInviteList.add(Bukkit.getPlayer(args[1]));
                    setInviteList(player, newInviteList);
                    Bukkit.getPlayer(args[1]).sendMessage(getConfigMessage("recipient-invite-message")
                            .replace("%player%", sender.getName()));
                    Player finalPlayer = player;
                    setMostRecentInvite(Bukkit.getPlayer(args[1]), player);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (getInviteList(finalPlayer).contains(Bukkit.getPlayer(args[1]))) {
                            sender.sendMessage(getConfigMessage("sender-invite-expired-message")
                                    .replace("%player%", Bukkit.getOfflinePlayer(args[1]).getName()));
                            Bukkit.getPlayer(args[1]).sendMessage(getConfigMessage("recipient-invite-expired-message")
                                    .replace("%player%", sender.getName()));
                            ArrayList<Player> newerInviteList = new ArrayList<>();
                            newerInviteList.remove(Bukkit.getPlayer(args[1]));
                            setInviteList(finalPlayer, newerInviteList);
                        }
                    }, 20 * 120);
                }
            } else if (args[0].equalsIgnoreCase("kick")) {
                if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("kick-invalid-arguments-message"));
                } else if (getCurrentParty(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                } else if (!getCurrentParty(player).getName().equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(getConfigMessage("not-party-leader-message"));
                } else if (!getMemberList(getCurrentParty(player)).contains(Bukkit.getPlayer(args[1]))) {
                    sender.sendMessage(getConfigMessage("player-not-in-party-message"));
                } else if (player.getName().equalsIgnoreCase(args[1])) {
                    sender.sendMessage(getConfigMessage("kick-self-message"));
                } else {
                    Player kickedPlayer = Bukkit.getPlayer(args[1]);
                    ArrayList<Player> newMemberList = getMemberList(getCurrentParty(kickedPlayer));
                    sender.sendMessage(getConfigMessage("sender-kick-message")
                            .replace("%player%", kickedPlayer.getName()));
                    for (Player player2 : getMemberList(getCurrentParty(kickedPlayer))) {
                        player2.sendMessage(getConfigMessage("party-leave-message")
                                .replace("%player%", kickedPlayer.getName()));
                    }
                    newMemberList.remove(kickedPlayer);
                    setMemberList(getCurrentParty(kickedPlayer), newMemberList);
                    setCurrentParty(kickedPlayer, null);
                    kickedPlayer.sendMessage(getConfigMessage("recipient-kick-message")
                            .replace("%player%", player.getName()));
                }
            } else if (args[0].equalsIgnoreCase("deny")) {
                if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("deny-invalid-arguments-message"));
                } else if (getInviteList(Bukkit.getPlayer(args[1])) == null || !(getInviteList(Bukkit.getPlayer(args[1])).contains(player))) {
                    sender.sendMessage(getConfigMessage("not-invited-message"));
                } else {
                    getInviteList(Bukkit.getPlayer(args[1])).remove(player);
                    sender.sendMessage(getConfigMessage("deny-success-message")
                            .replace("%player%", Bukkit.getPlayer(args[1]).getName()));
                }
            } else if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("accept")) {
                if (args.length == 1) {
                    if (getMostRecentInvite(player) != null && getInviteList(getMostRecentInvite(player)).contains(player)) {
                        ArrayList<Player> newInviteList = getInviteList(getMostRecentInvite(player));
                        newInviteList.remove(player);
                        ArrayList<Player> newMemberList = getMemberList(getMostRecentInvite(player));
                        newMemberList.add(player);
                        setInviteList(getMostRecentInvite(player), newInviteList);
                        setMemberList(getMostRecentInvite(player), newMemberList);
                        setCurrentParty(player, getMostRecentInvite(player));
                        for (Player player2 : getMemberList(getCurrentParty(player))) {
                            player2.sendMessage(getConfigMessage("party-join-message")
                                    .replace("%player%", player.getName()));
                        }
                    } else {
                        sender.sendMessage(getConfigMessage("join-invalid-arguments-message"));
                    }
                    return true;
                } else if (args.length != 2) {
                     sender.sendMessage(getConfigMessage("join-invalid-arguments-message"));
                } else if (getInviteList(Bukkit.getPlayer(args[1])) == null || !(getInviteList(Bukkit.getPlayer(args[1])).contains(player))) {
                    sender.sendMessage(getConfigMessage("not-invited-message"));
                } else if (getCurrentParty(player) != null) {
                    sender.sendMessage(getConfigMessage("already-in-party-message"));
                } else {
                    ArrayList<Player> newInviteList = getInviteList(Bukkit.getPlayer(args[1]));
                    newInviteList.remove(player);
                    ArrayList<Player> newMemberList = getMemberList(Bukkit.getPlayer(args[1]));
                    newMemberList.add(player);
                    setInviteList(Bukkit.getPlayer(args[1]), newInviteList);
                    setMemberList(Bukkit.getPlayer(args[1]), newMemberList);
                    setCurrentParty(player, Bukkit.getPlayer(args[1]));
                    for (Player player2 : getMemberList(getCurrentParty(player))) {
                        player2.sendMessage(getConfigMessage("party-join-message")
                                .replace("%player%", player.getName()));
                    }
                }
            } else if (args[0].equalsIgnoreCase("toggle")) {
                if (getCurrentParty(player) == null) {
                    sender.sendMessage(getConfigMessage("not-in-party-message"));
                    return true;
                }
                if (!getPartyChatEnabled(player)) {
                    setPartyChatEnabled(player, true);
                    sender.sendMessage(getConfigMessage("party-chat-enabled-message"));
                } else {
                    setPartyChatEnabled(player, false);
                    sender.sendMessage(getConfigMessage("party-chat-disabled-message"));
                }
            } else {
                if (getCurrentParty(player) != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < args.length; i++) {
                        stringBuilder.append(args[i] + " ");
                    }
                    for (Player member : getMemberList(getCurrentParty(player))) {
                        member.sendMessage(getConfigMessage("party-chat-message")
                                .replace("%player%", player.getName())
                                .replace("%message%", stringBuilder.toString()));
                    }
                    for (Player spy : Bukkit.getOnlinePlayers()) {
                        if (getSpying(spy)) {
                            spy.sendMessage(getConfigMessage("admin-party-chat-message")
                                    .replace("%player%", player.getName())
                                    .replace("%message%", stringBuilder.toString()));
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
                if (!getSpying(player)) {
                    setSpying(player, true);
                    sender.sendMessage(getConfigMessage("spy-enabled-message"));
                } else {
                    setSpying(player, false);
                    sender.sendMessage(getConfigMessage("spy-disabled-message"));
                }
            } else if (args[0].equalsIgnoreCase("show")) {
                if (args.length != 2) {
                    sender.sendMessage(getConfigMessage("show-invalid-arguments-message"));
                } else {
                    if (Bukkit.getPlayer(args[1]) == null) {
                        sender.sendMessage(getConfigMessage("recipient-not-online-message"));
                        return true;
                    }
                    Player checkedPlayer = Bukkit.getPlayer(args[1]);
                    if (!checkedPlayer.isOnline()) {
                        sender.sendMessage(getConfigMessage("recipient-not-online-message"));
                    } else if (getCurrentParty(checkedPlayer) == null) {
                        sender.sendMessage(getConfigMessage("recipient-not-in-party-message"));
                    } else {
                        sender.sendMessage(getConfigMessage("show-message")
                                .replace("%player%", checkedPlayer.getName())
                                .replace("%group%", getCurrentParty(checkedPlayer).getName()));
                    }
                }
            }
        }
        return true;
    }

    public void disbandParty(Player player, boolean hideMessages) {
        for (Player player2 : getMemberList(player)) {
            setCurrentParty(player2, null);
            if (!hideMessages) {
                if (player2 != player) {
                    player2.sendMessage(getConfigMessage("party-removed-message"));
                } else {
                    player2.sendMessage(getConfigMessage("party-disband-message"));
                }
            }
        }
        if (getInviteList(player) != null) {
            setInviteList(player, null);
        }
        setMemberList(player, null);
    }

}
