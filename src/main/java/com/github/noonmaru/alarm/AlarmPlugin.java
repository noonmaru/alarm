package com.github.noonmaru.alarm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class AlarmPlugin extends JavaPlugin implements Runnable
{

    private File alarmFile;

    private long lastModified = -1;

    private List<Alarm> alarms;

    private Queue<Alarm> queue;

    @Override
    public void onEnable()
    {
        File folder = getDataFolder();
        folder.mkdirs();

        alarmFile = new File(folder, "alarms.json");
        loadAlarm();

        getServer().getScheduler().runTaskTimer(this, this, 0, 1);
    }

    @Override
    public void onDisable()
    {
        saveAlarms();
    }

    private void saveAlarms()
    {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        json.add("alarms", array);

        for (Alarm alarm : this.alarms)
        {
            array.add(alarm.save());
        }

        try
        {
            JsonIO.save(json, alarmFile);
            this.lastModified = alarmFile.lastModified();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void loadAlarm()
    {
        long lastModified = alarmFile.lastModified();

        if (this.lastModified == lastModified)
            return;

        this.lastModified = lastModified;

        getLogger().info("Config reloaded!");

        if (alarmFile.exists())
        {
            JsonObject json;

            try
            {
                json = JsonIO.load(alarmFile);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return;
            }

            JsonArray array = json.getAsJsonArray("alarms");
            List<Alarm> alarms = new ArrayList<>(array.size());

            for (JsonElement element : array)
            {
                try
                {
                    Alarm alarm = new Alarm(element.getAsJsonObject());
                    alarms.add(alarm);
                }
                catch (ParseException e)
                {
                    e.printStackTrace();

                    getLogger().info("Failed to load: " + element.toString());
                }
            }

            this.alarms = alarms;
            this.queue = new PriorityQueue<>();

            long time = System.currentTimeMillis();

            for (Alarm alarm : alarms)
            {
                if (time < alarm.getDate().getTime())
                {
                    queue.offer(alarm);
                }
            }
        }
        else
        {
            this.alarms = new ArrayList<>();
            this.queue = new PriorityQueue<>();
        }
    }

    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyyMMddHHmmss");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0)
            return false;

        String sub = args[0];

        if ("add".equalsIgnoreCase(sub))
        {
            if (args.length < 2)
            {
                sender.sendMessage("/" + label + " " + sub + " <yyyyMMddHHmmss> [title...] / [subtitle...]");
                return true;
            }

            String timeString = args[1];
            Date date;

            try
            {
                date = DATE_PARSER.parse(timeString);
            }
            catch (ParseException e)
            {
                sender.sendMessage("날자를 바로 입력해주세요.");
                return true;
            }

            String title = null;
            String subtitle = null;

            if (args.length >= 3)
            {
                String s = ChatColor.translateAlternateColorCodes('&', StringUtils.join(args, ' ', 2, args.length));

                int slash = s.indexOf('/');

                if (slash > 0)
                {
                    title = s.substring(0, slash).trim();
                    subtitle = s.substring(slash + 1).trim();
                }
                else
                {
                    title = s;
                }
            }

            Alarm alarm = new Alarm(date, title, subtitle);
            alarms.add(alarm);

            if (System.currentTimeMillis() < date.getTime())
                queue.offer(alarm);

            sender.sendMessage("ADD: " + alarm.toString());

            saveAlarms();
        }
        else if ("remove".equalsIgnoreCase(sub))
        {
            if (args.length < 2)
            {
                sender.sendMessage("/" + label + " " + sub + " <index>");
                return true;
            }

            int index;

            try
            {
                index = Integer.parseInt(args[1]);

                if (index < 0 || index >= alarms.size())
                    throw new NumberFormatException();
            }
            catch (NumberFormatException e)
            {
                sender.sendMessage("인덱스를 바로 입력해주세요.");
                return true;
            }

            Alarm alarm = alarms.remove(index);
            queue.remove(alarm);

            sender.sendMessage("REMOVED: " + alarm);
            saveAlarms();
        }
        else if ("list".equalsIgnoreCase(sub))
        {
            int index = 0;
            long time = System.currentTimeMillis();

            for (Alarm alarm : alarms)
            {
                ChatColor color = time < alarm.getDate().getTime() ? ChatColor.AQUA : ChatColor.GRAY;

                sender.sendMessage(color.toString() + index + ". " + alarm.getDate() + " " + ChatColor.RESET + alarm.getTitle() + " " + ChatColor.RESET + alarm.getSubtitle());
            }

            sender.sendMessage("끝");
        }

        return true;
    }

    @Override
    public void run()
    {
        loadAlarm();

        long time = System.currentTimeMillis();

        while (true)
        {
            Alarm alarm = queue.peek();

            if (alarm == null || time < alarm.getDate().getTime())
                break;

            queue.remove();

            String title = alarm.getTitle();
            String subtitle = alarm.getSubtitle();

            if (title != null || subtitle != null)
            {
//                Packet.TITLE.compound(title, subtitle, 5, 200, 5).sendAll();

                for (Player player : Bukkit.getOnlinePlayers())
                {
                    player.sendTitle(title, subtitle, 2, 200, 5);
                }

                if (title != null)
                    Bukkit.broadcastMessage(String.valueOf(alarm.getTitle()));

                if (subtitle != null)
                    Bukkit.broadcastMessage(alarm.getSubtitle());
            }

            getLogger().info(alarm.toString());

            for (Player player : Bukkit.getOnlinePlayers())
            {
                Location loc = player.getEyeLocation();
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
//                Packet.EFFECT.namedSound(Sounds.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, loc.getX(), loc.getY(), loc.getZ(), 1.0F, 1.0F).sendTo(player);
            }
        }
    }
}
