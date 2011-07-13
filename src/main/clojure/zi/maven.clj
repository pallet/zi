(ns zi.maven
  "General functions for working with maven"
  (:require
   [zi.core :as core])
  (:import
   org.apache.maven.project.DefaultProjectBuildingRequest))

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
