# AndroidWifi (RN Bridge)

This sample project provides an example of how to include the Native Android Wifi library as React Native bridge:

## Installation

* **Step 1.** Copy the folder `MySampleApp/android/app/src/main/java/com/mysampleapp/WifiSharedModule` into your react native android project.


* **Step 2.** In `WifiSharedModule.java`, put your custom value to be returned by `getName()`.

* **Step 3.** In `MainApplication.java`, add your package to the `getPackages()` list: 
		
```JAVA
@Override
protected List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
            ...,
            new WifiSharedPackage()
    );
}
```


## Notes

* To update the [native android library](https://github.com/oth-libs/AndroidWifi), go to `MySampleApp/android/app/build.gradle` file and update `com.github.oth-libs:AndroidWifi` to the latest version.



