package com.heroku.java.cms;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CmsSetting {

    String cloudcubeAccessKeyId = System.getenv("CLOUDCUBE_ACCESS_KEY_ID");

    String cloudcubeSecretAccessKey = System.getenv("CLOUDCUBE_SECRET_ACCESS_KEY");

    String cloudcubeUrl = System.getenv("CLOUDCUBE_URL");

    String cloudcubeFilebase = System.getenv("CLOUDCUBE_FILEBASE");

    String bucket = "";

    String basePrefix = "";
}
