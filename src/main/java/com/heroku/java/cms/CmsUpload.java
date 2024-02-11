package com.heroku.java.cms;

import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
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

    @RequestMapping("/cms/delete")
    public String cmsDelete(Model model, CmsFormDelete form) {

        CmsSetting cmsSetting = getCmsSetting();
        model.addAttribute("message", "");

        if (StringUtils.isNotEmpty(form.getDeletePath())) {
            // クライアント
            try (S3Client s3Client = S3Client.builder()
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .region(Region.US_EAST_1)
                    .build()) {

                String delKey = cmsSetting.getBasePrefix()
                        + form.getDeletePath();
                log.info("del: " + delKey);

                DeleteObjectRequest delReq = DeleteObjectRequest
                        .builder()
                        .key(delKey)
                        .bucket(cmsSetting.getBucket())
                        .build();

                s3Client.deleteObject(delReq);

                model.addAttribute("message",
                        "deleted: " + form.getDeletePath());
            }
        } else {
            model.addAttribute("message",
                    "no delete file");
        }

        return "cms/delete";
    }

    @RequestMapping("/cms/upload")
    public String cmsUpload(Model model, CmsFormUpload form) {

        //　既存ファイルの確認
        CmsSetting cmsSetting = getCmsSetting();
        model.addAttribute("message", "");

        // クライアント
        try (S3Client s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build()) {

            try {

                // ルートのパス( /file以下）
                String key = cmsSetting.getBasePrefix() + FILE_SV;

                // S3リスト参照
                ListObjectsRequest listObjects = ListObjectsRequest
                        .builder()
                        .bucket(cmsSetting.getBucket())
                        .prefix(key)
                        .build();

                // アップロードファイル受付
                List<MultipartFile> files = form.getFiles();
                if (files != null && !files.isEmpty()) {
                    // ファイル名
                    List<String> uploadFiles = new ArrayList<>();
                    for (MultipartFile file : files) {
                        uploadFiles.add(file.getOriginalFilename());

                        // CloudCubeのベース＋指定したディレクトリ
                        String upKey = cmsSetting.getBasePrefix()
                                + StringUtils.defaultString(form.getDir());
                        // 追加指定したパス
                        if (StringUtils.isNotEmpty(form.getAddDir())) {
                            upKey += "/" + form.getAddDir();
                        }
                        // アップロードファイル名
                        upKey += "/" + file.getOriginalFilename();
                        // 重複除去
                        if (upKey.contains("//")) {
                            upKey = upKey.replaceAll("//*", "/");
                        }

                        // content-type を取得する
                        String contentType = "application/octet-stream";
                        if (file.getContentType() != null) {
                            contentType = file.getContentType();
                        }

                        // S3アップロード
                        log.info("key: " + upKey);
                        PutObjectRequest put = PutObjectRequest.builder()
                                .key(upKey)
                                .bucket(cmsSetting.getBucket())
                                .contentType(contentType)
                                .build();
                        s3Client.putObject(put,
                                RequestBody.fromInputStream(
                                        file.getInputStream(), file.getSize()));
                    }
                    model.addAttribute("files", uploadFiles);
                }

                // アップロード先リスト
                List<String> dirs = new ArrayList<>();
                dirs.add(FILE_SV);

                ListObjectsResponse res = s3Client.listObjects(listObjects);
                List<S3Object> objects = res.contents();
                if (objects.isEmpty()) {
                    model.addAttribute("message", FILE_SV + "　is empty.");
                } else {
                    List<Map<String, Object>> s3list = new ArrayList<>();
                    for (S3Object myValue : objects) {

                        // ファイルパス
                        String name = myValue.key().replaceFirst(
                                cmsSetting.getBasePrefix(), "");

                        // ファイルパスからディレクトリ名取得してリスト格納
                        String dir = name.substring(0, name.lastIndexOf("/"));
                        if (!dirs.contains(dir)) {
                            dirs.add(dir);
                        }

                        // ファイル情報表示用マップ
                        Map<String, Object> inMap = new HashMap<>();
                        inMap.put("name", name);
                        inMap.put("size", myValue.size());
                        inMap.put("time", myValue.lastModified());
                        s3list.add(inMap);
                    }
                    // 画面にS3の情報を渡す
                    model.addAttribute("s3list", s3list);
                    model.addAttribute("dirs", dirs);
                }

            } catch (S3Exception e) {
                System.err.println(e.awsErrorDetails().errorMessage());
                throw e;
            } catch (IOException e) {
                throw new RuntimeException(e);
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
