package com.heroku.java.cms;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@Log
public class CmsFile {

    private static String FILE_SV = "/file";
    private static String FILE_STATUS_SV = "/fileStatus";

    @GetMapping("/cms/input")
    public String cmsInput() {
        return "cms/input";
    }

    @GetMapping(value = "/file/**")
    public String cmsFile(
            HttpServletRequest request,
            HttpServletResponse response) {

        // herokuの設定を読む
        CmsSetting cmsSetting = getCmsSetting();

        // ファイルのパスを取得する
        String filePath = request.getRequestURI().replaceFirst(FILE_SV, "");
        log.info("filePath: " + filePath);

        // S3クライアントの作成
        S3Client s3Client =
                S3Client.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(Region.US_EAST_1)
                        .build();

        try {
            // S3上のパスを作成する 例：aaa/bbb/ccc.gif
            String key = cmsSetting.getBasePrefix() + cmsSetting.getCloudcubeFilebase() + filePath;

            // S3からコンテンツのヘッダ情報を取得する
            HeadObjectResponse head
                    = getContentType(s3Client, cmsSetting.getBucket(), key);

            // キャッシュファイルアクセス準備
            String cacheFilePath = "/tmp/cache/" + filePath;
            File cacheFile = new File(cacheFilePath);
            log.info("cache exist: " + cacheFile.isFile());

            // キャッシュファイルが存在するとき
            if (cacheFile.isFile()) {
                // コンテンツがなければファイル削除する
                if (head == null) {
                    // 削除
                    log.info(cacheFilePath + " deleted.");
                    cacheFile.delete();
                    // 404応答
                    log.info(filePath + " not found.");
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                    return null;
                }
                // キャッシュファイルの日付
                Instant instant =
                        Files.getLastModifiedTime(
                                Paths.get(cacheFile.getPath())).toInstant();
                log.info("file time : " + instant);
                if (instant.isAfter(head.lastModified())) {
                    log.info("output cache file.");
                    // キャッシュがS3より新しいときは出力して終わる
                    InputStream is = new FileInputStream(cacheFile);
                    // ブラウザに応答する
                    response.setContentType(head.contentType());
                    response.setContentLengthLong(head.contentLength());
                    OutputStream os = response.getOutputStream();
                    IOUtils.copy(is, os);
                    os.flush();
                    IOUtils.closeQuietly(os);
                    IOUtils.closeQuietly(is);
                    return null;
                }
            }

            // ヘッダ取得できないときはなしとする
            if (head == null) {
                // 404応答
                log.info(filePath + " not found.");
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return null;
            }

            log.info("output s3 content.");

            // キャッシュのディレクトリが無かったら作る
            // このあたりから、排他制御しないとやばい気がする
            if (!cacheFile.getParentFile().isDirectory()) {
                Files.createDirectories(Paths.get(cacheFile.getAbsolutePath()));
            }
            File tmpFile = new File(cacheFilePath + ".tmp");
            OutputStream fileOutputStream = new FileOutputStream(tmpFile);

            // S3のコンテンツ参照
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(key)
                    .bucket(cmsSetting.getBucket())
                    .build();
            ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(objectRequest);

            // ブラウザに応答する準備
            response.setContentType(head.contentType());
            response.setContentLengthLong(head.contentLength());
            OutputStream responseOutputStream = response.getOutputStream();

            // ブラウザ応答とキャッシュ保存を並行で実施する
            byte[] buffer = new byte[4098];
            int len = -1;
            while ((len = objectStream.read(buffer, 0, 4098)) != -1) {
                responseOutputStream.write(buffer, 0, len);
                fileOutputStream.write(buffer, 0, len);
            }
            responseOutputStream.flush();
            responseOutputStream.close();

            // ファイルクローズ
            fileOutputStream.flush();
            fileOutputStream.close();
            tmpFile.renameTo(cacheFile);

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }

        return null;
    }

    public HeadObjectResponse getContentType(S3Client s3, String bucketName, String keyName) {

        try {
            HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                    .key(keyName)
                    .bucket(bucketName)
                    .build();

            HeadObjectResponse objectHead = s3.headObject(objectRequest);
            System.out.println("content type   : " + objectHead.contentType());
            System.out.println("last modified  : " + objectHead.lastModified());
            System.out.println("content length : " + objectHead.contentLength());

            return objectHead;

        } catch (NoSuchKeyException e) {
            // コンテンツがないときはこれ？
            return null;
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    @GetMapping(value = "/fileStatus/**")
    public String cmsFileStatus(
            HttpServletRequest request,
            Model model) {

        CmsSetting cmsSetting = getCmsSetting();

        String filePath = request.getRequestURI().replaceFirst(FILE_STATUS_SV, "");

        log.info("filePath: " + filePath);

        model.addAttribute("message", "");

        // クライアント
        S3Client s3Client =
                S3Client.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(Region.US_EAST_1)
                        .build();

        try {

            // key ex. aaa/bbb/ccc.gif
            String key = cmsSetting.getBasePrefix() + FILE_SV + filePath;

            // リスト参照
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(cmsSetting.getBucket())
                    .prefix(key)
                    .build();

            ListObjectsResponse res = s3Client.listObjects(listObjects);
            List<S3Object> objects = res.contents();
            if (objects.size() == 0) {
                model.addAttribute("message", filePath + "is empty.");
            }
            String tmp = "";
            for (S3Object myValue : objects) {
                log.info("\n key: " + myValue.key());
                log.info("\n owner: " + myValue.owner());
                log.info("\n last modified: " + myValue.lastModified());
                tmp += myValue.key() + " : " + myValue.owner() + " : " + myValue.lastModified() + "\n";
            }
            model.addAttribute("message", tmp);

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }

        return "cms/fileStatus";
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
