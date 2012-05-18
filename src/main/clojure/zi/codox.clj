(ns zi.codox
  "Build codox output"
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [clojure.java.io :as io])
  (:use
   [zi.maven :only [resolve-dependency]])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
   org.apache.maven.plugin.MojoExecutionException
   org.apache.maven.artifact.Artifact))

(defn run-codox
  [repo-system repo-system-session project
   source-paths classpath-elements target-path version writer]
  (let [codox-deps (resolve-dependency
                    repo-system
                    repo-system-session
                    (.getRemoteProjectRepositories project)
                    "codox"
                    (or (System/getProperty "codox.artifact-id") "codox.core")
                    (or (System/getProperty "codox.version") "0.6.1")
                    {})
        codox-md-deps (resolve-dependency
                       repo-system
                       repo-system-session
                       (.getRemoteProjectRepositories project)
                       "codox-md" "codox-md"
                       (or (System/getProperty "codox-md.version") "0.1.0")
                       {})]
    (core/eval-clojure
     source-paths
     (vec (concat classpath-elements codox-deps codox-md-deps))
     `(do
        (require 'codox.main)
        (codox.main/generate-docs
         {:name ~(.getName project)
          :version ~version
          :description ~(core/unindent-description (.getDescription project))
          :sources ~source-paths
          :output-dir ~target-path
          :writer '~writer})))))

(mojo/defmojo Codox
  {Goal "codox"
   RequiresDependencyResolution "test"}
  [^{Component {:role "org.sonatype.aether.RepositorySystem"}}
   repoSystem
   ^{Parameter {:defaultValue "${repositorySystemSession}" :readonly true}}
   repoSystemSession
   ^{Parameter
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
     repoSystem
     repoSystemSession
     project
     (core/clojure-source-paths source-directory)
     test-classpath-elements
     codox-target-directory
     codox-api-version
     writer)))
