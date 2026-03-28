# android_unit3_2

### What is this example

A Volley example that contains the following example requests:

1. Upload Image as Multipart file

2. Image request - Download an bitmap image and display

---

## WebSocket Activity Instructions

### Step 1: Create the JAR File
For the third WebSocket activity, you'll need to create the JAR file because you are required to change the path in your image controller. Follow these steps:

- Open Backend folder and change path in line 16 of ImageController.java
- After saving the changes:

#### Option 1: Run the Backend File from the Title Bar
- Simply run the backend file directly from the title bar.

#### Option 2: Create the JAR File
1. Open a terminal in your project directory and run the following command:
   ```
   mvn clean package
   ```
2. If you encounter a build failure due to a class test, follow these additional steps:
   - Navigate to `test/java/../demoApplicationTest.java`.
   - Comment out everything in the file.
   - Run the `mvn clean package` command again.

3. This will generate the `backend_snapshot.jar` file in the `TARGET` folder.

4. To run the JAR file, change your directory to the `target` folder:
   ```
   cd target
   ```
   Then, run the JAR file using the following command:
   ```
   java -jar {jar file name}
   ```

---

## Adding More Images to the Emulator

To add more images to your emulator, follow these steps:

1. Open **Tools** > **Device Manager**.
2. Click the three vertical dots (settings) and select **Open in Device Explorer**.
3. In the Device Explorer, you can upload or drag-and-drop images from your local machine into the `sdcard/download` directory.
4. After uploading, the images will be available in the emulator’s **Files** app. Check on the images button.
4. The images will be available in the emulator’s **Google Photos** app. Open Google Photos to see and use the images.

Depending on different versions of emulator, there are different ways. More or less the procedure should be similar. 

The general idea is to upload images to the emulator's file system (typically the sdcard/download folder) and then access them via the emulator’s photo app.

---

### Important Notes

- Use `10.0.2.2` instead of `localhost` IF the server program is running on the same host as the Android Enmulator

- AndroidManifest.xml
    - add `<uses-permission android:name="android.permission.INTERNET" />` before `<application>`
    - add `android:usesCleartextTraffic="true"` inside `<application>`

- build.gradle (Module :app)
    - add `implementation 'com.android.volley:volley:1.2.1'` inside `dependencies{}`, then sync gradle.

### Version Tested

|Android Studio            | Android SDK | Gradle  | Gradle Plugin | Gradle JDK |                 Emulator                        |
|--------------------------|-------------|---------|---------------|------------|-------------------------------------------------|
|Ladybug 2024.2.2 Patch 1  |  35.3.12    | 8.10.2  |    8.8.1      |    21      | Pixel 3a API 34, Pixel 5 API 34, Pixel 7 API 32 |

|IntelliJ  | Project SDK | Springboot | Maven |
|----------|-------------|------------|-------|
|2023.2.2  |     17      | 3.1.4      | 3.6.3 |

Last Modified by : Dhvani Mistry
Last Modified on : March 9 2025