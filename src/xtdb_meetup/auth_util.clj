(ns xtdb-meetup.auth-util
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io])
  (:import (java.io File)))

(defn read-meetup-auth [^File file]
  (mapv (json/read-str (slurp file)) ["username" "password"]))

(def ^:dynamic *meetup-auth*
    (some-> (System/getenv "MEETUP_AUTH_FILE") io/file read-meetup-auth))