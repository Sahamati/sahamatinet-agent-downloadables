# snalib
SDK for easier integration with SNA
You can check the usage in the code snippet provided in examples for each language.

# Go guidlines
Use go replace directive to define the location for local pulling of the go library using go get. Otherwise, you can later use go get to pull the go library when the SDK get publicly published (this might take a while).

# Java guidelines
You can make your changes and build the java jar file from scratch using the commands below. Make sure that you are inside the java directory for this to work:

```
mvn package -q
mvn dependency:copy-dependencies -q
javac -cp "target/snalib-1.0.0.jar:target/dependency/*" examples/basic/Main.java -d target/example-classes
java -cp "target/snalib-1.0.0.jar:target/dependency/*:target/example-classes" examples.basic.Main
```

The target directory has been prebuilt and shared here. However, it is recommended to replace the entity Id and entity Secret in the examples/basic/Main.java and rebuild the example class using the commands shared.