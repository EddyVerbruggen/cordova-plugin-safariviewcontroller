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
  NSURL *url = [NSURL URLWithString:urlString];
  bool readerMode = [options[@"enterReaderModeIfAvailable"] isEqualToNumber:[NSNumber numberWithBool:YES]];
  self.animated = [options[@"animated"] isEqualToNumber:[NSNumber numberWithBool:YES]];
  self.callbackId = command.callbackId;
  
  vc = [[SFSafariViewController alloc] initWithURL:url entersReaderIfAvailable:readerMode];
  vc.delegate = self;

  bool hidden = [options[@"hidden"] isEqualToNumber:[NSNumber numberWithBool:YES]];
  if (hidden) {
    vc.view.userInteractionEnabled = NO;
    vc.view.alpha = 0.0;
    [self.viewController addChildViewController:vc];
    [self.viewController.view addSubview:vc.view];
    [vc didMoveToParentViewController:self.viewController];
    vc.view.frame = CGRectZero;
  } else {
    if (self.animated) {
      vc.modalTransitionStyle = [self getTransitionStyle:options[@"transition"]];
      [self.viewController showViewController:vc sender:self];
    } else {
      [self.viewController presentViewController:vc animated:NO completion:nil];
    }
  }
  
  NSString *tintColor = options[@"tintColor"];
  if (tintColor != nil) {
    vc.view.tintColor = [self colorFromHexString:options[@"tintColor"]];
  }


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
    NSString * cbid = self.callbackId;
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

@end
