name := """bulk-position-generator"""

version := "1.0"

scalaVersion := "2.11.6"

lazy val akkaVersion = "2.4.16"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "io.spray" %%  "spray-json" % "1.3.3",
  "com.vividsolutions" % "jts" % "1.13",
  "org.geotools" % "gt-geojson" % "11.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")


fork in run := true

resolvers += "it.geosolutions" at "http://maven.geo-solutions.it"
resolvers += "opengeo" at "http://repo.opengeo.org"
resolvers += "opengeo2" at "http://download.osgeo.org/webdav/geotools/"