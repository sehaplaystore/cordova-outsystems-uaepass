//
//  UAEPass.swift
//  HelloCordova
//
//  Created by Luis Bouça on 12/05/2022.
//

import Foundation
import UIKit
import NVActivityIndicatorView
import UAEPassClient

@available(iOS 13.0, *)
@objc(UAEPass) open class UAEPass: CDVPlugin {
    
    @objc public var uaePassAccessToken: String!
    @objc public var userPassCode: String!
    private var scope: String! = ""
    private var successSchemeUrl: String! = ""
    private var failSchemeUrl: String! = ""
    
    private var callbackid: String!
        
    @objc(initPlugin:) func initPlugin(command:CDVInvokedUrlCommand) {
        /// add your configuration
        let environment = command.arguments[0] as! String;
        let clientID = command.arguments[1] as! String;
        let clientSecret = command.arguments[2] as! String;
        let redirectUrl = command.arguments[3] as! String;

        switch(environment){
            case "PROD":
                UAEPASSRouter.shared.environmentConfig = UAEPassConfig(clientID: clientID, clientSecret: clientSecret, env: .production)
                break;
            default:
                UAEPASSRouter.shared.environmentConfig = UAEPassConfig(clientID: clientID, clientSecret: clientSecret, env: .qa)
                break;
        }
        UAEPASSRouter.shared.spConfig = SPConfig(redirectUriLogin: redirectUrl, // you entity return url.
                                                 scope: scope, // you entity return url.
                                                 state: generateState(), //Randomly Generated Code 24 alpha numeric.
                                                 successSchemeURL: successSchemeUrl, //your success scheme, ex: uaePassSuccess://.
                                                 failSchemeURL: failSchemeUrl, //your failure scheme, ex: uaePasssigningScopeFail://.
                                                 signingScope: "urn:safelayer:eidas:sign:process:document")
    }
    
    func generateState() -> String{
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
          return String((0..<24).map{ _ in letters.randomElement()! })
    }
    // MARK: - Getting -
    /// sample method just to be able to select environment type.
    @objc(getCode:) func getCode(command: CDVInvokedUrlCommand) {
        UAEPASSNetworkRequests.shared.getUAEPASSConfig(completion: {
            if let webVC = UAEPassWebViewController.instantiate() as? UAEPassWebViewController {
                webVC.urlString = UAEPassConfiguration.getServiceUrlForType(serviceType: .loginURL)
                webVC.onUAEPassSuccessBlock = {(code: String?) -> Void in
                    if let code = code {
                        self.userPassCode = code
                    }
                }
                webVC.onUAEPassFailureBlock = {(response: String?) -> Void in
                    
                }
                self.viewController.navigationController?.pushViewController(webVC, animated: true)
            }
        })
    }
        
    @objc(login:) func login(command: CDVInvokedUrlCommand) {
        UAEPASSNetworkRequests.shared.getUAEPassToken(code: userPassCode, completion: { (uaePassToken) in
            
            if( uaePassToken != nil) {
                self.uaePassAccessToken = uaePassToken?.accessToken
            }
        }) { (error) in
            //self.showErrorAlert(title: "Error", message: error.value())
        }
    }
    
    @objc(getProfile:) func getUaePassProfileForToken(command: CDVInvokedUrlCommand) {
        UAEPASSNetworkRequests.shared.getUAEPassUserProfile(token: uaePassAccessToken, completion: { (userProfile) in
            if let userProfile = userProfile {
                //self.showProfileDetails(userProfile: userProfile, userToken: token)
            } else {
                //self.showErrorAlert(title: "Error", message: "Couldn't get user profile, Please try again later")
            }
        }) { (error) in
            //self.showErrorAlert(title: "Error", message: error.value())
        }
    }
}

