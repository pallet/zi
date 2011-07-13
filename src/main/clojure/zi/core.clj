(ns zi.core
  "Core functions used across mojos"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   classlojure))

(defn escape-filename [filename]
  (string/replace filename "\\" "\\\\"))

(defn absolute-filename [filename]
  (.getPath (io/file filename)))

(defn filename-to-url-string
  [filename]
  (str (.toURL (io/file filename))))

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
  [source-paths classpath-elements form & args]
  (apply
   classlojure/eval-in
   (classloader-for (concat source-paths classpath-elements))
   form
   args))

(defn clj-files
  "Return a sequence of .clj files under the given base directory"
  [^String base-directory]
  {:pre [(.. (io/file base-directory) isDirectory)]}
  (filter
   (fn [^java.io.File file]
     (and (.isFile file) (.. (.getName file) (endsWith ".clj"))))
   (file-seq (io/file base-directory))))

(defn file-to-namespace
  "Convert a filename to a namespace name"
  [filename]
  (-> filename
      (string/replace #"\.clj$" "")
      (string/replace "_" "-")
      (string/replace "/" ".")))

(defn find-namespaces
  [base-directory]
  (let [offset (inc (count base-directory))]
    (->>
     (clj-files base-directory)
     (map #(subs (.getPath %) offset))
     (map file-to-namespace))))

(defn clojure-source-paths
  [source-directory]
  (if (.endsWith source-directory "/java")
    [(string/replace source-directory #"/java$" "/clojure") source-directory]
    [source-directory]))

(defn path-on-classpath?
  "Predicate to test whether a path matching the supplied regex is in the
   classpath-elements"
  [regex classpath-elements]
  (some #(re-find regex %) classpath-elements))
