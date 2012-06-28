# Release Notes

# 0.5.1

- in ring-genfiles, fix handler-source which was not returning wished fn

# 0.5.0

- Properly handle classpath for targets running in project classpath
  The dependencies for codox, marginalia, ritz, and swank-clojure are now
  handled properly.  The zi pom no longer depends on these projects.

- Automatically add tools.jar to classpath for the ritz task

- Use leiningen-core for calculating checkout dependency paths
  Upgrade to clojure 1.3.0 and leiningen-core 2.0.0-preview4

- Add clojure-maven-mojo as a dependency, and use it's log wrapper

- Add .ritz-exception-filters to .gitignore

- Add a test for the test mojo runner

- Update to marginalia 0.7.0, and run it in the project classpath

- Remove use of contrib in ring-genfiles
  Switch to data.xml

- Unindent pom description for marginalia and codox

- Update to maven 3.0.4 and aether 1.13

# 0.4.5

- Allow exclusion of tests based on a regex
  Provides a mechanism for disabling clojure.test test namespaces

- Allow specification of codox output writer

- Allow explicit specification of codox api version

# 0.4.4
- Add support for codoxTargetDirectory

- Add a codox goal
  The codox goal can be used to generate api documentation based on the
  codox project

- Allow setting of ritz loglevel via a system property
  Setting ritz.logglevel=:trace will enable ritz logging

## 0.4.3
- Allow tests to override core.test/report before redef'ing.
  Some test frameworks, such as midje register additional defmethods for
  core.test/report. By requiring the test namespaces before the report var
  is redef'ed to a function, these test frameworks become supported.

- Add note about source jars to readme

## 0.4.2

- ring gen files : listener-class and servlet-class in web.xml should point
  to the class file (no '-', use '_' instead)

- Update to ritz 0.2.0

- Enforce maven 3.0.3 requirement

## 0.4.1

- Update initScript handling in test mojo to handle multiple forms
  Only the first form was being evaluated.

## 0.4.0

Adds the ring-genfiles goal for lein-ring compatible Servlet,
ServletContextListener and web.xml generation.

- Fix an issue with keywords used (init and destroy)

- ring-genfiles formatting, add to readme, and add integration test

- Add ring-genfiles goal to allow servlet/listener/web.xml generation

## 0.3.10

- Fix for non-string used as a test message crashing zi:test

## 0.3.9

- Fix an issue preventing the test task from working in clojure-1.3.0
  The form passed for portable-redef was being quoted in clojure-1.2, and
  run in 1.3. The quoting was adding zi.test to the with-redefs symbol.

## 0.3.8

- Update for classlojure 0.6.1

## 0.3.7

- Remove class files from final jar
  The mojo extractor was creating class files that were packaged.

- Add initScript property for test target

## 0.3.6

- Fix skipTests handling in test mojo

### Fixes

## 0.3.5

### Features

- Honour skipTests, and fix for case when no tests defined

- Add available source jars to classpath

### Fixes
- Remove superfluous log statement

- Add sonatype repository to pom


## 0.3.4

- Throw a MojoFailureException when tests fail

- Don't run ritz on pom packaged projects

- Add testResources goal

## 0.3.3

- Ensure tests run inside a clojure.main/with-bindings
  Code that calls set! on common rebindable vars (such as
  *warn-on-reflection*) fails otherwise

- Fix the classpath elements used in ritz and test

## 0.3.2

- Fix resources mojo to write to target/classes

## 0.3.1

- Really add marginalia goal

- Add integration testing

## 0.3.0

- Add marginalia goal

- Factor out overridable-artifact-path

## 0.2.2

- Fix mojo/defmojo to actually read the project source paths

## 0.2.1

- Add checkouts to ritz and swank-clojure goals

- Update readme with new goals


## 0.2.0

- Allow overriding of ritz and swank-clojure artifacts

- Add a test goal for running clojure.test tests

- Add swank-clojure goal

- Update ritz mojo to use defmojo

- Add resources goal for copying clojure source

- Add macro for better mojo definition syntax

- Add compiler mojo
  The compile goal compiles clojure source. It respects the options from
  maven-compiler-plugin.

## 0.1.0

- Initial release
