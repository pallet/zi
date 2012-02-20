(ns zi.codox
  "Build codox output"
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [clojure.java.io :as io])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
   org.apache.maven.plugin.MojoExecutionException
   org.apache.maven.artifact.Artifact))

(def ^{:const true} codox-path-regex
  #"codox/codox/[0-9.]+(?:-SNAPSHOT)?/codox")

(def ^{:const true} tools-namespace-path-regex
  #"org/clojure/tools.namespace/[0-9.]+(?:-SNAPSHOT)?/tools.namespace")

(def ^{:const true} java-classpath-path-regex
  #"org/clojure/java.classpath/[0-9.]+(?:-SNAPSHOT)?/java.classpath")

(def ^{:const true} hiccup-path-regex
  #"hiccup/hiccup/[0-9.]+(?:-SNAPSHOT)?/hiccup")

(defn run-codox
  [project source-paths classpath-elements target-path version writer]
  (core/eval-clojure
   source-paths
   (vec (concat classpath-elements (core/zi-classpath-elements)))
   `(do
      (require 'codox.main)
      (codox.main/generate-docs
       {:name ~(.getName project)
        :version ~version
        :description ~(core/unindent-description (.getDescription project))
        :sources ~source-paths
        :output-dir ~target-path
        :writer '~writer}))))

(mojo/defmojo Codox
  {Goal "codox"
   RequiresDependencyResolution "test"}
  [^{Parameter
     {:defaultValue "${project.build.directory}/doc"
      :alias "codoxTargetDirectory"
      :description "Where to write codox output"}}
   ^String codox-target-directory

   ^{Parameter
     {:defaultValue "${project.version}"
      :alias "codoxApiVersion"
      :description "Where to write codox output"}}
   ^String codox-api-version

   ^{Parameter
     {:alias "codoxWriter"
      :description "Symbol of var used to write codox output"}}
   ^String codox-writer

   ^{Parameter
     {:expression "${project}"
      :description "Project"}}
   project]
  (let [writer (symbol (or codox-writer "codox.writer.html/write-docs"))]
    (.debug log (str "Writer is " writer))
    (run-codox
     project
     (core/clojure-source-paths source-directory)
     test-classpath-elements
     codox-target-directory
     codox-api-version
     writer)))
