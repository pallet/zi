(ns zi.marginalia
  "Build marginalia output"
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [marginalia.core :as marginalia]
   [marginalia.html :as html]
   [clojure.java.io :as io])
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
  [project source-paths target-path]
  (marginalia/ensure-directory! target-path)
  (binding [html/*resources* ""]
    (marginalia/uberdoc!
     (.getPath (io/file target-path "uberdoc.html"))
     (map #(.getPath %) (mapcat core/clj-files source-paths))
     {:name (.getName project)
      :version (.getVersion project)
      :description (.getDescription project)
      :dependencies (formatDependencies
                     (filter
                      #(= Artifact/SCOPE_COMPILE (.getScope %))
                      (.getDependencyArtifacts project)))
      :dev-dependencies (formatDependencies
                         (filter
                          #(= Artifact/SCOPE_TEST (.getScope %))
                          (.getDependencyArtifacts project)))})))

(mojo/defmojo Marginalia
  {Goal "marginalia"
   RequiresDependencyResolution "test"}
  [^{Parameter
     {:defaultValue "${project.build.directory}"
      :alias "marginaliaTargetDirectory"
      :description "Where to write marginalia output"}}
   ^String
   marginalia-target-directory
   ^{Parameter
     {:expression "${project}"
      :description "Where to write marginalia output"}}
   ^String
   project]

  (run-marginalia
   project
   (core/clojure-source-paths source-directory)
   marginalia-target-directory))
