//
//  UAEPassConfigQA.swift
//
//  Created by Mohammed Gomaa on 11/19/18.
//  Copyright © 2018 Mohammed Gomaa. All rights reserved.
//

import UIKit

@objc public class UAEPassConfig: NSObject {
    // MARK: **** UAE Pass Configuration ****
    var txBaseURL: String
    var authURL: String
    var tokenURL: String
    var txTokenURL: String
    var profileURL: String
    var clientID: String
    var clientSecret: String
    public var uaePassSchemeURL: String
    var configURL = ""
    /*
    Logout
    https://id.uaepass.ae/idshub/logout?redirect_uri= { url where to return after logout from UAE PASS}
     */

    @objc public required init(clientID: String, clientSecret: String, env: UAEPASSEnvirnonment) {
        self.clientID = clientID
        self.clientSecret = clientSecret
        switch env {
        case .production:
            configURL = "https://api.uaepass.ae/autoforms/view/idp/openid-configuration?client=\(clientID)"
            uaePassSchemeURL = "uaepass://"
            txBaseURL = BaseUrls.prodTX.get()
            authURL = "\(BaseUrls.prodTX.get())idshub/authorize"
            tokenURL = "\(BaseUrls.prodTX.get())idshub/token"
            txTokenURL = "\(BaseUrls.prodTX.get())trustedx-authserver/oauth/main-as/token"
            profileURL = "\(BaseUrls.prodTX.get())idshub/userinfo"
            
            //            txBaseURL = BaseUrls.qaTX.get()
            //            authURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as"
            //            tokenURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as/token"
            //            txTokenURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as/token"
            ////            signingTokenURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as/token"
            //            profileURL = "\(BaseUrls.qaTX.get())trustedx-resources/openid/v1/users/me"

        case .staging:
            configURL = "https://api.uaepass.ae/autoforms/view/idp/openid-configuration?client=\(clientID)"
            uaePassSchemeURL = "uaepassstg://"
            txBaseURL = BaseUrls.stgTX.get()
            authURL = "\(BaseUrls.stgTX.get())idshub/authorize"
            tokenURL = "\(BaseUrls.stgTX.get())idshub/token"
            txTokenURL = "\(BaseUrls.stgTX.get())trustedx-authserver/oauth/main-as/token"
            profileURL = "\(BaseUrls.stgTX.get())idshub/userinfo"
        case .qa:
            configURL = "https://qa-api.uaepass.ae/autoforms/view/idp/openid-configuration?client=\(clientID)"
            uaePassSchemeURL = "uaepassqa://"
            txBaseURL = BaseUrls.qaTX.get()
            authURL = "\(BaseUrls.qaTX.get())idshub/authorize"
            tokenURL = "\(BaseUrls.qaTX.get())idshub/token"
            txTokenURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as/token"
            profileURL = "\(BaseUrls.qaTX.get())idshub/userinfo"
            
//            txBaseURL = BaseUrls.qaTX.get()
//            authURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as"
//            tokenURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as/token"
//            txTokenURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as/token"
////            signingTokenURL = "\(BaseUrls.qaTX.get())trustedx-authserver/oauth/main-as/token"
//            profileURL = "\(BaseUrls.qaTX.get())trustedx-resources/openid/v1/users/me"

        case .dev:
            configURL = "https://dev-api.uaepass.ae/autoforms/view/idp/openid-configuration?client=\(clientID)"
            uaePassSchemeURL = "uaepassdev://"
            txBaseURL = BaseUrls.devTX.get()
            authURL = "\(BaseUrls.devTX.get())idshub/authorize"
            tokenURL = "\(BaseUrls.devTX.get())idshub/token"
            txTokenURL = "\(BaseUrls.devTX.get())trustedx-authserver/oauth/main-as/token"
            profileURL = "\(BaseUrls.devTX.get())idshub/userinfo"
        }
    }
}

