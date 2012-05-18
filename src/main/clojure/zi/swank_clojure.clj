(ns zi.swank-clojure
  "Start a swank-clojure swank server."
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [zi.checkouts :as checkouts]
   [clojure.java.io :as io])
  (:use
   [zi.maven :only [resolve-dependency]])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
   org.apache.maven.plugin.MojoExecutionException))

(mojo/defmojo Swank
  {Goal "swank-clojure"
   RequiresDependencyResolution "test"}
  [^{Component {:role "org.sonatype.aether.RepositorySystem"
                :alias "repoSystem"}}
   repo-system

   ^{Component {:role "org.apache.maven.project.ProjectBuilder"
                :alias "projectBuilder"}}
   project-builder

   ^{Parameter {:defaultValue "${repositorySystemSession}" :readonly true
                :alias "repoSystemSession"}}
   repo-system-session

   ^{Parameter
     {:expression "${clojure.swank.port}" :defaultValue "4005"
      :description "Swank server port"}}
   ^Integer
   port

   ^{Parameter
     {:expression "${clojure.swank.encoding}" :defaultValue "iso-8859-1"
      :description "Swank server protocol encoding"}}
   ^String
   encoding

   ^{Parameter
     {:expression "${clojure.swank.network}" :defaultValue "localhost"
      :description "Network address for the server to bind to"}}
   ^String
   network

   ^{Parameter
     {:expression "${project}"
      :description "Project"}}
   project]

  (let [swank-artifacts (resolve-dependency
                         repo-system
                         repo-system-session
                         (.getRemoteProjectRepositories project)
                         "swank-clojure" "swank-clojure"
                         (or (System/getProperty "swank-clojure.version")
                             "1.3.1")
                         {})]
    (core/eval-clojure
     (into (core/clojure-source-paths source-directory)
           (checkouts/checkout-paths
            repo-system repo-system-session project-builder))
     (concat test-classpath-elements swank-artifacts)
     `(do
        (require '~'swank.swank '~'swank.commands.basic)
        (swank.swank/start-repl
         ~(Integer/parseInt port)
         :host ~network
         :encoding ~encoding
         :dont-close true)
        (doseq [t# ((ns-resolve '~'swank.commands.basic '~'get-thread-list))]
          (.join t#))))))
