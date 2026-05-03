package com.sell.controller;

import com.sell.dataobject.RestaurantTable;
import com.sell.service.QrCodeService;
import com.sell.service.RestaurantTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 二维码管理 - PRD 5.3
 */
@Controller
@RequestMapping("/seller/qrcode")
public class SellerQrCodeController {

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private RestaurantTableService tableService;

    /** 单桌二维码 PNG 流. */
    @GetMapping("/single")
    public ResponseEntity<byte[]> single(@RequestParam("tableId") Integer tableId,
                                         @RequestParam(value = "size", defaultValue = "300") Integer size,
                                         @RequestParam(value = "download", defaultValue = "false") Boolean download) {
        RestaurantTable table = tableService.findOne(tableId);
        if (table == null) {
            return ResponseEntity.notFound().build();
        }
        String url = qrCodeService.buildTableQrUrl(table.getTableCode());
        byte[] png = qrCodeService.generatePng(url, size);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        if (download) {
            String filename = "qrcode-" + table.getTableCode() + ".png";
            String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        }
        return ResponseEntity.ok().headers(headers).body(png);
    }

    /** 按区域批量下载 ZIP. areaId 为空 = 全部桌. */
    @GetMapping("/batch")
    public ResponseEntity<byte[]> batch(@RequestParam(value = "areaId", required = false) Integer areaId,
                                        @RequestParam(value = "size", defaultValue = "300") Integer size) throws IOException {
        List<RestaurantTable> tables = tableService.findByAreaId(areaId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (RestaurantTable t : tables) {
                String url = qrCodeService.buildTableQrUrl(t.getTableCode());
                byte[] png = qrCodeService.generatePng(url, size);
                ZipEntry entry = new ZipEntry(t.getTableCode() + ".png");
                zos.putNextEntry(entry);
                zos.write(png);
                zos.closeEntry();
            }
        }
        String filename = "qrcodes-" + (areaId == null ? "all" : "area" + areaId) + ".zip";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);
        return ResponseEntity.ok().headers(headers).body(baos.toByteArray());
    }
}
