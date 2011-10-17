(ns zi.ring-genfiles
  "Generate files necessary to build a war using exported properties of lein-ring plugin."
  (:require
   [zi.mojo :as mojo]
   [zi.core :as core]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.contrib.prxml :as prxml])
  (:import
   java.io.File
   [clojure.maven.annotations
    Goal RequiresDependencyResolution Parameter Component]))


;;  Adapted from the lein-ring plugin : https://github.com/weavejester/lein-ring/
(defn generate-listener
  [listener-class ring-init ring-destroy]
  (let [  init-sym    (and ring-init (read-string ring-init))
          destroy-sym (and ring-destroy (read-string ring-destroy))
          init-ns     (and init-sym    (symbol (namespace init-sym)))
          destroy-ns  (and destroy-sym (symbol (namespace destroy-sym)))
          listener-ns  (symbol (read-string listener-class))]
    `(do (ns ~listener-ns
                 (:require ~@(set (remove nil? [init-ns destroy-ns])))
                 (:gen-class :implements [javax.servlet.ServletContextListener]))
               ~(let [servlet-context-event (gensym)]
                  `(do
                     (defn ~'-contextInitialized [this# ~servlet-context-event]
                       ~(if init-sym
                          `(~init-sym)))
                     (defn ~'-contextDestroyed [this# ~servlet-context-event]
                       ~(if destroy-sym
                          `(~destroy-sym))))))))

;;  Adapted from the lein-ring plugin : https://github.com/weavejester/lein-ring/
(defn generate-handler [handler-sym]
  `(fn [request#]
      (~handler-sym
        (assoc request#
          :path-info (.getPathInfo (:servlet-request request#))
          :context   (.getContextPath (:servlet-request request#)))))
  handler-sym)

;;  Adapted from the lein-ring plugin : https://github.com/weavejester/lein-ring/
(defn gen-servlet
  [servlet-class handler]
  (let [handler-sym (read-string handler)
        handler-ns  (symbol (namespace handler-sym))
        servlet-ns  (symbol (read-string servlet-class))]

      `(do (ns ~servlet-ns
             (:require ring.util.servlet ~handler-ns)
             (:gen-class :extends javax.servlet.http.HttpServlet))
           (ring.util.servlet/defservice
             ~(generate-handler handler-sym)))))

(def url-pattern #(or % "/*"))

;;  Adapted from the lein-ring plugin : https://github.com/weavejester/lein-ring/
(defn gen-webxml
 "Generate the webxml and return its string representation"
 [params]
 (with-out-str
   (prxml/prxml
     [:web-app
       [:listener
         [:listener-class (:listener-class params)]]
       [:servlet
         [:servlet-name  (:handler params)]
         [:servlet-class (:servlet-class params)]]
       [:servlet-mapping
         [:servlet-name (:handler params)]
         [:url-pattern (url-pattern (:url-pattern params))]]])))

(def webxml-path #(str % "/web.xml"))
(def gen-dir #(str % "/gen-src/"))
(def gen-sources-dir #(str (gen-dir %) "clj/"))

(defn write-webxml
  "Write the web.xml file"
  [params]
  (let [webxml-filename  (webxml-path (:build-dir params))]
    (spit webxml-filename (gen-webxml params))))

(defn ns-to-filename
  "Convert a namespace to a path to a clojure source file (.clj)"
  [a-ns]
  (-> (str a-ns)
                 (string/replace "-" "_")
                 (string/replace "." java.io.File/separator)
                 (str ".clj")))

(defn write-generated-source
  "Write generated source file into gen-sources directory"
  [class-ns class-body build-dir]
  (let [src-filename  (str (gen-sources-dir build-dir) (ns-to-filename class-ns))]
    (.mkdirs (.getParentFile (File. src-filename)))
    (spit src-filename (str class-body))))

(defn write-servlet-source
  "Write servlet source for the webapp according to handler and servlet-class"
  [{:keys [servlet-class handler build-dir]}]
  (write-generated-source servlet-class (gen-servlet servlet-class handler) build-dir))
    
(defn write-listener-source
  "Write listener (init/destroy) source for the webapp"
  [{:keys [listener-class init destroy build-dir]}]
  (write-generated-source listener-class (generate-listener listener-class init destroy) build-dir))

(defn process-genfiles
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
      :typename "java.lang.String"
      :required true)
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
       buildDirectory]
  (process-genfiles {
                    :servlet-class ringServletClass
                    :servlet-name ringServletName
                    :listener-class ringListenerClass
                    :handler ringHandler
                    :init ringInit
                    :destroy ringDestroy
                    :url-pattern ringUrlPattern
                    :build-dir buildDirectory}))
                    