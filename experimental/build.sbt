resolvers += "SCI Java Releases" at "https://maven.scijava.org/content/repositories/thirdparty/"

libraryDependencies ++= Seq(
  "org.scijava" % "script-editor"        % "1.1.0",
  "org.scijava" % "scripting-groovy"     % "1.0.0",
  "org.scijava" % "scripting-java"       % "0.4.1",
  "org.scijava" % "scripting-scala"      % "0.3.2",
  "org.scijava" % "script-editor-scala"  % "0.2.1",
  "org.scijava" % "script-editor-jython" % "1.1.0",
  //  "org.scijava" % "scripting-javascript" % "1.0.0",

  // not needed for script editor
  "org.python" % "jython-slim" % "2.7.3",

  // dependency overrides to pull bug fixes in transitive dependencies
  "org.scijava" % "scijava-optional" % "1.0.1",
  "org.scijava" % "scijava-table"    % "1.0.2",
  "org.scijava" % "scijava-ui-swing" % "1.0.1"
//  "org.codehaus.groovy" % "groovy"           % "3.0.17"
)
