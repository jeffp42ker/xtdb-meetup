(ns xtdb-meetup.util
  (:require
   [clojure.set :as set]))

(defn keys-diff
  "Show differences in keys between two maps"
  [m1 m2]
  [{(set/difference (set (keys m1)) (set (keys m2)))
    (set/difference (set (keys m2)) (set (keys m1)))}])


(comment

  (keys-diff
   {"one" 1 "two" 2 "three" 3}
   {"two" 2 "three" 3 "four" 4})
  
  )