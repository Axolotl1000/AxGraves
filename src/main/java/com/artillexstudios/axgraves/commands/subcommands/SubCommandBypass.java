package com.artillexstudios.axgraves.commands.subcommands;

import com.artillexstudios.axgraves.AxGraves;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public enum SubCommandBypass {
    INSTANCE;

    public void subCommand(@NotNull CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return;
        }

        UUID playerUnique = ((Player) sender).getUniqueId();

        if (AxGraves.bypassedPlayers.contains(playerUnique)) {
            AxGraves.bypassedPlayers.remove(playerUnique);
            sender.sendMessage(ChatColor.RED + "死亡時將會遺失物品");
        } else {
            AxGraves.bypassedPlayers.add(playerUnique);
            sender.sendMessage(ChatColor.GREEN + "死亡時將會保留物品");
        }

    }
}
