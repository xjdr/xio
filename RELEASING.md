
# did you remember to setup your bintray credentials?

```
$ cat ~/.gradle/gradle.properties
bintray_user=BINTRAY_USER_NAME
bintray_apikey=BINTRAY_API_KEY
```

# verify your bintray config

```
$ ./gradlew  verifyBintrayConfig

> Configure project :xio
-Xbootclasspath/p/Users/bvjp/.gradle/caches/modules-2/files-2.1/org.mortbay.jetty.alpn/alpn-boot/8.1.12.v20180117/2e64c9c4f641ea8da95276313844c53a4519fc18/alpn-boot-8.1.12.v20180117.jar

> Task :verifyBintrayConfig
bintray_user: pdex
bintray_apikey: 5a129669353ee8651e4eaee2edab2cec4815c5cf
bintray_dryrun: false

BUILD SUCCESSFUL in 0s
1 actionable task: 1 executed
```

# tag the release and push to bintray

```
$ ./gradlew release
```

## gradle output

FYI you will be prompted twice. Once for the tag you are about to create and once for the new snapshot tag.

```

> Configure project :xio
-Xbootclasspath/p/Users/bvjp/.gradle/caches/modules-2/files-2.1/org.mortbay.jetty.alpn/alpn-boot/8.1.12.v20180117/2e64c9c4f641ea8da95276313844c53a4519fc18/alpn-boot-8.1.12.v20180117.jar

> Configure project :xio-parent:xio
-Xbootclasspath/p/Users/bvjp/.gradle/caches/modules-2/files-2.1/org.mortbay.jetty.alpn/alpn-boot/8.1.12.v20180117/2e64c9c4f641ea8da95276313844c53a4519fc18/alpn-boot-8.1.12.v20180117.jar

> Task :xio-parent:confirmReleaseVersion

??> This release version: [0.13.11]  (WAITING FOR INPUT BELOW)
<-------------> 0% EXECUTING [13s]

> Configure project :xio-parent:xio-parent:xio
-Xbootclasspath/p/Users/bvjp/.gradle/caches/modules-2/files-2.1/org.mortbay.jetty.alpn/alpn-boot/8.1.12.v20180117/2e64c9c4f641ea8da95276313844c53a4519fc18/alpn-boot-8.1.12.v20180117.jar

> Task :xio-parent:xio-parent:imWithStupid
Initialized native services in: /Users/bvjp/.gradle/native
Removing 0 daemon stop events from registry
Previous Daemon (27295) stopped at Tue May 01 09:06:02 PDT 2018 other compatible daemons were started and after being idle for 0 minutes and not recently used
Previous Daemon (53843) stopped at Tue May 01 09:11:45 PDT 2018 other compatible daemons were started and after being idle for 0 minutes and not recently used
Previous Daemon (54604) stopped at Tue May 01 09:13:45 PDT 2018 other compatible daemons were started and after being idle for 0 minutes and not recently used
Previous Daemon (55459) stopped at Tue May 01 09:50:04 PDT 2018 other compatible daemons were started and after being idle for 0 minutes and not recently used
Starting a Gradle Daemon, 1 busy and 4 stopped Daemons could not be reused, use --status for details
Starting process 'Gradle build daemon'. Working directory: /Users/bvjp/.gradle/daemon/4.7 Command: /Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/bin/java -XX:+HeapDumpOnOutOfMemoryError -Xmx1024m -Dfile.encoding=UTF-8 -Duser.country=US -Duser.language=en -Duser.variant -cp /Users/bvjp/.gradle/wrapper/dists/gradle-4.7-all/4cret0dgl5o3b21weaoncl7ys/gradle-4.7/lib/gradle-launcher-4.7.jar org.gradle.launcher.daemon.bootstrap.GradleDaemon 4.7
Successfully started process 'Gradle build daemon'
An attempt to start the daemon took 0.892 secs.
The client will now receive all logging from the daemon (pid: 60053). The daemon log file: /Users/bvjp/.gradle/daemon/4.7/daemon-60053.out.log
Starting build in new daemon [memory: 954.7 MB]
Closing daemon's stdin at end of input.
The daemon will no longer process any standard input.
Using 4 worker leases.
Starting Build
Settings evaluated using settings file '/Users/bvjp/Documents/repos/xio/settings.gradle'.
Projects loaded. Root project using build file '/Users/bvjp/Documents/repos/xio/build.gradle'.
Included projects: [root project 'xio-parent', project ':configuration-server', project ':xio', project ':xio-test']

> Configure project :
Evaluating root project 'xio-parent' using build file '/Users/bvjp/Documents/repos/xio/build.gradle'.

> Configure project :configuration-server
Evaluating project ':configuration-server' using build file '/Users/bvjp/Documents/repos/xio/configuration-server/build.gradle'.

> Configure project :xio-test
Evaluating project ':xio-test' using build file '/Users/bvjp/Documents/repos/xio/xio-test/build.gradle'.

> Configure project :xio
Evaluating project ':xio' using build file '/Users/bvjp/Documents/repos/xio/xio-core/build.gradle'.
-Xbootclasspath/p/Users/bvjp/.gradle/caches/modules-2/files-2.1/org.mortbay.jetty.alpn/alpn-boot/8.1.12.v20180117/2e64c9c4f641ea8da95276313844c53a4519fc18/alpn-boot-8.1.12.v20180117.jar
------------------------------------------------------------------------
Detecting the operating system and CPU architecture
------------------------------------------------------------------------
os.detected.name=osx
os.detected.arch=x86_64
os.detected.classifier=osx-x86_64
All projects evaluated.
Selected primary task 'bintrayUpload' from project :
Tasks to be executed: [task ':configuration-server:bintrayUpload', task ':xio:generatePomFileForMavenPublication', task ':xio:extractIncludeProto', task ':xio:extractProto', task ':xio:generateProto', task ':xio:compileJava', task ':xio:processResources', task ':xio:classes', task ':xio:jar', task ':xio:publishMavenPublicationToMavenLocal', task ':xio:bintrayUpload', task ':xio-test:generatePomFileForMavenPublication', task ':xio-test:compileJava', task ':xio-test:processResources', task ':xio-test:classes', task ':xio-test:jar', task ':xio-test:publishMavenPublicationToMavenLocal', task ':xio-test:bintrayUpload', task ':bintrayUpload']
:configuration-server:bintrayUpload (Thread[Task worker for ':',5,main]) started.

> Task :configuration-server:bintrayUpload
Task ':configuration-server:bintrayUpload' is not up-to-date because:
  Task has not declared any outputs despite executing actions.
Gradle Bintray Plugin version: 1.8.0
Skipping task 'configuration-server:bintrayUpload' because user or apiKey is null.
:configuration-server:bintrayUpload (Thread[Task worker for ':',5,main]) completed. Took 0.049 secs.
:xio:generatePomFileForMavenPublication (Thread[Task worker for ':',5,main]) started.

> Task :xio:generatePomFileForMavenPublication
Task ':xio:generatePomFileForMavenPublication' is not up-to-date because:
  Task.upToDateWhen is false.
:xio:generatePomFileForMavenPublication (Thread[Task worker for ':',5,main]) completed. Took 0.057 secs.
:xio:extractIncludeProto (Thread[Task worker for ':',5,main]) started.

> Task :xio:extractIncludeProto UP-TO-DATE
Skipping task ':xio:extractIncludeProto' as it is up-to-date.
:xio:extractIncludeProto (Thread[Task worker for ':',5,main]) completed. Took 0.11 secs.
:xio:extractProto (Thread[Task worker for ':',5,main]) started.

> Task :xio:extractProto UP-TO-DATE
Skipping task ':xio:extractProto' as it is up-to-date.
:xio:extractProto (Thread[Task worker for ':',5,main]) completed. Took 0.003 secs.
:xio:generateProto (Thread[Task worker for ':',5,main]) started.

> Task :xio:generateProto NO-SOURCE
file or directory '/Users/bvjp/Documents/repos/xio/xio-core/src/main/proto', not found
Skipping task ':xio:generateProto' as it has no source files and no previous output files.
:xio:generateProto (Thread[Task worker for ':',5,main]) completed. Took 0.003 secs.
:xio:compileJava (Thread[Task worker for ':' Thread 3,5,main]) started.

> Task :xio:compileJava UP-TO-DATE
file or directory '/Users/bvjp/Documents/repos/xio/xio-core/build/generated/source/proto/main/java', not found
file or directory '/Users/bvjp/Documents/repos/xio/xio-core/build/generated/source/proto/main/grpc', not found
Skipping task ':xio:compileJava' as it is up-to-date.
:xio:compileJava (Thread[Task worker for ':' Thread 3,5,main]) completed. Took 0.176 secs.
:xio:processResources (Thread[Task worker for ':' Thread 3,5,main]) started.

> Task :xio:processResources UP-TO-DATE
file or directory '/Users/bvjp/Documents/repos/xio/xio-core/src/main/proto', not found
Skipping task ':xio:processResources' as it is up-to-date.
:xio:processResources (Thread[Task worker for ':' Thread 3,5,main]) completed. Took 0.008 secs.
:xio:classes (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio:classes UP-TO-DATE
Skipping task ':xio:classes' as it has no actions.
:xio:classes (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.0 secs.
:xio:jar (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio:jar
Task ':xio:jar' is not up-to-date because:
  Output property 'archivePath' file /Users/bvjp/Documents/repos/xio/xio-core/build/libs/xio-0.13.10.jar has been removed.
:xio:jar (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.398 secs.
:xio:publishMavenPublicationToMavenLocal (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio:publishMavenPublicationToMavenLocal
Task ':xio:publishMavenPublicationToMavenLocal' is not up-to-date because:
  Task has not declared any outputs despite executing actions.
Publishing to maven local repository
:xio:publishMavenPublicationToMavenLocal (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.54 secs.
:xio:bintrayUpload (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio:bintrayUpload
Task ':xio:bintrayUpload' is not up-to-date because:
  Task has not declared any outputs despite executing actions.
Gradle Bintray Plugin version: 1.8.0
Version 'nordstromoss/test_maven/xio/0.13.11' does not exist. Attempting to create it...
Created version '0.13.11'.
Uploading to https://api.bintray.com/content/nordstromoss/test_maven/xio/0.13.11/com/xjeffrose/xio/0.13.11/xio-0.13.11.jar...
Uploaded to 'https://api.bintray.com/content/nordstromoss/test_maven/xio/0.13.11/com/xjeffrose/xio/0.13.11/xio-0.13.11.jar'.
Uploading to https://api.bintray.com/content/nordstromoss/test_maven/xio/0.13.11/com/xjeffrose/xio/0.13.11/xio-0.13.11.pom...
Uploaded to 'https://api.bintray.com/content/nordstromoss/test_maven/xio/0.13.11/com/xjeffrose/xio/0.13.11/xio-0.13.11.pom'.
:xio:bintrayUpload (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 4.935 secs.
:xio-test:generatePomFileForMavenPublication (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio-test:generatePomFileForMavenPublication
Task ':xio-test:generatePomFileForMavenPublication' is not up-to-date because:
  Task.upToDateWhen is false.
:xio-test:generatePomFileForMavenPublication (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.014 secs.
:xio-test:compileJava (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio-test:compileJava UP-TO-DATE
Skipping task ':xio-test:compileJava' as it is up-to-date.
:xio-test:compileJava (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.021 secs.
:xio-test:processResources (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio-test:processResources NO-SOURCE
file or directory '/Users/bvjp/Documents/repos/xio/xio-test/src/main/resources', not found
Skipping task ':xio-test:processResources' as it has no source files and no previous output files.
:xio-test:processResources (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.005 secs.
:xio-test:classes (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio-test:classes UP-TO-DATE
Skipping task ':xio-test:classes' as it has no actions.
:xio-test:classes (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.0 secs.
:xio-test:jar (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio-test:jar
Task ':xio-test:jar' is not up-to-date because:
  Output property 'archivePath' file /Users/bvjp/Documents/repos/xio/xio-test/build/libs/xio-test-0.13.5-SNAPSHOT.jar has been removed.
:xio-test:jar (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.046 secs.
:xio-test:publishMavenPublicationToMavenLocal (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio-test:publishMavenPublicationToMavenLocal
Task ':xio-test:publishMavenPublicationToMavenLocal' is not up-to-date because:
  Task has not declared any outputs despite executing actions.
Publishing to maven local repository
:xio-test:publishMavenPublicationToMavenLocal (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.123 secs.
:xio-test:bintrayUpload (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :xio-test:bintrayUpload
Task ':xio-test:bintrayUpload' is not up-to-date because:
  Task has not declared any outputs despite executing actions.
Gradle Bintray Plugin version: 1.8.0
Version 'nordstromoss/test_maven/xio-test/0.13.11' does not exist. Attempting to create it...
Created version '0.13.11'.
Uploading to https://api.bintray.com/content/nordstromoss/test_maven/xio-test/0.13.11/com/xjeffrose/xio-test/0.13.11/xio-test-0.13.11.jar...
Uploaded to 'https://api.bintray.com/content/nordstromoss/test_maven/xio-test/0.13.11/com/xjeffrose/xio-test/0.13.11/xio-test-0.13.11.jar'.
Uploading to https://api.bintray.com/content/nordstromoss/test_maven/xio-test/0.13.11/com/xjeffrose/xio-test/0.13.11/xio-test-0.13.11.pom...
Uploaded to 'https://api.bintray.com/content/nordstromoss/test_maven/xio-test/0.13.11/com/xjeffrose/xio-test/0.13.11/xio-test-0.13.11.pom'.
Published 'nordstromoss/test_maven/xio-test/0.13.11'.
Published 'nordstromoss/test_maven/xio/0.13.11'.
:xio-test:bintrayUpload (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 2.932 secs.
:bintrayUpload (Thread[Task worker for ':' Thread 2,5,main]) started.

> Task :bintrayUpload
Task ':bintrayUpload' is not up-to-date because:
  Task has not declared any outputs despite executing actions.
Gradle Bintray Plugin version: 1.8.0
Skipping task 'xio-parent:bintrayUpload' because user or apiKey is null.
:bintrayUpload (Thread[Task worker for ':' Thread 2,5,main]) completed. Took 0.002 secs.

BUILD SUCCESSFUL in 16s
15 actionable tasks: 10 executed, 5 up-to-date

> Task :xio-parent:updateVersion

??> Enter the next version (current one released as [0.13.11]): [0.13.12-SNAPSHOT]  (WAITING FOR INPUT BELOW)
<-------------> 0% EXECUTING [43s]

BUILD SUCCESSFUL in 48ssion
1 actionable task: 1 executed```
