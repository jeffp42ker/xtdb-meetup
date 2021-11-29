(ns xtdb-meetup.dev
  (:require
   [portal.api :as p]
   [xtdb-meetup.core :refer [xtdb-node start-xtdb! stop-xtdb!]]))

(comment
  
  (p/open {:launcher :vscode})  
  (add-tap #'p/submit))