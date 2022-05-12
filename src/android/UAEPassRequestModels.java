package com.outsystems.uaepass;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.Objects;

import ae.sdg.libraryuaepass.business.Environment;
import ae.sdg.libraryuaepass.business.authentication.model.UAEPassAccessTokenRequestModel;
import ae.sdg.libraryuaepass.business.documentsigning.model.DocumentSigningRequestParams;
import ae.sdg.libraryuaepass.business.documentsigning.model.UAEPassDocumentDownloadRequestModel;
import ae.sdg.libraryuaepass.business.documentsigning.model.UAEPassDocumentSigningRequestModel;
import ae.sdg.libraryuaepass.business.profile.model.UAEPassProfileRequestModel;
import ae.sdg.libraryuaepass.utils.Utils;
import $appid.BuildConfig;

import static ae.sdg.libraryuaepass.business.Environment.PRODUCTION;
import static ae.sdg.libraryuaepass.business.Environment.STAGING;

/**
 * Created by Farooq Arshed on 12/11/18.
 */
public class UAEPassRequestModels {

    //UAE PASS START -- ADD BELOW FIELDS
    private static String UAE_PASS_CLIENT_ID;
    private static String UAE_PASS_CLIENT_SECRET;
    private static String REDIRECT_URL;
    private static Environment UAE_PASS_ENVIRONMENT;
    //UAE PASS END -- ADD BELOW FIELDS


    private static final String UAE_PASS_PACKAGE_ID = "ae.uaepass.mainapp";
    private static final String UAE_PASS_PACKAGE_ID_QA = "ae.uaepass.mainapp.stg";
    private static final String DOCUMENT_SIGNING_SCOPE = "urn:safelayer:eidas:sign:process:document";
    private static final String RESPONSE_TYPE = "code";
    private static final String SCOPE = "urn:uae:digitalid:profile:general";//:general
    private static final String ACR_VALUES_MOBILE = "urn:digitalid:authentication:flow:mobileondevice";
    private static final String ACR_VALUES_WEB = "urn:safelayer:tws:policies:authentication:level:low";
    private static final String STATE = Utils.INSTANCE.generateRandomString(24);


    private static final String SCHEME = BuildConfig.URI_SCHEME;
    private static final String FAILURE_HOST = BuildConfig.URI_HOST_FAILURE;
    private static final String SUCCESS_HOST = BuildConfig.URI_HOST_SUCCESS;

    public UAEPassRequestModels(String environment,String clientID, String clientSecret,String redirectUrl){
        UAE_PASS_CLIENT_ID = clientID;
        UAE_PASS_CLIENT_SECRET = clientSecret;
        REDIRECT_URL = redirectUrl;
        switch (environment){
            case "PROD":
                UAE_PASS_ENVIRONMENT = PRODUCTION;
                break;
            default:
                UAE_PASS_ENVIRONMENT = STAGING;
                break;

        }
    }

    private static boolean isPackageInstalled(PackageManager packageManager) {

        String packageName = null;
        if (UAEPassRequestModels.UAE_PASS_ENVIRONMENT == PRODUCTION) {
            packageName = UAE_PASS_PACKAGE_ID;
        } else if (UAEPassRequestModels.UAE_PASS_ENVIRONMENT == STAGING) {
            packageName = UAE_PASS_PACKAGE_ID_QA;
        }

        boolean found = true;
        try {
            packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            found = false;
        }

        return found;
    }

    public UAEPassAccessTokenRequestModel getAuthenticationRequestModel(Context context) {
        String ACR_VALUE = "";
        if (isPackageInstalled(context.getPackageManager())) {
            ACR_VALUE = ACR_VALUES_MOBILE;
        } else {
            ACR_VALUE = ACR_VALUES_WEB;
        }
        return new UAEPassAccessTokenRequestModel(
                UAE_PASS_ENVIRONMENT,
                UAE_PASS_CLIENT_ID,
                UAE_PASS_CLIENT_SECRET,
                SCHEME,
                FAILURE_HOST,
                SUCCESS_HOST,
                REDIRECT_URL,
                SCOPE,
                RESPONSE_TYPE,
                ACR_VALUE,
                STATE
        );
    }

    public UAEPassDocumentSigningRequestModel getDocumentRequestModel(File file, DocumentSigningRequestParams documentSigningParams) {
        return new UAEPassDocumentSigningRequestModel(
                UAE_PASS_ENVIRONMENT,
                UAE_PASS_CLIENT_ID,
                UAE_PASS_CLIENT_SECRET,
                SCHEME,
                FAILURE_HOST,
                SUCCESS_HOST,
                Objects.requireNonNull(documentSigningParams.getFinishCallbackUrl()),
                DOCUMENT_SIGNING_SCOPE,
                file,
                documentSigningParams);
    }

    public UAEPassDocumentDownloadRequestModel getDocumentDownloadRequestModel(String documentName, String documentURL) {
        return new UAEPassDocumentDownloadRequestModel(
                UAE_PASS_ENVIRONMENT,
                UAE_PASS_CLIENT_ID,
                UAE_PASS_CLIENT_SECRET,
                DOCUMENT_SIGNING_SCOPE,
                documentName,
                documentURL);

    }

    public UAEPassProfileRequestModel getProfileRequestModel(Context context) {
        String ACR_VALUE = "";
        if (isPackageInstalled(context.getPackageManager())) {
            ACR_VALUE = ACR_VALUES_MOBILE;
        } else {
            ACR_VALUE = ACR_VALUES_WEB;
        }

        return new UAEPassProfileRequestModel(
                UAE_PASS_ENVIRONMENT,
                UAE_PASS_CLIENT_ID,
                UAE_PASS_CLIENT_SECRET,
                SCHEME,
                FAILURE_HOST,
                SUCCESS_HOST,
                REDIRECT_URL,
                SCOPE,
                RESPONSE_TYPE,
                ACR_VALUE,
                STATE);
    }

}
