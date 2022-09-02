# Tutorial: Run FLASH-MAS on your own device
Or, how to build an .apk and test FLASH-MAS on your personal Android phone.

# Windows

## Needed software
- Android Studio
- Java
- Gradle wrapper
- Git

You can install all of those components, with a simple installation [2] of Android Studio, by following these steps:
1. First, be sure to download the latest version: https://developer.android.com/studio
2.  If you downloaded an `.exe` file (recommended), double-click to launch it.
    If you downloaded a `.zip` file, unpack the ZIP, copy the **android-studio** folder into your **Program Files** folder, and then open the **android-studio > bin** folder and launch `studio64.exe` (for 64-bit machines) or `studio.exe` (for 32-bit machines).
3. Follow the setup wizard in Android Studio and install any SDK packages that it recommends
4. If you don't have Java installed, check the corresponding box during setup wizard. 

## Prep Repository
Next, clone the current repository on your local machine. <br>
`git clone https://github.com/andreiolaru-ro/FLASH-MAS` <br>
Change directory to the new repo. <br>
`cd FLASH-MAS` <br>
Checkout to the **android-deployment** branch. <br>
`git checkout android-deployment` <br>
Change directory into **android-deployment** folder <br> 
`cd android-deployment` <br>

## Build APK (debug)
You can execute all the build tasks available to your Android project using the **Gradle wrapper command line tool**. It's available as a batch file for Windows (gradlew.bat) and it's accessible from the root of each project you create with Android Studio. 

By default, there are two build types available for every Android app: one for debugging your app (*the debug build*) and one for releasing your app to users (*the release build*).  Although building an app bundle is the best way to package your app and upload it to the Play Console, building an APK is better suited for when you want quickly test a debug build or share your app as a deployable artifact with others.
To see a list of all available build tasks for your project, execute tasks: <br>
`gradlew tasks` <br>

So, we will build a simple APK debug build: <br>
`gradlew assembleDebug` <br>

This creates an APK named `app-debug.apk` in `FLASH-MAS\android-deployment\app\build\outputs\apk\debug` directory. The file is already signed with the debug key and aligned with zipalign, so you can immediately install it on a device. [1]

# Linux

On Linux environment, the steps are similar. For 64-bit machines, you will need additional libraries. More info at [2].
You can access the FLASH-MAS repository in a similar method as described for Windows.

## Build APK (debug)
Regarding the build of the APK, the steps are almost identical. So, after changing directory to the `android-deployment` folder, you can run <br>
`./gradlew assembleDebug` <br>
to create the APK file.

# References
[1] https://developer.android.com/studio/build/building-cmdline <br>
[2] https://developer.android.com/studio/install
