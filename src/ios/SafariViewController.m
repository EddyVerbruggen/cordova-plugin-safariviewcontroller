#import "SafariViewController.h"

@implementation SafariViewController
{
  SFSafariViewController *vc;
  NSString *callbackId;
}

- (void) isAvailable:(CDVInvokedUrlCommand*)command {
  bool avail = NSClassFromString(@"SFSafariViewController") != nil;
  CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:avail];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) show:(CDVInvokedUrlCommand*)command {
  // testing safariviewcontroller --> requires an isAvailable function to check if isAtLeastVersion(9)
  NSDictionary* options = [command.arguments objectAtIndex:0];
  NSString* urlString = [options objectForKey:@"url"];
  if (urlString == nil) {
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"url can't be empty"] callbackId:command.callbackId];
    return;
  }
  NSURL *url = [NSURL URLWithString:urlString];
  bool readerMode = [[options objectForKey:@"enterReaderModeIfAvailable"] isEqualToNumber:[NSNumber numberWithBool:YES]];
  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];

  vc = [[SFSafariViewController alloc] initWithURL:url entersReaderIfAvailable:readerMode];
  vc.delegate = self;
  [self.viewController presentViewController:vc animated:YES completion:nil];

  callbackId = command.callbackId;
  [pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];

  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) hide:(CDVInvokedUrlCommand*)command {
  if (vc != nil) {
    [vc dismissViewControllerAnimated:YES completion:nil];
  }
  [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];

  // -safariViewControllerDidFinish: is not called when the controller is
  // hidden programmatically
  [self didDismissSafariViewController];
}

- (void) didDismissSafariViewController {
  if (callbackId != nil) {
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:@{ @"type": @"exit" }] callbackId:callbackId];
    callbackId = nil;
  }

  vc = nil;
}

# pragma mark - SFSafariViewControllerDelegate

/*! @abstract Delegate callback called when the user taps the Done button.
    Upon this call, the view controller is dismissed modally.
 */
- (void)safariViewControllerDidFinish:(SFSafariViewController *)controller {
  [self didDismissSafariViewController];
}

/*! @abstract Invoked when the initial URL load is complete.
    @param success YES if loading completed successfully, NO if loading failed.
    @discussion This method is invoked when SFSafariViewController completes the loading of the URL that you pass
    to its initializer. It is not invoked for any subsequent page loads in the same SFSafariViewController instance.
 */
- (void)safariViewController:(SFSafariViewController *)controller didCompleteInitialLoad:(BOOL)didLoadSuccessfully {
  // could emit event to JS, but don't see the usecase yet - perhaps check InAppBrowser impl
}

@end