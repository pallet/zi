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

(defn eval-clojure
  "Evaluate a closure form in a classloader with the specified paths"
  [source-paths classpath-elements form]
  (let [class-loader (apply
                      classlojure/classlojure
                      (->>
                       (concat source-paths classpath-elements)
                       (map absolute-filename)
                       (map filename-to-url-string)))]
    (classlojure/eval-in class-loader form)))
