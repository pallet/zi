(ns zi.marginalia
  "Build marginalia output"
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [classlojure.core :as classlojure]
   [clojure.java.io :as io])
  (:use
   [zi.maven :only [resolve-dependency]])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
   org.apache.maven.plugin.MojoExecutionException
   org.apache.maven.artifact.Artifact))

(defn formatDependencies [artifacts]
  (vec
   (map
    (fn [artifact]
      (vector
       (if (= (.getGroupId artifact) (.getArtifactId artifact))
         (.getGroupId artifact)
         (str (.getGroupId artifact) "/" (.getArtifactId artifact)))
       (.getVersion artifact)))
    artifacts)))

(defn run-marginalia
  [repo-system repo-system-session project classpath-elements
   source-paths target-path]
  (let [marginalia-deps (resolve-dependency
                         repo-system
                         repo-system-session
                         (.getRemoteProjectRepositories project)
                         "marginalia" "marginalia"
                         (or (System/getProperty "marginalia.version") "0.7.0")
                         {})
        cl (core/classloader-for
            (concat classpath-elements marginalia-deps))]
    (classlojure/eval-in
     cl
     `(do
        (require 'marginalia.core)
        (require 'marginalia.html)
        (marginalia.core/ensure-directory! ~target-path)
        (binding [marginalia.html/*resources* ""]
          (marginalia.core/uberdoc!
           ~(.getPath (io/file target-path "uberdoc.html"))
           [~@(map #(.getPath %) (mapcat core/clj-files source-paths))]
           {:name ~(.getName project)
            :version ~(.getVersion project)
            :description ~(core/unindent-description (.getDescription project))
            :dependencies [~@(formatDependencies
                              (filter
                               #(= Artifact/SCOPE_COMPILE (.getScope %))
                               (.getDependencyArtifacts project)))]
            :dev-dependencies [~@(formatDependencies
                                  (filter
                                   #(= Artifact/SCOPE_TEST (.getScope %))
                                   (.getDependencyArtifacts project)))]}))))))

(mojo/defmojo Marginalia
  {Goal "marginalia"
   RequiresDependencyResolution "test"}
  [^{Component {:role "org.sonatype.aether.RepositorySystem"}}
   repoSystem
   ^{Parameter {:defaultValue "${repositorySystemSession}" :readonly true}}
   repoSystemSession
   ^{Parameter
     {:defaultValue "${project.build.directory}"
      :alias "marginaliaTargetDirectory"
      :description "Where to write marginalia output"}}
   ^String
   marginalia-target-directory
   ^{Parameter
     {:expression "${project}"
      :description "Project"}}
   project]

  (run-marginalia
   repoSystem
   repoSystemSession
   project
   (vec classpath-elements)
   (core/clojure-source-paths source-directory)
   marginalia-target-directory))
