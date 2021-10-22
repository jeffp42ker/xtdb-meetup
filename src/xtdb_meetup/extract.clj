(ns xtdb-meetup.extract
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [xtdb.api :as xt]
            [xtdb-meetup.core :as xtdb-meetup]))

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
  [{:strs [created name id join_mode lat lon urlname who
           localized_location state country region timezone]}]
  (if id
    [::xt/put (remove-nil-values
                {:xt/id                           (keyword (str "meetup/group-" id))
                 :meetup/type                     :group
                 :meetup.group/created            created
                 :meetup.group/name               name
                 :meetup.group/id                 id
                 :meetup.group/join_mode          join_mode
                 :meetup.group/lat                lat
                 :meetup.group/lon                lon
                 :meetup.group/urlname            urlname
                 :meetup.group/who                who
                 :meetup.group/localized-location localized_location
                 :meetup.group/state              state
                 :meetup.group/country            country
                 :meetup.group/region             region
                 :meetup-group/timezone           timezone})]))

(defn venue-document
  [{:strs [id name lat lon repinned address_1 city
           country localized_country_name zip state]}]
  (if id
    [::xt/put (remove-nil-values
                {:xt/id                               (keyword (str "meetup/venue-" id))
                 :meetup/type                         :venue
                 :meetup.venue/id                     id
                 :meetup.venue/name                   name
                 :meetup.venue/lat                    lat
                 :meetup.venue/lon                    lon
                 :meetup.venue/repinned               repinned
                 :meetup.venue/address-1              address_1
                 :meetup.venue/city                   city
                 :meetup.venue/country                country
                 :meetup.venue/localized-country-name localized_country_name
                 :meetup.venue/zip                    zip
                 :meetup.venue/state                  state})]))

(defn event-document
  [{:strs [created duration id name date_in_series_pattern status time local_date local_time
           updated utc_offset waitlist_count yes_rsvp_count venue is_online_event
           group link description how_to_find_us visibility member_pay_fee why]}]
  (->> (list
         (group-document group)
         (venue-document venue)
         [::xt/put (remove-nil-values
                     {:xt/id                        (keyword (str "meetup/event-" id))
                      :meetup/type                  :event
                      :meetup.event/created         created
                      :meetup.event/duration        duration
                      :meetup.event/id              id
                      :meetup.event/name            name
                      :meetup.event/date_in_series_pattern date_in_series_pattern
                      :meetup.event/status          status
                      :meetup.event/time            time
                      :meetup.event/local_date      local_date
                      :meetup.event/local_time      local_time
                      :meetup.event/updated         updated
                      :meetup.event/utc_offset      utc_offset
                      :meetup.event/waitlist_count  waitlist_count
                      :meetup.event/yes_rsvp_count  yes_rsvp_count
                      :meetup.venue/id              (if venue (keyword (str "meetup/venue-" (venue "id"))))
                      :meetup.event/is_online_event is_online_event
                      :meetup.group/id              (if group (keyword (str "meetup/group-" (group "id"))))
                      :meetup.event/link            link
                      :meetup.event/description     description
                      :meetup.event/how_to_find_us  how_to_find_us
                      :meetup.event/visibility      visibility
                      :meetup.event/member_pay_fee  member_pay_fee
                      :meetup.event/why             why})])
       (remove nil?) (into [])))

(declare xtdb-node)

(defn load-group-data [group]
  (->> (dataset-reader {:meetup-group group :status "past,upcoming"})
       (json/read-value )
       (sort-by :time #(compare %2 %1))
       (map event-document)
       (apply concat)
       (into [])
       (xt/submit-tx xtdb-node)))

(def ny-groups #{
                 "Clojure-nyc"
                 "LispNYC"
                 "New-York-Emacs-Meetup"
                 "OWASP-New-York-City-Chapter"
                 "Papers-We-Love"
                 "TensorFlow-New-York"
                 })

(comment

  (def xtdb-node (xtdb-meetup/start-xtdb!))
  (xtdb-meetup/stop-xtdb!)


  (load-group-data "Papers-We-Love")
  (map load-group-data ny-groups)

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