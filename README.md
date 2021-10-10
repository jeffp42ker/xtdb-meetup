# XTDB Meetup

A small XTDB utility to extract the basic Events dataset for a Meetup group and turn it into XTDB transaction operations.

The utility pulls down all past and upcoming events data for a Meetup group as a list of Json maps by making a single request to the RESTful Meetup API v3.


The [XTDB Kaggle](https://github.com/xtdb/xtdb-kaggle) utility was used as a guide in modeling the Meetup data for XTDB.

A Meetup Event document has nested Group data and usually Venue data as well. Separate documents for Event, Group and Venue are prepared for each Event as XTDB puts. While it is inefficient to submit many redundant group and venue puts it relieves us of determining which are new since the puts are idempotent.

The documents are intended to be a system-of-record from Meetup so no transformation of values is performed apart from removing keys with `nil` values. Clean-up and transformations can be added in additional keys and values.

- Document types: `:meetup/type` `#{:event :venue :group}`
- Document ids are in the form `:xt/id` `[:meetup/event-<id> :meetup/venue-<id> :meetup/group-<id>]`
- Key prefixes: `#{ :meetup.event/` `:meetup.venue/` `:meetup.group/ }`.
```clojure
(defn event-document
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
```



```clojure
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
```
```clojure
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
```