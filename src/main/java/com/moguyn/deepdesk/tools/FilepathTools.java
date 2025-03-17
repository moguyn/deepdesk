package com.moguyn.deepdesk.tools;

import java.nio.file.Paths;

import org.springframework.ai.tool.annotation.Tool;

public class FilepathTools {

    @Tool(name = "getAbsolutePath", description = "get the absolute path of the provided filepath")
    public String getAbsolutePath(String filepath) {
        return Paths.get(filepath).toAbsolutePath().toString();
    }
}
