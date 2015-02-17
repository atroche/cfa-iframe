(ns iframe-app.fetch-data
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [iframe-app.selectors :refer [slave-fields-selector value-selector
                                  master-field-selector user-type-selector
                                  ticket-form-selector]]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [ankha.core :as ankha]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [cljs.core.async :refer [put! chan <!]]))

(defonce parent-app (.init js/ZAFClient))


(defn possible-values-for-field [ticket-field groups]
  (case (:type ticket-field)
    ("tickettype" "priority") (:system_field_options ticket-field)
    "tagger" (:custom_field_options ticket-field)
    "checkbox" [{:name "Yes" :value "yes"}, {:name "No" :value "no"}]
    "group" (for [group groups]
              {:name  (:name group)
               :value (:id group)})
    [{:name "Any" :value "any"}]))

(defn process-ticket-form [ticket-fields ticket-form]
  {:name          (:name ticket-form)
   :id            (:id ticket-form)
   :ticket-fields (filter (fn [ticket-field]
                            ((set (:ticket_field_ids ticket-form)) (:id ticket-field)))
                          ticket-fields)})

(defn process-ticket-field [groups ticket-field]
  {:name            (:title ticket-field)
   :id              (:id ticket-field)
   :type            (:type ticket-field)
   :possible-values (possible-values-for-field ticket-field groups)})

(defn get-data-from-response [response]
  (-> response
      (js->clj :keywordize-keys true)
      vals
      first))


(def data-type->url {:groups        "/api/v2/groups.json"
                     :ticket-fields "/api/v2/ticket_fields.json"
                     :ticket-forms  "/api/v2/ticket_forms.json"})

(defn request-data [data-type fetch-data-chan]
  (.request parent-app
            (data-type->url data-type)
            (fn [response]
              (put! fetch-data-chan response))))


(defn fetch-ticket-forms [ticket-forms-chan]
  (go
    (let [fetch-data-chan (chan)
          _ (request-data :groups fetch-data-chan)
          groups (get-data-from-response (<! fetch-data-chan))

          _ (request-data :ticket-fields fetch-data-chan)
          ticket-fields (map (partial process-ticket-field groups)
                             (get-data-from-response (<! fetch-data-chan)))

          _ (request-data :ticket-forms fetch-data-chan)
          ticket-forms (map (partial process-ticket-form ticket-fields)
                            (get-data-from-response (<! fetch-data-chan)))]

      (put! ticket-forms-chan ticket-forms))))