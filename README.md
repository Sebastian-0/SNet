# SNet
This networking library is aimed at making it easier to integrate online-content into programs without having to rewrite the basic
communication system each time. The system is based on the client-server architecture and can manage an arbitrary amount of 
connections simultaneously. The server automatically keeps track of the latency of all the clients and will disconnect them if 
they don't respond in a given period of time.
<p>
The system will go through a bunch of changes in the future to make it more customizable and efficient. Nevertheless, the current
version is stable and works just fine.
</p>

## Adding to your build
To add a dependency on SNet using Gradle, use the following:
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Sebastian-0:SNet:1.0.0'
}
```

Or using Maven:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Sebastian-0</groupId>
    <artifactId>SNet</artifactId>
    <version>1.0.0</version>
</dependency>
```

## License
This networking library is free to use as long as you comply to the GNU GPL v2 license (see LICENSE.txt for details). For clarification, you may compile this library into a jar archive and include it as a dependency in any project of your own, commercial or non-commercial, as long as credit is given to me. Furthermore, I reserve the exclusive right to re-license this library, either for use in specific projects or for public use. 

## Documentation
This may be added at a later date, given that I have time to write it.