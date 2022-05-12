package com.outsystems.uaepass;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import java.nio.charset.StandardCharsets;

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


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "initPlugin":
                uaePassRequestModels = new UAEPassRequestModels(
                        args.getString(0),
                        args.getString(1),
                        args.getString(2),
                        args.getString(3));
                return true;
            case "getWritePermission":
                getWritePermission(callbackContext);
                return true;
            case "getCode":
                getCode(callbackContext);
                return true;
            case "login":
                login(callbackContext);
                return true;
            case "getProfile":
                getProfile(callbackContext);
                return true;
            case "clearData":
                clearData(callbackContext);
                return true;
            default:
                return false;
        }
    }
    /**
     * Ask user for WRITE_EXTERNAL_STORAGE permission to save downloaded document.
     * @param callbackContext
     */
    private void getWritePermission(CallbackContext callbackContext) {
        if (ContextCompat.checkSelfPermission(cordova.getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(cordova.getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(cordova.getActivity(), "WRITE_EXTERNAL_STORAGE Permission is required to save the document", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(cordova.getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            signDocument();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                signDocument();
            } else {
                Toast.makeText(cordova.getActivity(), "WRITE_EXTERNAL_STORAGE Permission is required to save the document", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Login with UAE Pass and get the access Code.
     * @param callbackContext
     */
    private void getCode(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            UAEPassAccessTokenRequestModel requestModel = uaePassRequestModels.getAuthenticationRequestModel(cordova.getActivity());
            UAEPassController.INSTANCE.getAccessCode(cordova.getActivity(), requestModel, (code, error) -> {
                if (error != null) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,error));
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,code));
                }
            });
        });
    }

    /**
     * Login with UAE Pass and get the access token.
     * @param callbackContext
     */
    private void login(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            UAEPassAccessTokenRequestModel requestModel = uaePassRequestModels.getAuthenticationRequestModel(cordova.getActivity());
            UAEPassController.INSTANCE.getAccessToken(cordova.getActivity(), requestModel, (accessToken, state, error) -> {
                if (error != null) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,error));
                } else {
                    try {
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,new JSONObject("{\"accessToken\":\""+accessToken+"\",\"state\":\""+state+"\"}")));
                    } catch (JSONException e) {
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,e.getLocalizedMessage()));
                    }
                }
            });
        });
    }

    /**
     * Get User Profile from UAE Pass.
     * @param callbackContext
     */
    private void getProfile(CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            UAEPassProfileRequestModel requestModel = uaePassRequestModels.getProfileRequestModel(cordova.getActivity());
            UAEPassController.INSTANCE.getUserProfile(cordova.getActivity(), requestModel, (profileModel,state, error) -> {
                if (error != null) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,error));
                } else {
                    try {
                        assert profileModel != null;
                        JSONObject profile = new JSONObject();
                        profile.put("FullNameEN",profileModel.getFullnameEN());
                        profile.put("FullNameAR",profileModel.getFullnameAR());
                        profile.put("ACR",profileModel.getAcr());
                        profile.put("AMR",profileModel.getAmr());
                        profile.put("DOB",profileModel.getDob());
                        profile.put("CardHolderSignatureImage",profileModel.getCardHolderSignatureImage());
                        profile.put("Domain",profileModel.getDomain());
                        profile.put("Email",profileModel.getEmail());
                        profile.put("FirstNameAR",profileModel.getFirstnameAR());
                        profile.put("FirstNameEN",profileModel.getFirstnameEN());
                        profile.put("Gender",profileModel.getGender());
                        profile.put("HomeAddressAreaCode",profileModel.getHomeAddressAreaCode());
                        profile.put("HomeAddressAreaDescriptionAR",profileModel.getHomeAddressAreaDescriptionAR());
                        profile.put("HomeAddressAreaDescriptionEN",profileModel.getHomeAddressAreaDescriptionEN());
                        profile.put("HomeAddressCityCode",profileModel.getHomeAddressCityCode());
                        profile.put("HomeAddressCityDescriptionAR",profileModel.getHomeAddressCityDescriptionAR());
                        profile.put("HomeAddressCityDescriptionEN",profileModel.getHomeAddressCityDescriptionEN());
                        profile.put("HomeAddressEmirateCode",profileModel.getHomeAddressEmirateCode());
                        profile.put("HomeAddressEmirateDescriptionAR",profileModel.getHomeAddressEmirateDescriptionAR());
                        profile.put("HomeAddressEmirateDescriptionEN",profileModel.getHomeAddressEmirateDescriptionEN());
                        profile.put("HomeAddressMobilePhoneNumber",profileModel.getHomeAddressMobilePhoneNumber());
                        profile.put("HomeAddressPOBox",profileModel.getHomeAddressPOBox());
                        profile.put("HomeAddressTypeCode",profileModel.getHomeAddressTypeCode());
                        profile.put("IDN",profileModel.getIdn());
                        profile.put("IDType",profileModel.getIdType());
                        profile.put("LastnameAR",profileModel.getLastnameAR());
                        profile.put("LastnameEN",profileModel.getLastnameEN());
                        profile.put("Mobile",profileModel.getMobile());
                        profile.put("NationalityAR",profileModel.getNationalityAR());
                        profile.put("NationalityEN",profileModel.getNationalityEN());
                        profile.put("PassportNumber",profileModel.getPassportNumber());
                        profile.put("Photo",profileModel.getPhoto());
                        profile.put("Spuuid",profileModel.getSpuuid());
                        profile.put("Sub",profileModel.getSub());
                        profile.put("TitleAR",profileModel.getTitleAR());
                        profile.put("TitleEN",profileModel.getTitleEN());
                        profile.put("UserType",profileModel.getUserType());
                        profile.put("UUID",profileModel.getUuid());
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK,profile));
                    } catch (JSONException e) {
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR,e.getLocalizedMessage()));
                    }
                }
            });
        });
    }

    /**
     * Sign Document Using UAE Pass.
     */
    private void signDocument() {
        cordova.getActivity().runOnUiThread(() -> {
            final File file = loadDocumentFromAssets();
            DocumentSigningRequestParams documentSigningParams = loadDocumentSigningJson();
            UAEPassDocumentSigningRequestModel requestModel = uaePassRequestModels.getDocumentRequestModel(file, documentSigningParams);
            UAEPassController.INSTANCE.signDocument(cordova.getActivity(), requestModel, (spId, documentURL, error) -> {
                if (error != null) {
                    Toast.makeText(cordova.getActivity(), "Error while getting access token", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(cordova.getActivity(), "Document Signed Successfully", Toast.LENGTH_SHORT).show();
                    downloadDocument(file.getName(), documentURL);
                }
            });
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
            InputStream is = cordova.getActivity().getAssets().open("testSignData.json");
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
     */
    private File loadDocumentFromAssets() {
        File f = new File(cordova.getActivity().getFilesDir() + "/dummy.pdf");
        try {
            InputStream is = cordova.getActivity().getAssets().open("dummy.pdf");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(buffer);
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return f;
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
     * @param callbackContext
     */
    private void clearData(CallbackContext callbackContext) {
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
