(ns zi.log
  "Logging utilities for plugin development."
  (:require
    [clojure.string :as string]))

(def ^{:dynamic true} *plexus-log* nil)

(defmacro log-level? [level]
  (let [log-level-fn (str "is" (string/capitalize (name level)) "Enabled")]
    `(and
       (not (nil? *plexus-log*))
       (. *plexus-log* ~(symbol log-level-fn)))))

(defmacro log
  ([mesg level]
   `(when (log-level? ~level) (. *plexus-log* ~(symbol (name level)) ~mesg)))
  ([e mesg level]
   `(when (log-level? ~level) (. *plexus-log* ~(symbol (name level)) ~mesg ~e))))

(defn debug? [] (log-level? :debug))

(defmacro debug
  ([mesg] `(log ~mesg :debug))
  ([e mesg] `(log ~e ~mesg :debug)))

(defmacro debugf [mesg & args]
  `(debug (format ~mesg ~@args)))

(defn info? [] (log-level? :info))

(defmacro info
  ([mesg] `(log ~mesg :info))
  ([e mesg] `(log ~e ~mesg :info)))

(defmacro infof [mesg & args]
  `(info (format ~mesg ~@args)))

(defn warn? [] (log-level? :warn))

(defmacro warn
  ([mesg] `(log ~mesg :warn))
  ([e mesg] `(log ~e ~mesg :warn)))

(defmacro warnf [mesg & args]
  `(warn (format ~mesg ~@args)))

(defn error? [] (log-level? :error))

(defmacro error
  ([mesg] `(log ~mesg :error))
  ([e mesg] `(log ~e ~mesg :error)))

(defmacro errorf [mesg & args]
  `(error (format ~mesg ~@args)))

(defn fatal-error? [] (log-level? :fatal-error))

(defmacro fatal-error
  ([mesg] `(log ~mesg :fatal-error))
  ([e mesg] `(log ~e ~mesg :fatal-error)))

(defmacro fatal-errorf [mesg & args]
  `(fatal-error (format ~mesg ~@args)))

(defmacro with-log [log & body]
  `(binding [*plexus-log* ~log]
    ~@body))
