(ns zi.resource
  "Copy clojure sources."
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [clojure.java.io :as io])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]))

(defn copy-sources
  [source-directory output-directory]
  (let [source-paths (core/clojure-source-paths source-directory)
        source-abs-paths (map
                          #(.getAbsolutePath (java.io.File. %))
                          source-paths)
        relative (fn [file]
                   (let [path (.getAbsolutePath file)]
                     (when-let [root (first (filter
                                             #(.startsWith path %)
                                             source-abs-paths))]
                       (subs path (inc (count root))))))
        cp (fn [[from to]]
             (let [to (io/file output-directory to)
                   parent (java.io.File. (.getParent to))]
               (when-not (.exists parent)
                 (.mkdirs parent))
               (when (or (not (.exists to))
                         (> (.lastModified from) (.lastModified to)))
                 (io/copy from to))))]
    (->>
     source-paths
     (map #(java.io.File. %))
     (mapcat file-seq)
     (filter #(.isFile %))
     (filter #(.endsWith (.getName %) ".clj"))
     (map #(vector % (relative %)))
     (map cp)
     doall)))

(mojo/defmojo Resource
  {Goal "resources"
   Phase "process-resources"}
  [#=(mojo/parameter
      includes "Which source files to copy"
      :typename "java.util.LinkedList")
   #=(mojo/parameter
      excludes "Which source files not to copy"
      :typename "java.util.LinkedList")]
  (copy-sources source-directory output-directory))
