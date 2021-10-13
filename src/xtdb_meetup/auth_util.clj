(ns xtdb-meetup.auth-util
  (:import (java.io File)))

(defn read-meetup-auth [^File file]
  (map (json/read-value (slurp file)) ["username" "password"]))

(def ^:dynamic *meetup-auth*
  (some-> (System/getenv "MEETUP_AUTH_FILE") io/file read-meetup-auth))
