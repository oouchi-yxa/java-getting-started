package com.heroku.java.cms;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@Log
public class CmsFile {

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

        CmsSetting cmsSetting = new CmsSetting();

        String filePath = request.getRequestURI().replaceFirst("/file","");

        log.info("filePath: " + filePath);

        // CloudCube設定の参照
        String cloudCubeAccessKeyId = cmsSetting.getAccess_key_id();
        String cloudCubeSecretAccessKey = cmsSetting.getSecret_access_key();
        String cloudCubeUrl = cmsSetting.getUrl();

        System.setProperty("aws.accessKeyId", cloudCubeAccessKeyId);
        System.setProperty("aws.secretAccessKey", cloudCubeSecretAccessKey);

        model.addAttribute("message", "");

        // クライアント
        S3Client s3Client =
                S3Client.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(Region.US_EAST_1)
                        .build();

        try {
            // 設定値取り出し
            Pattern p = Pattern.compile("^https://(.*)\\.s3\\.amazonaws\\.com/(.*)$");
            Matcher m = p.matcher(cloudCubeUrl);
            String bucket = "";
            String basePrefix = "";
            if (m.find()){
                bucket = m.group(1);
                basePrefix = m.group(2);
            }

            getContentType(s3Client, bucket, filePath);

            // 参照
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(basePrefix + filePath)
                    .bucket(bucket)
                    .build();

            System.out.print("\n data get 1");

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();

            System.out.print("\n data get 2");

            model.addAttribute("message", new String(data, Charset.defaultCharset()));


            // Write the data to a local file.
            File myFile = new File("/tmp/test.txt");
            OutputStream os = new FileOutputStream(myFile);
            os.write(data);
            System.out.println("Successfully obtained bytes from an S3 object");
            os.close();


        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
//            System.exit(1);
        }

        return "cms/fileStatus";
    }

    @GetMapping(value = "/fileStatus/**")
    public String cmsFileStatus(
            HttpServletRequest request,
            Model model) {

        CmsSetting cmsSetting = new CmsSetting();

        String filePath = request.getRequestURI().replaceFirst("/fileStatus","");

        log.info("filePath: " + filePath);

        // CloudCube設定の参照
        String cloudCubeAccessKeyId = cmsSetting.getAccess_key_id();
        String cloudCubeSecretAccessKey = cmsSetting.getSecret_access_key();
        String cloudCubeUrl = cmsSetting.getUrl();

        System.setProperty("aws.accessKeyId", cloudCubeAccessKeyId);
        System.setProperty("aws.secretAccessKey", cloudCubeSecretAccessKey);

        model.addAttribute("message", "");

        // クライアント
        S3Client s3Client =
                S3Client.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(Region.US_EAST_1)
                        .build();

        try {
            // 設定値取り出し
            Pattern p = Pattern.compile("^https://(.*)\\.s3\\.amazonaws\\.com/(.*)$");
            Matcher m = p.matcher(cloudCubeUrl);
            String bucket = "";
            String basePrefix = "";
            if (m.find()){
                bucket = m.group(1);
                basePrefix = m.group(2);
            }

            // リスト参照
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(bucket)
                    .prefix(basePrefix + filePath)
                    .build();

            ListObjectsResponse res = s3Client.listObjects(listObjects);
            List<S3Object> objects = res.contents();
            if (objects.size() == 0) {
                model.addAttribute("message", filePath + "is empty.");
            }
            String tmp = "";
            for (S3Object myValue : objects) {
                log.info("\n The name of the key is " + myValue.key());
                log.info("\n The owner is " + myValue.owner());
                log.info("\n The last modified is " + myValue.lastModified());
                tmp +=  myValue.key() + ":" + myValue.owner() + ":" + myValue.lastModified() + "\n";
            }
            model.addAttribute("message", tmp);

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
//            System.exit(1);
        }

        return "cms/fileStatus";
    }

    public void getContentType (S3Client s3, String bucketName, String keyName) {

        try {
            HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                    .key(keyName)
                    .bucket(bucketName)
                    .build();

            HeadObjectResponse objectHead = s3.headObject(objectRequest);
            String type = objectHead.contentType();
            System.out.println("The object content type is "+type);

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
        }
    }


}
