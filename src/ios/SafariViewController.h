#import <Cordova/CDVPlugin.h>
#import <SafariServices/SafariServices.h>

@protocol ActivityItemProvider

- (NSArray<UIActivity *> *)safariViewController:(SFSafariViewController *)controller
                            activityItemsForURL:(NSURL *)URL
                                          title:(nullable NSString *)title;

@end

@interface SafariViewController : CDVPlugin <SFSafariViewControllerDelegate>

@property (nonatomic, copy) NSString* callbackId;
@property (nonatomic) bool animated;
@property (nonatomic) id<ActivityItemProvider> activityItemProvider;

- (void) isAvailable:(CDVInvokedUrlCommand*)command;
- (void) show:(CDVInvokedUrlCommand*)command;
- (void) hide:(CDVInvokedUrlCommand*)command;

@end
