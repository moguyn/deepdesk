package com.moguyn.deepdesk.tools;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class DateTimeTools {

    @Tool(description = "Get the current date and time")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    @Tool(description = "Translate a Unix timestamp to a date and time")
    public String translateUnixTimestamp(@ToolParam(description = "The Unix timestamp to translate") String timestamp) {
        long unixTimestamp = Long.parseLong(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(unixTimestamp, 0, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
}
