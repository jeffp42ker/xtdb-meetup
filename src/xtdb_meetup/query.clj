(ns xtdb-meetup.query
  (:require
   [xtdb.api :as xt]
   [xtdb-meetup.core :refer [xtdb-node]]
   [xtdb-meetup.util :as util]))

(defn group-venues [group-urlname]
  (let [headings [:urlname :max-date :min-date :meeting-count :coord :name-address]
        f-headers (fn [m] (into {} (mapv hash-map headings m)))]
    (->> (map (fn [[_ group-urlname max-date min-date count _ name address-1 lat lon]]
                [group-urlname
                 max-date min-date count
                 (str lat "," lon)
                 (str name ", " address-1)])
              (xt/q (xt/db xtdb-node)
                    '{:find [#_(pull ?event [*])
                             ?group group-urlname

                             (max local_date)
                             (min local_date)
                             (count ?event)
                             ?venue venue-name address-1
                             lat lon
                             #_(pull ?venue [*])]
                      :in [group-urlname]
                      :where [[?group :meetup.group/urlname group-urlname]
                              [?event :meetup.group/id ?group]
                              [?event :meetup.event/name event-name]
                              [?event :meetup.venue/id ?venue]
                              [?venue :meetup.venue/name venue-name]
                              [?venue :meetup.venue/address-1 address-1]
                              #_[?event :meetup.event/is_online_event false]
                              [?venue :meetup.venue/lat lat]
                              [?venue :meetup.venue/lon lon]
                              [?event :meetup.event/local_date local_date]]}
                    group-urlname))
         (sort-by second #(compare %2 %1))
         (mapv f-headers)
         (sort-by :meeting-count #(compare %2 %1)))))

(defn venue-events
  "Events nested in venues, reverse direction using pull syntax"
  []
  (->>
   (xt/q (xt/db xtdb-node)
         '{:find  [?group-name (pull ?venue [:xt/id :meetup.venue/name
                                             {(:meetup.venue/_id {:as :events, :into #{}})
                                              [:meetup.event/time :meetup.event/name :meetup.event/id]}])]
           :where [[?event :meetup.venue/id ?venue]
                   [?event :meetup.group/id ?group]
                   [?group :meetup.group/name ?group-name]]})))

(defn events-at-venue
  [name]
  (->>
   (xt/q (xt/db xtdb-node)
         '{:find  [date venue-name venue-address event-name]
           :in    [venue-name]
           :where [[?event :meetup.event/name event-name]
                   [?event :meetup.event/local_date date]
                   [?event :meetup.venue/id ?venue]
                   [?venue :meetup.venue/name venue-name]
                   [?venue :meetup.venue/address-1 venue-address]]}
         name)
   (sort-by first)))

(defn all-venues []
  (xt/q (xt/db xtdb-node)
        '{:find  [group-name (pull ?venue [*])]
          :where [[?event :meetup.group/id ?group]
                  [?event :meetup.venue/id ?venue]
                  [?group :meetup.group/name group-name]
                  [?venue :meetup.venue/name]]}))

(defn venues-without-a-name []
  (xt/q (xt/db xtdb-node)
        '{:find  [group-name (pull ?venue [*])]
          :where [[?event :meetup.group/id ?group]
                  [?event :meetup.venue/id ?venue]
                  [?group :meetup.group/name group-name]
                  [?venue :meetup.venue/name venue-name]
                  [?venue :meetup.venue/address-1 venue-name]]}))

(defn venue-map [name]
  (ffirst (xt/q (xt/db xtdb-node)
                '{:find  [(pull ?venue [*])]
                  :in    [name]
                  :where [[?venue :meetup.venue/name name]]}
                name)))

(defn venues-with-attributes-having-duplicate-values []
  (filter (fn [m] (= (:meetup.venue/name m)
                     (:meetup.venue/address-1 m))) (all-venues)))

(comment

  (venues-without-a-name)


  (->> (venue-map "Jane Street") tap>)

  (count (all-venues))


  (->> (group-venues "nyhackr") tap>)
  ;; => true
;;BUIDL
  (->> (group-venues "flatironschool")
       (sort-by :max-date #(compare %2 %1))
       tap>)

  (->> (group-venues "LispNYC")
       (sort-by :max-date #(compare %2 %1))
       (take 5)
       tap>)

  (tap> (venue-events))

  (->> (all-venues) (take 4) #_tap>)

  (tap> (venues-with-attributes-having-duplicate-values))

  (events-at-venue "Shareablee")

  (filter (fn [m] (= (:meetup.venue/name m)
                     (:meetup.venue/address-1 m))) (all-venues))

  (tap> (xt/q (xt/db xtdb-node) 6
              '{:find  [(pull ?venue [*])]
                :in [name]
                :where [[?venue :meetup.venue/name name]]} "Pierre's Roofdeck"))

  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?event [*]) group-name]
          :where [[?event :xt/id :meetup/event-210221942]
                  [?event :meetup.group/id ?group]
                  [?group :meetup.group/name group-name]]})

  (tap> (xt/q (xt/db xtdb-node)
              '{:find  [(pull ?group [*])]
                :where [[?group :meetup.group/name]]}))




  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?venue [:meetup.venue/name
                                :meetup.venue/id
                                :meetup.venue/urlname])]
          :in [venue-id]
          :where [[?venue :meetup/type :venue]
                  [?venue :meetup.venue/id venue-id]]}
        25186644)


  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?venue [:meetup.venue/name
                                :meetup.venue/id
                                :meetup.venue/urlname])]
          :where [[?venue :xt/id :meetup/venue-25186644]]})


  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?venue [:meetup.venue/name
                                :meetup.venue/id
                                :meetup.venue/urlname])]
          :where [[?venue :xt/id :meetup/venue-25186644]]})



  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?event [:meetup.event/name
                                :meetup.event/id
                                :meetup.event/urlname
                                :meetup.venue/id])]
          :where [[?event :meetup.venue/id ?venue]
                  [?venue :xt/id :meetup/venue-25186644]
                  #_[?venue :meetup.venue/name venue-name]]})

  (xt/q (xt/db xtdb-node)
        '{:find  [name urlname id]
          :where [[e :meetup/type :group]
                  [e :meetup.group/name name]
                  [e :meetup.group/urlname urlname]
                  [e :meetup.group/id id]]})

  )
  

