package com.heroku.java.cms;

import lombok.extern.java.Log;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
@Log
public class CmsUpload {

    @RequestMapping("/cms/upload")
    public String cmsUpload(Model model, CmsUploadForm form) {

        List<MultipartFile> files = form.getFiles();
        if (files != null || !files.isEmpty()) {
            // ファイル名
            List<String> uploadFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                uploadFiles.add(file.getOriginalFilename());
            }
            model.addAttribute("files", uploadFiles);
        }

        return "cms/upload";
    }
}
