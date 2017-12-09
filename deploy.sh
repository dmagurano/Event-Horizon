echo 'Building apk...'
./android/gradlew assembleDebug -p ./android 
echo -e '\nApk Build successful.'
echo -e '\nDeploying application on the backend(this may take several minutes)...'
gcloud auth activate-service-account --key-file=./backend/mcc-fall-2017-g04-35ff1cc15d6d.json 
gcloud config set project mcc-fall-2017-g04
gcloud app deploy ./backend/app.yaml ./backend/cron.yaml --quiet
echo -e '\nDeploy successful.'
echo -e '\nYou can find the apk in the folder android/app/build/outputs/apk/debug'