rm -rf ./sync-server
sbt Universal/packageBin
unzip ./target/universal/dsl-apps-1.0.zip
mkdir sync-server
mv ./dsl-apps-1.0/* ./sync-server
rm -rf ./dsl-apps-1.0
echo "Build is prepared inside 'sync-server' folder. Don't forget to supply it with correct configuration, including SSH tokens for GitHub integration!"
