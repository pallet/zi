(ns zi.log
  "Logging utilities for plugin development."
  (:import
    org.codehaus.plexus.logging.Logger))

; "A non-functional logger that acts as a stand-in until the var *plexus-log*
; is rebound."
(deftype MockLogger []
  Logger
  (debug [this _])
  (debug [this _ _])
  (isDebugEnabled [this] false)
  (info [this _])
  (info [this _ _])
  (isInfoEnabled [this] false)
  (warn [this _])
  (warn [this _ _])
  (isWarnEnabled [this] false)
  (error [this _])
  (error [this _ _])
  (isErrorEnabled [this] false)
  (fatalError [this _])
  (fatalError [this _ _])
  (isFatalErrorEnabled [this] false)
  (getThreshold [this] Logger/LEVEL_DISABLED)
  (setThreshold [this _])
  (getChildLogger [this _] (MockLogger.))
  (getName [this] ""))

(def *plexus-log* (MockLogger.))

(defn debug? [] (.isDebugEnabled *plexus-log*))
(defn debug
  ([mesg] (if (debug?) (.debug *plexus-log* mesg)))
  ([mesg e] (if (debug?) (.debug *plexus-log* mesg e))))

(defn info? [] (.isInfoEnabled *plexus-log*))
(defn info
  ([mesg] (if (info?) (.info *plexus-log* mesg)))
  ([mesg e] (if (info?) (.info *plexus-log* mesg e))))

(defn warn? [] (.isWarnEnabled *plexus-log*))
(defn warn
  ([mesg] (if (warn?) (.warn *plexus-log* mesg)))
  ([mesg e] (if (warn?) (.warn *plexus-log* mesg e))))

(defn error? [] (.isErrorEnabled *plexus-log*))
(defn error
  ([mesg] (if (error?) (.error *plexus-log* mesg)))
  ([mesg e] (if (error?) (.error *plexus-log* mesg e))))

(defn fatal-error? [] (.isFatalErrorEnabled *plexus-log*))
(defn fatal-error
  ([mesg] (if (fatal-error?) (.fatal-error *plexus-log* mesg)))
  ([mesg e] (if (fatal-error?) (.fatal-error *plexus-log* mesg e))))

(defmacro with-log [log & body]
  `(binding [*plexus-log* ~log]
    ~@body))

