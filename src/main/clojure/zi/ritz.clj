(ns zi.ritz
  "ritz mojo for zi plugin"
  (:require
   [zi.core :as core]
   [zi.mojo :as mojo]
   [zi.log :as log]
   [zi.checkouts :as checkouts])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
    org.apache.maven.plugin.ContextEnabled
    org.apache.maven.plugin.Mojo
    org.apache.maven.plugin.MojoExecutionException))

(def ^{:const true} ritz-path-regex
  #"ritz/ritz/[0-9.]+(?:-SNAPSHOT)?/ritz")

(mojo/defmojo RitzMojo
  {Goal "ritz"
   RequiresDependencyResolution "test"}
  [
   ^{Component {:role "org.sonatype.aether.RepositorySystem"}}
   repoSystem

   ^{Component {:role "org.apache.maven.project.ProjectBuilder"}}
   projectBuilder

   ^{Parameter {:defaultValue "${repositorySystemSession}" :readonly true}}
   repoSystemSession

   ^{Parameter
     {:expression "${clojure.swank.port}" :defaultValue "4005"}}
   ^Integer
   port

   ^{Parameter
     {:expression "${clojure.swank.encoding}" :defaultValue "iso-8859-1"}}
   ^String
   encoding

   ^{Parameter
     {:expression "${clojure.swank.network}" :defaultValue "localhost"}}
   ^String
   network

   ^{Parameter
     {:defaultValue "${project.packaging}" :required true}}
   ^String
   packaging]

  (if (= packaging "pom")
    (log/info "Ritz can not be run on a project with pom packaging")
    (let [tmpfile (try
                    (File/createTempFile "swank" ".port")
                    (catch java.io.IOException e
                      (throw
                       (MojoExecutionException.
                        "Could not create swank port file" e))))
          ritz-artifact (core/overridable-artifact-path
                         ritz-path-regex test-classpath-elements)
          source-paths (->
                        (core/clojure-source-paths source-directory)
                        (into (core/clojure-source-paths test-source-directory))
                        (into (checkouts/checkout-paths
                               repoSystem repoSystemSession projectBuilder)))
          classpath-elements (->
                              (vec test-classpath-elements)
                              (into ritz-artifact))
          log-level (if-let [p (System/getProperty "ritz.loglevel")]
                      (read-string p)
                      :warn)]
      (log/debug (format "source paths: %s" (vec source-paths)))
      (log/debug (format "classpath elements: %s" (vec classpath-elements)))
      (log/debug (format "log-level %s" (pr-str log-level)))
      (core/eval-clojure
       source-paths
       (core/classpath-with-source-jars classpath-elements)
       `(do
          (require '~'ritz.socket-server)
          (ritz.socket-server/start
           {:port-file ~(.getPath tmpfile)
            :host ~network
            :port ~(Integer/parseInt port)
            :encoding ~encoding
            :dont-close true
            :log-level ~log-level}))))))
