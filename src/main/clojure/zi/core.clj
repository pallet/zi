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
  (let [base (io/file base-directory)]
    (when (and (.exists base) (.isDirectory base))
      (filter
       (fn [^java.io.File file]
         (and (.isFile file) (.. (.getName file) (endsWith ".clj"))))
       (file-seq base)))))

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

(defn overridable-artifact-path
  "Get the artifact path, either from the current project, or if not specified
   there, from zi. n.b. This does not deal with the artifact's dependencies."
  [regex project-classpath-elements]
  (when-not (path-on-classpath? regex project-classpath-elements)
    (filter
     #(re-find regex %)
     (map #(.getPath %) (.getURLs (.getClassLoader clojure.lang.RT))))))

(defn source-jar
  "Return the path to the source jar if it exists."
  [path]
  (if-let [[p basename] (re-matches #"(.*).jar" path)]
    (let [file (io/file (str basename "-sources.jar"))]
      (when (.canRead file)
        (.getPath file)))))

(defn classpath-with-source-jars
  "Try adding source code jars to the classpath"
  [classpath-elements]
  (concat
   classpath-elements
   (->>
    classpath-elements
    (map source-jar)
    (filter identity))))
