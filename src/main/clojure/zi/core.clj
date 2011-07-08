(ns zi.core
  "Core functions used across mojos"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   classlojure))

(defn escape-filename [filename]
  (string/replace filename "\\" "\\\\"))

(defn absolute-filename [filename]
  (.getPath (java.io.File. filename)))

(defn filename-to-url-string
  [filename]
  (str (.toURL (java.io.File. filename))))

(defn classloader-for
  "Classloader for the specified files"
  [files]
  (apply
   classlojure/classlojure
   (->>
    files
    (map absolute-filename)
    (map filename-to-url-string))))

(defn eval-clojure
  "Evaluate a closure form in a classloader with the specified paths"
  [source-paths classpath-elements form]
  (classlojure/eval-in
   (classloader-for (concat source-paths classpath-elements))
   form))

(defn clj-files
  "Return a sequence of .clj files under the given base directory"
  [base-directory]
  {:pre [(.. (java.io.File. base-directory) isDirectory)]}
  (filter
   #(and (.isFile %) (.. (.getName %) (.endsWith % ".clj")))
   (file-seq base-directory)))

(defn file-to-namespace
  "Convert a filename to a namespace name"
  [filename]
  (-> filename (string/replace "_" "-") (string/replace "/" ".")))

(defn find-namespaces
  [base-directory]
  (map file-to-namespace (clj-files base-directory)))
