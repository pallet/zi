(ns zi.nrepl
  "nrepl mojo for zi plugin"
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

(mojo/defmojo NreplMojo
    {Goal "nrepl"
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
    (log/info "nrepl can not be run on a project with pom packaging")
    (let [nrepl-version (or (System/getProperty "nrepl.version") "0.2.0-beta9")
          nrepl-artifacts (resolve-dependency
                                repo-system
                                repo-system-session
                                (.getRemoteProjectRepositories project)
                                "org.clojure" "tools.nrepl" nrepl-version
                                {})
          source-paths (->
                        (core/clojure-source-paths source-directory)
                        (into (core/clojure-source-paths test-source-directory))
                        (into (checkouts/checkout-paths
                               repo-system repo-system-session
                               project-builder)))
          classpath-elements (concat
                              test-classpath-elements nrepl-artifacts)]
      (log/debugf "source paths: %s" (vec source-paths))
      (log/debugf "classpath elements: %s" (vec classpath-elements))
      (core/eval-clojure
       source-paths
       (-> classpath-elements
           core/classpath-with-source-jars
           core/classpath-with-tools-jar)
       `(do
          (require '~'clojure.tools.nrepl.server)
          (let [server# (~'clojure.tools.nrepl.server/start-server
                         :bind ~network
                         :port ~(Integer/parseInt port)
                         :handler (~'clojure.tools.nrepl.server/default-handler
                                    ~@(vec middleware)))
                port# (-> server# deref :ss .getLocalPort)]
            (println "nREPL server started on port" port#)
            (spit ~repl-port-file port#)
            (.deleteOnExit (java.io.File. ~repl-port-file)))))
      @(promise))))
