package com.heroku.java.cms;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.java.Log;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@Log
public class CmsFileStatus {

    @GetMapping("/cms/input")
    public String mailInput()
    {
        return "cms/input";
    }

    @GetMapping(value = "/cmsFileStatus/**")
    public String cmsFileStatus(
            HttpServletRequest request,
            Model model) {
        CmsSetting cmsSetting = new CmsSetting();

        // リクエストが欲しいのだが…
//        HttpServletRequest request = ((ServletRequestAttributes) Objects
//                .requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        String filePath = request.getPathInfo();

        log.info("filePath: " + filePath);

        // CloudCube設定の参照
        String cloudcubeAccessKeyId = cmsSetting.getAccess_key_id();
        String cloudcubeSecretAccessKey = cmsSetting.getSecret_access_key();
        String cloudcubeUrl = cmsSetting.getUrl();

        System.setProperty("aws.accessKeyId", cloudcubeAccessKeyId);
        System.setProperty("aws.secretAccessKey", cloudcubeSecretAccessKey);

        model.addAttribute("message", "");

        // 試しに組み込み
        S3Client s3Client =
                S3Client.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(Region.US_EAST_1)
                        .build();

        try {
            // 設定値取り出し
            Pattern p = Pattern.compile("^https://(.*)\\.s3\\.amazonaws\\.com/(.*)$");
            Matcher m = p.matcher(cloudcubeUrl);
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

                tmp +=  myValue.key() + ":" + myValue.owner() + "<br>\n";
            }
            model.addAttribute("message", tmp);

            /*
            System.out.print("\n data get 0");

            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key("hxlzsmew3hjg/public/tree_sample.html")
                    .bucket("cloud-cube-us2")
                    .build();

            System.out.print("\n data get 1");

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();

            System.out.print("\n data get 2");

            // Write the data to a local file.
            File myFile = new File("/tmp/test.txt");
            OutputStream os = new FileOutputStream(myFile);
            os.write(data);
            System.out.println("Successfully obtained bytes from an S3 object");
            os.close();


        } catch (IOException ex) {
            ex.printStackTrace();
             */
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            throw e;
//            System.exit(1);
        }

        return "cms/fileStatus";
    }

}
