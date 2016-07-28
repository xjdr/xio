libraryDependencies += "com.google.code.findbugs" % "jsr305" % "3.0.1"
libraryDependencies += "com.google.guava" % "guava" % "19.0"
libraryDependencies += "io.netty" % "netty-all" % "4.1.0.Final"
libraryDependencies += "io.netty" % "netty-tcnative" % "1.1.33.Fork17"
libraryDependencies += "log4j" % "log4j" % "1.2.17"
libraryDependencies += "io.netty" % "netty-tcnative-boringssl-static" % "1.1.33.Fork17"
libraryDependencies += "org.apache.curator" % "curator-framework" % "3.1.0"
libraryDependencies += "org.apache.curator" % "curator-recipes" % "3.1.0"
// Testing
libraryDependencies += "junit" % "junit" % "4.12" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test
libraryDependencies += "org.eclipse.jetty" % "jetty-server" % "9.3.1.v20150714" % Test
libraryDependencies += "com.squareup.okhttp" % "okhttp" % "2.4.0" % Test
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % Test
libraryDependencies += "org.codehaus.groovy" % "groovy-all" % "2.4.1" % Test
// http://mvnrepository.com/artifact/org.slf4j/log4j-over-slf4j
libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.7.21"
// http://mvnrepository.com/artifact/ch.qos.logback/logback-classic
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7" % Test

unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "example"

lazy val Serial = config("serial") extend(Test)

parallelExecution in Serial := false

parallelExecution := true

fork := true

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

testOptions in Test += Tests.Setup( () => println("Setup") )

testOptions in Test += Tests.Cleanup( () => println("Cleanup") )
