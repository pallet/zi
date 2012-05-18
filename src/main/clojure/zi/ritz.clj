(ns zi.ritz
  "ritz mojo for zi plugin"
  (:require
   [zi.core :as core]
   [zi.mojo :as mojo]
   [clojure.maven.mojo.log :as log]
   [zi.checkouts :as checkouts])
  (:use
   [zi.maven :only [resolve-dependency]])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
    org.apache.maven.plugin.ContextEnabled
    org.apache.maven.plugin.Mojo
    org.apache.maven.plugin.MojoExecutionException))

(mojo/defmojo RitzMojo
  {Goal "ritz"
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
   packaging

   ^{Parameter
     {:expression "${project}"
      :description "Project"}}
   project]

  (if (= packaging "pom")
    (log/info "Ritz can not be run on a project with pom packaging")
    (let [tmpfile (try
                    (File/createTempFile "swank" ".port")
                    (catch java.io.IOException e
                      (throw
                       (MojoExecutionException.
                        "Could not create swank port file" e))))
          ritz-artifacts (resolve-dependency
                         repo-system
                         repo-system-session
                         (.getRemoteProjectRepositories project)
                         "ritz" "ritz"
                         (or (System/getProperty "ritz.version") "0.3.0")
                         {})
          source-paths (->
                        (core/clojure-source-paths source-directory)
                        (into (core/clojure-source-paths test-source-directory))
                        (into (checkouts/checkout-paths
                               repo-system repo-system-session
                               project-builder)))
          classpath-elements (concat test-classpath-elements ritz-artifacts)
          log-level (if-let [p (System/getProperty "ritz.loglevel")]
                      (read-string p)
                      :warn)]
      (log/debugf "source paths: %s" (vec source-paths))
      (log/debugf "classpath elements: %s" (vec classpath-elements))
      (log/debugf "log-level %s" (pr-str log-level))
      (core/eval-clojure
       source-paths
       (-> classpath-elements
           core/classpath-with-source-jars
           core/classpath-with-tools-jar)
       `(do
          (require '~'ritz.socket-server)
          (ritz.socket-server/start
           {:port-file ~(.getPath tmpfile)
            :host ~network
            :port ~(Integer/parseInt port)
            :encoding ~encoding
            :dont-close true
            :log-level ~log-level}))))))
