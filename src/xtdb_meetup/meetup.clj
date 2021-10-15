(ns xtdb-meetup.meetup
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [xtdb.api :as xt]))

; RESTful Meetup API v3
(def host "https://api.meetup.com")

(defn dataset-reader
  "Retrieve all group meetup events matching comma-delimited status \"past,upcoming\""
  [{:keys [meetup-group status]}]
  (let [endpoint (str host (format "/%s/events" meetup-group))
        resp (http/get endpoint
                       {:query-params {"status" status}
                        :as           :stream})]
    (io/reader (:body resp))))

(defn remove-nil-values [m]
  (->> m (partition 2) (apply concat) (filter #(some? (second %))) (into {})))

(defn group-document
  [{:strs [id name who localized_location state country region lat lon
           timezone urlname join_mode created]}]
  (if id
    [::xt/put (remove-nil-values
                {:xt/id                           (keyword (str "meetup/group-" id))
                 :meetup/type                     :group
                 :meetup.group/id                 id
                 :meetup.group/name               name
                 :meetup.group/who                who
                 :meetup.group/localized-location localized_location
                 :meetup.group/state              state
                 :meetup.group/country            country
                 :meetup.group/region             region
                 :meetup.group/lat                lat
                 :meetup.group/lon                lon
                 :meetup-group/timezone           timezone
                 :meetup.group/urlname            urlname
                 :meetup.group/join_mode          join_mode
                 :meetup.group/created            created})]))

(defn venue-document
  [{:strs [id name localized_country_name repinned
           address_1 city state zip country lat lon]}]
  (if id
    [::xt/put (remove-nil-values
                {:xt/id                               (keyword (str "meetup/venue-" id))
                 :meetup/type                         :venue
                 :meetup.venue/address-1              address_1
                 :meetup.venue/city                   city
                 :meetup.venue/country                country
                 :meetup.venue/id                     id
                 :meetup.venue/lat                    lat
                 :meetup.venue/localized-country-name localized_country_name
                 :meetup.venue/lon                    lon
                 :meetup.venue/name                   name
                 :meetup.venue/repinned               repinned
                 :meetup.venue/state                  state
                 :meetup.venue/zip                    zip})]))

(defn event-document
  [{:strs [group id name description
           time local_date local_time utc_offset created updated
           status link is_online_event venue yes_rsvp_count waitlist_count
           why visibility member_pay_fee date_in_series_pattern]}]
  (->> (list
         (group-document group)
         (venue-document venue)
         [::xt/put (remove-nil-values
                     {:xt/id                          (keyword (str "meetup/event-" id))
                      :meetup/type                    :event
                      :meetup.group/id                (and group (keyword (str "meetup/group-" (group "id"))))
                      :meetup.event/created         created
                      :meetup.event/date_in_series_pattern date_in_series_pattern
                      :meetup.event/description     description
                      :meetup.event/id              id
                      :meetup.event/is_online_event is_online_event
                      :meetup.event/link            link
                      :meetup.event/local_date      local_date
                      :meetup.event/local_time      local_time
                      :meetup.event/member_pay_fee  member_pay_fee
                      :meetup.event/name            name
                      :meetup.event/status          status
                      :meetup.event/time            time
                      :meetup.event/updated         updated
                      :meetup.event/utc_offset      utc_offset
                      :meetup.event/visibility      visibility
                      :meetup.event/waitlist_count  waitlist_count
                      :meetup.event/why             why
                      :meetup.event/yes_rsvp_count  yes_rsvp_count
                      :meetup.venue/id                (and venue (keyword (str "meetup/venue-" (venue "id"))))})])
       (remove nil?) (into [])))

(defn start-xtdb!
  []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store,
                        :db-dir      (io/file dir),
                        :sync?       true}})]
    (xt/start-node
      {:xtdb/tx-log         (kv-store "data/dev/tx-log"),
       :xtdb/document-store (kv-store "data/dev/document-store"),
       :xtdb/index-store    (kv-store "data/dev/index-store")})))

(def xtdb-node (start-xtdb!))

(defn stop-xtdb! []
  (.close xtdb-node))

(defn load-group-data [group]
  (->> (dataset-reader {:meetup-group group :status "past,upcoming"})
       (json/read-value )
       (sort-by :time #(compare %2 %1))
       (map event-document)
       (apply concat)
       (into [])
       (xt/submit-tx xtdb-node)))

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
          '{:find  [(pull ?venue [:meetup.venue/id :meetup.venue/name
                                  {(:meetup.venue/_id {:as :events, :into #{}})
                                   [:meetup.event/local_date :meetup.event/name :meetup.event/id]}])]
            :where [[?venue :meetup.venue/id]]})))


(def ny-groups #{
                 "Clojure-nyc"
                 "LispNYC"
                 "New-York-Emacs-Meetup"
                 "OWASP-New-York-City-Chapter"
                 "Papers-We-Love"
                 "TensorFlow-New-York"
                 })

(comment

  (start-xtdb!)
  (stop-xtdb!)


  (load-group-data "Papers-We-Love")
  (map load-group-data ny-groups)

  (group-venues "LispNYC")
  (venue-events)


  (xt/q (xt/db xtdb-node)
        '{:find  [name]
          :where [[e :meetup/type :event]
                  [e :meetup.event/name name]]})

  (xt/q (xt/db xtdb-node)
        '{:find  [name]
          :where [[e :meetup/type :venue]
                  [e :meetup.venue/name name]]})

  (xt/q (xt/db xtdb-node)
        '{:find  [(pull ?venue [:meetup.venue/name
                                :meetup.venue/id
                                :meetup.venue/urlname])]
          :where [[?venue :meetup.venue/urlname "Papers-We-Love"]]})

  (xt/q (xt/db xtdb-node)
        '{:find  [name urlname id]
          :where [[e :meetup/type :group]
                  [e :meetup.group/name name]
                  [e :meetup.group/urlname urlname]
                  [e :meetup.group/id id]]})

  (->> (dataset-reader {:meetup-group "LispNYC" :status "past,upcoming"})
       (json/read-value)
       (first)
       (spit "test/data/event.edn"))

  (->> (dataset-reader {:meetup-group "LispNYC" :status "past,upcoming"})
       (json/read-value)
       (sort-by :time #(compare %2 %1))
       #_(filter #(= (get (% "venue") "id") 1446724))
       #_(take 5)
       (map event-document)
       (apply concat)
       (into [])
       (xt/submit-tx xtdb-node))

,)