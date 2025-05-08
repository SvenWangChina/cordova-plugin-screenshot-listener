#import <Cordova/CDV.h>

@interface ScreenshotListener : CDVPlugin
@end

@implementation ScreenshotListener

- (void)pluginInitialize {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(userDidTakeScreenshot)
                                                 name:UIApplicationUserDidTakeScreenshotNotification
                                               object:nil];
}

- (void)userDidTakeScreenshot {
    [self.commandDelegate evalJs:@"cordova.fireDocumentEvent('screenshotTaken');"];
}

@end
