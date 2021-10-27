(ns xtdb-meetup.query
  (:require [xtdb.api :as xt]
            [xtdb-meetup.core :as xtdb-meetup]))

(declare xtdb-node)

(defn group-venues [group-name]
  (->>
    (xt/q
      (xt/db xtdb-node)
      '{:find  [(max ?date) (min ?date) (count ?date)
                ?venue ?venue-name ?group ?group-name]
        :in [?group-name]
        :where [[?event :xt/id ?event-id]
                [?event :meetup.event/local_date ?date]
                [?event :meetup.venue/id ?venue]
                [?event :meetup.group/id ?group]
                [?venue :meetup.venue/name ?venue-name]
                [?group :meetup.group/name ?group-name]]}
      group-name)
    (sort-by first)))

(defn venue-events
  "Events nested in venues, reverse direction using pull syntax"
  []
  (->>
    (xt/q (xt/db xtdb-node)
          '{:find  [?group-name (pull ?venue [:xt/id :meetup.venue/name
                                  {(:meetup.venue/_id {:as :events, :into #{}})
                                   [:meetup.event/time :meetup.event/name :meetup.event/id ]}])]
            :where [[?event :meetup.venue/id ?venue]
                    [?event :meetup.group/id ?group]
                    [?group :meetup.group/name ?group-name]]} )))

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
                    [?venue :meetup.venue/address-1 venue-address]
                    ]}
          name)
    (sort-by first)))

(defn all-venues []
  (apply concat (xt/q (xt/db xtdb-node)
                '{:find  [(pull ?venue [*])]
                  :where [[?venue :meetup.venue/name]]})))


(defn venue-map [name]
  (ffirst (xt/q (xt/db xtdb-node)
                '{:find  [(pull ?venue [*])]
                  :in    [name]
                  :where [[?venue :meetup.venue/name name]]}
                name)))

(def ny-groups #{
                 "Clojure-nyc"
                 "LispNYC"
                 "New-York-Emacs-Meetup"
                 "OWASP-New-York-City-Chapter"
                 "Papers-We-Love"
                 "TensorFlow-New-York"
                 })

(defn venues-with-attributes-having-duplicate-values []
  (filter (fn [m] (= (:meetup.venue/name m)
                     (:meetup.venue/address-1 m))) (all-venues)))

(comment
  (def xtdb-node (xtdb-meetup/start-xtdb!))
  (xtdb-meetup/stop-xtdb!)

  (group-venues "LispNYC")
  (venue-events)
  (venues-with-attributes-having-duplicate-values)
  (events-at-venue "Shareablee")
  (filter (fn [m] (= (:meetup.venue/name m)
                     (:meetup.venue/address-1 m))) (all-venues))
  (venue-map "Pierre's Roofdeck")


  :meetup/event-210221942

  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?venue [*])]
          :in [name]
          :where [[?venue :meetup.venue/name name]]} "Pierre's Roofdeck")


  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?event [*]) group-name]
          :where [[?event :xt/id :meetup/event-210221942]
                  [?event :meetup.group/id ?group]
                  [?group :meetup.group/name group-name]
                  ]})

  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?group [*])]
          :where [[?group :meetup.group/name]]})

  (xt/q (xt/db xtdb-node)
        '{:find [?event venue-name group-name event-name]
          :where [[?event :meetup/type :event]
                  [?event :meetup.venue/id ?venue]
                  [?event :meetup.group/id ?group]
                  [?event :meetup.event/name event-name]
                  [?venue :meetup.venue/name venue-name]
                  [?group :meetup.group/name group-name]]})

  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?venue [:meetup.venue/name
                                :meetup.venue/id
                                :meetup.venue/urlname])]
          :in [venue-id]
          :where [[?venue :meetup/type :venue]
                  [?venue :meetup.venue/id venue-id]
                  ]}
        25186644)

  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?venue [:meetup.venue/name
                                :meetup.venue/id
                                :meetup.venue/urlname])]
          :where [[?venue :xtdb/id :meetup/venue-25186644]]})

  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?venue [:meetup.venue/name
                                :meetup.venue/id
                                :meetup.venue/urlname])]
          :where [[?venue :meetup.venue/id :meetup/venue-25186644]]})


  (xt/q (xt/db xtdb-node)
      '{:find  [(pull ?event [:meetup.event/name
                              :meetup.event/id
                              :meetup.event/urlname
                              :meetup.venue/id])]
        :where [[?event :meetup.venue/id ?venue]
                [?venue :xdb/id :meetup/venue-25186644]
                #_[?venue :meetup.venue/name venue-name]]})
  :meetup/venue-26671289

  (xt/q (xt/db xtdb-node)
        '{:find  [name urlname id]
          :where [[e :meetup/type :group]
                  [e :meetup.group/name name]
                  [e :meetup.group/urlname urlname]
                  [e :meetup.group/id id]]})

,)