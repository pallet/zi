(ns zi.log
  "Logging utilities for plugin development."
  (:use
    [clojure.string :only (capitalize)]))

(def ^{:dynamic true} *plexus-log* nil)

(defmacro log-level? [level]
  (let [log-level-fn (str "is" (capitalize (name level)) "Enabled")]
    `(and
       (not (nil? *plexus-log*))
       (. *plexus-log* ~(symbol log-level-fn)))))

(defmacro log
  ([mesg level]
   `(when (log-level? ~level) (. *plexus-log* ~(symbol (name level)) ~mesg)))
  ([e mesg level]
   `(when (log-level? ~level) (. *plexus-log* ~(symbol (name level)) ~mesg ~e))))

(defmacro deflog [level]
  "Given a keyword defines a set of functions for that log level.  The
  functions are as follows, where <level> is the name string of the keyword:

  <level>? []
  Returns true/false as to whether that log level is enabled.

  <level>  [mesg] [e mesg]
  Logs a string message along with an optional exception.

  <level>f [mesg & args]
  Logs a format string along with a sequence of arguments."
  (let [log-fn (name level)
        query-fn (str log-fn "?")
        format-fn (str log-fn "f")]
    `(do
       (defn ~(symbol query-fn) [] (log-level? ~level))

       (defmacro ~(symbol log-fn)
         ([~'mesg] `(log ~~'mesg ~~level))
         ([~'e ~'mesg] `(log ~~'e ~~'mesg ~~level)))

       (defmacro ~(symbol format-fn) [~'fmesg & ~'args]
         `(log (format ~~'fmesg ~@~'args) ~~level)))))

(deflog :debug)

(deflog :info)

(deflog :warn)

(deflog :error)

(deflog :fatal-error)

(defmacro with-log [log & body]
  `(binding [*plexus-log* ~log]
     ~@body))
