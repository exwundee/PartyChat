package com.exwundee.partychat;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.ChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
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
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("party")) {
            Player player = null;
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You are not real. Peel your skin off, hurry!");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party create " + ChatColor.WHITE + " - Creates a party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party invite " + ChatColor.WHITE + " - Invites player to party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party leave " + ChatColor.WHITE + " - Leaves current party.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party info " + ChatColor.WHITE + " - Shows current party info.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party chat " + ChatColor.WHITE + " - Toggle party chat.");
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "/party <message> " + ChatColor.WHITE + " - Send a chat to party.");
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
            } else if(args[0].equalsIgnoreCase("info")) {
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
                        for (Player player2 : memberList.get(player)) {
                            currentParty.put(player2, null);
                            player2.sendMessage(ChatColor.RED + "You have been removed from your party.");
                        }
                        if (inviteList.get(player) != null) {
                            inviteList.get(player).clear();
                        }
                        memberList.get(player).clear();
                    } else {
                        ArrayList<Player> newMemberList = memberList.get(player);
                        for (Player player2 : memberList.get(currentParty.get(player))) {
                            player2.sendMessage(ChatColor.RED + player.getName() + " has left the party.");
                        }
                        newMemberList.remove(player);
                        memberList.put(currentParty.get(player), newMemberList);
                        currentParty.put(player, null);
                    }
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "You have left your party.");
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
                        player2.sendMessage(ChatColor.LIGHT_PURPLE + " has joined the party!");
                    }
                }
            } else if (args[0].equalsIgnoreCase("chat")) {
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
                }
            }
        }
        return true;
    }

}
