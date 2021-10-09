# XTDB Meetup

A small XTDB utility to extract the basic Events dataset for a Meetup group and turn it into XTDB transaction operations.

The utility pulls down all past and upcoming events data for a Meetup group as a list of Json maps by making a single request to the RESTful Meetup API v3.


The [XTDB Kaggle](https://github.com/xtdb/xtdb-kaggle) utility was used as a guide in modeling the Meetup data for XTDB.

A Meetup Event document has nested Group data and usually Venue data as well. Separate documents for Meeting, Group and Venue are prepared for each Event as XTDB puts. While it is inefficient to submit many redundant group and venue puts it relieves us of determining which are new since the puts are idempotent.

The documents are intended to be a system-of-record from Meetup so no transformation of values is performed apart from removing keys with `nil` values. CLean-up and transformations can be added in additional keys and values.

- Document types: `:meetup/type` `#{:meeting :venue :group}`
- Document ids are in the form `:xt/id` `[:meetup/meeting-<id> :meetup/venue-<id> :meetup/group-<id>]`
- Key prefixes: `#{ :meetup.meeting/` `:meetup.venue/` `:meetup.group/ }`.
```clojure
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