# cordova-plugin-jitsimeet
Cordova plugin for Jitsi Meet React Native SDK. Works with both iOS and Android, and fixes the 64-bit binary dependency issue with Android found in previous versions of this plugin.

# Summary 
This is based on the repo by findmate, but I updated the JitsiMeet SDK and WebRTC framework to the latest version, to get all features working in a Cordova app.
The original repo is here: https://github.com/findmate/cordova-plugin-jitsimeet

# Installation
`cordova plugin add https://github.com/seamlink-dev/cordova-plugin-jitsimeet`

# Usage
```
const roomId = 'your-custom-room-id';

jitsiplugin.join('https://meet.jit.si/', roomId, false, (data) => {
	//CONFERENCE_WILL_JOIN
    //CONFERENCE_JOINED
    //CONFERENCE_TERMINATED
    //CONFERENCE_FINISHED
    //CONFERENCE_DESTROYED
    if (data === "CONFERENCE_TERMINATED") {
        jitsiplugin.destroy((data) => {
            // call finished
        }, f(err) => {
            console.log(err);
        });
    }
}, (err) => {
    console.log(err);
});
```
