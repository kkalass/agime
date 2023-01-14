Known Issues after updating to target api level 33 (january 2023)
 * Google Drive backup support had to be removed
 * Backup to external storage does not seem to do anything
 * notifications / foreground service do not seem to do anything => changed with android 13 / api level 33, see https://developer.android.com/develop/ui/views/notifications/notification-permission#exemptions - going down to 32 fixes it for now
 * dropbox backup not working -> probably needs re-implementation with https://github.com/dropbox/dropbox-sdk-java#dropbox-for-java-tutorial
 
