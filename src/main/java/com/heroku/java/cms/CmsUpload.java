package com.heroku.java.cms;

import lombok.extern.java.Log;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@Log
public class CmsUpload {

    private static String FILE_SV = "/file";

    @RequestMapping("/cms/upload")
    public String cmsUpload(Model model, CmsUploadForm form) {

        // アップロードファイル受付
        List<MultipartFile> files = form.getFiles();
        if (files != null && !files.isEmpty()) {
            // ファイル名
            List<String> uploadFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                uploadFiles.add(file.getOriginalFilename());
            }
            model.addAttribute("files", uploadFiles);
        }

        //　既存ファイルの確認
        CmsSetting cmsSetting = getCmsSetting();
        model.addAttribute("message", "");

        // クライアント
        try (S3Client s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build()) {

            try {

                // ルートのパス
                String key = cmsSetting.getBasePrefix() + FILE_SV;

                // リスト参照
                ListObjectsRequest listObjects = ListObjectsRequest
                        .builder()
                        .bucket(cmsSetting.getBucket())
                        .prefix(key)
                        .build();

                ListObjectsResponse res = s3Client.listObjects(listObjects);
                List<S3Object> objects = res.contents();
                if (objects.isEmpty()) {
                    model.addAttribute("message", FILE_SV + "　is empty.");
                } else {
                    List<Map<String, Object>> s3list = new ArrayList<>();
                    for (S3Object myValue : objects) {
                        Map<String, Object> inMap = new HashMap<>();
                        inMap.put("name", myValue.key().replaceFirst(
                                cmsSetting.getBasePrefix(), ""));
                        inMap.put("size", myValue.size());
                        inMap.put("time", myValue.lastModified());
                        s3list.add(inMap);
                    }
                    model.addAttribute("s3list", s3list);
                }

            } catch (S3Exception e) {
                System.err.println(e.awsErrorDetails().errorMessage());
                throw e;
            }
        }

        return "cms/upload";
    }

    private CmsSetting getCmsSetting() {
        // 環境変数から設定読み込み
        CmsSetting cmsSetting = new CmsSetting();

        // CloudCube設定の参照
        String cloudCubeAccessKeyId = cmsSetting.getCloudcubeAccessKeyId();
        String cloudCubeSecretAccessKey = cmsSetting.getCloudcubeSecretAccessKey();
        String cloudCubeUrl = cmsSetting.getCloudcubeUrl();

        // アクセスキー情報をセット
        System.setProperty("aws.accessKeyId", cloudCubeAccessKeyId);
        System.setProperty("aws.secretAccessKey", cloudCubeSecretAccessKey);

        // 設定値取り出し
        Pattern p = Pattern.compile("^https://(.*)\\.s3\\.amazonaws\\.com/(.*)$");
        Matcher m = p.matcher(cloudCubeUrl);
        if (m.find()) {
            cmsSetting.setBucket(m.group(1));
            cmsSetting.setBasePrefix(m.group(2));
        }

        // 設定の返却
        return cmsSetting;
    }


}
