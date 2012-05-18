(ns zi.maven
  "General functions for working with maven"
  (:require
   [zi.core :as core])
  (:import
   org.apache.maven.project.DefaultProjectBuildingRequest
   org.sonatype.aether.collection.CollectRequest
   [org.sonatype.aether.graph Dependency Exclusion DependencyNode]
   org.sonatype.aether.resolution.DependencyRequest
   org.sonatype.aether.util.artifact.DefaultArtifact))

(defn paths-for-checkout
  "Return the source paths for a given pom file"
  [repo-system repo-system-session project-builder pom-file]
  (let [project (.getProject
                 (.build
                  project-builder pom-file
                  (doto (DefaultProjectBuildingRequest.)
                    (.setRepositorySession repo-system-session))))]
    (concat
     (core/clojure-source-paths (.. project getBuild getSourceDirectory))
     [(.. project getBuild getOutputDirectory)]
     (map #(.getDirectory %) (.. project getBuild getResources)))))

;;; Based on code from pomegranate
(defn- exclusion
  [[group-id artifact-id & {:as opts}]]
  (Exclusion.
   group-id artifact-id (:classifier opts "*") (:extension opts "*")))

(defn- coordinate-string
  "Produces a coordinate string with a format of
   <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>>
   given a lein-style dependency spec.  :extension defaults to jar."
  [group-id artifact-id version
   {:keys [classifier extension] :or {extension "jar"}}]
  (->>
   [group-id artifact-id extension classifier version]
   (remove nil?)
   (interpose \:)
   (apply str)))

(defn- dependency
  [group-id artifact-id version {:keys [scope optional exclusions classifier]
                                 :as opts
                                 :or {scope "compile" optional false}}]
  (Dependency.
   (DefaultArtifact. (coordinate-string group-id artifact-id version opts))
   scope
   optional
   (map exclusion exclusions)))

(defn dependency-file
  [^Dependency dependency]
  (.. dependency getArtifact getFile getCanonicalPath))

(defn- dependency-files
  [node]
  (reduce
   (fn [files ^DependencyNode n]
     (if-let [dependency (.getDependency n)]
       (concat
        files
        [(dependency-file dependency)]
        (->> (.getChildren n) (map #(.getDependency %)) (map dependency-file)))
       files))
   []
   (tree-seq (constantly true) #(seq (.getChildren %)) node)))

(defn resolve-dependency
  [repo-system repo-system-session repositories
   group-id artifact version {:keys [scope exclusions classifier] :as opts}]
  (let [dependency (dependency group-id artifact version opts)
        request (CollectRequest. [dependency] nil repositories)]
    (.setRequestContext request "runtime")
    (->
     (.resolveDependencies
      repo-system repo-system-session (DependencyRequest. request nil))
     .getRoot
     dependency-files)))
