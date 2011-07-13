(ns zi.lein
  "Leiningen project functions")

(defn paths-for-checkout
  "Return the source paths for a given pom file"
  [project-file]
  (let [project (read-string (slurp project-file))]
    [(:source-path project "src")
     (:compile-path project "classes")
     (:resources-path project "resources")]))
