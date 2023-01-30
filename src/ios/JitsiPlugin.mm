#import "JitsiPlugin.h"
#import <JitsiMeetSDK/JitsiMeetConferenceOptions.h>

@implementation JitsiPlugin
{
    CDVPluginResult *pluginResult;
}

- (void)join:(CDVInvokedUrlCommand *)command {
    NSString* serverUrl = [command.arguments objectAtIndex:0];
    NSString* room = [command.arguments objectAtIndex:1];
    Boolean isAudioOnly = [[command.arguments objectAtIndex:2] boolValue];
    NSString* token = [command.arguments objectAtIndex:3];
    commandBack = command;
    jitsiMeetView = [[JitsiMeetView alloc] initWithFrame:self.viewController.view.frame];
    jitsiMeetView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    jitsiMeetView.delegate = self;
    
    JitsiMeetConferenceOptions *options = [JitsiMeetConferenceOptions fromBuilder:^(JitsiMeetConferenceOptionsBuilder *builder) {
        builder.serverURL = [NSURL URLWithString: serverUrl];
        builder.room = room;
        builder.subject = @" ";
        builder.token = token;
        builder.welcomePageEnabled = NO;
        builder.audioOnly = isAudioOnly;
        [builder setFeatureFlag:@"chat.enabled" withBoolean:true];
        [builder setFeatureFlag:@"invite.enabled" withBoolean:false];
        [builder setFeatureFlag:@"add-people.enabled" withBoolean:false];
        [builder setFeatureFlag:@"calendar.enabled" withBoolean:false];
        [builder setFeatureFlag:@"pip.enabled" withBoolean:true];
        [builder setFeatureFlag:@"call-integration.enabled" withBoolean:false];
        [builder setFeatureFlag:@"close-captions.enabled" withBoolean:false];
        [builder setFeatureFlag:@"ios.recording.enabled" withBoolean:false];
        [builder setFeatureFlag:@"kick-out.enabled" withBoolean:false];
        [builder setFeatureFlag:@"live-streaming.enabled" withBoolean:false];
        [builder setFeatureFlag:@"meeting-name.enabled" withBoolean:false];
        [builder setFeatureFlag:@"meeting-password.enabled" withBoolean:false];
        [builder setFeatureFlag:@"raise-hand.enabled" withBoolean:false];
        [builder setFeatureFlag:@"recording.enabled" withBoolean:false];
        [builder setFeatureFlag:@"server-url-change.enabled" withBoolean:false];
        [builder setFeatureFlag:@"video-share.enabled" withBoolean:false];
        [builder setFeatureFlag:@"help.enabled" withBoolean:false];
        [builder setFeatureFlag:@"lobby-mode.enabled" withBoolean:false];
        [builder setFeatureFlag:@"server-url-change.enabled" withBoolean:false];
    }];
    
    [jitsiMeetView join: options];
    [self.viewController.view addSubview:jitsiMeetView];
}


- (void)destroy:(CDVInvokedUrlCommand *)command {
    if(jitsiMeetView){
        [jitsiMeetView removeFromSuperview];
        jitsiMeetView = nil;
    }
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"DESTROYED"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

void _onJitsiMeetViewDelegateEvent(NSString *name, NSDictionary *data) {
    NSLog(
        @"[%s:%d] JitsiMeetViewDelegate %@ %@",
        __FILE__, __LINE__, name, data);

}

- (void)conferenceFailed:(NSDictionary *)data {
    _onJitsiMeetViewDelegateEvent(@"CONFERENCE_FAILED", data);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"CONFERENCE_FAILED"];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandBack.callbackId];
}

- (void)conferenceJoined:(NSDictionary *)data {
    _onJitsiMeetViewDelegateEvent(@"CONFERENCE_JOINED", data);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"CONFERENCE_JOINED"];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandBack.callbackId];
}

- (void)conferenceLeft:(NSDictionary *)data {
    _onJitsiMeetViewDelegateEvent(@"CONFERENCE_LEFT", data);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"CONFERENCE_LEFT"];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandBack.callbackId];

}

- (void)conferenceWillJoin:(NSDictionary *)data {
    _onJitsiMeetViewDelegateEvent(@"CONFERENCE_WILL_JOIN", data);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"CONFERENCE_WILL_JOIN"];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandBack.callbackId];
}

- (void)conferenceTerminated:(NSDictionary *)data {
    _onJitsiMeetViewDelegateEvent(@"CONFERENCE_TERMINATED", data);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"CONFERENCE_TERMINATED"];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandBack.callbackId];
}

- (void)loadConfigError:(NSDictionary *)data {
    _onJitsiMeetViewDelegateEvent(@"LOAD_CONFIG_ERROR", data);
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"LOAD_CONFIG_ERROR"];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:commandBack.callbackId];
}

//- (void)enterPictureInPicture:(NSDictionary *)data {
//}


@end
