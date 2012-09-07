(ns zi.ritz-nrepl
  "ritz-nrepl mojo for zi plugin"
  (:require
   [zi.core :as core]
   [zi.mojo :as mojo]
   [clojure.maven.mojo.log :as log]
   [clojure.string :as string]
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

(mojo/defmojo RitzNreplMojo
    {Goal "ritz-nrepl"
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
       {:expression "${clojure.nrepl.port}" :defaultValue "4005"}}
     ^Integer
     port

     ^{Parameter
       {:expression "${clojure.nrepl.network}" :defaultValue "localhost"}}
     ^String
     network

     ^{Parameter
       {:defaultValue "${project.packaging}" :required true}}
     ^String
     packaging

     ^{Parameter
       {:expression "${project}"
        :description "Project"}}
     project

     ^{Parameter
       {:defaultValue "${project.build.directory}/repl-port"
        :alias "replPortFile"
        :description "Where to write the repl port number"}}
     ^String repl-port-file

     ^{Parameter
       {:alias "middlewares"
        :description "Middleware"}}
     ^{:tag (Class/forName "[Ljava.lang.String;")} middleware]

  (if (= packaging "pom")
    (log/info "Ritz-nrepl can not be run on a project with pom packaging")
    (let [ritz-nrepl-version (or (System/getProperty "ritz-nrepl.version")
                                 "0.4.1")
          ritz-nrepl-artifacts (resolve-dependency
                                repo-system
                                repo-system-session
                                (.getRemoteProjectRepositories project)
                                "ritz" "ritz-nrepl"
                                ritz-nrepl-version
                                {})
          source-paths (->
                        (core/clojure-source-paths source-directory)
                        (into (core/clojure-source-paths test-source-directory))
                        (into (checkouts/checkout-paths
                               repo-system repo-system-session
                               project-builder)))
          classpath-elements (concat
                              test-classpath-elements ritz-nrepl-artifacts)
          log-level (if-let [p (System/getProperty "ritz-nrepl.loglevel")]
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
          (require '~'ritz.nrepl)
          (~'ritz.nrepl/start-jpda-server
           ~network ~(Integer/parseInt port) nil
           ~repl-port-file
           ~(string/join ":" (concat source-paths
                                    (-> classpath-elements
                                        core/classpath-with-source-jars)))
           ~(vec middleware))))
      @(promise))))
