Mobile Cloud Computing Group 4
==============================

## Deploy instruction

To build the Android apk and deploy the backend application on Google Cloud Flexible Engine simply run the following command in the root directory:

```sh
$ ./deploy.sh
```

Then you can find the apk file to be installed in the following directory:

```  
android/app/build/outputs/apk/debug
```

## File and folder layout

    .
    ├── android
    │   ├── ...
    │   ├── app
    │   │   ├── ...
    │   │   └── src                                  # Android app source files
    │   │   ├── ...
    │   └── ...
    ├── backend                              
    │   ├── app.js                                   # Main backend application
    │   ├── app.yaml                                 
    │   ├── cron.yaml
    │   ├── ...
    ├── deploy.sh                                    # Deploy script
    └── README.MD