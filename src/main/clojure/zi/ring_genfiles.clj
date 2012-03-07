(ns zi.ring-genfiles
  "Generate files necessary to build a war. The file generation is controlled
by properties, that match those used by the lein-ring plugin, with the aim
of facilitating maven builds of lein projects using this plugin.

The properties of lein-ring plugin maybe generated with `lein pom`, using the
companion lein plugin for this task.

The code is based on the
 [lein-ring plugin](https://github.com/weavejester/lein-ring/)."
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [clojure.java.io :as io]
   [clojure.string :as string])
  (:use
   [clojure.data.xml :only [element emit]])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]))

;; Adapted from the lein-ring plugin
(defn listener-source
  [listener-class init destroy]
  (let [init-sym (and init (read-string init))
        destroy-sym (and destroy (read-string destroy))
        init-ns (and init-sym (symbol (namespace init-sym)))
        destroy-ns (and destroy-sym (symbol (namespace destroy-sym)))
        listener-ns (symbol (read-string listener-class))]
    (when (or init destroy)
      `(do
         (ns ~listener-ns
           (:require ~@(distinct (remove nil? [init-ns destroy-ns])))
           (:gen-class :implements [javax.servlet.ServletContextListener]))
         ~(let [servlet-context-event (gensym)]
            `(do
               (defn ~'-contextInitialized [this# ~servlet-context-event]
                 ~(when init-sym
                    `(~init-sym)))
               (defn ~'-contextDestroyed [this# ~servlet-context-event]
                 ~(when destroy-sym
                    `(~destroy-sym)))))))))

;; Adapted from the lein-ring plugin
(defn handler-source
  [handler-sym]
  `(fn [request#]
     (~handler-sym
      (assoc request#
        :path-info (.getPathInfo (:servlet-request request#))
        :context (.getContextPath (:servlet-request request#)))))
  handler-sym)

;; Adapted from the lein-ring plugin
(defn servlet-source
  [servlet-class handler]
  (let [handler-sym (read-string handler)
        handler-ns (symbol (namespace handler-sym))
        servlet-ns (symbol (read-string servlet-class))]
    `(do (ns ~servlet-ns
           (:require ring.util.servlet ~handler-ns)
           (:gen-class :extends javax.servlet.http.HttpServlet))
         (ring.util.servlet/defservice
           ~(handler-source handler-sym)))))

;; Adapted from the lein-ring plugin
(defn webxml-source
  "Generate the webxml and return its string representation"
  [{:keys
    [init destroy listener-class handler servlet-class url-pattern]}]
  (with-out-str
    (emit
     (element :web-app {}
       (when (or init destroy)
         (element :listener {}
          (element :listener-class {} (string/replace listener-class "-" "_"))))
       (element :servlet {}
         (element :servlet-name {} handler)
         (element :servlet-class {} (string/replace servlet-class "-" "_")))
       (element :servlet-mapping {}
                (element :servlet-name {} handler)
                (element :url-pattern {} (or url-pattern "/*"))))
     *out*)))

(defn resource-path
  [root]
  (str root "/zi-ring-genfiles/"))

(defn webxml-path
  [root]
  (str (resource-path root) "web.xml"))

(defn gen-dir
  [base]
  (str base "/gen-src/"))

(defn gen-sources-dir
  [base]
  (str (gen-dir base) "clj/"))

(defn mkpath
  [filename]
  (.mkdirs (.getParentFile (File. filename))))

(defn write-webxml
  "Write the web.xml file"
  [params]
  (let [webxml-filename (webxml-path (:build-dir params))]
    (mkpath webxml-filename)
    (spit webxml-filename (webxml-source params))))

(defn write-generated-source
  "Write generated source file into gen-sources directory"
  [class-ns class-body build-dir]
  (let [src-filename (str
                      (gen-sources-dir build-dir)
                      (core/namespace-to-file class-ns))]
    (mkpath src-filename)
    (spit src-filename (str class-body))))

(defn write-servlet-source
  "Write servlet source for the webapp according to handler and servlet-class"
  [{:keys [servlet-class handler build-dir]}]
  (write-generated-source
   servlet-class (servlet-source servlet-class handler) build-dir))

(defn write-listener-source
  "Write listener (init/destroy) source for the webapp"
  [{:keys [listener-class init destroy build-dir]}]
  (when-let [source (listener-source listener-class init destroy)]
    (write-generated-source listener-class source build-dir)))

(defn write-files
  "Generate files required for the WAR"
  [params]
  (write-servlet-source params)
  (write-listener-source params)
  (write-webxml params))

(mojo/defmojo RingGenFiles
  {Goal "ring-genfiles"
   Phase "generate-sources"}
  [ #=(mojo/parameter
       ringServletClass "ringServletClass"
       :typename "java.lang.String"
       :required true)
    #=(mojo/parameter
       ringServletName "ringServletName"
       :typename "java.lang.String"
       :required true)
    #=(mojo/parameter
       ringListenerClass "ringListenerClass"
       :typename "java.lang.String")
    #=(mojo/parameter
       ringHandler "ringHandler"
       :typename "java.lang.String"
       :required true)
    #=(mojo/parameter
       ringInit "ringInit"
       :typename "java.lang.String")
    #=(mojo/parameter
       ringDestroy "ringDestroy"
       :typename "java.lang.String")
    #=(mojo/parameter
       ringUrlPattern "ringUrlPattern"
       :typename "java.lang.String")
    ^{Parameter
      {:defaultValue "${project.build.directory}" :required true}}
    ^String
    buildDirectory
    ^{Parameter
      {:expression "${project}"
       :description "Project"}}
    project]
  (write-files {:servlet-class ringServletClass
                :servlet-name ringServletName
                :listener-class ringListenerClass
                :handler ringHandler
                :init ringInit
                :destroy ringDestroy
                :url-pattern ringUrlPattern
                :build-dir buildDirectory})
  ;; Ideally, we would like to point the war plugin at the generated web.xml
  ;; but I'm nt sure how to do that...
  (.addCompileSourceRoot
   project
   (gen-sources-dir buildDirectory))
  (.addResource
   project
   (doto (org.apache.maven.model.Resource.)
     (.setDirectory (resource-path buildDirectory))
     (.setTargetPath "WEB-INF"))))
