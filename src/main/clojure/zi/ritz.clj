(ns zi.ritz
  "ritz mojo for zi plugin"
  (:require
   [zi.core :as core]
   [zi.mojo :as mojo]
   [clojure.maven.mojo.log :as log]
   [zi.checkouts :as checkouts])
  (:use
   [clojure.set :only [difference union]]
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
          ritz-version (or (System/getProperty "ritz.version") "0.4.1")
          lein-version (or (System/getProperty "lein.version")
                           "2.0.0-preview10")
          clojure-version (or (System/getProperty "ritz.clojure.version")
                              "1.4.0")
          ritz-0-3 (.startsWith ritz-version "0.3")
          ritz-artifacts (resolve-dependency
                          repo-system
                          repo-system-session
                          (.getRemoteProjectRepositories project)
                          "ritz" (if ritz-0-3 "ritz" "ritz-swank")
                          ritz-version
                          {:exclusions [["org.clojure" "clojure"]]})
          clojure-artifacts (resolve-dependency
                             repo-system
                             repo-system-session
                             (.getRemoteProjectRepositories project)
                             "org.clojure" "clojure" clojure-version {})
          lein-artifacts (resolve-dependency
                          repo-system
                          repo-system-session
                          (.getRemoteProjectRepositories project)
                          "leiningen" "leiningen" lein-version
                          {:exclusions [["org.clojure" "clojure"]
                                        ["org.clojure" "data.xml"]
                                        ["reply" "reply"]
                                        ["clj-http" "clj-http"]
                                        ["org.apache.maven.indexer"
                                         "indexer-core"]]})
          source-paths (->
                        (core/clojure-source-paths source-directory)
                        (into (core/clojure-source-paths test-source-directory))
                        (into (checkouts/checkout-paths
                               repo-system repo-system-session
                               project-builder)))
          classpath-elements test-classpath-elements
          debug-cp (-> (concat clojure-artifacts ritz-artifacts lein-artifacts)
                       core/classpath-with-tools-jar
                       distinct)
          vm-cp debug-cp
          cp (distinct (concat source-paths
                               (-> classpath-elements
                                   core/classpath-with-source-jars)))
          cp-with-ritz (concat cp ritz-artifacts)
          log-level (if-let [p (System/getProperty "ritz.loglevel")]
                      (keyword p)
                      :warn)
          extra-cp (union
                    (difference (set cp-with-ritz) (set cp))
                    (set (core/jpda-jars)))]
      (log/debugf "source paths: %s" (vec source-paths))
      (log/debugf "classpath elements: %s" (vec classpath-elements))
      (log/debugf "debug-cp: %s" (vec debug-cp))
      (log/debugf "vm-cp: %s" (vec vm-cp))
      (log/debugf "cp: %s" (vec cp))
      (log/debugf "log-level %s" (pr-str log-level))
      (core/eval-clojure
       source-paths
       debug-cp
       `(do
          (require '~(if ritz-0-3
                       'ritz.socket-server
                       'ritz.swank.socket-server))
          (~(if ritz-0-3
              'ritz.socket-server/start
              'ritz.swank.socket-server/start)
           {:port-file ~(.getPath tmpfile)
            :host ~network
            :port ~(Integer/parseInt port)
            :encoding ~encoding
            :dont-close true
            :log-level ~log-level
            :classpath ~(vec cp-with-ritz)
            :vm-classpath ~(vec vm-cp)
            :extra-classpath ~(vec extra-cp)}))))))
