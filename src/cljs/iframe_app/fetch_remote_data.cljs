(ns iframe-app.fetch-remote-data
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require
    [om.core :as om :include-macros true]
    [om-tools.dom :as dom :include-macros true]
    [om-tools.core :refer-macros [defcomponent]]
    [sablono.core :as html :refer-macros [html]]
    [clojure.set :refer [difference]]
    [ankha.core :as ankha]
    [iframe-app.utils :refer [active-conditions form->form-kw]]
    [cljs.core.async :refer [put! chan <!]]))

(defonce parent-app (.init js/ZAFClient))

(def dummy-ticket-forms [{:name "Default Ticket Form", :id 22786, :ticket-fields '({:name "Subject", :id 23640438, :type "subject", :possible-values [{:name "Any", :value "any"}]} {:name "Description", :id 23640448, :type "description", :possible-values [{:name "Any", :value "any"}]} {:name "Status", :id 23640458, :type "status", :possible-values [{:name "Any", :value "any"}]} {:name "Type", :id 23640468, :type "tickettype", :possible-values [{:name "Question", :value "question"} {:name "Incident", :value "incident"} {:name "Problem", :value "problem"} {:name "Task", :value "task"}]} {:name "Priority", :id 23640478, :type "priority", :possible-values [{:name "Low", :value "low"} {:name "Normal", :value "normal"} {:name "High", :value "high"} {:name "Urgent", :value "urgent"}]} {:name "Group", :id 23640488, :type "group", :possible-values ({:name "Support", :value 21407398})} {:name "Assignee", :id 23640498, :type "assignee", :possible-values [{:name "Any", :value "any"}]} {:name "Linked Data", :id 24275528, :type "text", :possible-values [{:name "Any", :value "any"}]} {:name "Thing to show", :id 24154856, :type "tagger", :possible-values [{:id 62051666, :name "what", :raw_name "what", :value "yep"}]})} {:name "Default Ticket Form", :id 43487, :ticket-fields '({:name "Subject", :id 23640438, :type "subject", :possible-values [{:name "Any", :value "any"}]} {:name "Description", :id 23640448, :type "description", :possible-values [{:name "Any", :value "any"}]} {:name "Status", :id 23640458, :type "status", :possible-values [{:name "Any", :value "any"}]} {:name "Type", :id 23640468, :type "tickettype", :possible-values [{:name "Question", :value "question"} {:name "Incident", :value "incident"} {:name "Problem", :value "problem"} {:name "Task", :value "task"}]} {:name "Priority", :id 23640478, :type "priority", :possible-values [{:name "Low", :value "low"} {:name "Normal", :value "normal"} {:name "High", :value "high"} {:name "Urgent", :value "urgent"}]} {:name "Group", :id 23640488, :type "group", :possible-values ({:name "Support", :value 21407398})} {:name "Assignee", :id 23640498, :type "assignee", :possible-values [{:name "Any", :value "any"}]})} {:name "Default Ticket Form", :id 127468, :ticket-fields '({:name "Subject", :id 23640438, :type "subject", :possible-values [{:name "Any", :value "any"}]} {:name "Description", :id 23640448, :type "description", :possible-values [{:name "Any", :value "any"}]} {:name "Status", :id 23640458, :type "status", :possible-values [{:name "Any", :value "any"}]} {:name "Type", :id 23640468, :type "tickettype", :possible-values [{:name "Question", :value "question"} {:name "Incident", :value "incident"} {:name "Problem", :value "problem"} {:name "Task", :value "task"}]} {:name "Priority", :id 23640478, :type "priority", :possible-values [{:name "Low", :value "low"} {:name "Normal", :value "normal"} {:name "High", :value "high"} {:name "Urgent", :value "urgent"}]} {:name "Group", :id 23640488, :type "group", :possible-values ({:name "Support", :value 21407398})} {:name "Assignee", :id 23640498, :type "assignee", :possible-values [{:name "Any", :value "any"}]})}])

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
   :possible-values (possible-values-for-field ticket-field groups)
   :show-to-end-user (:visible_in_portal ticket-field)})

(defn get-data-from-response [response]
  (-> response
      (js->clj :keywordize-keys true)
      vals
      first))


(def data-type->url {:groups        "/api/v2/groups.json"
                     :ticket-fields "/api/v2/ticket_fields.json"
                     :ticket-forms  "/api/v2/ticket_forms.json"})

(defn request-data [data-type fetch-data-chan]
  (let [promise (.request parent-app
                          (data-type->url data-type))]
    (.then promise
           (fn [response]
             (put! fetch-data-chan response)))))

(def field-types-to-ignore #{"assignee" "subject" "description" "ccs" "ticketsharing" "status"})

(defn valid-ticket-field [{:keys [active type]}]
  (and active
       (not (field-types-to-ignore type))))

(defn fetch-ticket-forms []
  (let [ticket-forms-chan (chan)]
    (go
      (when-not parent-app
        ; when not inside zendesk
        (put! ticket-forms-chan dummy-ticket-forms))

      (when parent-app
        (let [fetch-data-chan (chan)
              _ (request-data :groups fetch-data-chan)
              groups (get-data-from-response (<! fetch-data-chan))

              _ (request-data :ticket-fields fetch-data-chan)
              ticket-fields (->> (<! fetch-data-chan)
                                 (get-data-from-response)
                                 (filter valid-ticket-field)
                                 (map (partial process-ticket-field groups)))

              _ (request-data :ticket-forms fetch-data-chan)
              ticket-forms (map (partial process-ticket-form ticket-fields)
                                (get-data-from-response (<! fetch-data-chan)))]

          (put! ticket-forms-chan ticket-forms))))
    ticket-forms-chan))

