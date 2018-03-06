#import "SafariViewController.h"

@implementation SafariViewController
{
  SFSafariViewController *vc;
}

- (void) isAvailable:(CDVInvokedUrlCommand*)command {
  bool avail = NSClassFromString(@"SFSafariViewController") != nil;
  CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:avail];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) show:(CDVInvokedUrlCommand*)command {
  NSDictionary* options = [command.arguments objectAtIndex:0];
  NSString* urlString = options[@"url"];
  if (urlString == nil) {
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"url can't be empty"] callbackId:command.callbackId];
    return;
  }
  if (![[urlString lowercaseString] hasPrefix:@"http"]) {
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"url must start with http or https"] callbackId:command.callbackId];
    return;
  }
  NSURL *url = [NSURL URLWithString:urlString];
  if (url == nil) {
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"bad url"] callbackId:command.callbackId];
    return;
  }
  bool readerMode = [options[@"enterReaderModeIfAvailable"] isEqual:[NSNumber numberWithBool:YES]];
  self.animated = [options[@"animated"] isEqual:[NSNumber numberWithBool:YES]];
  self.callbackId = command.callbackId;

  vc = [[SFSafariViewController alloc] initWithURL:url entersReaderIfAvailable:readerMode];
  vc.delegate = self;

  bool hidden = [options[@"hidden"] isEqualToNumber:[NSNumber numberWithBool:YES]];
  if (hidden) {
    vc.view.userInteractionEnabled = NO;
    vc.view.alpha = 0.05;
    [self.viewController addChildViewController:vc];
    [self.viewController.view addSubview:vc.view];
    [vc didMoveToParentViewController:self.viewController];
    vc.view.frame = CGRectMake(0.0, 0.0, 0.5, 0.5);
  } else {
    if (self.animated) {
      // note that Apple dropped support for other animations in iOS 9.2 or 9.3 in favor of a slide-back gesture
      vc.modalTransitionStyle = [self getTransitionStyle:options[@"transition"]];
    }
    [self.viewController presentViewController:vc animated:self.animated completion:nil];
  }

  NSString *tintColor = options[@"tintColor"];
  NSString *controlTintColor = options[@"controlTintColor"];
  NSString *barColor = options[@"barColor"];

  // if only tintColor is set, use that as the controlTintColor for iOS 10
  if (barColor == nil && controlTintColor == nil) {
    controlTintColor = tintColor;
  } else if (tintColor == nil) {
    tintColor = controlTintColor;
  }

  if (tintColor != nil) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 100000 // iOS 10.0 supported (compile time)
    if (IsAtLeastiOSVersion(@"10")) { // iOS 10.0 supported (runtime)
      vc.preferredControlTintColor = [self colorFromHexString:controlTintColor];
    } else {
      vc.view.tintColor = [self colorFromHexString:tintColor];
    }
#else
    vc.view.tintColor = [self colorFromHexString:tintColor];
#endif
  }

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 100000 // iOS 10.0 supported
  if (IsAtLeastiOSVersion(@"10")) { // iOS 10.0 supported (runtime)
    if (barColor != nil) {
      vc.preferredBarTintColor = [self colorFromHexString:barColor];
    }
  }
#endif

  CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{@"event":@"opened"}];
  [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
}

- (UIModalTransitionStyle) getTransitionStyle:(NSString*) input {
  if (input == nil) {
    return UIModalTransitionStyleCoverVertical;
  } else if ([input isEqualToString:@"curl"]) {
    return UIModalTransitionStylePartialCurl;
  } else if ([input isEqualToString:@"fade"]) {
    return UIModalTransitionStyleCrossDissolve;
  } else if ([input isEqualToString:@"flip"]) {
    return UIModalTransitionStyleFlipHorizontal;
  } else {
    return UIModalTransitionStyleCoverVertical;
  }
}

- (UIColor *)colorFromHexString:(NSString *)hexString {
    unsigned rgbValue = 0;
    NSScanner *scanner = [NSScanner scannerWithString:hexString];
    [scanner setScanLocation:1]; // bypass '#' character
    [scanner scanHexInt:&rgbValue];
    return [UIColor colorWithRed:((rgbValue & 0xFF0000) >> 16)/255.0 green:((rgbValue & 0xFF00) >> 8)/255.0 blue:(rgbValue & 0xFF)/255.0 alpha:1.0];
}

- (void) hide:(CDVInvokedUrlCommand*)command {
  SFSafariViewController *childVc = [self.viewController.childViewControllers lastObject];
  if (childVc != nil) {
    [childVc willMoveToParentViewController:nil];
    [childVc.view removeFromSuperview];
    [childVc removeFromParentViewController];
    childVc = nil;
  }
  
  if (vc != nil) {
    [vc dismissViewControllerAnimated:self.animated completion:nil];
    vc = nil;
  }
  [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

# pragma mark - SFSafariViewControllerDelegate

/*! @abstract Delegate callback called when the user taps the Done button.
    Upon this call, the view controller is dismissed modally.
 */
- (void)safariViewControllerDidFinish:(SFSafariViewController *)controller {
  if (self.callbackId != nil) {
    NSString * cbid = [self.callbackId copy];
    self.callbackId = nil;
    CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{@"event":@"closed"}];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:cbid];
  }
}

/*! @abstract Invoked when the initial URL load is complete.
    @param success YES if loading completed successfully, NO if loading failed.
    @discussion This method is invoked when SFSafariViewController completes the loading of the URL that you pass
    to its initializer. It is not invoked for any subsequent page loads in the same SFSafariViewController instance.
 */
- (void)safariViewController:(SFSafariViewController *)controller didCompleteInitialLoad:(BOOL)didLoadSuccessfully {
  if (self.callbackId != nil) {
    CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{@"event":@"loaded"}];
    [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
  }
}

- (NSArray<UIActivity *> *)safariViewController:(SFSafariViewController *)
              controller activityItemsForURL:(NSURL *)URL
              title:(nullable NSString *)title {

    if(self.activityItemProvider)
        return [self.activityItemProvider safariViewController:controller activityItemsForURL:URL title:title];
    else
        return nil;
}

@end
