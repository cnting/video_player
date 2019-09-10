####path:/lib/video_player.dart
#####fix:
* line:300 - line:320
* example

```
 ///iOS——fix
  Future<void> changeScreenOrientation(DeviceOrientation orientation) async {
    if (!value.initialized || _isDisposed) {
      return;
    }
    String o;
    switch (orientation) {
      case DeviceOrientation.portraitUp:
        o = 'portraitUp';
        break;
      case DeviceOrientation.portraitDown:
        o = 'portraitDown';
        break;
      case DeviceOrientation.landscapeLeft:
        o = 'landscapeLeft';
        break;
      case DeviceOrientation.landscapeRight:
        o = 'landscapeRight';
        break;
    }
    await _channel.invokeMethod<void>('change_screen_orientation', [o]);
  }

```

####path:ios/Classes/VideoPlayerPlugin.m
#####fix:
* line:9 - line:13
* example

```
static NSString* const METHOD_CHANGE_ORIENTATION = @"change_screen_orientation";
static NSString* const ORIENTATION_PORTRAIT_UP = @"portraitUp";
static NSString* const ORIENTATION_PORTRAIT_DOWN = @"portraitDown";
static NSString* const ORIENTATION_LANDSCAPE_LEFT = @"landscapeLeft";
static NSString* const ORIENTATION_LANDSCAPE_RIGHT = @"landscapeRight";

```

* line:489 - line:511
* methodName: - (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result{}
* example

```
else if ([METHOD_CHANGE_ORIENTATION isEqualToString:call.method]) {
      NSArray *arguments = call.arguments;
      NSString *orientation = arguments[0];
      bool isLandscape = NO;
      NSInteger iOSOrientation;
      if ([orientation isEqualToString:ORIENTATION_LANDSCAPE_LEFT]){
          iOSOrientation = UIDeviceOrientationLandscapeLeft;
          isLandscape = YES;
      }else if([orientation isEqualToString:ORIENTATION_LANDSCAPE_RIGHT]){
          iOSOrientation = UIDeviceOrientationLandscapeRight;
          isLandscape = YES;
      }else if ([orientation isEqualToString:ORIENTATION_PORTRAIT_DOWN]){
          iOSOrientation = UIDeviceOrientationPortraitUpsideDown;
          isLandscape = NO;
      }else{
          iOSOrientation = UIDeviceOrientationPortrait;
          isLandscape = NO;
      }
      [[NSUserDefaults standardUserDefaults] setBool:isLandscape forKey:@"videoPlayerPlugin_isLandscape"];
      [[NSUserDefaults standardUserDefaults] synchronize];
      result(nil);
      
  }

```