import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import ComposeApp

final class PushAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    private var syncObserver: NSObjectProtocol?
    private var permissionRequestInFlight = false
    private var firebaseReady = false

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        guard Bundle.main.url(forResource: "GoogleService-Info", withExtension: "plist") != nil else {
            NSLog("HRMS push unavailable: GoogleService-Info.plist is missing from the app bundle.")
            return true
        }
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        firebaseReady = true
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        syncObserver = NotificationCenter.default.addObserver(forName: NSNotification.Name("HrmsRequestPushTokenSync"), object: nil, queue: .main) { [weak self] _ in
            self?.requestRegistration()
        }
        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        guard firebaseReady else { return }
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if [.authorized, .provisional, .ephemeral].contains(settings.authorizationStatus) {
                DispatchQueue.main.async { application.registerForRemoteNotifications() }
            }
        }
    }

    private func requestRegistration() {
        guard firebaseReady, !permissionRequestInFlight else { return }
        permissionRequestInFlight = true
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] granted, error in
            DispatchQueue.main.async {
                self?.permissionRequestInFlight = false
                guard error == nil, granted else {
                    NSLog("HRMS push permission is disabled. Enable notifications in iPhone Settings.")
                    return
                }
                UIApplication.shared.registerForRemoteNotifications()
                // Request an FCM token only after APNs has registered this installation.
                if Messaging.messaging().apnsToken != nil { self?.refreshMessagingToken() }
            }
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        guard firebaseReady else { return }
        // Swizzling is disabled: explicitly associate the APNs and FCM registration tokens.
        Messaging.messaging().apnsToken = deviceToken
        refreshMessagingToken()
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        NSLog("HRMS APNs registration failed: %@", error.localizedDescription)
    }

    private func refreshMessagingToken() {
        Messaging.messaging().token { token, error in
            guard error == nil, let token = token, !token.isEmpty else {
                NSLog("HRMS Firebase registration is not ready; it will retry when the app resumes.")
                return
            }
            DispatchQueue.main.async { IosPushBridgeKt.acceptIosPushToken(token: token) }
        }
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard messaging.apnsToken != nil, let token = fcmToken, !token.isEmpty else { return }
        DispatchQueue.main.async { IosPushBridgeKt.acceptIosPushToken(token: token) }
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        // iOS otherwise hides a foreground push even when APNs delivery succeeded.
        completionHandler([.banner, .list, .sound, .badge])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        UIApplication.shared.applicationIconBadgeNumber = 0
        completionHandler()
    }
}
