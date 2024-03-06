package com.heroku.java.cms;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class CmsFormUpload {
    private List<MultipartFile> files;
    private String dir;
    private String addDir;
}
