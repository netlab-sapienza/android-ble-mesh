## BE-MESH: Privacy policy

This is an open source Android app developed by Andrea Lacava, Pierluigi Locatelli and Gianluigi Nero while they were Master Student at Sapienza, University of Rome.
The source code is available on GitHub under the Apache license; the app is also available on Google Play.

We state, to the best of my knowledge and belief, that we have not programmed this app to collect any personally identifiable information. 
All data is the one used to create the BLE Mesh network and it is not stored in the app through each reboot.
The data is always store on your device device only, and can be simply erased by clearing the app's data or uninstalling it.

### Explanation of permissions requested in the app

The list of permissions required by the app can be found in the `AndroidManifest.xml` file [here](https://github.com/netlab-sapienza/android-ble-mesh/blob/2343404b060392829bb3e4303a2e7c318c8e65e3/app/src/main/AndroidManifest.xml).

<br/>

| Permission | Why it is required |
| :---: | --- |
| `android.permission.BLUETOOTH_ADMIN` and `android.permission.BLUETOOTH`| We use these permessions to access to the BLE device on the phone to connect to create the BLE Mesh network |
| `android.permission.ACCESS_COARSE_LOCATION` and  `android.permission.ACCESS_FINE_LOCATION`| This is required in some older phone to access bluetooth, we also use it to get a precise offeset to synch the BLE devices using the NTP protocol. |
| `android.permission.WRITE_EXTERNAL_STORAGE` | Required to save some state variables. Permission automatically granted by the system; can't be revoked by user. |
| `android.permission.ACCESS_NETWORK_STATE`  and`android.permission.INTERNET` | The only sensitive permissions that the app requests, and can be revoked by the system or the user at any time. This is required to send emails and tweets through our service. In the Google play version this feature is present but disabled. |

 <hr style="border:1px solid gray">

If you find any security vulnerability that has been inadvertently caused by us, or have any question regarding how the app protectes your privacy, please send us an email or post a issue on GitHub, and we will surely try to fix it/help you.

Yours sincerely,  
Netlab Team
andrea.lacava@uniroma1.it
