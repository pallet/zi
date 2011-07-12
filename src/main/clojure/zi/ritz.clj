(ns zi.ritz
  "ritz mojo for zi plugin"
  (:require
   [zi.core :as core]
   [zi.mojo :as mojo])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]
    org.apache.maven.plugin.ContextEnabled
    org.apache.maven.plugin.Mojo
    org.apache.maven.plugin.MojoExecutionException))

(def ^{:const true} ritz-path-regex
  #"ritz/ritz/[0-9.]+(?:-SNAPSHOT)?/ritz")

(mojo/defmojo RitzMojo
  {Goal "ritz"
   RequiresDependencyResolution "test"}
  [
   ^{Parameter
     {:expression "${clojure.swank.port}" :defaultValue "4005"}}
   ^Integer
   port

   ^{Parameter
     {:expression "${clojure.swank.encoding}" :defaultValue "iso-8859-1"}}
   ^String
   encoding

   ^{Parameter
     {:expression "${clojure.swank.network}" :defaultValue "localhost"}}
   ^String
   network]

  (let [tmpfile (try
                  (File/createTempFile "swank" ".port")
                  (catch java.io.IOException e
                    (throw
                     (MojoExecutionException.
                      "Could not create swank port file" e))))
        ritz-artifact (when-not (core/path-on-classpath?
                                 ritz-path-regex test-classpath-elements)
                        (filter
                         #(re-find ritz-path-regex %)
                         (map
                          #(.getPath %)
                          (.getURLs (.getClassLoader clojure.lang.RT)))))]
    (core/eval-clojure
     (core/clojure-source-paths source-directory)
     (into (vec test-classpath-elements) ritz-artifact)
     `(do
        (require '~'ritz.socket-server)
        (ritz.socket-server/start
         {:port-file ~(.getPath tmpfile)
          :host ~network
          :port ~(Integer/parseInt port)
          :encoding ~encoding
          :dont-close true})))))
