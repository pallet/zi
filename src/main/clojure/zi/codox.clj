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
  [project source-paths classpath-elements target-path]
  (let [artifacts
        (mapcat
         #(core/overridable-artifact-path % classpath-elements)
         [codox-path-regex tools-namespace-path-regex java-classpath-path-regex
          hiccup-path-regex])]
    (core/eval-clojure
     source-paths
     (vec (concat classpath-elements artifacts))
     `(do
        (require 'codox.main)
        (codox.main/generate-docs
         {:name ~(.getName project)
          :version ~(.getVersion project)
          :description ~(.getDescription project)
          :sources ~source-paths
          :output-dir ~target-path})))))

(mojo/defmojo Codox
  {Goal "codox"
   RequiresDependencyResolution "test"}
  [^{Parameter
     {:defaultValue "${project.build.directory}/doc"
      :alias "codoxTargetDirectory"
      :description "Where to write codox output"}}
   ^String codox-target-directory

   ^{Parameter
     {:expression "${project}"
      :description "Project"}}
   project]
  (run-codox
   project
   (core/clojure-source-paths source-directory)
   test-classpath-elements
   codox-target-directory))
