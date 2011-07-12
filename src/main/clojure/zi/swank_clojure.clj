(ns zi.swank-clojure
  "Start a swank-clojure swank server."
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [clojure.java.io :as io])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
   org.apache.maven.plugin.MojoExecutionException))

(def ^{:const true} swank-clojure-path-regex
  #"swank-clojure/swank-clojure/[0-9.]+(?:-SNAPSHOT)?/swank-clojure")

(def ^{:const true} swank-clojure-version-regex
  #"/([0-9]+)\.([0-9]+)\.([0-9]+)(?:-SNAPSHOT)?")

(mojo/defmojo Swank
  {Goal "swank-clojure"
   RequiresDependencyResolution "test"}
  [^{Parameter
     {:expression "${clojure.swank.port}" :defaultValue "4005"
      :description "Swank server port"}}
   ^Integer
   port

   ^{Parameter
     {:expression "${clojure.swank.encoding}" :defaultValue "iso-8859-1"
      :description "Swank server protocol encoding"}}
   ^String
   encoding

   ^{Parameter
     {:expression "${clojure.swank.network}" :defaultValue "localhost"
      :description "Network address for the server to bind to"}}
   ^String
   network]

  (let [swank-artifact (when-not (core/path-on-classpath?
                                  swank-clojure-path-regex
                                  test-classpath-elements)
                         (filter
                          #(re-find swank-clojure-path-regex %)
                          (map
                           #(.getPath %)
                           (.getURLs (.getClassLoader clojure.lang.RT)))))]
    (core/eval-clojure
     (core/clojure-source-paths source-directory)
     (into (vec test-classpath-elements) swank-artifact)
     `(do
        (require '~'swank.swank '~'swank.commands.basic)
        (swank.swank/start-repl
         ~(Integer/parseInt port)
         :host ~network
         :encoding ~encoding
         :dont-close true)
        (doseq [t# ((ns-resolve '~'swank.commands.basic '~'get-thread-list))]
          (.join t#))))))
