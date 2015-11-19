SafariViewController Cordova Plugin
===================================
by Eddy Verbruggen - [@eddyverbruggen](https://twitter.com/eddyverbruggen)

## 0. Index

1. [Description](#1-description)
2. [Screenshots](#2-screenshots)
3. [Installation](#3-installation)
4. [Usage](#4-usage)
5. [Advantages over InAppBrowser](#5-advantages-over-inappbrowser)

## 1. Description
* Use in cases where you'd otherwise use InAppBrowser
* Use the new and powerful iOS9 viewcontroller to show webcontent in your PhoneGap app
* Requires XCode 7 / iOS9 SDK to build
* Requires iOS9 to use, lower versions need to fall back to InAppBrowser (example below!)

Note that I didn't decide to clobber window.open to override InAppBrowser when applicable
because that would mean you could never use InAppBrowser in case you need its advanced features
in one place and are happy with a simple readonly view in other cases.

## 2. Screenshots
As you can see from these shots: you can preload a page in reader mode or normal mode,
and Safari gives you the option to use the share sheet!

Pressing 'Done' returns the user to your app as you'd expect.

<img src="screenshots/01-demoapp.PNG" width="350"/>&nbsp;
<img src="screenshots/02-demoapp.PNG" width="350"/>&nbsp;
<img src="screenshots/03-demoapp.PNG" width="350"/>&nbsp;
<img src="screenshots/04-demoapp.PNG" width="350"/>&nbsp;

## 3. Installation
To install the plugin with the Cordova CLI from npm:

```
$ cordova plugin add cordova-plugin-safariviewcontroller
```

### Graceful fallback to InAppBrowser
Since SafariViewController is new in iOS9 you need to have a fallback for older versions (and other platforms),
so if `available` returns false (see the snippet below) you want to open the URL in the InAppBrowser probably,
so be sure to include that plugin as well:

```
$ cordova plugin add cordova-plugin-inappbrowser
```

I'm not including it as a depency as not all folks may have this requirement.

## 4. Usage
Check the [demo code](demo/index.html) for an easy to drop in example, otherwise copy-paste this:

```js
function openUrl(url, readerMode) {
  SafariViewController.isAvailable(function (available) {
    if (available) {
      SafariViewController.show({
            'url': url,
            'enterReaderModeIfAvailable': readerMode // default false
          },
          function(msg) {
            console.log("OK: " + msg);
          },
          function(msg) {
            alert("KO: " + msg);
          });

        SafariViewController.addEventListener("exit", function onExit() {
          console.log("Browser was dismissed");

          SafariViewController.removeEventListener("exit", onExit);
        });
    } else {
      // potentially powered by InAppBrowser because that (currently) clobbers window.open
      window.open(url, '_blank', 'location=yes');
    }
  })
}

function dismissSafari() {
  SafariViewController.hide()
}
```

## 5. Advantages over InAppBrowser
* InAppBrowser uses the slow UIWebView (even when you're using a WKWebView plugin!), this plugin uses the ultra fast Safari Webview.
* This is now Apple's recommended way to use a browser in your app.
* A nicer / cleaner UI which is consistent with Safari and all other apps using a `SFSafariViewController`.
* Since this is the system's main browser, assets like cookies are shared with your app, so the user is still logged on in his favorite websites.
* Whereas `cordova-plugin-inappbrowser` is affected by [ATS](https://developer.apple.com/library/prerelease/ios/technotes/App-Transport-Security-Technote/), this plugin is not. This means you can even load `http` URL's without whitelisting them.