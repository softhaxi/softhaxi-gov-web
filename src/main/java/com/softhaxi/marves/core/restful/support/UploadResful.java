package com.softhaxi.marves.core.restful.support;

import java.io.IOException;

import com.softhaxi.marves.core.service.storage.FileStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadResful {
    private static final Logger logger = LoggerFactory.getLogger(UploadResful.class);

    @Autowired
    private FileStorageService storageService;

    @PostMapping()
    public ResponseEntity<?> post(@RequestParam("file") MultipartFile file) {
        try {
            String path = storageService.store("/today", "testingfile", file);
            logger.info("[post] return path..." + path);
            return ResponseEntity.ok(path);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
