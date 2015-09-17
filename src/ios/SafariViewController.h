#import <Cordova/CDVPlugin.h>
#import <SafariServices/SafariServices.h>

@interface SafariViewController : CDVPlugin <SFSafariViewControllerDelegate>

- (void) isAvailable:(CDVInvokedUrlCommand*)command;
- (void) show:(CDVInvokedUrlCommand*)command;

@end