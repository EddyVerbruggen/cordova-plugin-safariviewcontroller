#import <Cordova/CDVPlugin.h>
#import <SafariServices/SafariServices.h>

@interface SafariViewController : CDVPlugin <SFSafariViewControllerDelegate>

@property (nonatomic, copy) NSString* callbackId;
@property (nonatomic) bool animated;

- (void) isAvailable:(CDVInvokedUrlCommand*)command;
- (void) show:(CDVInvokedUrlCommand*)command;
- (void) hide:(CDVInvokedUrlCommand*)command;

@end