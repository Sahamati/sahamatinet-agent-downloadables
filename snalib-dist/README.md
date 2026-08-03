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

# Node.js guidelines
Requires Node 22 or later. The dist directory is prebuilt and shared here, so no build step is needed on your side. Add the library to your package.json using a file path and install it:

```json
"dependencies": {
  "@sahamati/snalib": "file:/path/to/snalib-dist/snalib-node"
}
```

```
npm install
```

Alternatively you can rebuild the library from source using the commands below. Make sure that you are inside the snalib-node directory for this to work:

```
npm install
npm run build
```

It is recommended to replace the base URL and the identifiers in examples/basic/index.ts with your own before running the example.

# .NET guidelines
Requires the .NET 8 SDK. The package is prebuilt and shared here in snalib-dotnet/dist. NuGet cannot install a .nupkg from a bare file path, so expose that folder as a package source using a nuget.config beside your solution:

```xml
<configuration>
  <packageSources>
    <add key="sahamati-local" value="/path/to/snalib-dist/snalib-dotnet/dist" />
  </packageSources>
</configuration>
```

```
dotnet add package Sahamati.SnaLib --version 1.0.0
```

Alternatively you can build the library and run the example from scratch using the commands below. Make sure that you are inside the snalib-dotnet directory for this to work:

```
dotnet build SnaLib.csproj -c Release
dotnet run --project examples/basic/Example.csproj
```

It is recommended to replace the base URL and the identifiers in examples/basic/Program.cs with your own before running the example.