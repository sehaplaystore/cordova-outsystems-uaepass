//
//  UAEPass.swift
//  HelloCordova
//
//  Created by Luis Bouça on 02/05/2022.
//

import Foundation
import UIKit
import NVActivityIndicatorView
import UAEPassClient

@available(iOS 13.0, *)
@objc(UAEPass) open class UAEPass: CDVPlugin {
    
    private var scope: String!
    private var successSchemeUrl: String!
    private var failSchemeUrl: String!
    
    private var callbackid: String!
    
    private var webVC: UAEPassWebViewController!
        
    @objc(initPlugin:) func initPlugin(command:CDVInvokedUrlCommand) {
        /// add your configuration
        let environment = command.arguments[0] as! String;
        let clientID = command.arguments[1] as! String;
        let clientSecret = command.arguments[2] as! String;
        let redirectUrl = command.arguments[3] as! String;
        scope = "urn:uae:digitalid:profile:general";
        successSchemeUrl = "$success";
        failSchemeUrl = "$failure";

        switch(environment){
            case "PROD":
                UAEPASSRouter.shared.environmentConfig = UAEPassConfig(clientID: clientID, clientSecret: clientSecret, env: .production)
                break;
            default:
                UAEPASSRouter.shared.environmentConfig = UAEPassConfig(clientID: clientID, clientSecret: clientSecret, env: .staging)
                break;
        }
        UAEPASSRouter.shared.spConfig = SPConfig(redirectUriLogin: redirectUrl, // you entity return url.
                                                 scope: scope, // you entity return url.
                                                 state: generateState(), //Randomly Generated Code 24 alpha numeric.
                                                 successSchemeURL: successSchemeUrl, //your success scheme, ex: uaePassSuccess://.
                                                 failSchemeURL: failSchemeUrl, //your failure scheme, ex: uaePasssigningScopeFail://.
                                                 signingScope: "urn:safelayer:eidas:sign:process:document")
        commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_OK), callbackId: command.callbackId)
    }
    
    func generateState() -> String{
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
          return String((0..<24).map{ _ in letters.randomElement()! })
    }
    // MARK: - Getting -
    /// sample method just to be able to select environment type.
    @objc(getCode:) func getCode(command: CDVInvokedUrlCommand) {
        self.callbackid = command.callbackId;
        UAEPASSNetworkRequests.shared.getUAEPASSConfig(completion: {
            self.webVC = UAEPassWebViewController.instantiate() as? UAEPassWebViewController
            if self.webVC != nil {
                self.webVC.urlString = UAEPassConfiguration.getServiceUrlForType(serviceType: .loginURL)
                self.webVC.onUAEPassSuccessBlock = {(code: String?) -> Void in
                    if let code = code {
                        self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_OK, messageAs: code), callbackId: self.callbackid)
                        self.webVC.dismiss(animated: true)
                    }
                }
                self.webVC.onUAEPassFailureBlock = {(response: String?) -> Void in
                    self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: response), callbackId: self.callbackid)
                    self.webVC.dismiss(animated: true)
                    
                }
                self.viewController.present(self.webVC, animated: true, completion: nil)
                self.webVC.reloadwithURL(url: self.webVC.urlString)
            }
        })
    }
        
    @objc(getAccessToken:) func getAccessToken(command: CDVInvokedUrlCommand) {
        let code = command.argument(at: 0) as! String
        self.callbackid = command.callbackId;
        UAEPASSNetworkRequests.shared.getUAEPassToken(code: code, completion: { (uaePassToken) in
            
            if( uaePassToken != nil) {
                self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_OK, messageAs: uaePassToken!.accessToken), callbackId: command.callbackId)
            }
        }) { (error) in
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error.rawValue), callbackId: command.callbackId)
        }
    }
    
    @objc(getProfile:) func getUaePassProfileForToken(command: CDVInvokedUrlCommand) {
        let accessToken = command.argument(at: 0) as! String
        self.callbackid = command.callbackId;
        UAEPASSNetworkRequests.shared.getUAEPassUserProfile(token: accessToken, completion: { (userProfile) in
            if let userProfile = userProfile {
                let profile = NSMutableDictionary();
                profile.setValue(userProfile.firstnameEN, forKey: "FirstNameEN")
                profile.setValue(userProfile.uuid, forKey: "UUID")
                profile.setValue(userProfile.acr, forKey: "ACR")
                profile.setValue(userProfile.amr, forKey: "AMR")
                profile.setValue(userProfile.cardHolderSignatureImage, forKey: "CardHolderSignatureImage")
                profile.setValue(userProfile.dob, forKey: "DOB")
                profile.setValue(userProfile.email, forKey: "Email")
                profile.setValue(userProfile.gender, forKey: "Gender")
                profile.setValue(userProfile.homeAddressEmirateCode, forKey: "HomeAddressEmirateCode")
                profile.setValue(userProfile.idn, forKey: "IDN")
                profile.setValue(userProfile.lastnameEN, forKey: "LastnameEN")
                profile.setValue(userProfile.mobile, forKey: "Mobile")
                profile.setValue(userProfile.nationalityEN, forKey: "NationalityEN")
                profile.setValue(userProfile.photo, forKey: "Photo")
                profile.setValue(userProfile.sub, forKey: "Sub")
                profile.setValue(userProfile.userType, forKey: "UserType")
                profile.setValue(userProfile.domain, forKey: "Domain")
                do{
                    let jsonData = try JSONSerialization.data(withJSONObject: profile)
                    let resultString =  NSString(data: jsonData, encoding: String.Encoding.utf8.rawValue)! as String
                    let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: resultString);
                    self.commandDelegate.send(result, callbackId: command.callbackId)
                }catch{
                    self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Unexpected error: \(error)."), callbackId: command.callbackId)
                }
                
                //self.showProfileDetails(userProfile: userProfile, userToken: token)
            } else {
                self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Couldn't get user profile, Please try again later"), callbackId: command.callbackId)
            }
        }) { (error) in
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error.value()), callbackId: command.callbackId)
        }
    }
    
    @objc(signDocument:) func signDocument(command: CDVInvokedUrlCommand) {
        // download and start signing
        print("download and start signing")
        let bundle = Bundle(for: type(of: self))
        guard let uaePassSigningParameters = ReadJSONHelper().getUAEPAssSigningParametersFrom(fileName: "signData", bundle) else { return }
            
        let downloadUrl: String = command.argument(at: 0) as! String//"http://www.africau.edu/images/default/sample.pdf"
        let fileName = "SamplePDF"+generateState()+".pdf"
        UAEPASSNetworkRequests.shared.downloadPdf(pdfName: fileName, documentURL: downloadUrl) { _, _ in
            var requestData = UAEPassSigningRequest()
            requestData.serviceType = UAEPassServiceType.tokenTX
            requestData.tokenParams = TokenParams.getInitialisedObject()
            requestData.processParams = uaePassSigningParameters
            
            UAEPASSNetworkRequests.shared.generateSigningToken(requestData: requestData) { response in
                if response == "DONE" {
                    UAEPASSNetworkRequests.shared.uploadDocument(requestData: requestData, pdfName: fileName) { responseSign, success in
                        if let response = responseSign, let document = response.documents?.first,let content = document.content,success == true {
                            if let range = content.range(of: "documents/") {
                                do{
                                    let jsonResponse = NSMutableDictionary()
                                    jsonResponse.setValue(content[range.upperBound...].trimmingCharacters(in: .whitespaces), forKey: "pdfName");
                                    jsonResponse.setValue(response.documents?[0].content?.slice(from:"documents/", to: "/content") ?? "", forKey: "pdfID");
                                    jsonResponse.setValue(response.tasks?.pending?.first?.url ?? "", forKey: "url");
                                    
                                    UAEPASSRouter.shared.uploadSignDocumentResponse = response
                                    
                                    let jsonData = try JSONSerialization.data(withJSONObject: jsonResponse)
                                    let resultString =  NSString(data: jsonData, encoding: String.Encoding.utf8.rawValue)! as String
                                    self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: resultString), callbackId: command.callbackId)
                                }catch{
                                    self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error.localizedDescription), callbackId: command.callbackId)
                                }
                            }
                        }
                    }
                }
            } onError: { error in
                self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error.rawValue), callbackId: command.callbackId)
            }

        } onError: { error in
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error.rawValue), callbackId: command.callbackId)
        }
    }
}

