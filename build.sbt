import sbt.Keys.version
// @formatter:off

lazy val _version       = "0.2.2.1-SNAPSHOT"
lazy val _scalaVersions = Seq("2.13.1", "2.12.10")
lazy val _scalaVersion  = _scalaVersions.head

name         := "ijp-javacv"
scalaVersion := _scalaVersion

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.getPlatform
val commonSettings = Seq(
  organization := "net.sf.ij-plugins",
  version      := _version,
  scalaVersion := _scalaVersion,
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint", "-explaintypes"),
  javacOptions  ++= Seq("-deprecation", "-Xlint"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco"   % "javacpp"    % "1.5.3"       withSources() withJavadoc(),
    "org.bytedeco"   % "javacpp"    % "1.5.3"       classifier platform,
    "org.bytedeco"   % "javacv"     % "1.5.3"       withSources() withJavadoc(),
    "org.bytedeco"   % "opencv"     % "4.3.0-1.5.3" withSources() withJavadoc(),
    "org.bytedeco"   % "opencv"     % "4.3.0-1.5.3" classifier platform,
    "org.bytedeco"   % "openblas"   % "0.3.9-1.5.3" withSources() withJavadoc(),
    "org.bytedeco"   % "openblas"   % "0.3.9-1.5.3" classifier platform,
    "net.imagej"     % "ij"         % "1.52n",
//    "com.beachape"  %% "enumeratum" % "1.5.13",
//    "mpicbg"         % "mpicbg"     % "1.1.1",
    // tests             
    "org.scalatest" %% "scalatest"  % "3.1.2"  % "test",
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    // ImageJ repo for mpicbg
    "ImageJ Releases" at "https://maven.imagej.net/content/repositories/releases/",
    // Use local maven repo for local javacv builds
    Resolver.mavenLocal
  ),
  autoCompilerPlugins := true,
  // fork a new JVM for 'run' and 'test:run'
  fork := true,
  // ImageJ Plugins
  ijRuntimeSubDir         := "sandbox",
  ijPluginsSubDir         := "ij-plugins",
  ijCleanBeforePrepareRun := true,
  cleanFiles              += ijPluginsDir.value,
  //
  manifestSetting,
  publishSetting
)

// Resolvers
lazy val sonatypeNexusSnapshots = Resolver.sonatypeRepo("snapshots")
lazy val sonatypeNexusStaging = "Sonatype Nexus Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

lazy val publishSetting = publishTo := {
  val version: String = _version
  if (version.trim.endsWith("SNAPSHOT"))
    Some(sonatypeNexusSnapshots)
  else
    Some(sonatypeNexusStaging)
}

lazy val ijp_javacv_core =
  project.in(file("ijp-javacv-core"))
    .settings(commonSettings,
      name := "ijp-javacv-core",
      description := "IJP JavaCV Core")

lazy val ijp_javacv_plugins =
  project.in(file("ijp-javacv-plugins"))
    .settings(commonSettings,
      name := "ijp-javacv-plugins",
      description := "IJP JavaCV ImageJ Plugins",
      publishArtifact := false)
    .dependsOn(ijp_javacv_core)

lazy val examples =
  project.in(file("examples"))
    .settings(commonSettings,
      name := "examples",
      description := "IJP JavaCV Examples",
      publishArtifact := false)
    .dependsOn(ijp_javacv_plugins)


// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> " }
publishArtifact := false

lazy val manifestSetting = packageOptions += {
  Package.ManifestAttributes(
    "Created-By"               -> "Simple Build Tool",
    "Built-By"                 -> Option(System.getenv("JAR_BUILT_BY"))
                                    .getOrElse(System.getProperty("user.name")),
    "Build-Jdk"                -> System.getProperty("java.version"),
    "Specification-Title"      -> name.value,
    "Specification-Version"    -> version.value,
    "Specification-Vendor"     -> organization.value,
    "Implementation-Title"     -> name.value,
    "Implementation-Version"   -> version.value,
    "Implementation-Vendor-Id" -> organization.value,
    "Implementation-Vendor"    -> organization.value
  )
}
pomExtra :=
  <scm>
    <url>https://github.com/ij-plugins/ijp-toolkit</url>
    <connection>scm:https://github.com/ij-plugins/ijp-toolkit.git</connection>
  </scm>
    <developers>
      <developer>
        <id>jpsacha</id>
        <name>Jarek Sacha</name>
        <url>https://github.com/jpsacha</url>
      </developer>
    </developers>