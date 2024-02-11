package com.heroku.java.cms;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UploadForm {
    private List<MultipartFile> file;
}
