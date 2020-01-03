package com.github.noonmaru.alarm;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

/**
 * @author Nemo
 */
public class Alarm implements Comparable<Alarm>
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMDD-HHmmss");

    private final Date date;

    private final String title;

    private final String subtitle;

    public Alarm(Date date, String title, String subtitle)
    {
        this.date = date;
        this.title = title;
        this.subtitle = subtitle;
    }

    public Alarm(JsonObject json) throws ParseException
    {
        this.date = DATE_FORMAT.parse(json.get("date").getAsString());
        this.title = json.has("title") ? json.get("title").getAsString() : null;
        this.subtitle = json.has("subtitle") ? json.get("subtitle").getAsString() : null;
    }

    public Date getDate()
    {
        return date;
    }

    public String getTitle()
    {
        return title;
    }

    public String getSubtitle()
    {
        return subtitle;
    }

    public JsonObject save()
    {
        JsonObject json = new JsonObject();
        json.addProperty("date", DATE_FORMAT.format(date));
        if (title != null)
            json.addProperty("title", this.title);
        if (subtitle != null)
            json.addProperty("subtitle", this.subtitle);

        return json;
    }

    @Override
    public int compareTo(Alarm o)
    {
        return this.date.compareTo(o.date);
    }
    @Override
    public String toString()
    {
        return "Alarm{" +
                "date=" + date +
                ", title='" + title + '\'' +
                ", subtitle='" + subtitle + '\'' +
                '}';
    }
}
