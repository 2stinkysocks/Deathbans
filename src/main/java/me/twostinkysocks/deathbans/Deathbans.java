package me.twostinkysocks.deathbans;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class Deathbans extends JavaPlugin implements Listener, CommandExecutor {
    public String prefix;
    public String bandcmd;
    @Override
    public void onEnable() {
        this.getServer().getConsoleSender().sendMessage("§aDeathbans has loaded.");
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.prefix = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("prefix"));
        this.bandcmd = getConfig().getString("ban-command");
        this.getServer().getPluginManager().registerEvents(this, this);
        getCommand("setplaytime").setExecutor(this);
        getCommand("setplaytime").setTabCompleter(this);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void deathban(PlayerDeathEvent e) {
        final Player p = e.getEntity();
        if ((!getConfig().getBoolean("ops-bypass") || !p.isOp()) && (getConfig().getString("bypass-perm").equals("") || !p.hasPermission(getConfig().getString("bypass-perm")))) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                int playtimeTicks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
                String banTime = getBanTime(playtimeTicks);
                String bancmd = bandcmd.replaceAll("%player%", p.getName()).replaceAll("%uuid%", p.getUniqueId().toString()).replaceAll("%reason%", sanitize(getConfig().getString("default-ban-reason"))).replaceAll("%death-cause%", sanitize(e.getDeathMessage())).replaceAll("%time%", banTime);
                getServer().dispatchCommand(getServer().getConsoleSender(), bancmd);
                String message = getConfig().getString("default-broadcast").replaceAll("%player%", p.getName()).replaceAll("%uuid%", p.getUniqueId().toString()).replaceAll("%reason%", sanitize(getConfig().getString("default-ban-reason"))).replaceAll("%death-cause%", sanitize(e.getDeathMessage())).replaceAll("%time%", banTime);
                Bukkit.broadcastMessage(prefix + message);
            }, 2L);
        }
    }

    private String getBanTime(int playtimeTicks) {
        int playtimeHours = (int)(playtimeTicks/20.0/60.0/60.0);
        int playtimeMinutes = (int) ((playtimeTicks - (playtimeHours*60*60*20))/20.0/60.0);
        if(playtimeHours >= 15) {
            return "15m";
        } else {
            return playtimeHours + "m" + playtimeMinutes + "s";
        }
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(label.equals("setplaytime")) {
            if(!sender.hasPermission("deathbans.setplaytime")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission!");
                return true;
            }
            if(args.length < 2 || Bukkit.getPlayer(args[0]) == null || !isInteger(args[1])) {
                sender.sendMessage(ChatColor.RED + "Usage: /setplaytime <player> <playtimeMins>");
                return true;
            }
            Player p = Bukkit.getPlayer(args[0]);
            int playtimeMins = Integer.parseInt(args[1]);
            p.setStatistic(Statistic.PLAY_ONE_MINUTE, playtimeMins*60*20);
            sender.sendMessage(ChatColor.AQUA + "Set " + p.getName() + "'s playtime to " + p.getStatistic(Statistic.PLAY_ONE_MINUTE)/1200);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if(alias.equals("setplaytime")) {
            if(args.length == 1) {
                StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toList()), completions);
                return completions;
            }
        }
        return completions;
    }

    private static String sanitize(String var0) {
        if(var0 == null) return "";
        StringBuilder var1 = new StringBuilder();
        char[] p = var0.toCharArray();
        char[] var3 = p;
        int var4 = p.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            char var6 = var3[var5];
            if (var6 != '$') {
                var1.append(var6);
            }
        }

        return var1.toString();
    }

    public boolean isInteger(String str) {
        try{
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

}
