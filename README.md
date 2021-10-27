# XTDB Meetup

A small XTDB utility to extract the basic Events dataset for a Meetup group and turn it into XTDB transaction operations.

The utility pulls down all past and upcoming events data for a Meetup group as a list of Json maps by making a single RESTful request to the Meetup API v3.

The [XTDB Kaggle](https://github.com/xtdb/xtdb-kaggle) utility was used as a guide in modeling the Meetup data for XTDB.

A Meetup Event document has nested Group data and usually Venue data as well. Each event's data is normalized into separate XTDB document puts for Event, Group and Venue. The puts are [idempotent](https://en.wikipedia.org/wiki/Idempotence) so the redundant puts for Group and Venue documents with each Event put does not duplicate data in the database.

These Meetup-sourced document attributes are intended as a system-of-record so no value transformation is performed apart from removing keys with `nil` values. Clean-up and transformations can be added as additional keys and values.

- Document types: `{:meetup/type #{ :event :venue :group }}`
- Document ids: `{:xt/id #{ :meetup/event-<id> :meetup/venue-<id> :meetup/group-<id> }`
- Meetup key prefixes: `#{ :meetup.event/<key> :meetup.venue/<key> :meetup.group/<key> }`.

Meetup API: `https://api.meetup.com/<GROUP_URLNAME>/events?status=past,upcoming`

### Attributes mapped in XTDB documents

### :group

```clojure
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
```
### :venue
```clojure
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
```
### :event
```clojure
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
                      :meetup.venue/id              (and venue (keyword (str "meetup/venue-" (venue "id"))))
                      :meetup.event/is_online_event is_online_event
                      :meetup.group/id              (and group (keyword (str "meetup/group-" (group "id"))))
                      :meetup.event/link            link
                      :meetup.event/description     description
                      :meetup.event/how_to_find_us  how_to_find_us
                      :meetup.event/visibility      visibility
                      :meetup.event/member_pay_fee  member_pay_fee
                      :meetup.event/why             why})])
       (remove nil?) (into [])))
```