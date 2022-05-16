package com.outsystems.uaepass;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import ae.sdg.libraryuaepass.utils.Utils;

import ae.sdg.libraryuaepass.UAEPassController;
import ae.sdg.libraryuaepass.business.Environment;
import ae.sdg.libraryuaepass.business.authentication.model.UAEPassAccessTokenRequestModel;
import ae.sdg.libraryuaepass.business.documentsigning.model.DocumentSigningRequestParams;
import ae.sdg.libraryuaepass.business.documentsigning.model.UAEPassDocumentDownloadRequestModel;
import ae.sdg.libraryuaepass.business.documentsigning.model.UAEPassDocumentSigningRequestModel;
import ae.sdg.libraryuaepass.business.profile.model.UAEPassProfileRequestModel;
import ae.sdg.libraryuaepass.network.SDGAbstractHttpClient;
import ae.sdg.libraryuaepass.utils.FileUtils;
import $appid.BuildConfig;


/**
 * This class echoes a string called from JavaScript.
 */
public class UAEPass extends CordovaPlugin {

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private UAEPassRequestModels uaePassRequestModels;

    private CallbackContext callbackContext;

    private BroadcastReceiver downloadcompletedBR;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        downloadcompletedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Uri uri = Uri.parse("content://"+cordova.getActivity().getPackageName()+"/Download/" + intent.getStringExtra("Document_title") + ".pdf");
                File pdfFile = new File(uri.getPath());

                DocumentSigningRequestParams documentSigningParams = loadDocumentSigningJson();
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UAEPassDocumentSigningRequestModel requestModel = uaePassRequestModels.getDocumentRequestModel(pdfFile, documentSigningParams);
                        UAEPassController.INSTANCE.signDocument(cordova.getActivity(), requestModel, (spId, documentURL, error) -> {
                            if (error != null) {
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,error));
                            } else {
                                try {
                                    JSONObject resultJSON = new JSONObject();
                                    resultJSON.put("pdfName", pdfFile.getName());
                                    resultJSON.put("url", documentURL);
                                    resultJSON.put("pdfID", spId);
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, resultJSON.toString()));
                                }catch (JSONException e){
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,e.getLocalizedMessage()));
                                }
                            }
                        });
                    }
                });

            }
        };
        cordova.getActivity().registerReceiver(downloadcompletedBR, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        switch (action) {
            case "initPlugin":
                uaePassRequestModels = new UAEPassRequestModels(
                        args.getString(0),
                        args.getString(1),
                        args.getString(2),
                        args.getString(3));
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                return true;
            case "getWritePermission":
                getWritePermission();
                return true;
            case "getCode":
                getCode();
                return true;
            case "getAccessToken":
                getAccessToken();
                return true;
            case "getProfile":
                getProfile();
                return true;
            case "signDocument":
                signDocument(args.getString(0));
                return true;
            case "clearData":
                clearData();
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                return true;
            default:
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,"Action not mapped in the plugin!"));
                return false;
        }
    }
    /**
     * Ask user for WRITE_EXTERNAL_STORAGE permission to save downloaded document.
     */
    private void getWritePermission() {
        if (ContextCompat.checkSelfPermission(cordova.getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(cordova.getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                callbackContext.error("WRITE_EXTERNAL_STORAGE Permission is required to save the document");
            } else {
                ActivityCompat.requestPermissions(cordova.getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            callbackContext.success();
        }
    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callbackContext.success();
            } else {
                callbackContext.error("WRITE_EXTERNAL_STORAGE Permission is required to save the document");
            }
        }
    }*/

    /**
     * Login with UAE Pass and get the access Code.
     */
    private void getCode() {
        cordova.getActivity().runOnUiThread(() -> {
            UAEPassAccessTokenRequestModel requestModel = uaePassRequestModels.getAuthenticationRequestModel(cordova.getActivity());
            UAEPassController.INSTANCE.getAccessCode(cordova.getActivity(), requestModel, (code, error) -> {
                if (error != null) {
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,error));
                } else {
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,code));
                }
            });
        });
    }

    /**
     * Login with UAE Pass and get the access token.
     */
    private void getAccessToken() {
        cordova.getActivity().runOnUiThread(() -> {
            UAEPassAccessTokenRequestModel requestModel = uaePassRequestModels.getAuthenticationRequestModel(cordova.getActivity());
            UAEPassController.INSTANCE.getAccessToken(cordova.getActivity(), requestModel, (accessToken, state, error) -> {
                if (error != null) {
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,error));
                } else {
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,accessToken));
                }
            });
        });
    }

    /**
     * Get User Profile from UAE Pass.
     */
    private void getProfile() {
        cordova.getActivity().runOnUiThread(() -> {
            UAEPassProfileRequestModel requestModel = uaePassRequestModels.getProfileRequestModel(cordova.getActivity());
            UAEPassController.INSTANCE.getUserProfile(cordova.getActivity(), requestModel, (profileModel,state, error) -> {
                if (error != null) {
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,error));
                } else {
                    try {
                        assert profileModel != null;
                        JSONObject profile = new JSONObject();
                        profile.put("ACR",profileModel.getAcr());
                        profile.put("AMR",profileModel.getAmr());
                        profile.put("DOB",profileModel.getDob());
                        profile.put("CardHolderSignatureImage",profileModel.getCardHolderSignatureImage());
                        profile.put("Domain",profileModel.getDomain());
                        profile.put("Email",profileModel.getEmail());
                        profile.put("FirstNameEN",profileModel.getFirstnameEN());
                        profile.put("Gender",profileModel.getGender());
                        profile.put("HomeAddressEmirateCode",profileModel.getHomeAddressEmirateCode());
                        profile.put("IDN",profileModel.getIdn());
                        profile.put("LastnameEN",profileModel.getLastnameEN());
                        profile.put("Mobile",profileModel.getMobile());
                        profile.put("NationalityEN",profileModel.getNationalityEN());
                        profile.put("Photo",profileModel.getPhoto());
                        profile.put("Sub",profileModel.getSub());
                        profile.put("UserType",profileModel.getUserType());
                        profile.put("UUID",profileModel.getUuid());

                        this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,profile.toString()));
                    } catch (JSONException e) {
                        this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,e.getLocalizedMessage()));
                    }
                }
            });
        });
    }

    /**
     * Sign Document Using UAE Pass.
     * @param documentUrl
     */
    private void signDocument(String documentUrl) {
        cordova.getActivity().runOnUiThread(() -> {
            downloadDocument(documentUrl);
        });
    }

    /**
     * Load Document Signing Json from assets.
     *
     * @return DocumentSigningRequestParams Mandatory Parameters
     */
    private DocumentSigningRequestParams loadDocumentSigningJson() {
        String json = null;
        try {
            InputStream is = cordova.getActivity().getAssets().open("signData.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return new Gson().fromJson(json, DocumentSigningRequestParams.class);
    }

    /**
     * Load PDF File from assets for signing.
     *
     * @return File PDF file.
     * @param documentUrl
     */
    private void downloadDocument(String documentUrl) {
        URL url = null;
        try {
            url = new URL(documentUrl);
        } catch (MalformedURLException e) {
            callbackContext.error(e.getLocalizedMessage());
            return;
        }

        String fileName = "PDF"+ Utils.INSTANCE.generateRandomString(24)+".pdf";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url + ""));
        request.setTitle(fileName);
        request.setMimeType("application/pdf");
        request.allowScanningByMediaScanner();
        request.setAllowedOverMetered(true);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setDestinationInExternalFilesDir(cordova.getContext(), android.os.Environment.DIRECTORY_DOCUMENTS, fileName);
        DownloadManager downloadManager = (DownloadManager) cordova.getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }

    /**
     * Download the signed document from UAE Pass.
     *
     * @param documentName Document Name with which the document will be saved after downloading.
     * @param documentURL  Document URL received after signing the document.
     */
    private void downloadDocument(final String documentName, final String documentURL) {
        cordova.getActivity().runOnUiThread(() -> {
            UAEPassDocumentDownloadRequestModel requestModel = uaePassRequestModels.getDocumentDownloadRequestModel(documentName, documentURL);
            UAEPassController.INSTANCE.downloadDocument(cordova.getActivity(), requestModel, (documentBytes, error) -> {
                boolean result = FileUtils.saveToExternalStorage(documentName, documentBytes);
                if (result) {
                    Toast.makeText(cordova.getActivity(), "File Successfully downloaded in Downloads folder.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(cordova.getActivity(), "File Download Failed.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /**
     * Clear Webview data to open UAE Pass app again.
     */
    private void clearData() {
        CookieManager.getInstance().removeAllCookies(value -> {

        });
        CookieManager.getInstance().flush();
    }


    //UAE PASS START -- Callback to handle UAE Pass callback
    @Override
     public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getData() != null) {
            if (BuildConfig.URI_SCHEME.equals(intent.getData().getScheme())) {
                UAEPassController.INSTANCE.resume(intent.getDataString());
            }
        }
    }
    //UAE PASS END -- Callback to handle UAE Pass callback

}
