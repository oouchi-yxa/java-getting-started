package com.heroku.java.cms;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@Log
public class CmsUpload {

    @PostMapping("/cms/upload")
    public String cmsUpload(Model model, UploadForm form) {

        if (form.getFile()==null || form.getFile().isEmpty()) {
            List<String> errors = new ArrayList<>();
            errors.add("no file.");
            model.addAttribute("errors", errors);
            return "cms/input";
        }

        // ファイル名
        for (MultipartFile file : form.getFile()) {
            List<String> files = new ArrayList<>();
            files.add(file.getName());
            model.addAttribute("files", files);
        }

        return "cms/upload";
    }
}
