//
//  Service.swift
//  UaePassDemo
//
//  Created by Mohammed Gomaa on 12/27/18.
//  Copyright © 2018 Mohammed Gomaa. All rights reserved.
//

import Foundation
import Alamofire

@objc public class UAEPASSNetworkRequests: NSObject {
    
    @objc public static let shared = UAEPASSNetworkRequests()
    private override init() {}
    
    @objc public func getUAEPASSConfig(completion: @escaping () -> Void) {
        let path: String = UAEPASSRouter.shared.environmentConfig.configURL
        AF.request(path, method: .get ,encoding: URLEncoding.httpBody).responseString { response in
            if response.error != nil {
                completion()
                return
            }
            if let data = response.data {
                do {
                    let jsonDecoder = JSONDecoder()
                    let responseModel = try jsonDecoder.decode(UAEPASSConfig.self, from: data)
                    
                    if let authURl = responseModel.authURL {
                        UAEPASSRouter.shared.environmentConfig.authURL = authURl
                    }
                    if let tokenURl = responseModel.tokenURL {
                        UAEPASSRouter.shared.environmentConfig.tokenURL = tokenURl
                    }
                    if let profileURL = responseModel.profileURL {
                        UAEPASSRouter.shared.environmentConfig.profileURL = profileURL
                    }
                    print("####Auth url:\(UAEPASSRouter.shared.environmentConfig.authURL)")
                    completion()
                } catch {
                    debugPrint(error)
                    completion()
                }
            } else {
                completion()
            }
        }
    }
    // MARK: - Get UAE Pass Token
    @objc public func getUAEPassToken(code: String, completion: @escaping (UAEPassToken?) -> Void, onError: @escaping (ServiceErrorType) -> Void) {
        let path: String = UAEPassConfiguration.getServiceUrlForType(serviceType: .token)
        let parameters: [String: String] = ["grant_type": "authorization_code", "redirect_uri": UAEPASSRouter.shared.spConfig.redirectUriLogin, "code": code]
        
        let authUser = UAEPASSRouter.shared.environmentConfig.clientID
        let authPass = UAEPASSRouter.shared.environmentConfig.clientSecret
        let authStr = "\(authUser):\(authPass)"
        let authData = authStr.data(using: .ascii)!
        let authValue = "Basic \(authData.base64EncodedString(options: []))"
        
        let headers: HTTPHeaders = ["Authorization": authValue,
            "Accept": "application/json",
            "Content-Type": "application/x-www-form-urlencoded"]
        AF.request(path, method: .post, parameters: parameters, encoding: URLEncoding.httpBody, headers: headers).responseString { response in
            if response.error != nil {
                onError(.unAuthorizedUAEPass)
            }
            if let data = response.data {
                do {
                    let jsonDecoder = JSONDecoder()
                    let responseModel = try jsonDecoder.decode(UAEPassToken.self, from: data)
                    
                    if responseModel.error != nil {
                        if responseModel.error == "invalid_request" {
                            onError(.unAuthorizedUAEPass)
                        } else {
                            onError(.unknown)
                        }
                    } else {
                        UAEPASSRouter.shared.uaePassToken = responseModel.accessToken ?? nil
                        completion(responseModel)
                        print("### UAE Pass Token : \(responseModel.accessToken ?? "")")
                    }
                } catch {
                    debugPrint(error)
                    onError(.unAuthorizedUAEPass)
                }
            } else {
                completion(nil)
            }
        }
    }
    
    // MARK: - Get UAE Pass User Profile
    public func getUAEPassUserProfile(token: String, completion: @escaping (UAEPassUserProfile?) -> Void, onError: @escaping (ServiceErrorType) -> Void) {
        let path: String = UAEPassConfiguration.getServiceUrlForType(serviceType: .userProfileURL)
        let headers: HTTPHeaders = ["Authorization": "Bearer \(token)",
                       "Accept": "application/json",
                       "Content-Type": "application/x-www-form-urlencoded"]
        AF.request(path, method: .get, encoding: URLEncoding.httpBody, headers: headers).responseString { response in
            if response.error != nil {
                onError(.unAuthorizedUAEPass)
            }
            if let data = response.data {
                do {
                    let jsonDecoder = JSONDecoder()
                    let responseModel = try jsonDecoder.decode(UAEPassUserProfile.self, from: data)
                    completion(responseModel)
                    print("### UAE Pass User email : \(responseModel.email ?? "")")
                } catch {
                    debugPrint(error)
                    onError(.unableToFetchUserData)
                }
            } else {
                completion(nil)
            }
        }
    }
    
    // MARK: - Downloading the document -
    public func downloadPdf(pdfName: String, documentURL: String, completion: @escaping (String, Bool) -> Void, onError: @escaping (ServiceErrorType) -> Void) {
        let suggestedDestinationPath = DownloadRequest.suggestedDownloadDestination(for: .documentDirectory, in: .userDomainMask, options: [.removePreviousFile, .createIntermediateDirectories])

        AF.download(documentURL, method: .get, to: suggestedDestinationPath)
            .downloadProgress { progress in
                print("Download progress : \(progress)")
            }
            .responseData { response in
                print("response: \(response)")
                if response.response?.statusCode == 401 {
                    onError(.unAuthorizedUAEPass)
                    return
                }
                switch response.result {
                case .success:
                    if response.fileURL != nil, let filePath = response.fileURL?.absoluteString {
                        completion(filePath, true)
                    }
                case .failure:
                    completion("", false)
                }
        }
    }
    
    public func downloadSignedPdf(pdfID: String, pdfName: String, completion: @escaping (String, Bool) -> Void, onError: @escaping (ServiceErrorType) -> Void) {
        let downloadUrl: String = "\(UAEPASSRouter.shared.environmentConfig.txBaseURL)trustedx-resources/esignsp/v2/documents/\(pdfID)/content"
        let documentDirectory = try! FileManager.default
            .url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            .appendingPathComponent("DSPDFs")
        let fileUrl = documentDirectory.appendingPathComponent("\(pdfName).pdf")
        let destination: DownloadRequest.Destination = { _, response in
            return (fileUrl, [.removePreviousFile, .createIntermediateDirectories])
        }

        let uaePassSigningToken = UserDefaults.standard.string(forKey: "UAEPassSigningBearer") ?? ""
        let headers: HTTPHeaders = [
            "Authorization": "Bearer \(uaePassSigningToken)",
        ]
        print(downloadUrl)
        AF.download(downloadUrl, method: .get, headers: headers, to: destination)
            .downloadProgress { progress in
                print("Download progress : \(progress)")
            }
            .responseData { response in
                print("response: \(response)")
                if response.response?.statusCode == 401 {
                    onError(.unAuthorizedUAEPass)
                    return
                }
                switch response.result {
                case .success:
                    if response.fileURL != nil, let filePath = response.fileURL?.absoluteString {
                        completion(filePath, true)
                    }
                case .failure:
                    completion("", false)
                }
        }
    }
    
    public func generateSigningToken(requestData: UAEPassSigningRequest,
                              completion: @escaping (String?) -> Void,
                              onError: @escaping (ServiceErrorType) -> Void) {
        guard let urlRequest = self.buildUAEPASSSigningRequest(requestData: requestData) else {
            onError(.unknown)
            return
        }
        AF.request(urlRequest).responseJSON { response in
            
            if let error = response.error {
                debugPrint(error)
                return
            }
            if let data = response.data {
                do {
                    let str = String(data: data, encoding: .utf8) ?? ""
                    print(str)
                    guard let serviceType = requestData.serviceType else {
                        return
                    }
                    let jsonDecoder = JSONDecoder()
                    switch serviceType {
                    case .token, .tokenTX:
                        let responseModel = try jsonDecoder.decode(UAEPassToken.self, from: data)
                        UserDefaults.standard.set(responseModel.accessToken ?? "", forKey: "UAEPassSigningBearer")
                        completion("DONE")
                    case .deleteFile:
                        completion("DONE")
                    default:
                        break
                    }
                } catch { debugPrint(error)
                    onError(.optionaWrappingError)
                }
            }
        }
    }
    
    
    // MARK: - Uploading the document to UAE Pass -
    public func uploadDocument(requestData: UAEPassSigningRequest, pdfName: String, completionHandler: @escaping(UploadSignDocumentResponse?, Bool) -> Void) {
        
        let url = UAEPassConfiguration.getServiceUrlForType(serviceType: .uploadFile)
        let token = UserDefaults.standard.string(forKey: "UAEPassSigningBearer") ?? ""

        
        let headers: HTTPHeaders = [
            "Authorization": "Bearer \(token)",  /*in case you need authorization header */
            "Content-type": requestData.serviceType?.getContentType() ?? ""
        ]
        // swiftlint:disable next multiple_closures_with_trailing_closure
        AF.upload(multipartFormData: { (multipartFormData) in
            
            do {
                if let singingData = requestData.signingData, let documentURL = requestData.documentURL {
                    let jsonString = String(data: singingData, encoding: .utf8)!
                    multipartFormData.append(jsonString.data(using: String.Encoding.utf8)!, withName: "process" as String)
                    multipartFormData.append(documentURL, withName: "document")
                } else {
                    let paramsProcess = requestData.processParams
                    paramsProcess?.finishCallbackUrl = HandleURLScheme.externalURLSchemeSuccess()
                    let paramsJsonStrong = try JSONEncoder().encode(paramsProcess)
                    let jsonString = String(data: paramsJsonStrong, encoding: .utf8)!
                    multipartFormData.append(jsonString.data(using: String.Encoding.utf8)!, withName: "process" as String)
                    let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0]
                    let fileURL = URL(fileURLWithPath: documentsPath, isDirectory: true).appendingPathComponent(pdfName)
                    multipartFormData.append(fileURL, withName: "document")
                }
            } catch {
                print(error)
            }
            
        }, to: url, usingThreshold: UInt64.init(), method: .post,
        headers: headers).responseJSON(completionHandler: { result in
            if let error = result.error {
                print(error)
                completionHandler(nil, false)
            } else if let data = result.data {
                do {
                    let jsonDecoder = JSONDecoder()
                    let responseModel = try jsonDecoder.decode(UploadSignDocumentResponse.self, from: data)
                    let str = String(data: data, encoding: .utf8) ?? ""
                    print(str)
                    print("Succesfully uploaded")
                    completionHandler(responseModel, true)
                    return
                } catch {
                    completionHandler(nil, false)
                }
            }
        })
    }
    
    func buildUAEPASSSigningRequest(requestData: UAEPassSigningRequest) -> URLRequest? {
        do {
            guard let serviceType = requestData.serviceType else {
                return nil
            }
            let path: String = UAEPassConfiguration.getServiceUrlForType(serviceType: serviceType)
            guard let url = URL(string: path) else {
                return nil
            }
            print(url)
            
            var urlRequest = URLRequest(url: url)
            urlRequest.httpMethod = serviceType.getRequestType()
            if let contentType = serviceType.getContentType() {
                urlRequest.setValue(contentType, forHTTPHeaderField: "Content-type")
            }
            
            let authUser = UAEPASSRouter.shared.environmentConfig.clientID
            let authPass = UAEPASSRouter.shared.environmentConfig.clientSecret
            let authStr = "\(authUser):\(authPass)"
            let authData = authStr.data(using: .ascii)!
            let authValue = "Basic \(authData.base64EncodedString(options: []))"
            
            urlRequest.setValue(authValue, forHTTPHeaderField: "Authorization")
            let postData = NSMutableData(data: "grant_type=client_credentials".data(using: String.Encoding.utf8)!)
            if let signScope = UAEPASSRouter.shared.spConfig.signScope {
                postData.append("&scope=\(signScope)".data(using: String.Encoding.utf8)!)
            }
            urlRequest.httpBody = postData as Data
            
            debugPrint(urlRequest)
            return urlRequest
        }
    }
}
