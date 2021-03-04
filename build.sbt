import sbt.Keys.version
import xerial.sbt.Sonatype.GitHubHosting

// @formatter:off

lazy val _version       = "0.3.2.0-SNAPSHOT"
lazy val _scalaVersions = Seq("2.13.4", "2.12.13")
lazy val _scalaVersion  = _scalaVersions.head

name         := "ijp-javacv"
scalaVersion := _scalaVersion
publishArtifact     := false
skip in publish     := true
sonatypeProfileName := "net.sf.ij-plugins"

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform
// @formatter:off
val commonSettings = Seq(
  version      := _version,
  organization := "net.sf.ij-plugins",
  homepage     := Some(new URL("https://github.com/ij-plugins/ijp-javacv")),
  startYear    := Some(2002),
  licenses     := Seq(("LGPL-2.1", new URL("http://opensource.org/licenses/LGPL-2.1"))),
  //
  scalaVersion := _scalaVersion,
  //
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint", "-explaintypes"),
  javacOptions  ++= Seq("-deprecation", "-Xlint"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco"   % "javacpp"    % "1.5.5-SNAPSHOT"       withSources() withJavadoc(),
    "org.bytedeco"   % "javacpp"    % "1.5.5-SNAPSHOT"       classifier platform,
    "org.bytedeco"   % "javacv"     % "1.5.5-SNAPSHOT"       withSources() withJavadoc(),
    "org.bytedeco"   % "opencv"     % "4.5.1-1.5.5-SNAPSHOT" withSources() withJavadoc(),
    "org.bytedeco"   % "opencv"     % "4.5.1-1.5.5-SNAPSHOT" classifier platform,
    "org.bytedeco"   % "openblas"   % "0.3.13-1.5.5-SNAPSHOT" withSources() withJavadoc(),
    "org.bytedeco"   % "openblas"   % "0.3.13-1.5.5-SNAPSHOT" classifier platform,
    "net.imagej"     % "ij"         % "1.53g",
//    "com.beachape"  %% "enumeratum" % "1.5.13",
//    "mpicbg"         % "mpicbg"     % "1.1.1",
    // tests             
    "org.scalatest" %% "scalatest"  % "3.2.3"  % "test",
  ),
  scalacOptions in(Compile, doc) ++= Opts.doc.title("IJP JavaCV API"),
  scalacOptions in(Compile, doc) ++= Opts.doc.version(_version),
  scalacOptions in(Compile, doc) ++= Seq(
    "-doc-footer", s"IJP JavaCV API v.${_version}",
    "-doc-root-content", baseDirectory.value + "/src/main/scala/root-doc.creole"
  ),
  scalacOptions in(Compile, doc) ++= (
    Option(System.getenv("GRAPHVIZ_DOT_PATH")) match {
      case Some(path) => Seq("-diagrams", "-diagrams-dot-path", path, "-diagrams-debug")
      case None => Seq.empty[String]
    }),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    // ImageJ repo for mpicbg
    "ImageJ Releases" at "https://maven.imagej.net/content/repositories/releases/",
    // Use local maven repo for local javacv builds
    Resolver.mavenLocal
  ),
  //
  exportJars := true,
  //
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
  // Setup publishing
  publishMavenStyle := true,
  sonatypeProfileName := "net.sf.ij-plugins",
  sonatypeProjectHosting := Some(GitHubHosting("ij-plugins", "ijp-javacv", "jpsacha@gmail.com")),
  publishTo := sonatypePublishToBundle.value,
  developers := List(
    Developer(id="jpsacha", name="Jarek Sacha", email="jpsacha@gmail.com", url=url("https://github.com/jpsacha"))
  )
)
// @formatter:on

// Resolvers
lazy val sonatypeNexusSnapshots = Resolver.sonatypeRepo("snapshots")
lazy val sonatypeNexusStaging = "Sonatype Nexus Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

lazy val ijp_javacv_core =
  project
    .in(file("ijp-javacv-core"))
    .settings(
      commonSettings,
      name := "ijp-javacv-core",
      description := "IJP JavaCV Core"
    )

lazy val ijp_javacv_plugins =
  project
    .in(file("ijp-javacv-plugins"))
    .settings(
      commonSettings,
      name := "ijp-javacv-plugins",
      description := "IJP JavaCV ImageJ Plugins",
      publishArtifact := false,
      skip in publish := true,
    )
    .dependsOn(ijp_javacv_core)

lazy val examples =
  project
    .in(file("examples"))
    .settings(
      commonSettings,
      name := "examples",
      description := "IJP JavaCV Examples",
      publishArtifact := false,
      skip in publish := true,
    )
    .dependsOn(ijp_javacv_plugins)

// The 'experimental' is not a part of distribution.
// It is intended for ImageJ with plugins and fast local experimentation with new features.
lazy val experimental = project
  .in(file("experimental"))
  .settings(
    commonSettings,
    name := "experimental",
    // Do not publish this artifact
    publishArtifact := false,
    skip in publish := true,
    // Customize `sbt-imagej` plugin
    ijRuntimeSubDir := "sandbox",
    ijPluginsSubDir := "ij-plugins",
    ijCleanBeforePrepareRun := true,
    cleanFiles += ijPluginsDir.value,
  )
  .dependsOn(ijp_javacv_plugins)

addCommandAlias("ijRun", "experimental/ijRun")


// @formatter:off
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
// @formatter:on