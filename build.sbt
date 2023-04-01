import sbt.Keys.version
import xerial.sbt.Sonatype.GitHubHosting

lazy val _version       = "0.5.0.1-SNAPSHOT"
lazy val _scalaVersions = Seq("3.2.2", "2.13.10")
lazy val _scalaVersion  = _scalaVersions.head

name := "ijp-javacv"
publishArtifact := false
publish / skip := true

ThisBuild / scalaVersion := _scalaVersion
ThisBuild / crossScalaVersions := _scalaVersions
ThisBuild / sonatypeProfileName := "net.sf.ij-plugins"
ThisBuild / version := _version
ThisBuild / organization := "net.sf.ij-plugins"
ThisBuild / homepage := Some(new URL("https://github.com/ij-plugins/ijp-javacv"))
ThisBuild / startYear := Some(2002)
ThisBuild / licenses := Seq(("LGPL-2.1", new URL("https://opensource.org/licenses/LGPL-2.1")))

def isScala2(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, _)) => true
    case _ => false
  }

// Platform classifier for native library dependencies
val platform       = org.bytedeco.javacpp.Loader.Detector.getPlatform
val commonSettings = Seq(
  //
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-unchecked",
    "-deprecation",
    "-feature",
    // Java 8 for compatibility with ImageJ/FIJI
    "-release",
    "8"
    ) ++ (
    if (isScala2(scalaVersion.value))
      Seq(
        "-explaintypes",
        "-Xsource:3",
        "-Wunused:imports,privates,locals",
        "-Wvalue-discard",
        "-Xlint",
        "-Xcheckinit",
        "-Xlint:missing-interpolator",
        "-Xlint:nonlocal-return",
        "-Ymacro-annotations",
        "-Ywarn-dead-code",
        "-Ywarn-unused:-patvars,_",
        )
    else
      Seq(
        "-explain",
        "-explain-types"
        )
    ),

  javacOptions ++= Seq("-deprecation", "-Xlint"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  // @formatter:off
  libraryDependencies ++= Seq(
    "org.bytedeco" % "javacpp"  % "1.5.8" withSources() withJavadoc(),
    "org.bytedeco" % "javacpp"  % "1.5.8" classifier platform,
    "org.bytedeco" % "javacv"   % "1.5.8" withSources() withJavadoc(),
    "org.bytedeco" % "opencv"   % "4.6.0-1.5.8" withSources() withJavadoc(),
    "org.bytedeco" % "opencv"   % "4.6.0-1.5.8" classifier platform,
    "org.bytedeco" % "openblas" % "0.3.21-1.5.8" withSources() withJavadoc(),
    "org.bytedeco" % "openblas" % "0.3.21-1.5.8" classifier platform,
    "net.imagej"   % "ij"       % "1.54c",
    //    "com.beachape"  %% "enumeratum" % "1.5.13",
    //    "mpicbg"         % "mpicbg"     % "1.1.1",
    // tests             
    "org.scalatest" %% "scalatest" % "3.2.15" % "test",
    ),
  // @formatter:on
  Compile / doc / scalacOptions ++= Opts.doc.title("IJP JavaCV API"),
  Compile / doc / scalacOptions ++= Opts.doc.version(_version),
  Compile / doc / scalacOptions ++= Seq(
    "-doc-footer", s"IJP JavaCV API v.${_version}",
    "-doc-root-content", baseDirectory.value + "/src/main/scala/root-doc.creole"
    ),
  Compile / doc / scalacOptions ++= (
    Option(System.getenv("GRAPHVIZ_DOT_PATH")) match {
      case Some(path) => Seq("-diagrams", "-diagrams-dot-path", path, "-diagrams-debug")
      case None => Seq.empty[String]
    }),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  resolvers ++= Seq(
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
  ijRuntimeSubDir := "sandbox",
  ijPluginsSubDir := "ij-plugins",
  ijCleanBeforePrepareRun := true,
  cleanFiles += ijPluginsDir.value,
  //
  manifestSetting,
  // Setup publishing
  publishMavenStyle := true,
  sonatypeProfileName := "net.sf.ij-plugins",
  sonatypeProjectHosting := Some(GitHubHosting("ij-plugins", "ijp-javacv", "jpsacha@gmail.com")),
  publishTo := sonatypePublishToBundle.value,
  developers := List(
    Developer(id = "jpsacha", name = "Jarek Sacha", email = "jpsacha@gmail.com", url = url("https://github.com/jpsacha"))
    )
  )

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
      publish / skip := true,
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
    publish / skip := true,
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