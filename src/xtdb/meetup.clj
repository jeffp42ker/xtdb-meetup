(ns xtdb.meetup
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [xtdb.api :as xt])
  (:import (java.io File)
           (java.util.zip ZipInputStream)))

(defn read-meetup-auth [^File file]
  (map (json/read-value (slurp file)) ["username" "password"]))

(def ^:dynamic *meetup-auth*
  (some-> (System/getenv "MEETUP_AUTH_FILE") io/file read-meetup-auth))

(def host "https://api.meetup.com")

(defn dataset-reader
  "Retrieve group meetup events using deprecated V2 meetup api"
  [{:keys [meetup-group status]}]
  (let [endpoint (str host (format "/%s/events" meetup-group))
        resp (http/get endpoint
                       {:basic-auth   *meetup-auth*
                        :query-params {"status" status}
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

(defn meeting-document
  [{:strs [group id name description
           time local_date local_time utc_offset created updated
           status link is_online_event venue yes_rsvp_count waitlist_count
           why visibility member_pay_fee date_in_series_pattern]}]
  (->> (list
         (group-document group)
         (venue-document venue)
         [::xt/put (remove-nil-values
                     {:xt/id                          (keyword (str "meetup/meeting-" id))
                      :meetup/type                    :meeting
                      :meetup.group/id                (and group (keyword (str "meetup/group-" (group "id"))))
                      :meetup.meeting/created         created
                      :meetup.meeting/date_in_series_pattern date_in_series_pattern
                      :meetup.meeting/description     description
                      :meetup.meeting/id              id
                      :meetup.meeting/is_online_event is_online_event
                      :meetup.meeting/link            link
                      :meetup.meeting/local_date      local_date
                      :meetup.meeting/local_time      local_time
                      :meetup.meeting/member_pay_fee  member_pay_fee
                      :meetup.meeting/name            name
                      :meetup.meeting/status          status
                      :meetup.meeting/time            time
                      :meetup.meeting/updated         updated
                      :meetup.meeting/utc_offset      utc_offset
                      :meetup.meeting/visibility      visibility
                      :meetup.meeting/waitlist_count  waitlist_count
                      :meetup.meeting/why             why
                      :meetup.meeting/yes_rsvp_count  yes_rsvp_count
                      :meetup.venue/id                (and venue (keyword (str "meetup/venue-" (venue "id"))))})])
       (remove nil?) (into [])))

(def node (xt/start-node {}))



(comment

  (xt/q (xt/db node)
        '{:find [name]
          :where [[e :meetup/type :meeting]
                  [e :meetup.meeting/name name]]})

  (xt/q (xt/db node)
        '{:find [name]
          :where [[e :meetup/type :venue]
                  [e :meetup.venue/name name]]})

  (xt/q (xt/db node)
        '{:find [name urlname id]
          :where [[e :meetup/type :group]
                  [e :meetup.group/name name]
                  [e :meetup.group/urlname urlname]
                  [e :meetup.group/id id]]})

  (xt/q (xt/db node)
        '{:find  [name v]
          :where [[evt :meetup/type :meeting]
                  [ven :xt/id v]
                  [grp :xt/id g]
                  [evt :meetup.venue/id v]
                  [evt :meetup.group/id g]
                  [ven :meetup.venue/name name]
                  [grp :meetup.group/urlname "Clojure-nyc"]]})


  #{
    "Clojure-nyc"
    "LispNYC"
    "New-York-Emacs-Meetup"
    "OWASP-New-York-City-Chapter"
    "Papers-We-Love"
    "TensorFlow-New-York"
    }

  (->> (dataset-reader {:meetup-group "Clojure-nyc" :status "past,upcoming"})
       (json/read-value )
       (sort-by :time #(compare %2 %1))
       #_(filter #(= (get (% "venue") "id") 1446724))
       #_(take 5)
       (map meeting-document)
       (apply concat)
       (into [])
       (xt/submit-tx node))

  (group-document {"country" "us",
                   "created" 1291904633000,
                   "id" 1748515,
                   "join_mode" "open"
                   "lat" 40.75,
                   "localized_location" "New York, NY",
                   "lon" -73.98999786376953,
                   "name" "LispNYC",
                   "region" "en_US",
                   "state" "NY",
                   "timezone" "US/Eastern",
                   "urlname" "LispNYC",
                   "who" "Lispnycs",})

  (venue-document {"address_1" "380 Columbus Ave (and 78th)",
                   "city" "New York",
                   "country" "us",
                   "id" 1446724,
                   "lat" 40.78126525878906,
                   "localized_country_name" "USA",
                   "lon" -73.97598266601562,
                   "name" "P&G's Lounge",
                   "repinned" false,
                   "state" "NY",
                   "zip" "10024"})

  (meeting-document {"created"                1291907200000,
                     "date_in_series_pattern" false,
                     "description"            "<p>Help celebrate another year of Lisp! Our annual holiday party will be held in the back room of P&amp;G's Lounge, food will be provided.</p> "
                     "group"                  {"created"            1291904633000,
                                               "who"                "Lispnycs",
                                               "country"            "us",
                                               "timezone"           "US/Eastern",
                                               "id"                 1748515,
                                               "localized_location" "New York, NY",
                                               "name"               "LispNYC",
                                               "lon"                -73.98999786376953,
                                               "region"             "en_US",
                                               "lat"                40.75,
                                               "state"              "NY",
                                               "urlname"            "LispNYC",
                                               "join_mode"          "open"},
                     "id"                     "jvqzpynqbsb",
                     "is_online_event"        false,
                     "link"                   "https://www.meetup.com/LispNYC/events/jvqzpynqbsb/",
                     "local_date"             "2010-12-14",
                     "local_time"             "19:00",
                     "member_pay_fee"         false,
                     "name"                   "LispNYC Holiday Party - 2010",
                     "status"                 "past",
                     "time"                   1292371200000,
                     "updated"                1292375995000,
                     "utc_offset"             -18000000,
                     "venue"                  {"address_1"              "380 Columbus Ave (and 78th)",
                                               "country"                "us",
                                               "city"                   "New York",
                                               "repinned"               false,
                                               "id"                     1446724,
                                               "name"                   "P&G's Lounge",
                                               "localized_country_name" "USA",
                                               "lon"                    -73.97598266601562,
                                               "lat"                    40.78126525878906,
                                               "state"                  "NY",
                                               "zip"                    "10024"},
                     "visibility"             "public",
                     "waitlist_count"         0,
                     "yes_rsvp_count"         3,})

  ,)
