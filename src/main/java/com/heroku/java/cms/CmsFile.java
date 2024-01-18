package com.heroku.java.cms;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    public String mailInput()
    {
        return "cms/input";
    }

    @GetMapping(value = "/file/**")
    public String cmsFile(
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        CmsSetting cmsSetting = getCmsSetting();

        String filePath = request.getRequestURI().replaceFirst(FILE_SV,"");

        log.info("filePath: " + filePath);

        // クライアント
        S3Client s3Client =
                S3Client.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(Region.US_EAST_1)
                        .build();

        try {
            // key ex. aaa/bbb/ccc.gif
            String key = cmsSetting.getBasePrefix() + FILE_SV + filePath;

            // ヘッダ情報
            HeadObjectResponse head
                    = getContentType(s3Client, cmsSetting.getBucket(), key);

            // キャッシュ準備
            String cacheFilePath = "/tmp/" + filePath;
            File cacheFile = new File(cacheFilePath);
            Path cachePath = Paths.get(cacheFile.getPath());

            log.info("cache exist:0 " + cacheFile.isFile());
            log.info("cache size:0 " + Files.size(Paths.get(cacheFile.getPath())));

            if (cacheFile.isFile()) {
                FileTime fileTime = Files.getLastModifiedTime(cachePath);
                Instant instant = fileTime.toInstant();
                log.info("file instant:" + instant);
            }

            if (!cacheFile.getParentFile().isDirectory()) {
                Files.createDirectories(Paths.get(cacheFile.getAbsolutePath()));
            }
            File tmpFile = new File(cacheFilePath + ".tmp");
            OutputStream fileOutputStream = new FileOutputStream(tmpFile);

            // ファイル参照
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(key)
                    .bucket(cmsSetting.getBucket())
                    .build();
            ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(objectRequest);

            // ブラウザに応答する
            response.setContentType(head.contentType());
            response.setContentLengthLong(head.contentLength());
            OutputStream responseOutputStream = response.getOutputStream();

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

            log.info("cache exist: " + cacheFile.isFile());
            log.info("cache size: " + Files.size(Paths.get(cacheFile.getPath())));

            //            IOUtils.copy(objectStream, responseOutputStream);
//            responseOutputStream.close();

            // キャッシュに保存する
//            File myFile = new File("/tmp/test.txt");
//            OutputStream os = new FileOutputStream(myFile);
//            // S3オブジェクトの巻き戻し　うまくいかない　自分でループまわすか
//            objectStream.reset();
//            IOUtils.copy(objectStream, os);
//            os.close();
//            objectStream.close();

//            System.out.println("cache exist: " + myFile.isFile());
//            System.out.println("cache size: " + Files.size(Paths.get(myFile.getPath())));

//            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
//            byte[] data = objectBytes.asByteArray();
//
//            response.setContentType(head.contentType());
//            response.setContentLengthLong(head.contentLength());
//
//            OutputStream responseOutputStream = response.getOutputStream();
//            responseOutputStream.write(data);
//            responseOutputStream.close();
//
//            // Write the data to a local file.
//            File myFile = new File("/tmp/test.txt");
//            OutputStream os = new FileOutputStream(myFile);
//            os.write(data);
//            System.out.println("Successfully obtained bytes from an S3 object");
//            os.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }

        return null;
    }


    public HeadObjectResponse getContentType (S3Client s3, String bucketName, String keyName) {

        try {
            HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                    .key(keyName)
                    .bucket(bucketName)
                    .build();

            HeadObjectResponse objectHead = s3.headObject(objectRequest);
            String type = objectHead.contentType();
            System.out.println("The object content type is "+ type);
            System.out.println("The object last modified is "+ objectHead.lastModified());
            System.out.println("The object content length is "+ objectHead.contentLength());

            return objectHead;

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

        String filePath = request.getRequestURI().replaceFirst(FILE_STATUS_SV,"");

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
                tmp +=  myValue.key() + " : " + myValue.owner() + " : " + myValue.lastModified() + "\n";
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
        String cloudCubeAccessKeyId = cmsSetting.getAccess_key_id();
        String cloudCubeSecretAccessKey = cmsSetting.getSecret_access_key();
        String cloudCubeUrl = cmsSetting.getUrl();

        // アクセスキー情報をセット
        System.setProperty("aws.accessKeyId", cloudCubeAccessKeyId);
        System.setProperty("aws.secretAccessKey", cloudCubeSecretAccessKey);

        // 設定値取り出し
        Pattern p = Pattern.compile("^https://(.*)\\.s3\\.amazonaws\\.com/(.*)$");
        Matcher m = p.matcher(cloudCubeUrl);
        if (m.find()){
            cmsSetting.setBucket(m.group(1));
            cmsSetting.setBasePrefix(m.group(2));
        }

        // 設定の返却
        return cmsSetting;
    }

}
