(ns iframe-app.journeys
  (:require-macros [cljs.core.typed :refer [ann]])
  (:require [cljs.core.typed]))


(defmulti transform :action-type)

(defmethod transform :select-master-field
  [action state]
  (assoc state :master-field (:value action)
               :field-value nil
               :slave-fields #{}))

(defmethod transform :select-field-value
  [action state]
  (assoc state :field-value (:value action)
               :slave-fields #{}))

(defrecord Action [action-type value])

(defn field-values-from-fields [fields]
  (flatten (map :possible-values fields)))

(def starting-state {:master-field nil
                     :field-value  nil
                     :slave-fields #{}})

(ann possible-selection-states [#{TicketField} -> #{SelectionState}])
(defn possible-selection-states [ticket-fields]
  {:post [(contains? % starting-state)
          (not-empty %)]}
  "Returns a set of possible states that {:master-field field :field-value value}
   could be in."
  (set
    (let [field-values (field-values-from-fields ticket-fields)]
      (for [field ticket-fields
            field-value field-values
            master-nil? [false true]
            value-nil? [false true]]
        {:master-field (if-not master-nil? field)
         :field-value  (if-not value-nil? field-value)
         :slave-fields #{}}))))

(defn categorize-state [{:keys [:master-field field-value] :as state}]
  (cond (= starting-state state) :start
        (and (not-empty master-field) (nil? field-value)) :on-master-field
        (every? not-empty [master-field field-value]) :on-field-value
        :else :error))


(defn next-actions-for-state [{:keys [master-field field-value] :as state} ticket-fields]
  {:post [(every? (fn [action] (= Action (type action))) %)]}
  (let [state-category (categorize-state state)
        other-fields (remove #(= % master-field) ticket-fields)
        available-field-values (:possible-values master-field)
        field-values (field-values-from-fields ticket-fields)
        other-field-values (remove #(= % field-value) available-field-values)]
    (when state-category
      (let [values->actions (fn [values action-type]
                             (for [value values]
                               (->Action action-type value)))
            next-actions (case state-category
                           :start (values->actions ticket-fields :select-master-field)
                           :on-master-field (concat (values->actions other-fields :select-master-field)
                                                    (values->actions available-field-values :select-field-value))
                           :on-field-value (concat (values->actions other-fields :select-master-field)
                                                   (values->actions other-field-values :select-field-value))
                           '())]
        next-actions))))

(defn choose-action [actions]
  (rand-nth actions))

(defn generate-user-journey [ticket-fields]
  {:post [(every? (fn [action] (= Action (type action))) %)]}
  (loop [state starting-state
         path []]
    (let [possible-actions (next-actions-for-state state ticket-fields)]
      (if (or (> (count path) 200) (empty? possible-actions))
        path
        (let [action (choose-action possible-actions)]
          (recur (transform action state)
                 (conj path action)))))))

