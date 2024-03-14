package com.exwundee.partychat;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.ChatEvent;
import it.unimi.dsi.fastutil.Hash;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class PartyChat extends JavaPlugin implements Listener {

    HashMap<Player, Player> currentParty = new HashMap<Player, Player>();
    HashMap<Player, ArrayList<Player>> inviteList = new HashMap<Player, ArrayList<Player>>();

    HashMap<Player, ArrayList<Player>> memberList = new HashMap<Player, ArrayList<Player>>();

    HashMap<Player, Boolean> isPartyChatEnabled = new HashMap<Player, Boolean>();
    HashMap<Player, Boolean> isSpying = new HashMap<Player, Boolean>();


    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (isPartyChatEnabled.get(event.getPlayer()) != null && (currentParty.get(event.getPlayer()) != null && isPartyChatEnabled.get(event.getPlayer()))) {
            event.setCancelled(true);
            for (Player member : memberList.get(currentParty.get(event.getPlayer()))) {
                member.sendMessage(ChatColor.LIGHT_PURPLE + "[P] " + ChatColor.WHITE + event.getPlayer().getName() + ": " + event.signedMessage().message());
            }
            for (Player spy : Bukkit.getOnlinePlayers()) {
                if (isSpying.get(spy) != null && isSpying.get(spy)) {
                    spy.sendMessage(ChatColor.RED + "[P] " + ChatColor.WHITE + event.getPlayer().getName() + ": " + event.signedMessage().message());
                }
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (currentParty.get(player).equals(player)) {
            disbandParty(player, false);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("party")) {
            Player player = null;
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You are not real. Peel your skin off, hurry!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party create " + ChatColor.WHITE + "- Creates a party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party invite " + ChatColor.WHITE + "- Invites player to party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party leave " + ChatColor.WHITE + "- Leaves current party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party list " + ChatColor.WHITE + "- Shows current party info.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party kick " + ChatColor.WHITE + "- Kicks player from party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party disband " + ChatColor.WHITE + "- Disbands the party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party transfer " + ChatColor.WHITE + "- Transfers ownership of the party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party toggle " + ChatColor.WHITE + "- Toggle party chat.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party <message> " + ChatColor.WHITE + "- Send a chat to party.");
                return true;
            }
            player = (Player) sender;
            if (args[0].equalsIgnoreCase("create")) {
                if (currentParty.get(player) != null) {
                    sender.sendMessage(ChatColor.RED + "You are already in a party.");
                    return true;
                } else {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "You created a party!");
                    currentParty.put(player, player);
                    ArrayList<Player> newMemberList = new ArrayList<>();
                    newMemberList.add(player);
                    memberList.put(player, newMemberList);
                }
            } else if (args[0].equalsIgnoreCase("list")) {
                if (currentParty.get(player) == null) {
                    sender.sendMessage(ChatColor.RED + "You are not in a party.");
                    return true;
                }
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "You are currently in " + currentParty.get(player).getName() + "'s party.");
                ArrayList<String> memberNames = new ArrayList<>();
                for (Player player2 : memberList.get(currentParty.get(player))) {
                    memberNames.add(player2.getName());
                }
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Member List: " + memberNames.toString().replaceAll("\\[", "").replaceAll("]", ""));
            } else if (args[0].equalsIgnoreCase("leave")) {
                if (currentParty.get(player) == null) {
                    sender.sendMessage(ChatColor.RED + "You are not in a party to leave, dumbass.");
                    return true;
                } else {
                    if (currentParty.get(player) == player) {
                        disbandParty(player, false);
                    } else {
                        ArrayList<Player> newMemberList = memberList.get(currentParty.get(player));
                        for (Player player2 : memberList.get(currentParty.get(player))) {
                            player2.sendMessage(ChatColor.RED + player.getName() + " has left the party.");
                        }
                        newMemberList.remove(player);
                        memberList.put(currentParty.get(player), newMemberList);
                        currentParty.put(player, null);
                    }
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have left your party.");
                }
            } else if (args[0].equalsIgnoreCase("disband")) {
                if (currentParty.get(player) == null) {
                    sender.sendMessage(ChatColor.RED + "Ok, so how are you going to disband the party if you aren't even in one? You gotta be fucking stupid.");
                } else if (!currentParty.get(player).equals(player)) {
                    sender.sendMessage(ChatColor.RED + "You can't take down someone else's fort.");
                } else {
                    disbandParty(player, false);
                }
            } else if (args[0].equalsIgnoreCase("transfer")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Who do you want to transfer ownerhsip to?");
                } else if (currentParty.get(player) == null) {
                    sender.sendMessage(ChatColor.RED + "You aren't in a party.");
                } else if (!currentParty.get(player).equals(player)) {
                    sender.sendMessage(ChatColor.RED + "You can't transfer ownership as a dirty member.");
                } else if (!memberList.get(player).contains(Bukkit.getOfflinePlayer(args[1]))) {
                    sender.sendMessage(ChatColor.RED + "That player isn't in your party.");
                } else if (args[1].equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(ChatColor.RED + "You can't transfer it to yourself!");
                } else {
                    Player newOwner = Bukkit.getPlayer(args[1]);
                    memberList.put(newOwner, memberList.get(player));
                    for (Player newPlayer : memberList.get(player)) {
                        currentParty.put(newPlayer, newOwner);
                        newPlayer.sendMessage(ChatColor.GREEN + "Party ownership has been transferred over to " + newOwner.getName());
                    }
                    memberList.put(player, null);
                    inviteList.put(player, null);
                    sender.sendMessage("You have transferred ownership to " + newOwner.getName());
                }
            } else if (args[0].equalsIgnoreCase("invite")) {
                if (currentParty.get(player) != player) {
                    sender.sendMessage(ChatColor.RED + "You must be the leader of the friend group to invite players.");
                    return true;
                } else if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Please provide a player.");
                    return true;
                } else if (currentParty.get(Bukkit.getPlayer(args[1])) != null) {
                    sender.sendMessage(ChatColor.RED + "That player is already in a party.");
                    return true;
                } else if (inviteList.get(player) != null && inviteList.get(player).contains(Bukkit.getPlayer(args[1]))) {
                    sender.sendMessage(ChatColor.RED + "They have already been invited.");
                    return true;
                } else if (args[1].equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(ChatColor.RED + "You can't invite yourself");
                    return true;
                } else if (!Bukkit.getServer().getOfflinePlayer(args[1]).isOnline()) {
                    sender.sendMessage(ChatColor.RED + "You can't invite a player who isn't online, or do you just not have any friends?");
                } else {
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have invited " + args[1] + " to the party.");
                    ArrayList<Player> newInviteList = new ArrayList<>();
                    newInviteList.add(Bukkit.getPlayer(args[1]));
                    inviteList.put(player, newInviteList);
                    Bukkit.getPlayer(args[1]).sendMessage(ChatColor.GREEN + "You have been invited to " + sender.getName() + "'s party.");
                }
            } else if (args[0].equalsIgnoreCase("kick")) {
                // TODO Prevent player from kicking themselves.
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Well, who do you wanna kick?");
                } else if (currentParty.get(player) == null) {
                    sender.sendMessage(ChatColor.RED + "You must be in a party.");
                } else if (!currentParty.get(player).getName().equalsIgnoreCase(player.getName())) {
                    sender.sendMessage(ChatColor.RED + "You must be the party leader to kick members.");
                } else if (!memberList.get(currentParty.get(player)).contains(Bukkit.getPlayer(args[1]))) {
                    sender.sendMessage(ChatColor.RED + "That player isn't in the party.");
                } else if (player.getName().equalsIgnoreCase(args[1])) {
                    sender.sendMessage(ChatColor.RED + "You can't kick yourself!");
                } else {
                    Player kickedPlayer = Bukkit.getPlayer(args[1]);
                    ArrayList<Player> newMemberList = memberList.get(currentParty.get(kickedPlayer));
                    sender.sendMessage(ChatColor.GREEN + "You have kicked " + kickedPlayer.getName() + " from the party.");
                    for (Player player2 : memberList.get(currentParty.get(kickedPlayer))) {
                        player2.sendMessage(ChatColor.RED + kickedPlayer.getName() + " has left the party.");
                    }
                    newMemberList.remove(kickedPlayer);
                    memberList.put(currentParty.get(kickedPlayer), newMemberList);
                    currentParty.put(kickedPlayer, null);
                    kickedPlayer.sendMessage(ChatColor.RED + "You have been kicked from the party.");
                }
            } else if (args[0].equalsIgnoreCase("join")) {
                 if (args.length != 2) {
                     sender.sendMessage(ChatColor.RED + "Well, who do you wanna join?");
                 } else if (inviteList.get(Bukkit.getPlayer(args[1])) == null || !(inviteList.get(Bukkit.getPlayer(args[1])).contains(player))) {
                    sender.sendMessage(ChatColor.RED + "You were not invited to that party. I can tell why, you worthless fuck.");
                } else if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Well, who do you wanna join?");
                } else if (currentParty.get(player) != null) {
                    sender.sendMessage(ChatColor.RED + "You are already in a party!");
                } else {
                    inviteList.get(Bukkit.getPlayer(args[1])).remove(player);
                    memberList.get(Bukkit.getPlayer(args[1])).add(player);
                    currentParty.put(player, Bukkit.getPlayer(args[1]));
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have joined " + args[1] + "'s party.");
                    for (Player player2 : memberList.get(currentParty.get(player))) {
                        player2.sendMessage(ChatColor.LIGHT_PURPLE + player.getName() + " has joined the party!");
                    }
                }
            } else if (args[0].equalsIgnoreCase("toggle")) {
                if (currentParty.get(player) == null) {
                    sender.sendMessage(ChatColor.RED + "You are aware that you aren't in a party, right?");
                    return true;
                }
                if (isPartyChatEnabled.get(player) == null || !isPartyChatEnabled.get(player)) {
                    isPartyChatEnabled.put(player, true);
                    sender.sendMessage(ChatColor.GREEN + "Party chat enabled.");
                } else {
                    isPartyChatEnabled.put(player, false);
                    sender.sendMessage(ChatColor.RED + "Party chat disabled.");
                }
            } else {
                if (currentParty.get(player) != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < args.length; i++) {
                        stringBuilder.append(args[i] + " ");
                    }
                    for (Player member : memberList.get(currentParty.get(player))) {
                        member.sendMessage(ChatColor.LIGHT_PURPLE + "[P] " + ChatColor.WHITE + player.getName() + ": " + stringBuilder.toString());
                    }
                    for (Player spy : Bukkit.getOnlinePlayers()) {
                        if (isSpying.get(spy) != null && isSpying.get(spy)) {
                            spy.sendMessage(ChatColor.RED + "[P] " + ChatColor.WHITE + player.getName() + ": " + stringBuilder.toString());
                        }
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("adminparty")) {
            Player player = (Player) sender;
            if (args[0].equalsIgnoreCase("spy")) {
                if (isSpying.get(player) == null || !isSpying.get(player)) {
                    isSpying.put(player, true);
                    sender.sendMessage(ChatColor.GREEN + "Spy mode enabled. (you sneaky bastard!)");
                } else {
                    isSpying.put(player, false);
                    sender.sendMessage(ChatColor.RED + "Spy mode disabled. (thanks for respecting the privacy)");
                }
            } else if (args[0].equalsIgnoreCase("show")) {
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Who do you want to show?");
                } else {
                    OfflinePlayer checkedPlayer = Bukkit.getOfflinePlayer(args[1]);
                    if (!checkedPlayer.isOnline()) {
                        sender.sendMessage(ChatColor.RED + "Erm, buddy, you know they aren't online?");
                    } else if (currentParty.get(checkedPlayer) == null) {
                        sender.sendMessage(ChatColor.RED + checkedPlayer.getName() + " isn't in a party.");
                    } else {
                        sender.sendMessage(ChatColor.LIGHT_PURPLE + checkedPlayer.getName() + " is in " + currentParty.get(checkedPlayer).getName() + "'s party.");
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
                    player2.sendMessage(ChatColor.RED + "You have been removed from your party.");
                } else {
                    player2.sendMessage(ChatColor.GREEN + "You have disbanded your party.");
                }
            }
        }
        if (inviteList.get(player) != null) {
            inviteList.get(player).clear();
        }
        memberList.get(player).clear();
    }

}
