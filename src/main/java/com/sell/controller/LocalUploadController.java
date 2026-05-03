package com.sell.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class LocalUploadController {

    private static final Path DIR =
            Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();

    @PostMapping("/image")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return err("文件为空");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";
        if (!ext.matches("png|jpg|jpeg|gif|webp")) {
            return err("仅支持 png / jpg / jpeg / gif / webp");
        }
        Files.createDirectories(DIR);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = DIR.resolve(name);
        file.transferTo(target.toFile());

        Map<String, Object> r = new HashMap<>();
        r.put("ok", true);
        // 通过 ImageUploadConfig 暴露的静态资源 + Spring server.servlet.context-path=/sell
        r.put("url", "/sell/uploads/" + name);
        return r;
    }

    private Map<String, Object> err(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("ok", false);
        r.put("msg", msg);
        return r;
    }
}
