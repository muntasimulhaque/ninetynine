# Privacy Policy: Ninety Nine

Effective date: 1 August 2026

This app collects no data. Period.

- No personal information is collected, stored, or transmitted.
- No analytics, advertising, or tracking SDKs are included.
- **The app has no INTERNET permission.** It cannot open a network connection
  at all, not to the developer, not to anyone.
- The optional daily notification is generated locally on your device.

**About the permission list.** If you inspect the app you will see five
permissions rather than one. `POST_NOTIFICATIONS` is the app's own, and it is
only used if you turn the daily name on. The other four (`WAKE_LOCK`,
`ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE`)
come from Android's own WorkManager library, which is what schedules the daily
reminder and survives a reboot. A sixth, signature-level permission named
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, is added automatically by AndroidX;
it is granted only to the app itself and never appears in Settings. The app
sets no network conditions on any of its scheduled work, and none of these
permissions can move data off your device without INTERNET, which the app does
not have.

**On your device.** Your learned-names progress, your bookmarked names, theme,
text size, notification time, best quiz score and practice settings are kept in
the app's own storage. The app never sends them anywhere; it has no INTERNET
permission, so it cannot.

**Android's backup.** If you have Android's system backup switched on, Android
includes this app's data in your device backup, exactly as it does for other
apps, so that your progress survives moving to a new phone. Android does this,
not the app. It goes to your own Google account, and on Android 9 and later it
is encrypted with your device's screen lock, so neither Google nor the developer
can read it. On Android 12 and later the same data can also move directly from
one phone to another when you set up a new device, without going through the
cloud at all. You can switch backup off in Android Settings › System › Backup.

Contact: muntasim.haque@gmail.com
