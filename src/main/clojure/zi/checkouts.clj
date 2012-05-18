(ns zi.checkouts
  "Checkouts feature for repl servers"
  (:require
   [zi.lein :as lein]
   [zi.maven :as maven]
   [clojure.java.io :as io]))

(defn checkouts
  "Return a list of checkout directories"
  []
  (filter #(.isDirectory %) (.listFiles (io/file "checkouts"))))

(defn checkout-paths
  "Returns a sequence of checkout paths to add to the classpath"
  [repo-system repo-system-session project-builder]
  (let [checkouts (checkouts)]
    (concat
     (when repo-system
       (mapcat identity
               (for [checkout checkouts
                     :let [pom (io/file checkout "pom.xml")]
                     :when (.exists pom)]
                 (maven/paths-for-checkout
                  repo-system repo-system-session project-builder pom))))
     (mapcat identity
             (for [checkout checkouts
                   :let [project (io/file checkout "project.clj")]
                   :when (.exists project)]
               (lein/paths-for-checkout (.getPath project)))))))
